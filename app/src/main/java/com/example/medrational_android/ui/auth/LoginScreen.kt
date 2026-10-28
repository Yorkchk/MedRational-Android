package com.example.medrational_android.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import com.example.medrational_android.ui.components.PrimaryButton
import com.example.medrational_android.ui.components.StatusBanner
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.AuthUiState
import com.example.medrational_android.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToUserDashboard: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val isLoading = uiState is AuthUiState.Loading
    val canSignIn = !isLoading && email.isNotBlank() && password.isNotBlank()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoginSuccess) {
            val role = (uiState as AuthUiState.LoginSuccess).role
            if (role.equals("ROLE_ADMIN", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)) {
                onNavigateToAdminDashboard()
            } else {
                onNavigateToUserDashboard()
            }
            viewModel.resetState()
        }
    }

    val signIn = {
        focusManager.clearFocus()
        viewModel.login(email, password)
    }

    AuthLayout(
        title = stringResource(R.string.login_welcome_back),
        subtitle = stringResource(R.string.login_subtitle),
        onBackClick = onBackClick
    ) {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.auth_email_label),
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.auth_password_label),
            leadingIcon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { if (canSignIn) signIn() },
            isPassword = true
        )
        TextButton(onClick = onNavigateToForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.login_forgot_password))
        }
        ErrorBanner(uiState)
        Spacer(modifier = Modifier.height(Spacing.sm))
        PrimaryButton(
            text = stringResource(R.string.auth_log_in),
            onClick = signIn,
            enabled = canSignIn,
            loading = isLoading
        )
    }
}

/** The auth error, if any, as a banner with some space below it. */
@Composable
internal fun ErrorBanner(uiState: AuthUiState) {
    if (uiState is AuthUiState.Error) {
        StatusBanner(message = uiState.error.asString())
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}
