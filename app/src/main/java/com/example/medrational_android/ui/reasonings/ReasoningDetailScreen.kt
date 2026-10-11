package com.example.medrational_android.ui.reasonings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.isImageFile
import com.example.medrational_android.ui.components.HashtagPickerDialog
import com.example.medrational_android.ui.files.fileTypeIcon
import com.example.medrational_android.viewmodel.BulkHashtagUiState
import com.example.medrational_android.viewmodel.BulkHashtagViewModel
import com.example.medrational_android.viewmodel.BulkTag
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.HashtagCatalogViewModel
import com.example.medrational_android.viewmodel.ReasoningUiState
import com.example.medrational_android.viewmodel.ReasoningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningDetailScreen(
    categoryId: Long,
    categoryName: String,
    viewModel: ReasoningViewModel,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }
    val tokenManager = remember { TokenManager(context) }
    val favoriteViewModel: FavoriteViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        FavoriteViewModel(tokenManager)
    }
    val favoritedIds by favoriteViewModel.favoritedFileIds.collectAsState()

    // Re-runs whenever this destination re-enters composition (e.g. back from Favorites),
    // so hearts always reflect the latest server state
    LaunchedEffect(categoryId) {
        viewModel.loadReasoningsForCategory(categoryId)
        favoriteViewModel.loadFavorites()
    }

    // Check if the current user holds the admin role
    val isAdmin = remember {
        val role = tokenManager.getRole() ?: ""
        role.contains("ADMIN", ignoreCase = true)
    }

    var showCreateReasoningDialog by remember { mutableStateOf(false) }
    var activeUploadReasoningId by remember { mutableStateOf<Long?>(null) }
    // Picked but not yet uploaded: the admin chooses its hashtags first
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }

    // Only refreshed from admin controls: its endpoint is admin-only and a 403 logs the user out
    val catalogViewModel: HashtagCatalogViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        HashtagCatalogViewModel(ApiClient.api)
    }
    val hashtagCatalog by catalogViewModel.catalog.collectAsState()
    val bulkViewModel: BulkHashtagViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        BulkHashtagViewModel(ApiClient.api)
    }
    val bulkState by bulkViewModel.uiState.collectAsState()

    // Admin bulk selection; a List so it can be saved across rotation
    var selectedFileIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val selectionMode = selectedFileIds.isNotEmpty()
    val toggleSelection: (Long) -> Unit = { id ->
        selectedFileIds = if (id in selectedFileIds) selectedFileIds - id else selectedFileIds + id
    }
    var showBulkAddDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = selectionMode) { selectedFileIds = emptyList() }

    val uploadMessage by viewModel.message.collectAsState()
    val bulkMessage by bulkViewModel.message.collectAsState()
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(bulkMessage) {
        bulkMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            bulkViewModel.onMessageShown()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeUploadReasoningId != null) {
            catalogViewModel.refresh()
            pendingUploadUri = uri
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                val busy = bulkState is BulkHashtagUiState.LoadingTags || bulkState is BulkHashtagUiState.Working
                TopAppBar(
                    title = { Text("${selectedFileIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedFileIds = emptyList() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp))
                        } else {
                            IconButton(onClick = {
                                catalogViewModel.refresh()
                                showBulkAddDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.NewLabel, contentDescription = "Add hashtags to selected files")
                            }
                            IconButton(onClick = { bulkViewModel.loadTagsFor(selectedFileIds.toSet()) }) {
                                Icon(imageVector = Icons.Default.LabelOff, contentDescription = "Remove hashtags from selected files")
                            }
                        }
                    }
                )
            } else TopAppBar(
                title = { Text(text = categoryName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val categoryZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/category/$categoryId/zip"
                        downloader.downloadFile(categoryZipUrl, "${categoryName}_Materials.zip")
                        Toast.makeText(context, "Downloading $categoryName ZIP...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download Category ZIP")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showCreateReasoningDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Clinical Reasoning")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ReasoningUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ReasoningUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadReasoningsForCategory(categoryId) }) { Text("Retry") }
                    }
                }
                is ReasoningUiState.Success -> {
                    if (state.reasonings.isEmpty()) {
                        Text(
                            text = "No reasonings added for this category.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.reasonings, key = { it.id }) { reasoning ->
                                ReasoningItemCard(
                                    reasoning = reasoning,
                                    isAdmin = isAdmin,
                                    favoritedIds = favoritedIds, // <-- Pass the set of favorited IDs
                                    onToggleFavorite = { fileId -> favoriteViewModel.toggleFavorite(fileId) }, // <-- Pass the toggle lambda
                                    selectedFileIds = selectedFileIds,
                                    onFileClick = { file ->
                                        if (selectionMode) toggleSelection(file.id) else onFileClick(reasoning.id, file.id)
                                    },
                                    // Long-press starts bulk hashtag selection (admins only)
                                    onFileLongClick = { file -> if (isAdmin) toggleSelection(file.id) },
                                    onDeleteReasoning = { viewModel.deleteReasoning(reasoning.id) },
                                    onAddFile = {
                                        activeUploadReasoningId = reasoning.id
                                        filePickerLauncher.launch("*/*")
                                    },
                                    onDeleteFile = { fileId -> viewModel.deleteFile(fileId) },
                                    onDownloadReasoningZip = {
                                        val reasoningZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/reasoning/${reasoning.id}/zip"
                                        downloader.downloadFile(reasoningZipUrl, "${reasoning.title}_Files.zip")
                                        Toast.makeText(context, "Downloading ${reasoning.title} files...", Toast.LENGTH_SHORT).show()
                                    },
                                    onDownloadSingleFile = { file ->
                                        val singleFileUrl = "${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}"
                                        downloader.downloadFile(singleFileUrl, file.fileName)
                                        Toast.makeText(context, "Downloading ${file.fileName}...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUploadUri?.let { uri ->
        val fileName = remember(uri) { displayName(context, uri) }
        HashtagPickerDialog(
            title = "Upload file",
            confirmLabel = "Upload",
            catalog = hashtagCatalog,
            supportingText = "Optional. Separate several tags with spaces or commas",
            allowEmpty = true,
            header = {
                Text(
                    text = fileName ?: "Selected file",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            onConfirm = { tags ->
                activeUploadReasoningId?.let { viewModel.uploadFile(context, it, uri, tags) }
                Toast.makeText(context, "Uploading file...", Toast.LENGTH_SHORT).show()
                pendingUploadUri = null
            },
            onDismissRequest = { pendingUploadUri = null }
        )
    }

    if (showBulkAddDialog) {
        HashtagPickerDialog(
            title = "Tag ${selectedFileIds.size} files",
            confirmLabel = "Add",
            catalog = hashtagCatalog,
            supportingText = "Added to every selected file",
            onConfirm = { tags ->
                bulkViewModel.addTags(selectedFileIds.toSet(), tags)
                showBulkAddDialog = false
                selectedFileIds = emptyList()
            },
            onDismissRequest = { showBulkAddDialog = false }
        )
    }

    (bulkState as? BulkHashtagUiState.RemoveChoices)?.let { choices ->
        BulkRemoveHashtagsDialog(
            choices = choices,
            onConfirm = { tags ->
                bulkViewModel.removeTags(tags)
                selectedFileIds = emptyList()
            },
            onDismissRequest = bulkViewModel::dismiss
        )
    }

    if (showCreateReasoningDialog) {
        CreateReasoningDialog(
            onDismiss = { showCreateReasoningDialog = false },
            onConfirm = { title, content ->
                viewModel.createReasoning(title, content)
                showCreateReasoningDialog = false
            }
        )
    }
}

@Composable
fun ReasoningItemCard(
    reasoning: Reasoning,
    isAdmin: Boolean,
    onFileClick: (StudyFile) -> Unit,
    onFileLongClick: (StudyFile) -> Unit,
    selectedFileIds: List<Long>,
    onDeleteReasoning: () -> Unit,
    favoritedIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onAddFile: () -> Unit,
    onDeleteFile: (Long) -> Unit,
    onDownloadReasoningZip: () -> Unit,
    onDownloadSingleFile: (StudyFile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reasoning.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    if (!reasoning.files.isNullOrEmpty()) {
                        IconButton(onClick = onDownloadReasoningZip) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download all files",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isAdmin) {
                        IconButton(onClick = onAddFile) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Upload Study File",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeleteReasoning) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete reasoning",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (!reasoning.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = reasoning.content, style = MaterialTheme.typography.bodyMedium)
            }

            if (!reasoning.files.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Attached Files (${reasoning.files.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                reasoning.files?.forEach { file ->
                    StudyFileRow(
                        file = file,
                        isAdmin = isAdmin,
                        isFavorited = favoritedIds.contains(file.id), // <-- Fixes 'No value passed for parameter isFavorited'
                        onToggleFavorite = { onToggleFavorite(file.id) }, // <-- Fixes 'No value passed for parameter onToggleFavorite'
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        selectionMode = selectedFileIds.isNotEmpty(),
                        isSelected = file.id in selectedFileIds,
                        onDeleteFile = { onDeleteFile(file.id) },
                        onDownloadFile = { onDownloadSingleFile(file) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudyFileRow(
    file: StudyFile,
    isAdmin: Boolean,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onDeleteFile: () -> Unit,
    onDownloadFile: () -> Unit,
    onLongClick: () -> Unit = {},
    // While selecting, rows show a checkbox and hide their actions
    selectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val isImage = isImageFile(file.fileName, file.fileType)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else if (isImage) {
                AsyncImage(
                    model = ApiClient.filePreviewUrl(file.id),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = fileTypeIcon(file.fileName, file.fileType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isImage || file.previewable) "Tap to preview" else "Tap to open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectionMode) return@Row
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDownloadFile) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary
                )
            }


            if (isAdmin) {
                IconButton(onClick = onDeleteFile) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete file",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CreateReasoningDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Clinical Reasoning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Diagnosis Algorithm") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Algorithm / Protocol Steps") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onConfirm(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Shows each tag on the selected files with how many of them carry it; the admin ticks which to remove
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkRemoveHashtagsDialog(
    choices: BulkHashtagUiState.RemoveChoices,
    onConfirm: (List<BulkTag>) -> Unit,
    onDismissRequest: () -> Unit
) {
    var checked by rememberSaveable { mutableStateOf(emptyList<String>()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Remove hashtags") },
        text = {
            if (choices.tags.isEmpty()) {
                Text("The selected files have no hashtags.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Removed from each selected file that has them. A tag no other file uses is deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        choices.tags.forEach { tag ->
                            val isChecked = tag.name in checked
                            FilterChip(
                                selected = isChecked,
                                onClick = { checked = if (isChecked) checked - tag.name else checked + tag.name },
                                label = { Text("#${tag.name} · ${tag.hashtagIdByFile.size}/${choices.fileCount}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(choices.tags.filter { it.name in checked }) },
                enabled = checked.isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
