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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.fileMetaLine
import com.example.medrational_android.data.model.isImageFile
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.ui.components.AdaptiveCardList
import com.example.medrational_android.ui.components.ContentWidth
import com.example.medrational_android.ui.components.EmptyState
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.FileTypeBadge
import com.example.medrational_android.ui.components.HashtagPickerDialog
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.components.NoMatch
import com.example.medrational_android.ui.components.QuickSearchBar
import com.example.medrational_android.ui.components.RatingBadge
import com.example.medrational_android.ui.components.SwisraTopBar
import com.example.medrational_android.ui.components.TintedTile
import com.example.medrational_android.ui.components.adaptiveHorizontalPadding
import com.example.medrational_android.ui.files.ShareEffects
import com.example.medrational_android.ui.search.filterReasonings
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.SwisraTints
import com.example.medrational_android.viewmodel.BulkHashtagUiState
import com.example.medrational_android.viewmodel.BulkHashtagViewModel
import com.example.medrational_android.viewmodel.BulkTag
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.HashtagCatalogViewModel
import com.example.medrational_android.viewmodel.ReasoningUiState
import com.example.medrational_android.viewmodel.ReasoningViewModel
import com.example.medrational_android.viewmodel.ShareViewModel
import java.io.File

// Long protocols are folded to this many lines so the files stay in sight
private const val CONTENT_PREVIEW_LINES = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningDetailScreen(
    categoryId: Long,
    categoryName: String,
    // From the home list; null when the category has none (or the list isn't loaded)
    categoryDescription: String?,
    viewModel: ReasoningViewModel,
    // Opens the full search with what was typed, on this category
    onAdvancedSearch: (query: String) -> Unit,
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
    val ratings by FileRatingStore.shared.ratings.collectAsState()

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
    // Picked but not yet uploaded: the admin chooses their hashtags first
    var pendingUploadUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadDescription by rememberSaveable { mutableStateOf("") }
    // Deleting is permanent, so it always asks first
    var reasoningToDelete by remember { mutableStateOf<Reasoning?>(null) }
    var fileToDelete by remember { mutableStateOf<StudyFile?>(null) }

    // Only refreshed from admin controls: its endpoint is admin-only and a 403 logs the user out
    val catalogViewModel: HashtagCatalogViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        HashtagCatalogViewModel(ApiClient.api)
    }
    val hashtagCatalog by catalogViewModel.catalog.collectAsState()
    val bulkViewModel: BulkHashtagViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        BulkHashtagViewModel(ApiClient.api)
    }
    val bulkState by bulkViewModel.uiState.collectAsState()
    val shareViewModel: ShareViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        ShareViewModel(ApiClient.api, File(context.cacheDir, "shared"))
    }

    // Selection for sharing (everyone) and bulk hashtags (admins); a List so it can be saved across rotation
    var selection by rememberSaveable(stateSaver = FileSelection.Saver) { mutableStateOf(FileSelection()) }
    val selectedFileIds = selection.ids
    val selectionMode = selection.active
    val toggleSelection: (Long) -> Unit = { id -> selection = selection.toggle(id) }
    val clearSelection: () -> Unit = { selection = selection.clear() }
    var showBulkAddDialog by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = selectionMode) { clearSelection() }
    // The selection ends once the share sheet has opened
    ShareEffects(shareViewModel, onShared = clearSelection)

    val uploadMessage by viewModel.message.collectAsState()
    val bulkMessage by bulkViewModel.message.collectAsState()
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(bulkMessage) {
        bulkMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_LONG).show()
            bulkViewModel.onMessageShown()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && activeUploadReasoningId != null) {
            catalogViewModel.refresh()
            pendingUploadUris = uris
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                val busy = bulkState is BulkHashtagUiState.LoadingTags || bulkState is BulkHashtagUiState.Working
                val hasSelection = selectedFileIds.isNotEmpty()
                TopAppBar(
                    title = {
                        Text(
                            if (hasSelection) {
                                pluralStringResource(R.plurals.selection_count, selectedFileIds.size, selectedFileIds.size)
                            } else {
                                stringResource(R.string.selection_title_empty)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = clearSelection) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.selection_cancel))
                        }
                    },
                    actions = {
                        IconButton(enabled = hasSelection, onClick = {
                            val files = (uiState as? ReasoningUiState.Success)?.reasonings.orEmpty()
                                .flatMap { it.files.orEmpty() }
                                .associateBy { it.id }
                            shareViewModel.shareFiles(selectedFileIds.mapNotNull { files[it] })
                        }) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = stringResource(R.string.share_selected))
                        }
                        if (!isAdmin) return@TopAppBar
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.lg).size(24.dp))
                        } else {
                            IconButton(enabled = hasSelection, onClick = {
                                catalogViewModel.refresh()
                                showBulkAddDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.NewLabel, contentDescription = stringResource(R.string.selection_add_tags))
                            }
                            IconButton(enabled = hasSelection, onClick = { bulkViewModel.loadTagsFor(selectedFileIds.toSet()) }) {
                                Icon(imageVector = Icons.Default.LabelOff, contentDescription = stringResource(R.string.selection_remove_tags))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else {
                SwisraTopBar(title = categoryName, onBackClick = onBackClick) {
                    IconButton(onClick = {
                        shareViewModel.shareCategory(categoryId, context.getString(R.string.download_category_zip_name, categoryName))
                    }) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = stringResource(R.string.category_share))
                    }
                    IconButton(onClick = {
                        val categoryZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/category/$categoryId/zip"
                        val zipName = context.getString(R.string.download_category_zip_name, categoryName)
                        downloader.downloadFile(categoryZipUrl, zipName)
                        Toast.makeText(context, context.getString(R.string.download_started, zipName), Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.reasonings_download_category))
                    }
                }
            }
        },
        floatingActionButton = {
            if (isAdmin && !selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateReasoningDialog = true },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.reasoning_new_short)) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ReasoningUiState.Loading -> LoadingState()
                is ReasoningUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadReasoningsForCategory(categoryId) }
                )
                is ReasoningUiState.Success -> {
                    if (state.reasonings.isEmpty()) {
                        EmptyState(title = stringResource(R.string.reasonings_empty), icon = Icons.Outlined.Psychology)
                    } else {
                        val shown = remember(state.reasonings, query) { filterReasonings(state.reasonings, query) }
                        // Lines up with the single column of reasonings below
                        val sidePadding = adaptiveHorizontalPadding(ContentWidth.Reading)
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Above the list, so it stays at hand; hidden while picking files
                            if (!selectionMode) {
                                QuickSearchBar(
                                    query = query,
                                    onQueryChange = { query = it },
                                    placeholder = stringResource(R.string.reasonings_search_hint),
                                    onAdvancedSearch = { onAdvancedSearch(query.trim()) },
                                    modifier = Modifier.padding(start = sidePadding, end = sidePadding, top = Spacing.xs, bottom = Spacing.sm)
                                )
                            }
                            // One column even on tablets: reasonings differ a lot in height, and two
                            // ragged columns read badly. Room at the bottom for the admin button
                            AdaptiveCardList(modifier = Modifier.weight(1f), bottom = 96.dp, spacing = Spacing.lg, maxColumns = 1) {
                                // The home list may cut a long description; here it reads in full
                                if (!categoryDescription.isNullOrBlank()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        CategoryAboutCard(description = categoryDescription, categoryId = categoryId)
                                    }
                                }
                                if (shown.isEmpty()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        NoMatch(
                                            message = stringResource(R.string.reasonings_no_match, query.trim()),
                                            onAdvancedSearch = { onAdvancedSearch(query.trim()) }
                                        )
                                    }
                                } else {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = pluralStringResource(R.plurals.reasonings_count, shown.size, shown.size),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(shown, key = { it.id }) { reasoning ->
                                    ReasoningItemCard(
                                        reasoning = reasoning,
                                        isAdmin = isAdmin,
                                        favoritedIds = favoritedIds,
                                        ratings = ratings,
                                        onToggleFavorite = { fileId -> favoriteViewModel.toggleFavorite(fileId) },
                                        selectionMode = selectionMode,
                                        selectedFileIds = selectedFileIds,
                                        onStartSelection = { selection = selection.start() },
                                        onFileClick = { file ->
                                            if (selectionMode) toggleSelection(file.id) else onFileClick(reasoning.id, file.id)
                                        },
                                        // Long-press starts selection, to share several files (or tag them, for admins)
                                        onFileLongClick = { file -> toggleSelection(file.id) },
                                        onDeleteReasoning = { reasoningToDelete = reasoning },
                                        onAddFile = {
                                            activeUploadReasoningId = reasoning.id
                                            filePickerLauncher.launch("*/*")
                                        },
                                        onDeleteFile = { file -> fileToDelete = file },
                                        onDownloadReasoningZip = {
                                            val reasoningZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/reasoning/${reasoning.id}/zip"
                                            val zipName = context.getString(R.string.download_reasoning_zip_name, reasoning.title)
                                            downloader.downloadFile(reasoningZipUrl, zipName)
                                            Toast.makeText(context, context.getString(R.string.download_started, zipName), Toast.LENGTH_SHORT).show()
                                        },
                                        onDownloadSingleFile = { file ->
                                            val singleFileUrl = "${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}"
                                            downloader.downloadFile(singleFileUrl, file.fileName)
                                            Toast.makeText(context, context.getString(R.string.download_started, file.fileName), Toast.LENGTH_SHORT).show()
                                        },
                                        onShareReasoningZip = {
                                            shareViewModel.shareReasoning(
                                                reasoning.id,
                                                context.getString(R.string.download_reasoning_zip_name, reasoning.title)
                                            )
                                        },
                                        onShareSingleFile = { file -> shareViewModel.shareFiles(listOf(file)) },
                                        onSelectFile = { file -> toggleSelection(file.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reasoningToDelete?.let { reasoning ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.reasoning_delete),
            message = stringResource(R.string.reasoning_delete_confirm, reasoning.title),
            onConfirm = {
                viewModel.deleteReasoning(reasoning.id)
                reasoningToDelete = null
            },
            onDismissRequest = { reasoningToDelete = null }
        )
    }
    fileToDelete?.let { file ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.file_delete),
            message = stringResource(R.string.file_delete_confirm, file.fileName),
            onConfirm = {
                viewModel.deleteFile(file.id)
                fileToDelete = null
            },
            onDismissRequest = { fileToDelete = null }
        )
    }

    if (pendingUploadUris.isNotEmpty()) {
        val uris = pendingUploadUris
        HashtagPickerDialog(
            title = stringResource(R.string.upload_dialog_title),
            confirmLabel = stringResource(R.string.upload_dialog_confirm),
            catalog = hashtagCatalog,
            supportingText = stringResource(R.string.upload_dialog_tags_hint),
            allowEmpty = true,
            header = {
                PickedFilesHeader(uris)
                // One description for every picked file, like the tags
                OutlinedTextField(
                    value = uploadDescription,
                    onValueChange = { uploadDescription = it.take(MAX_DESCRIPTION_LENGTH) },
                    label = { Text(stringResource(R.string.upload_dialog_description_label)) },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onConfirm = { tags ->
                activeUploadReasoningId?.let { viewModel.uploadFiles(context, it, uris, tags, uploadDescription) }
                val progress = if (uris.size == 1) {
                    context.getString(R.string.upload_in_progress)
                } else {
                    context.resources.getQuantityString(R.plurals.upload_many_in_progress, uris.size, uris.size)
                }
                Toast.makeText(context, progress, Toast.LENGTH_SHORT).show()
                pendingUploadUris = emptyList()
                uploadDescription = ""
            },
            onDismissRequest = {
                pendingUploadUris = emptyList()
                uploadDescription = ""
            }
        )
    }

    if (showBulkAddDialog) {
        HashtagPickerDialog(
            title = pluralStringResource(R.plurals.bulk_tag_dialog_title, selectedFileIds.size, selectedFileIds.size),
            confirmLabel = stringResource(R.string.bulk_tag_dialog_confirm),
            catalog = hashtagCatalog,
            supportingText = stringResource(R.string.bulk_tag_dialog_hint),
            onConfirm = { tags ->
                bulkViewModel.addTags(selectedFileIds.toSet(), tags)
                showBulkAddDialog = false
                clearSelection()
            },
            onDismissRequest = { showBulkAddDialog = false }
        )
    }

    (bulkState as? BulkHashtagUiState.RemoveChoices)?.let { choices ->
        BulkRemoveHashtagsDialog(
            choices = choices,
            onConfirm = { tags ->
                bulkViewModel.removeTags(tags)
                clearSelection()
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
    // While selecting, every file shows a checkbox, even before one is ticked
    selectionMode: Boolean,
    selectedFileIds: List<Long>,
    onStartSelection: () -> Unit,
    onDeleteReasoning: () -> Unit,
    favoritedIds: Set<Long>,
    ratings: Map<Long, FileRatingSummary>,
    onToggleFavorite: (Long) -> Unit,
    onAddFile: () -> Unit,
    onDeleteFile: (StudyFile) -> Unit,
    onDownloadReasoningZip: () -> Unit,
    onDownloadSingleFile: (StudyFile) -> Unit,
    onShareReasoningZip: () -> Unit,
    onShareSingleFile: (StudyFile) -> Unit,
    onSelectFile: (StudyFile) -> Unit
) {
    val files = reasoning.files.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                TintedTile(tint = SwisraTints.forKey(reasoning.id), size = 40.dp) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = reasoning.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(top = Spacing.xxs)
                )
                ReasoningMenu(
                    hasFiles = files.isNotEmpty(),
                    isAdmin = isAdmin,
                    onDownloadAll = onDownloadReasoningZip,
                    onShareAll = onShareReasoningZip,
                    onAddFile = onAddFile,
                    onDelete = onDeleteReasoning
                )
            }

            if (!reasoning.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                ExpandableText(text = reasoning.content, stateKey = "reasoning-${reasoning.id}")
            }

            if (files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                // A visible way into selection, since few students know the long-press.
                // The row keeps the button's height while selecting, so the list doesn't jump
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reasoning_attached_files, files.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!selectionMode) {
                        TextButton(onClick = onStartSelection, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                            Icon(Icons.Outlined.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(stringResource(R.string.file_select))
                        }
                    }
                }
                files.forEachIndexed { index, file ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    StudyFileRow(
                        file = file,
                        isAdmin = isAdmin,
                        isFavorited = favoritedIds.contains(file.id),
                        knownRating = ratings[file.id],
                        onToggleFavorite = { onToggleFavorite(file.id) },
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        selectionMode = selectionMode,
                        isSelected = file.id in selectedFileIds,
                        onDeleteFile = { onDeleteFile(file) },
                        onDownloadFile = { onDownloadSingleFile(file) },
                        onShareFile = { onShareSingleFile(file) },
                        onSelectFile = { onSelectFile(file) }
                    )
                }
            }
        }
    }
}

/** The category's description, in a card of its own so it reads as the page's introduction. */
@Composable
internal fun CategoryAboutCard(description: String, categoryId: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.category_about),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            ExpandableText(text = description, stateKey = "category-$categoryId")
        }
    }
}

