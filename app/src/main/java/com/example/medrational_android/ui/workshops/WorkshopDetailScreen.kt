package com.example.medrational_android.ui.workshops

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopAttachment
import com.example.medrational_android.ui.suggestions.readAttachment
import com.example.medrational_android.ui.suggestions.rememberAttachmentPicker
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.PreviewState
import com.example.medrational_android.viewmodel.WorkshopAttachmentPreviewsViewModel
import com.example.medrational_android.viewmodel.WorkshopDetailUiState
import com.example.medrational_android.viewmodel.WorkshopDetailViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * A workshop project. Students see its dates and, while registration is open, a button to the
 * external registration form. Admins can also force a status, manage attachments and delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopDetailScreen(
    projectId: Long,
    viewModel: WorkshopDetailViewModel,
    isAdmin: Boolean,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val previewsViewModel: WorkshopAttachmentPreviewsViewModel = viewModel {
        WorkshopAttachmentPreviewsViewModel(ApiClient.api, File(context.cacheDir, "previews"))
    }
    val previews by previewsViewModel.previews.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val downloader = remember { AndroidDownloader(context) }
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var attachmentToDelete by remember { mutableStateOf<WorkshopAttachment?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val pickAttachment = rememberAttachmentPicker { picked ->
        scope.launch {
            readAttachment(context, picked).onSuccess { part -> part?.let(viewModel::uploadAttachment) }
        }
    }

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(uiState) {
        if (uiState == WorkshopDetailUiState.Deleted) onBackClick()
    }

    val success = uiState as? WorkshopDetailUiState.Success
    val attachments = success?.project?.attachments
    LaunchedEffect(attachments) {
        attachments?.let(previewsViewModel::load)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = success?.project?.title ?: stringResource(R.string.workshop_default_title),
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
                    if (isAdmin && success != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }, enabled = !success.busy) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.workshop_admin_actions))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workshop_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onEditClick()
                                    }
                                )
                                HorizontalDivider()
                                ProjectStatus.entries.filter { it != success.project.status }.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(status.markAsLabel())) },
                                        onClick = {
                                            menuOpen = false
                                            viewModel.changeStatus(status)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workshop_add_attachment)) },
                                    leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        pickAttachment()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.workshop_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
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
                is WorkshopDetailUiState.Loading, WorkshopDetailUiState.Deleted ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is WorkshopDetailUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.load(projectId) }) { Text(stringResource(R.string.common_retry)) }
                }
                is WorkshopDetailUiState.Success -> ProjectDetails(
                    state = state,
                    isAdmin = isAdmin,
                    onRegisterClick = { link ->
                        try {
                            uriHandler.openUri(link)
                        } catch (e: IllegalArgumentException) {
                            Toast.makeText(context, R.string.workshop_link_error, Toast.LENGTH_LONG).show()
                        }
                    },
                    previews = previews,
                    onDownloadAttachmentClick = { attachment ->
                        downloader.downloadFile(ApiClient.workshopAttachmentDownloadUrl(attachment.id), attachment.fileName)
                        Toast.makeText(context, context.getString(R.string.download_started, attachment.fileName), Toast.LENGTH_SHORT).show()
                    },
                    onRetryPreviewClick = previewsViewModel::retry,
                    onPreviewCorrupted = previewsViewModel::onPreviewCorrupted,
                    onDeleteAttachmentClick = { attachmentToDelete = it }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.workshop_delete_confirm_title)) },
            text = { Text(stringResource(R.string.workshop_delete_confirm_body)) },
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

    attachmentToDelete?.let { attachment ->
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text(stringResource(R.string.workshop_remove_attachment_title)) },
            text = { Text(stringResource(R.string.workshop_remove_attachment_body, attachment.fileName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        attachmentToDelete = null
                        viewModel.deleteAttachment(attachment.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.hashtags_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun ProjectDetails(
    state: WorkshopDetailUiState.Success,
    isAdmin: Boolean,
    previews: Map<Long, PreviewState>,
    onRegisterClick: (String) -> Unit,
    onDownloadAttachmentClick: (WorkshopAttachment) -> Unit,
    onRetryPreviewClick: (Long) -> Unit,
    onPreviewCorrupted: (Long) -> Unit,
    onDeleteAttachmentClick: (WorkshopAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    val project = state.project
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                WorkshopStatusChip(status = project.status)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val projectType = project.projectType ?: ProjectType.FIRST_COME_FIRST_SERVED
                    DetailLine(label = stringResource(R.string.workshop_project_type), value = stringResource(projectType.label()))
                    DetailLine(label = stringResource(R.string.workshop_registration_opens), value = project.opensLabel(appLocale()))
                    DetailLine(label = stringResource(R.string.workshop_registration_closes), value = project.closesLabel(appLocale()))
                    DetailLine(label = stringResource(R.string.workshop_capacity), value = project.capacityLabel()?.asString() ?: stringResource(R.string.workshop_no_limit))
                    Text(
                        text = stringResource(projectType.description()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RegistrationSection(state = state, onRegisterClick = onRegisterClick)

            // Admins receive the link in every status so they can check it before opening
            if (isAdmin && project.status != ProjectStatus.OPEN && !project.formLink.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.workshop_admin_link, project.formLink),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = project.description, style = MaterialTheme.typography.bodyLarge)

            if (project.attachments.isNotEmpty()) {
                Text(stringResource(R.string.workshop_documents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                project.attachments.forEach { attachment ->
                    WorkshopAttachmentCard(
                        attachment = attachment,
                        preview = previews[attachment.id],
                        onDownloadClick = { onDownloadAttachmentClick(attachment) },
                        onRetryPreviewClick = { onRetryPreviewClick(attachment.id) },
                        onPreviewCorrupted = { onPreviewCorrupted(attachment.id) },
                        onDeleteClick = if (isAdmin) { { onDeleteAttachmentClick(attachment) } } else null,
                        deleteEnabled = !state.busy
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationSection(
    state: WorkshopDetailUiState.Success,
    onRegisterClick: (String) -> Unit
) {
    val project = state.project
    val link = state.registrationLink
    when {
        link != null -> Button(onClick = { onRegisterClick(link) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + stringResource(R.string.workshop_register_now))
        }
        project.status == ProjectStatus.UPCOMING -> Notice(
            icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            text = stringResource(R.string.workshop_opens_notice, project.opensLabel(appLocale()))
        )
        project.status == ProjectStatus.OPEN -> Notice(
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            text = stringResource(R.string.workshop_open_no_link)
        )
        else -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + stringResource(R.string.workshop_registration_closed))
        }
    }
}

@Composable
private fun Notice(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row {
        Text(
            text = stringResource(R.string.workshop_detail_label, label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
