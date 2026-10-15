package com.example.medrational_android.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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

private const val NAME_MAX = 50
private const val PHONE_DIGITS = 10

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignUpCompleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var otpEmail by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState) { otpEmail = otpStepEmail(otpEmail, uiState) }

    // Validation rules
    val isFirstNameValid = firstName.trim().isNotEmpty() && firstName.length <= NAME_MAX
    val isLastNameValid = lastName.trim().isNotEmpty() && lastName.length <= NAME_MAX
    val cleanPhone = phoneNumber.filter { it.isDigit() }
    val isPhoneValid = cleanPhone.length == PHONE_DIGITS
    val isEmailValid = email.trim().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isPasswordValid = password.length >= 6
    val isFormValid = isFirstNameValid && isLastNameValid && isPhoneValid && isEmailValid && isPasswordValid
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SignUpCompleted) {
            viewModel.resetState()
            onSignUpCompleted()
        }
    }

    val sendCode = {
        focusManager.clearFocus()
        viewModel.registerUser(firstName, lastName, cleanPhone, email, password)
    }

    val codeEmail = otpStepEmail(otpEmail, uiState)
    if (codeEmail == null) {
        AuthLayout(
            title = stringResource(R.string.signup_title),
            subtitle = stringResource(R.string.signup_subtitle),
            onBackClick = onBackClick
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AuthTextField(
                    value = firstName,
                    onValueChange = { if (it.length <= NAME_MAX) firstName = it },
                    label = stringResource(R.string.signup_first_name),
                    leadingIcon = Icons.Outlined.Person,
                    errorText = if (firstName.isNotEmpty() && !isFirstNameValid) stringResource(R.string.signup_max_50) else null,
                    modifier = Modifier.weight(1f)
                )
                AuthTextField(
                    value = lastName,
                    onValueChange = { if (it.length <= NAME_MAX) lastName = it },
                    label = stringResource(R.string.signup_last_name),
                    leadingIcon = Icons.Outlined.Badge,
                    errorText = if (lastName.isNotEmpty() && !isLastNameValid) stringResource(R.string.signup_max_50) else null,
                    modifier = Modifier.weight(1f)
                )
            }
            AuthTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= PHONE_DIGITS) phoneNumber = digits
                },
                label = stringResource(R.string.signup_phone),
                leadingIcon = Icons.Outlined.Phone,
                placeholder = stringResource(R.string.signup_phone_placeholder),
                keyboardType = KeyboardType.Phone,
                errorText = if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                    stringResource(R.string.signup_phone_digits, cleanPhone.length)
                } else {
                    null
                }
            )
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.auth_email_label),
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                errorText = if (email.isNotEmpty() && !isEmailValid) stringResource(R.string.auth_invalid_email) else null
            )
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_password_label),
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { if (isFormValid && !isLoading) sendCode() },
                isPassword = true,
                errorText = if (password.isNotEmpty() && !isPasswordValid) stringResource(R.string.auth_password_min_length) else null
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            ErrorBanner(uiState)
            PrimaryButton(
                text = stringResource(R.string.signup_send_code),
                onClick = sendCode,
                enabled = isFormValid,
                loading = isLoading
            )
        }
    } else {
        val verify = { viewModel.verifySignUpOtp(codeEmail, otpCode) }
        val leaveCodeStep = {
            otpEmail = null
            otpCode = ""
            viewModel.resetState()
        }
        AuthLayout(
            title = stringResource(R.string.signup_verify_email),
            subtitle = stringResource(R.string.signup_code_sent_to, codeEmail),
            onBackClick = leaveCodeStep
        ) {
            OtpCodeField(
                value = otpCode,
                onValueChange = { otpCode = it },
                onDone = verify,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            ErrorBanner(uiState)
            PrimaryButton(
                text = stringResource(R.string.signup_complete),
                onClick = verify,
                enabled = otpCode.length == 6,
                loading = isLoading
            )
            TextButton(onClick = leaveCodeStep, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.signup_edit_details))
            }
        }
    }
}
