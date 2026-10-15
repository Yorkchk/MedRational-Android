package com.example.medrational_android.ui.files

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.fileExtension
import com.example.medrational_android.data.model.fileMetaLine
import com.example.medrational_android.data.model.fileNameWithoutExtension
import com.example.medrational_android.data.preview.PdfDocumentRenderer
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.FileTypeBadge
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.FileDetailUiState
import com.example.medrational_android.viewmodel.FileDetailViewModel
import com.example.medrational_android.viewmodel.FileHashtagsViewModel
import com.example.medrational_android.viewmodel.FileRatingUiState
import com.example.medrational_android.viewmodel.FileRatingViewModel
import com.example.medrational_android.viewmodel.HashtagCatalogViewModel
import com.example.medrational_android.viewmodel.HashtagsUiState
import com.example.medrational_android.viewmodel.PreviewState
import com.example.medrational_android.viewmodel.ShareState
import java.io.File

private const val PAGE_KEY_PREFIX = "page-"
private const val A4_RATIO = 1f / 1.414f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    reasoningId: Long,
    fileId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }
    val tokenManager = remember { TokenManager(context) }
    val viewModel: FileDetailViewModel = viewModel {
        FileDetailViewModel(
            ApiClient.api,
            File(context.cacheDir, "previews"),
            recordViews = tokenManager.isLoggedIn() && !tokenManager.isAdmin(),
            shareDir = File(context.cacheDir, "shared")
        )
    }
    val favoriteViewModel: FavoriteViewModel = viewModel { FavoriteViewModel(tokenManager) }
    val hashtagsViewModel: FileHashtagsViewModel = viewModel { FileHashtagsViewModel(ApiClient.api) }
    val ratingViewModel: FileRatingViewModel = viewModel { FileRatingViewModel(ApiClient.api) }
    val isAdmin = remember { tokenManager.isAdmin() }
    // Only refreshed from admin controls: its endpoint is admin-only and a 403 logs the user out
    val catalogViewModel: HashtagCatalogViewModel = viewModel { HashtagCatalogViewModel(ApiClient.api) }
    val hashtagCatalog by catalogViewModel.catalog.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val favoritedIds by favoriteViewModel.favoritedFileIds.collectAsState()
    val hashtagsState by hashtagsViewModel.uiState.collectAsState()
    val ratingState by ratingViewModel.uiState.collectAsState()
    val shareState by viewModel.shareState.collectAsState()

    LaunchedEffect(reasoningId, fileId) {
        viewModel.load(reasoningId, fileId)
        favoriteViewModel.loadFavorites()
        hashtagsViewModel.load(fileId)
        ratingViewModel.load(fileId)
    }

    val ratingMessage = (ratingState as? FileRatingUiState.Success)?.message
    LaunchedEffect(ratingMessage) {
        if (ratingMessage != null) {
            Toast.makeText(context, ratingMessage.asString(context), Toast.LENGTH_LONG).show()
            ratingViewModel.onMessageShown()
        }
    }

    LaunchedEffect(shareState) {
        when (val state = shareState) {
            is ShareState.Ready -> {
                context.shareFile(state.file, state.mimeType)
                viewModel.onShareHandled()
            }
            is ShareState.Error -> {
                Toast.makeText(context, state.message.asString(context), Toast.LENGTH_LONG).show()
                viewModel.onShareHandled()
            }
            ShareState.Idle, ShareState.Preparing -> Unit
        }
    }
    val isSharing = shareState == ShareState.Preparing

    val loadedHashtags = hashtagsState as? HashtagsUiState.Success
    val showHashtags = loadedHashtags != null && (isAdmin || loadedHashtags.hashtags.isNotEmpty())
    val hashtagMessage = loadedHashtags?.message
    LaunchedEffect(hashtagMessage) {
        if (hashtagMessage != null) {
            Toast.makeText(context, hashtagMessage.asString(context), Toast.LENGTH_LONG).show()
            hashtagsViewModel.onMessageShown()
        }
    }

    val listState = rememberLazyListState()
    // The file name moves into the top bar once the in-page title has scrolled away
    val showTitleInBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val success = uiState as? FileDetailUiState.Success

    val onDownload: (StudyFile) -> Unit = { file ->
        downloader.downloadFile("${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}", file.fileName)
        Toast.makeText(context, context.getString(R.string.download_started, file.fileName), Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (showTitleInBar && success != null) {
                        Text(
                            text = success.file.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // The header shows these buttons; the bar takes them over once it has scrolled away
                    if (success != null && showTitleInBar) {
                        val isFavorited = favoritedIds.contains(success.file.id)
                        IconButton(onClick = { favoriteViewModel.toggleFavorite(success.file.id) }) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorited) stringResource(R.string.favorite_remove) else stringResource(R.string.favorite_add),
                                tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDownload(success.file) }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.common_download))
                        }
                        IconButton(onClick = { viewModel.share(success.file) }, enabled = !isSharing) {
                            ShareIcon(isSharing = isSharing)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is FileDetailUiState.Loading -> LoadingState()
                is FileDetailUiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.load(reasoningId, fileId) })
                is FileDetailUiState.Success -> FileDetailContent(
                    state = state,
                    listState = listState,
                    isFavorited = favoritedIds.contains(state.file.id),
                    onToggleFavorite = { favoriteViewModel.toggleFavorite(state.file.id) },
                    onRetryPreview = viewModel::retryPreview,
                    onPreviewCorrupted = viewModel::onPreviewCorrupted,
                    onDownload = { onDownload(state.file) },
                    isSharing = isSharing,
                    onShare = { viewModel.share(state.file) },
                    rating = {
                        RatingSection(
                            state = ratingState,
                            onRate = ratingViewModel::rate,
                            onRemoveRating = ratingViewModel::removeRating,
                            onRetry = { ratingViewModel.load(fileId) },
                            modifier = Modifier.padding(horizontal = Spacing.screen)
                        )
                    },
                    // Skipped entirely when hidden so the list doesn't get an empty, spaced-out row
                    hashtags = if (!showHashtags) null else {
                        {
                            SectionCard(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                                HashtagSection(
                                    state = hashtagsState,
                                    isAdmin = isAdmin,
                                    onAddHashtags = hashtagsViewModel::addHashtags,
                                    onRenameHashtag = hashtagsViewModel::renameHashtag,
                                    onRemoveHashtag = hashtagsViewModel::removeHashtag,
                                    catalog = hashtagCatalog,
                                    onRefreshCatalog = catalogViewModel::refresh
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.FileDetailContent(
    state: FileDetailUiState.Success,
    listState: LazyListState,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onRetryPreview: () -> Unit,
    onPreviewCorrupted: () -> Unit,
    onDownload: () -> Unit,
    isSharing: Boolean,
    onShare: () -> Unit,
    rating: @Composable () -> Unit,
    hashtags: (@Composable () -> Unit)?
) {
    val preview = state.preview
    val pdfFile = (preview as? PreviewState.Pdf)?.file
    val renderer = remember(pdfFile) {
        pdfFile?.let { runCatching { PdfDocumentRenderer(it) }.getOrNull() }
    }
    DisposableEffect(renderer) {
        onDispose { renderer?.close() }
    }
    if (pdfFile != null && renderer == null) {
        LaunchedEffect(pdfFile) { onPreviewCorrupted() }
    }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    var firstPageRatio by remember(renderer) { mutableFloatStateOf(A4_RATIO) }
    LaunchedEffect(renderer) {
        if (renderer != null && renderer.pageCount > 0) firstPageRatio = renderer.pageAspectRatio(0)
    }

    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var imageFailed by remember(preview) { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.xs, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "header") {
            FileHeader(
                file = state.file,
                reasoning = state.reasoning,
                isFavorited = isFavorited,
                onToggleFavorite = onToggleFavorite,
                onDownload = onDownload,
                isSharing = isSharing,
                onShare = onShare,
                modifier = Modifier.padding(horizontal = Spacing.screen)
            )
        }
        item(key = "rating") { rating() }
        if (hashtags != null) {
            item(key = "hashtags") { hashtags() }
        }
        item(key = "reasoning") {
            ReasoningCard(reasoning = state.reasoning, modifier = Modifier.padding(horizontal = Spacing.screen))
        }

        when (preview) {
            is PreviewState.Pdf -> if (renderer != null) {
                item(key = "preview-header") {
                    PreviewSectionHeader(
                        pageCount = renderer.pageCount,
                        onFullScreen = { fullScreen = true },
                        modifier = Modifier.padding(start = Spacing.screen, end = Spacing.sm)
                    )
                }
                items(count = renderer.pageCount, key = { PAGE_KEY_PREFIX + it }) { index ->
                    PdfPage(
                        renderer = renderer,
                        index = index,
                        renderWidthPx = screenWidthPx,
                        placeholderRatio = firstPageRatio,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            is PreviewState.Image -> if (imageFailed) {
                // e.g. HEIC before Android 9 or AVIF before Android 12, which the OS can't decode
                item(key = "image-failed") {
                    PreviewMessage(
                        title = stringResource(R.string.preview_unavailable),
                        body = stringResource(R.string.preview_image_failed),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        leading = { Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(32.dp)) },
                        action = { Button(onClick = onDownload) { Text(stringResource(R.string.common_download)) } }
                    )
                }
            } else {
                item(key = "image") {
                    Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                        PreviewSectionHeader(pageCount = null, onFullScreen = { fullScreen = true })
                        AsyncImage(
                            model = preview.url,
                            contentDescription = state.file.fileName,
                            contentScale = ContentScale.FillWidth,
                            onError = { imageFailed = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { fullScreen = true }
                        )
                    }
                }
            }
            PreviewState.Loading -> item(key = "preview-loading") {
                PreviewMessage(
                    title = stringResource(R.string.preview_preparing),
                    body = stringResource(R.string.preview_preparing_hint),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                )
            }
            PreviewState.NotSupported -> item(key = "preview-unsupported") {
                val extension = fileExtension(state.file.fileName)
                PreviewMessage(
                    title = if (extension.isNotEmpty()) stringResource(R.string.preview_none_for_extension, extension) else stringResource(R.string.preview_none),
                    body = stringResource(R.string.preview_supported_types),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(32.dp)) },
                    action = { Button(onClick = onDownload) { Text(stringResource(R.string.common_download)) } }
                )
            }
            is PreviewState.Unavailable -> item(key = "preview-unavailable") {
                PreviewMessage(
                    title = stringResource(R.string.preview_unavailable),
                    body = preview.message.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(32.dp)) },
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDownload) { Text(stringResource(R.string.common_download)) }
                            Button(onClick = onRetryPreview) { Text(stringResource(R.string.common_retry)) }
                        }
                    }
                )
            }
        }
    }

    if (renderer != null) {
        PageIndicator(listState = listState, pageCount = renderer.pageCount, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (fullScreen) {
        FullScreenViewer(onDismissRequest = { fullScreen = false }) {
            when {
                renderer != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = renderer.pageCount, key = { PAGE_KEY_PREFIX + it }) { index ->
                        PdfPage(
                            renderer = renderer,
                            index = index,
                            // Sharper bitmaps so text stays readable when zoomed in
                            renderWidthPx = screenWidthPx * 2,
                            placeholderRatio = firstPageRatio
                        )
                    }
                }
                preview is PreviewState.Image -> AsyncImage(
                    model = preview.url,
                    contentDescription = state.file.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FileHeader(
    file: StudyFile,
    reasoning: Reasoning,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    isSharing: Boolean,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FileTypeBadge(fileName = file.fileName, fileType = file.fileType, size = 52.dp)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reasoning.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fileNameWithoutExtension(file.fileName),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        val meta = fileMetaLine(file.fileName, file.fileType, file.fileSizeBytes, file.uploadedAt, appLocale())
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = onDownload, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(stringResource(R.string.common_download))
            }
            FilledTonalButton(onClick = onToggleFavorite, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorited) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(stringResource(if (isFavorited) R.string.file_in_favorites else R.string.favorite_add_short))
            }
            FilledTonalIconButton(onClick = onShare, enabled = !isSharing, shape = MaterialTheme.shapes.medium) {
                ShareIcon(isSharing = isSharing)
            }
        }
    }
}

