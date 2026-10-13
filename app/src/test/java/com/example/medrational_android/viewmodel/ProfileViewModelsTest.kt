package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.AuthMessageResponse
import com.example.medrational_android.data.model.AuthResponse
import com.example.medrational_android.data.model.ChangePasswordRequest
import com.example.medrational_android.data.model.ConfirmCodeRequest
import com.example.medrational_android.data.model.RequestEmailChangeRequest
import com.example.medrational_android.data.model.UpdateProfileRequest
import com.example.medrational_android.data.model.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ProfileViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)

    private val profile = UserProfile(
        id = 1L,
        email = "old@example.com",
        firstName = "Amina",
        lastName = "Ben Ali",
        fullName = "Amina Ben Ali",
        phoneNumber = "0612345678",
        role = "ROLE_USER",
        createdAt = "2026-10-01T09:00:00"
    )

    private val sent = Response.success(AuthMessageResponse("Verification code sent"))

    private fun <T> badRequest(error: String): Response<T> =
        Response.error(400, """{"status":400,"error":"$error"}""".toResponseBody())

    // --- Profile ---

    @Test
    fun `load shows the profile`() {
        coEvery { api.getMyProfile() } returns Response.success(profile)

        val viewModel = ProfileViewModel(api, tokenManager).apply { load() }

        assertEquals(ProfileUiState.Success(profile), viewModel.uiState.value)
    }

    @Test
    fun `load offline shows an error`() {
        coEvery { api.getMyProfile() } throws IOException("offline")

        val viewModel = ProfileViewModel(api, tokenManager).apply { load() }

        assertTrue(viewModel.uiState.value is ProfileUiState.Error)
    }

    @Test
    fun `saving updates the profile and the name shown in the menu`() {
        val updated = profile.copy(firstName = "Sara", lastName = "Idrissi", fullName = "Sara Idrissi", phoneNumber = null)
        coEvery { api.getMyProfile() } returns Response.success(profile)
        coEvery { api.updateMyProfile(UpdateProfileRequest("Sara", "Idrissi", null)) } returns Response.success(updated)
        val viewModel = ProfileViewModel(api, tokenManager).apply { load() }

        viewModel.save(" Sara ", "Idrissi ", "  ")

        assertEquals(ProfileSaveState.Saved, viewModel.saveState.value)
        assertEquals(ProfileUiState.Success(updated), viewModel.uiState.value)
        verify { tokenManager.saveUserName("Sara Idrissi") }
    }

    @Test
    fun `saving shows Saving until the backend answers`() {
        val pending = CompletableDeferred<Response<UserProfile>>()
        coEvery { api.updateMyProfile(any()) } coAnswers { pending.await() }
        val viewModel = ProfileViewModel(api, tokenManager)

        viewModel.save("Sara", "Idrissi", "")

        assertEquals(ProfileSaveState.Saving, viewModel.saveState.value)
        pending.complete(Response.success(profile))
        assertEquals(ProfileSaveState.Saved, viewModel.saveState.value)
    }

    @Test
    fun `blank name or bad phone is rejected without calling the backend`() {
        val viewModel = ProfileViewModel(api, tokenManager)

        viewModel.save(" ", "Idrissi", "")
        assertTrue(viewModel.saveState.value is ProfileSaveState.Error)

        viewModel.save("Sara", "Idrissi", "06123")
        assertEquals(
            ProfileSaveState.Error("Phone number must be exactly 10 digits."),
            viewModel.saveState.value
        )
        coVerify(exactly = 0) { api.updateMyProfile(any()) }
    }

    @Test
    fun `a rejected save shows the backend's field error`() {
        coEvery { api.updateMyProfile(any()) } returns
            Response.error(400, """{"status":400,"errors":{"lastName":"Last name is required"}}""".toResponseBody())
        val viewModel = ProfileViewModel(api, tokenManager)

        viewModel.save("Sara", "Idrissi", "")

        assertEquals(ProfileSaveState.Error("Last name is required"), viewModel.saveState.value)
        verify(exactly = 0) { tokenManager.saveUserName(any()) }
    }

    // --- Email ---

    @Test
    fun `requesting a code moves to the code step for the new address`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(RequestEmailChangeRequest("new@example.com")) } returns sent
        val viewModel = ChangeEmailViewModel(api, tokenManager)

        viewModel.requestCode(" new@example.com ")

        assertEquals(ChangeEmailUiState.EnterCode(newEmail = "new@example.com"), viewModel.uiState.value)
    }

    @Test
    fun `an email in use shows the backend's message`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(any()) } returns badRequest("This email is already in use.")
        val viewModel = ChangeEmailViewModel(api, tokenManager)

        viewModel.requestCode("taken@example.com")

        assertEquals(ChangeEmailUiState.EnterEmail(error = "This email is already in use."), viewModel.uiState.value)
    }

    @Test
    fun `invalid or current email is rejected locally`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        val viewModel = ChangeEmailViewModel(api, tokenManager)

        viewModel.requestCode("not-an-email")
        assertTrue((viewModel.uiState.value as ChangeEmailUiState.EnterEmail).error != null)

        viewModel.requestCode("OLD@example.com")
        assertEquals(ChangeEmailUiState.EnterEmail(error = "This is already your email."), viewModel.uiState.value)
        coVerify(exactly = 0) { api.requestEmailChangeCode(any()) }
    }

    @Test
    fun `confirming saves the new token and email`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(any()) } returns sent
        coEvery { api.confirmEmailChange(ConfirmCodeRequest("123456")) } returns Response.success(
            AuthResponse("new-token", "ROLE_USER", 1L, "new@example.com", "Amina Ben Ali", "Email updated")
        )
        val viewModel = ChangeEmailViewModel(api, tokenManager)
        viewModel.requestCode("new@example.com")

        viewModel.confirm("123456")

        assertEquals(ChangeEmailUiState.Done, viewModel.uiState.value)
        verify { tokenManager.saveAuth("new-token", "ROLE_USER", 1L, "new@example.com", "Amina Ben Ali") }
    }

    @Test
    fun `a wrong code keeps the user on the code step with the error`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(any()) } returns sent
        coEvery { api.confirmEmailChange(any()) } returns badRequest("Invalid verification code.")
        val viewModel = ChangeEmailViewModel(api, tokenManager)
        viewModel.requestCode("new@example.com")

        viewModel.confirm("000000")

        assertEquals(
            ChangeEmailUiState.EnterCode(newEmail = "new@example.com", error = "Invalid verification code."),
            viewModel.uiState.value
        )
        verify(exactly = 0) { tokenManager.saveAuth(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a short code is rejected locally`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(any()) } returns sent
        val viewModel = ChangeEmailViewModel(api, tokenManager)
        viewModel.requestCode("new@example.com")

        viewModel.confirm("123")

        assertEquals("Please enter the 6-digit code.", (viewModel.uiState.value as ChangeEmailUiState.EnterCode).error)
        coVerify(exactly = 0) { api.confirmEmailChange(any()) }
    }

    @Test
    fun `resending tells the user a new code was sent`() {
        every { tokenManager.getEmail() } returns "old@example.com"
        coEvery { api.requestEmailChangeCode(any()) } returns sent
        val viewModel = ChangeEmailViewModel(api, tokenManager)
        viewModel.requestCode("new@example.com")

        viewModel.resendCode()

        val state = viewModel.uiState.value as ChangeEmailUiState.EnterCode
        assertEquals("A new code was sent to new@example.com.", state.notice)
        coVerify(exactly = 2) { api.requestEmailChangeCode(RequestEmailChangeRequest("new@example.com")) }
    }

    // --- Password ---

    private fun passwordViewModelAtCodeStep(): ChangePasswordViewModel {
        coEvery { api.requestPasswordChangeCode() } returns sent
        return ChangePasswordViewModel(api).apply { requestCode() }
    }

    @Test
    fun `requesting a password code moves to the code step`() {
        val viewModel = passwordViewModelAtCodeStep()

        assertEquals(ChangePasswordUiState.EnterCode(), viewModel.uiState.value)
    }

    @Test
    fun `a failed password code request stays on the first step`() {
        coEvery { api.requestPasswordChangeCode() } throws IOException("offline")
        val viewModel = ChangePasswordViewModel(api)

        viewModel.requestCode()

        assertTrue((viewModel.uiState.value as ChangePasswordUiState.RequestCode).error != null)
    }

    @Test
    fun `short or mismatched passwords are rejected locally`() {
        val viewModel = passwordViewModelAtCodeStep()

        viewModel.confirm("123456", "abc", "abc")
        assertEquals(
            "Password must be at least 6 characters.",
            (viewModel.uiState.value as ChangePasswordUiState.EnterCode).error
        )

        viewModel.confirm("123456", "secret1", "secret2")
        assertEquals("Passwords don't match.", (viewModel.uiState.value as ChangePasswordUiState.EnterCode).error)
        coVerify(exactly = 0) { api.changePassword(any()) }
    }

    @Test
    fun `the right code changes the password`() {
        val viewModel = passwordViewModelAtCodeStep()
        coEvery { api.changePassword(ChangePasswordRequest("123456", "secret1")) } returns
            Response.success(AuthMessageResponse("Password updated successfully."))

        viewModel.confirm("123456", "secret1", "secret1")

        assertEquals(ChangePasswordUiState.Done, viewModel.uiState.value)
    }

    @Test
    fun `a wrong password code shows the backend's message`() {
        val viewModel = passwordViewModelAtCodeStep()
        coEvery { api.changePassword(any()) } returns badRequest("Invalid verification code.")

        viewModel.confirm("000000", "secret1", "secret1")

        val state = viewModel.uiState.value as ChangePasswordUiState.EnterCode
        assertEquals("Invalid verification code.", state.error)
        assertNull(state.notice)
    }

    // --- Error parsing ---

    @Test
    fun `an error body without a message falls back to the status code`() {
        val response = Response.error<Unit>(500, "".toResponseBody())

        assertEquals("Couldn't save (HTTP 500).", profileErrorMessage(response, "Couldn't save"))
    }
}