/** Long text cut to [CONTENT_PREVIEW_LINES] with a Show more / Show less toggle. */
@Composable
internal fun ExpandableText(text: String, stateKey: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(stateKey) { mutableStateOf(false) }
    var overflows by remember(stateKey) { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else CONTENT_PREVIEW_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(stringResource(if (expanded) R.string.reasoning_show_less else R.string.reasoning_show_more))
            }
        }
    }
}

// Reasoning actions live in a menu, so the title keeps the card's width
@Composable
private fun ReasoningMenu(
    hasFiles: Boolean,
    isAdmin: Boolean,
    onDownloadAll: () -> Unit,
    onShareAll: () -> Unit,
    onAddFile: () -> Unit,
    onDelete: () -> Unit
) {
    if (!hasFiles && !isAdmin) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.common_more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (hasFiles) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reasoning_download_all)) },
                    leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                    onClick = {
                        open = false
                        onDownloadAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reasoning_share_all)) },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = {
                        open = false
                        onShareAll()
                    }
                )
            }
            if (isAdmin) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reasoning_upload_file)) },
                    leadingIcon = { Icon(Icons.Outlined.UploadFile, contentDescription = null) },
                    onClick = {
                        open = false
                        onAddFile()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reasoning_delete), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        open = false
                        onDelete()
                    }
                )
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
    onShareFile: () -> Unit,
    onSelectFile: () -> Unit,
    // Latest rating from the shared store; the file's own figures are used until there is one
    knownRating: FileRatingSummary? = null,
    onLongClick: () -> Unit = {},
    // While selecting, rows show a checkbox and hide their actions
    selectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val isImage = isImageFile(file.fileName, file.fileType)
    val locale = appLocale()
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.sm, horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                selectionMode -> Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                isImage -> AsyncImage(
                    model = ApiClient.filePreviewUrl(file.id),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                else -> FileTypeBadge(fileName = file.fileName, fileType = file.fileType)
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingBadge(known = knownRating, avgRating = file.avgRating, totalRatings = file.totalRatings)
                    val meta = fileMetaLine(file.fileName, file.fileType, file.fileSizeBytes, null, locale)
                    if (meta.isNotEmpty()) {
                        Text(
                            text = " · $meta",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (selectionMode) return@Row
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorited) stringResource(R.string.favorite_remove) else stringResource(R.string.favorite_add),
                    tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.common_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_download)) },
                        leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDownloadFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_share)) },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onShareFile()
                        }
                    )
                    // Same as a long-press, for students who don't know that gesture
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.file_select)) },
                        leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSelectFile()
                        }
                    )
                    if (isAdmin) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.file_delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDeleteFile()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.common_cancel)) }
        }
    )
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
        title = { Text(stringResource(R.string.reasoning_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reasoning_title_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.reasoning_content_label)) },
                    minLines = 3,
                    shape = MaterialTheme.shapes.small
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onConfirm(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
        title = { Text(stringResource(R.string.bulk_remove_title)) },
        text = {
            if (choices.tags.isEmpty()) {
                Text(stringResource(R.string.bulk_remove_none))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = stringResource(R.string.bulk_remove_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                Text(stringResource(R.string.bulk_remove_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

// How many names the upload dialog lists before summing up the rest
private const val MAX_LISTED_UPLOADS = 5

private const val MAX_DESCRIPTION_LENGTH = 2000

// The picked file's name, or for several the count and the first few names
@Composable
private fun PickedFilesHeader(uris: List<Uri>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val names = remember(uris) { uris.take(MAX_LISTED_UPLOADS).map { displayName(context, it) } }
    if (uris.size == 1) {
        Text(
            text = names.first() ?: stringResource(R.string.upload_selected_file),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            text = pluralStringResource(R.plurals.upload_selected_count, uris.size, uris.size),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        names.forEach { name ->
            Text(
                text = name ?: stringResource(R.string.upload_selected_file),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val more = uris.size - names.size
        if (more > 0) {
            Text(
                text = pluralStringResource(R.plurals.upload_more_files, more, more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
