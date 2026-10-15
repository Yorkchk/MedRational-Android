package com.example.medrational_android.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.OtpCodeField
import com.example.medrational_android.ui.components.PrimaryButton
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.AuthUiState
import com.example.medrational_android.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onPasswordResetCompleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var otpEmail by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState) { otpEmail = otpStepEmail(otpEmail, uiState) }

    val isEmailValid = email.trim().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isPasswordValid = newPassword.length >= 6
    val doPasswordsMatch = newPassword.isNotEmpty() && newPassword == repeatPassword
    val isFormValid = isEmailValid && isPasswordValid && doPasswordsMatch
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.PasswordResetCompleted) {
            viewModel.resetState()
            onPasswordResetCompleted()
        }
    }

    val sendCode = {
        focusManager.clearFocus()
        viewModel.requestPasswordResetOtp(email)
    }

    val codeEmail = otpStepEmail(otpEmail, uiState)
    if (codeEmail == null) {
        AuthLayout(
            title = stringResource(R.string.forgot_heading),
            subtitle = stringResource(R.string.forgot_subtitle),
            onBackClick = onBackClick
        ) {
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.forgot_registered_email),
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                errorText = if (email.isNotEmpty() && !isEmailValid) stringResource(R.string.auth_invalid_email) else null
            )
            AuthTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = stringResource(R.string.forgot_new_password),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                errorText = if (newPassword.isNotEmpty() && !isPasswordValid) stringResource(R.string.auth_password_min_length) else null
            )
            AuthTextField(
                value = repeatPassword,
                onValueChange = { repeatPassword = it },
                label = stringResource(R.string.forgot_repeat_password),
                leadingIcon = Icons.Outlined.LockReset,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { if (isFormValid && !isLoading) sendCode() },
                isPassword = true,
                errorText = if (repeatPassword.isNotEmpty() && !doPasswordsMatch) stringResource(R.string.forgot_passwords_mismatch) else null
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            ErrorBanner(uiState)
            PrimaryButton(
                text = stringResource(R.string.forgot_send_code),
                onClick = sendCode,
                enabled = isFormValid,
                loading = isLoading
            )
        }
    } else {
        val confirm = { viewModel.confirmPasswordReset(codeEmail, otpCode, newPassword) }
        val leaveCodeStep = {
            otpEmail = null
            otpCode = ""
            viewModel.resetState()
        }
        AuthLayout(
            title = stringResource(R.string.forgot_confirm_code),
            subtitle = stringResource(R.string.forgot_code_sent_to, codeEmail),
            onBackClick = leaveCodeStep
        ) {
            OtpCodeField(
                value = otpCode,
                onValueChange = { otpCode = it },
                onDone = confirm,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            ErrorBanner(uiState)
            PrimaryButton(
                text = stringResource(R.string.forgot_confirm_update),
                onClick = confirm,
                enabled = otpCode.length == 6,
                loading = isLoading
            )
            TextButton(onClick = leaveCodeStep, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.signup_edit_details))
            }
        }
    }
}
