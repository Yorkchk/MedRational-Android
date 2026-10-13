package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.TestStrings.fr
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.AuthMessageResponse
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

// The backend's messages are in English, so the app shows its own, in the user's language
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val viewModel = AuthViewModel(mockk<TokenManager>(relaxed = true), api)

    private fun <T> backendError(code: Int, message: String) =
        Response.error<T>(code, """{"status":$code,"message":"$message"}""".toResponseBody())

    private fun error() = (viewModel.uiState.value as AuthUiState.Error).error

    @Test
    fun `a sent code moves to the code step without the backend's text`() {
        coEvery { api.login(any()) } returns Response.success(AuthMessageResponse("Verification code sent to a@b.c"))

        viewModel.requestLoginOtp(" a@b.c ", "secret")

        assertEquals(AuthUiState.OtpSent(email = "a@b.c"), viewModel.uiState.value)
    }

    @Test
    fun `wrong credentials are explained in the app's language`() {
        coEvery { api.login(any()) } returns backendError(401, "Invalid email or password")

        viewModel.requestLoginOtp("a@b.c", "wrong")

        assertEquals("Invalid email or password.", en(error()))
        assertEquals("E-mail ou mot de passe incorrect.", fr(error()))
    }

    @Test
    fun `an unknown email on forgot password is explained`() {
        coEvery { api.forgotPassword(any()) } returns backendError(400, "No account registered with this email.")

        viewModel.requestPasswordResetOtp("nobody@b.c")

        assertEquals("Aucun compte n’est associé à cet e-mail.", fr(error()))
    }

    @Test
    fun `a wrong reset code is explained`() {
        coEvery { api.resetPassword(any()) } returns backendError(400, "Invalid verification code.")

        viewModel.confirmPasswordReset("a@b.c", "000000", "secret1")

        assertEquals("Invalid or expired verification code.", en(error()))
    }

    @Test
    fun `no connection shows the network error, not the exception text`() {
        coEvery { api.requestUserOtp(any()) } throws IOException("Failed to connect to /10.0.2.2:8080")

        viewModel.registerUser("Amina", "Ben Ali", "0612345678", "a@b.c", "secret1")

        assertEquals("Network error. Check your connection and try again.", en(error()))
    }

    @Test
    fun `missing fields are rejected locally`() {
        viewModel.registerUser("Amina", " ", "", "a@b.c", "secret1")

        assertEquals("Tous les champs sont obligatoires.", fr(error()))
    }
}