// A spinner while the file is fetched for the share sheet
@Composable
private fun ShareIcon(isSharing: Boolean) {
    if (isSharing) {
        val preparing = stringResource(R.string.share_preparing)
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier
                .size(18.dp)
                .semantics { contentDescription = preparing }
        )
    } else {
        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.common_share))
    }
}

/** A white rounded card for one block of the file page. */
@Composable
internal fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}

// Clamped to a few lines so a long description never pushes the preview off a small screen
@Composable
private fun ReasoningCard(reasoning: Reasoning, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)) {
            Text(
                text = stringResource(R.string.file_from_reasoning),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = reasoning.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!reasoning.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reasoning.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result -> if (!expanded) overflows = result.hasVisualOverflow }
                )
            }
            if (overflows || expanded) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) stringResource(R.string.show_less) else stringResource(R.string.show_more))
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PreviewSectionHeader(
    pageCount: Int?,
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = when (pageCount) {
                null -> stringResource(R.string.preview_title)
                else -> pluralStringResource(R.plurals.preview_title_pages, pageCount, pageCount)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onFullScreen) {
            Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.preview_open_fullscreen))
        }
    }
}

// "3 / 12" pill for the page under the middle of the screen
@Composable
private fun PageIndicator(listState: LazyListState, pageCount: Int, modifier: Modifier = Modifier) {
    val currentPage by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .firstOrNull { item ->
                    (item.key as? String)?.startsWith(PAGE_KEY_PREFIX) == true &&
                        item.offset <= center && item.offset + item.size >= center
                }
                ?.let { (it.key as String).removePrefix(PAGE_KEY_PREFIX).toInt() + 1 }
        }
    }

    AnimatedVisibility(
        visible = currentPage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 4.dp
        ) {
            Text(
                text = "${currentPage ?: 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
