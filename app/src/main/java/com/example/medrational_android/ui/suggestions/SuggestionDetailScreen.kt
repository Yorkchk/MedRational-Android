package com.example.medrational_android.ui.suggestions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.SuggestionAttachment
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionMessage
import com.example.medrational_android.data.model.SuggestionSender
import com.example.medrational_android.data.model.formatIsoDateTime
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.SuggestionDetailUiState
import com.example.medrational_android.viewmodel.SuggestionDetailViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionDetailScreen(
    suggestionId: Long,
    viewModel: SuggestionDetailViewModel,
    isAdmin: Boolean,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    var showCloseDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(suggestionId) {
        viewModel.load(suggestionId)
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    // Attachments are private, so they stream through the API with the user's token
    val download: (path: String, attachment: SuggestionAttachment) -> Unit = { path, attachment ->
        downloader.downloadFile("${ApiClient.BASE_URL}api/v1/suggestions/$suggestionId/$path", attachment.fileName)
        Toast.makeText(context, context.getString(R.string.download_started, attachment.fileName), Toast.LENGTH_SHORT).show()
    }

    val success = uiState as? SuggestionDetailUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = success?.suggestion?.title ?: stringResource(R.string.suggestion_default_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                    if (isAdmin && success?.canPost == true) {
                        IconButton(onClick = { showCloseDialog = true }, enabled = !success.busy) {
                            Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.suggestion_close))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (success != null) {
                if (success.canPost) {
                    Composer(
                        busy = success.busy,
                        onSend = { body, file, onSent ->
                            viewModel.sendMessage(body, file, onSent)
                        }
                    )
                } else {
                    ClosedBanner()
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SuggestionDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is SuggestionDetailUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.load(suggestionId) }) { Text(stringResource(R.string.common_retry)) }
                }
                is SuggestionDetailUiState.Success -> Conversation(
                    suggestion = state.suggestion,
                    isAdmin = isAdmin,
                    onSuggestionAttachmentClick = { download("attachment", it) },
                    onMessageAttachmentClick = { msg, attachment -> download("messages/${msg.id}/attachment", attachment) }
                )
            }
        }
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text(stringResource(R.string.suggestion_close_confirm_title)) },
            text = {
                Text(stringResource(R.string.suggestion_close_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseDialog = false
                        viewModel.close()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.suggestion_close)) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun Conversation(
    suggestion: SuggestionDetail,
    isAdmin: Boolean,
    onSuggestionAttachmentClick: (SuggestionAttachment) -> Unit,
    onMessageAttachmentClick: (SuggestionMessage, SuggestionAttachment) -> Unit
) {
    val listState = rememberLazyListState()
    // Jump to the newest message when the thread grows
    LaunchedEffect(suggestion.messages.size) {
        if (suggestion.messages.isNotEmpty()) listState.animateScrollToItem(suggestion.messages.size)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            SuggestionHeader(
                suggestion = suggestion,
                showSender = isAdmin,
                onAttachmentClick = onSuggestionAttachmentClick
            )
        }
        items(suggestion.messages, key = { it.id }) { msg ->
            // "Mine" is the admin side for admins and the user side for users
            MessageBubble(
                message = msg,
                mine = msg.fromAdmin == isAdmin,
                onAttachmentClick = { attachment -> onMessageAttachmentClick(msg, attachment) }
            )
        }
    }
}

@Composable
private fun SuggestionHeader(
    suggestion: SuggestionDetail,
    showSender: Boolean,
    onAttachmentClick: (SuggestionAttachment) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SuggestionStatusChip(status = suggestion.status)
            val sent = suggestion.createdAt?.let { formatIsoDateTime(it, appLocale()) }
            if (sent != null) {
                Text(
                    text = "  " + stringResource(R.string.suggestion_sent_on, sent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(text = suggestion.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (showSender) {
            SenderCard(sender = suggestion.sender)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = suggestion.description, style = MaterialTheme.typography.bodyMedium)
                suggestion.attachment?.let { attachment ->
                    AttachmentChip(attachment = attachment, onClick = { onAttachmentClick(attachment) })
                }
            }
        }
        if (suggestion.messages.isEmpty()) {
            Text(
                text = if (showSender) stringResource(R.string.suggestion_no_replies_admin) else stringResource(R.string.suggestion_no_replies_user),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            HorizontalDivider()
        }
    }
}

@Composable
private fun SenderCard(sender: SuggestionSender) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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

@Composable
private fun MessageBubble(
    message: SuggestionMessage,
    mine: Boolean,
    onAttachmentClick: (SuggestionAttachment) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp
            ),
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (mine) stringResource(R.string.suggestion_author_you) else message.authorName ?: if (message.fromAdmin) stringResource(R.string.suggestion_author_admin) else stringResource(R.string.suggestion_author_user),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
                message.attachment?.let { attachment ->
                    AttachmentChip(attachment = attachment, onClick = { onAttachmentClick(attachment) })
                }
                message.createdAt?.let { formatIsoDateTime(it, appLocale()) }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(
    busy: Boolean,
    onSend: (body: String, file: MultipartBody.Part?, onSent: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var attachment by remember { mutableStateOf<PickedFile?>(null) }
    val pickAttachment = rememberAttachmentPicker { attachment = it }

    Surface(tonalElevation = 3.dp, modifier = Modifier.imePadding()) {
        Column(modifier = Modifier.navigationBarsPadding().padding(8.dp)) {
            attachment?.let { picked ->
                PickedFileChip(
                    file = picked,
                    onRemoveClick = { attachment = null },
                    enabled = !busy,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = pickAttachment, enabled = !busy && attachment == null) {
                    Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.suggestion_attach_max))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.suggestion_write_message)) },
                    enabled = !busy,
                    maxLines = 5
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            readAttachment(context, attachment).onSuccess { part ->
                                onSend(text, part) {
                                    text = ""
                                    attachment = null
                                }
                            }
                        }
                    },
                    enabled = !busy && text.isNotBlank()
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.common_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedBanner() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "  " + stringResource(R.string.suggestion_archived_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
