package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.TestStrings.fr
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.AuthResponse
import com.example.medrational_android.data.model.LoginRequest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
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
    private val tokenManager: TokenManager = mockk(relaxed = true)
    private val viewModel = AuthViewModel(tokenManager, api)

    private fun <T> backendError(code: Int, message: String) =
        Response.error<T>(code, """{"status":$code,"message":"$message"}""".toResponseBody())

    private fun error() = (viewModel.uiState.value as AuthUiState.Error).error

    // Regression: sign-in used to email a code and wait for it; now the password is enough
    @Test
    fun `the right password signs in straight away`() {
        coEvery { api.login(LoginRequest("a@b.c", "secret")) } returns Response.success(
            AuthResponse(
                token = "jwt",
                role = "ROLE_USER",
                userId = 7L,
                email = "a@b.c",
                fullName = "Amina Ben Ali",
                message = "Authentication successful"
            )
        )

        viewModel.login(" a@b.c ", "secret")

        assertEquals(AuthUiState.LoginSuccess(role = "ROLE_USER"), viewModel.uiState.value)
        verify { tokenManager.saveAuth("jwt", "ROLE_USER", 7L, "a@b.c", "Amina Ben Ali") }
    }

    @Test
    fun `wrong credentials are explained in the app's language`() {
        coEvery { api.login(any()) } returns backendError(401, "Invalid email or password")

        viewModel.login("a@b.c", "wrong")

        assertEquals("Invalid email or password.", en(error()))
        assertEquals("E-mail ou mot de passe incorrect.", fr(error()))
        verify(exactly = 0) { tokenManager.saveAuth(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `an unconfirmed sign-up is told to sign up again`() {
        coEvery { api.login(any()) } returns backendError(403, "Email not verified.")

        viewModel.login("a@b.c", "secret")

        assertEquals("Your email hasn’t been confirmed yet. Sign up again to get a new code.", en(error()))
        assertEquals(
            "Votre e-mail n’a pas encore été confirmé. Inscrivez-vous à nouveau pour recevoir un nouveau code.",
            fr(error())
        )
    }

    @Test
    fun `missing credentials are rejected locally`() {
        viewModel.login("a@b.c", " ")

        assertEquals("Email and password are required.", en(error()))
    }

    @Test
    fun `signing up with the email of an existing account is explained`() {
        coEvery { api.requestUserOtp(any()) } returns backendError(409, "An account already exists with this email.")

        viewModel.registerUser("Amina", "Ben Ali", "0612345678", "a@b.c", "secret1")

        assertEquals("An account already exists with this email. Sign in or reset your password.", en(error()))
        assertEquals(
            "Un compte existe déjà avec cet e-mail. Connectez-vous ou réinitialisez votre mot de passe.",
            fr(error())
        )
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
