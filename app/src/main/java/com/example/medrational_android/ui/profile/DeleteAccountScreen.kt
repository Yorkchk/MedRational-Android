package com.example.medrational_android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.OtpCodeField
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.DeleteAccountUiState
import com.example.medrational_android.viewmodel.DeleteAccountViewModel

/**
 * Deletes the signed-in account after a code emailed to [email]. The last step asks once more in a
 * dialog, since nothing can be recovered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    email: String?,
    onAccountDeleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is DeleteAccountUiState.Deleted) onAccountDeleted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_delete_account), fontWeight = FontWeight.Bold) },
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
                is DeleteAccountUiState.RequestCode -> {
                    WhatHappensCard()
                    Text(
                        if (email != null) {
                            stringResource(R.string.profile_delete_account_code_to, email)
                        } else {
                            stringResource(R.string.profile_password_code_to_your_email)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    state.error?.let { ErrorText(it) }
                    BusyButton(text = stringResource(R.string.profile_send_code), isBusy = state.isSending, onClick = viewModel::requestCode)
                }
                is DeleteAccountUiState.EnterCode -> {
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
                    state.notice?.let {
                        Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let { ErrorText(it) }
                    BusyButton(
                        text = stringResource(R.string.profile_delete_account_confirm),
                        isBusy = state.isBusy,
                        enabled = code.length == 6,
                        onClick = { showConfirmDialog = true }
                    )
                    TextButton(onClick = viewModel::resendCode, enabled = !state.isBusy) { Text(stringResource(R.string.profile_resend_code)) }
                }
                // Signing out
                is DeleteAccountUiState.Deleted -> Unit
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.profile_delete_account_dialog_title)) },
            text = { Text(stringResource(R.string.profile_delete_account_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirm(code)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun WhatHappensCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.profile_delete_account_what),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                stringResource(R.string.profile_delete_account_kept),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
