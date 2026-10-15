package com.example.medrational_android.ui.auth

import com.example.medrational_android.ui.text.UiText
import com.example.medrational_android.viewmodel.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpStepTest {

    private val email = "salma@example.com"

    @Test
    fun `sending the code opens the code step`() {
        assertEquals(email, otpStepEmail(null, AuthUiState.OtpSent(email)))
    }

    // Regression: the screens used to show the code step only while the state was OtpSent, so they
    // jumped back to the email and password form while verifying and after a wrong code
    @Test
    fun `the code step stays open while verifying and after a wrong code`() {
        assertEquals(email, otpStepEmail(email, AuthUiState.Loading))
        assertEquals(email, otpStepEmail(email, AuthUiState.Error(UiText.Res(0))))
    }

    @Test
    fun `the form step stays the form until a code is sent`() {
        assertNull(otpStepEmail(null, AuthUiState.Idle))
        assertNull(otpStepEmail(null, AuthUiState.Loading))
        assertNull(otpStepEmail(null, AuthUiState.Error(UiText.Res(0))))
    }

    @Test
    fun `resending to another address follows the new address`() {
        assertEquals("new@example.com", otpStepEmail(email, AuthUiState.OtpSent("new@example.com")))
    }
}
