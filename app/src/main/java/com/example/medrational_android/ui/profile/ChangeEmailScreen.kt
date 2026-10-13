package com.example.medrational_android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.OtpCodeField
import com.example.medrational_android.ui.text.UiText
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.ChangeEmailUiState
import com.example.medrational_android.viewmodel.ChangeEmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    viewModel: ChangeEmailViewModel,
    currentEmail: String?,
    onEmailChanged: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var newEmail by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is ChangeEmailUiState.Done) onEmailChanged()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_change_email), fontWeight = FontWeight.Bold) },
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
                is ChangeEmailUiState.EnterEmail -> {
                    if (currentEmail != null) {
                        Text(
                            stringResource(R.string.profile_current_email, currentEmail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(stringResource(R.string.profile_new_email)) },
                        singleLine = true,
                        enabled = !state.isSending,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.profile_new_email_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.error?.let { ErrorText(it) }
                    BusyButton(
                        text = stringResource(R.string.profile_send_code),
                        isBusy = state.isSending,
                        enabled = newEmail.isNotBlank(),
                        onClick = {
                            code = ""
                            viewModel.requestCode(newEmail)
                        }
                    )
                }
                is ChangeEmailUiState.EnterCode -> {
                    Text(stringResource(R.string.profile_code_sent_to, state.newEmail), style = MaterialTheme.typography.bodyLarge)
                    OtpCodeField(
                        value = code,
                        onValueChange = { code = it },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.notice?.let {
                        Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let { ErrorText(it) }
                    BusyButton(
                        text = stringResource(R.string.profile_confirm_new_email),
                        isBusy = state.isBusy,
                        enabled = code.length == 6,
                        onClick = { viewModel.confirm(code) }
                    )
                    TextButton(onClick = viewModel::resendCode, enabled = !state.isBusy) { Text(stringResource(R.string.profile_resend_code)) }
                    TextButton(onClick = viewModel::editEmail, enabled = !state.isBusy) { Text(stringResource(R.string.profile_use_different_email)) }
                }
                // Navigating away
                is ChangeEmailUiState.Done -> Unit
            }
        }
    }
}

@Composable
internal fun ErrorText(message: UiText, modifier: Modifier = Modifier) {
    Text(
        text = message.asString(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

@Composable
internal fun BusyButton(
    text: String,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(onClick = onClick, enabled = enabled && !isBusy, modifier = modifier.fillMaxWidth()) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}
