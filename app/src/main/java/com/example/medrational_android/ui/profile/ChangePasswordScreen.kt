package com.example.medrational_android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.medrational_android.ui.components.OtpCodeField
import com.example.medrational_android.viewmodel.ChangePasswordUiState
import com.example.medrational_android.viewmodel.ChangePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    email: String?,
    onPasswordChanged: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ChangePasswordUiState.Done) onPasswordChanged()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                is ChangePasswordUiState.RequestCode -> {
                    Text(
                        if (email != null) {
                            "To confirm it's you, we'll send a 6-digit code to $email."
                        } else {
                            "To confirm it's you, we'll send a 6-digit code to your email."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    state.error?.let { ErrorText(it) }
                    BusyButton(text = "Send code", isBusy = state.isSending, onClick = viewModel::requestCode)
                }
                is ChangePasswordUiState.EnterCode -> {
                    Text(
                        if (email != null) "We sent a code to $email." else "We sent a code to your email.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OtpCodeField(
                        value = code,
                        onValueChange = { code = it },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val visualTransformation =
                        if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { Text("At least 6 characters") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repeatPassword,
                        onValueChange = { repeatPassword = it },
                        label = { Text("Repeat new password") },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.notice?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let { ErrorText(it) }
                    BusyButton(
                        text = "Change password",
                        isBusy = state.isBusy,
                        enabled = code.length == 6 && newPassword.isNotEmpty() && repeatPassword.isNotEmpty(),
                        onClick = { viewModel.confirm(code, newPassword, repeatPassword) }
                    )
                    TextButton(onClick = viewModel::resendCode, enabled = !state.isBusy) { Text("Resend code") }
                }
                // Navigating away
                is ChangePasswordUiState.Done -> Unit
            }
        }
    }
}
