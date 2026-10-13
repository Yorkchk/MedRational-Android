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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.OtpCodeField
import com.example.medrational_android.ui.text.asString
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
                title = { Text(stringResource(R.string.profile_change_password), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                            stringResource(R.string.profile_password_code_to, email)
                        } else {
                            stringResource(R.string.profile_password_code_to_your_email)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    state.error?.let { ErrorText(it) }
                    BusyButton(text = stringResource(R.string.profile_send_code), isBusy = state.isSending, onClick = viewModel::requestCode)
                }
                is ChangePasswordUiState.EnterCode -> {
                    Text(
                        if (email != null) stringResource(R.string.profile_code_sent_to, email) else stringResource(R.string.profile_code_sent_to_your_email),
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
                        label = { Text(stringResource(R.string.profile_new_password)) },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { Text(stringResource(R.string.profile_password_min)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repeatPassword,
                        onValueChange = { repeatPassword = it },
                        label = { Text(stringResource(R.string.profile_repeat_password)) },
                        singleLine = true,
                        enabled = !state.isBusy,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.notice?.let {
                        Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let { ErrorText(it) }
                    BusyButton(
                        text = stringResource(R.string.profile_change_password),
                        isBusy = state.isBusy,
                        enabled = code.length == 6 && newPassword.isNotEmpty() && repeatPassword.isNotEmpty(),
                        onClick = { viewModel.confirm(code, newPassword, repeatPassword) }
                    )
                    TextButton(onClick = viewModel::resendCode, enabled = !state.isBusy) { Text(stringResource(R.string.profile_resend_code)) }
                }
                // Navigating away
                is ChangePasswordUiState.Done -> Unit
            }
        }
    }
}
