package com.example.medrational_android.ui.suggestions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.SuggestionAttachment
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionSender
import com.example.medrational_android.data.model.formatIsoDateTime
import com.example.medrational_android.ui.components.ContentWidth
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.components.SwisraTopBar
import com.example.medrational_android.ui.components.centeredContent
import com.example.medrational_android.ui.files.AttachmentPreviewCard
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.PreviewState
import com.example.medrational_android.viewmodel.SuggestionDetailUiState
import com.example.medrational_android.viewmodel.SuggestionDetailViewModel

/** Admin only: one suggestion, who sent it, and a way to delete it. */
@Composable
fun SuggestionDetailScreen(
    suggestionId: Long,
    viewModel: SuggestionDetailViewModel,
    onDeleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val preview by viewModel.preview.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(suggestionId) {
        viewModel.load(suggestionId)
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(uiState) {
        if (uiState == SuggestionDetailUiState.Deleted) {
            Toast.makeText(context, context.getString(R.string.suggestion_deleted), Toast.LENGTH_SHORT).show()
            onDeleted()
        }
    }

    val success = uiState as? SuggestionDetailUiState.Success

    Scaffold(
        topBar = {
            SwisraTopBar(
                title = success?.suggestion?.title ?: stringResource(R.string.suggestion_default_title),
                onBackClick = onBackClick,
                actions = {
                    if (success != null) {
                        IconButton(onClick = { showDeleteDialog = true }, enabled = !success.busy) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.suggestion_delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SuggestionDetailUiState.Loading, SuggestionDetailUiState.Deleted -> LoadingState()
                is SuggestionDetailUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(suggestionId) }
                )
                is SuggestionDetailUiState.Success -> SuggestionContent(
                    suggestion = state.suggestion,
                    preview = preview,
                    // Attachments are private, so they stream through the API with the token
                    onDownloadClick = { attachment ->
                        downloader.downloadFile(
                            ApiClient.suggestionAttachmentDownloadUrl(state.suggestion.id),
                            attachment.fileName
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.download_started, attachment.fileName),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onRetryPreviewClick = viewModel::retryPreview,
                    onPreviewCorrupted = viewModel::onPreviewCorrupted
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.suggestion_delete_confirm_title)) },
            text = { Text(stringResource(R.string.suggestion_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun SuggestionContent(
    suggestion: SuggestionDetail,
    preview: PreviewState?,
    onDownloadClick: (SuggestionAttachment) -> Unit,
    onRetryPreviewClick: () -> Unit,
    onPreviewCorrupted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Centered and capped on tablets; the margins still scroll
            .centeredContent(ContentWidth.Reading)
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = suggestion.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        suggestion.createdAt?.let { formatIsoDateTime(it, appLocale()) }?.let { sent ->
            Text(
                text = stringResource(R.string.suggestion_sent_on, sent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SenderCard(sender = suggestion.sender)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.lg)
            )
        }
        suggestion.attachment?.let { attachment ->
            AttachmentPreviewCard(
                fileName = attachment.fileName,
                preview = preview,
                onDownloadClick = { onDownloadClick(attachment) },
                onRetryPreviewClick = onRetryPreviewClick,
                onPreviewCorrupted = onPreviewCorrupted
            )
        }
    }
}

@Composable
private fun SenderCard(sender: SuggestionSender) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(R.string.suggestion_sent_by),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = sender.fullName?.takeIf { it.isNotBlank() } ?: sender.email,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = sender.email, style = MaterialTheme.typography.bodyMedium)
            sender.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
