package com.example.medrational_android.ui.files

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.fileExtension
import com.example.medrational_android.data.model.fileMetaLine
import com.example.medrational_android.data.model.fileNameWithoutExtension
import com.example.medrational_android.data.preview.PdfDocumentRenderer
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.FileDetailUiState
import com.example.medrational_android.viewmodel.FileDetailViewModel
import com.example.medrational_android.viewmodel.PreviewState
import java.io.File

private const val PAGE_KEY_PREFIX = "page-"
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
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
        FileDetailViewModel(ApiClient.api, File(context.cacheDir, "previews"))
    }
    val favoriteViewModel: FavoriteViewModel = viewModel { FavoriteViewModel(tokenManager) }

    val uiState by viewModel.uiState.collectAsState()
    val favoritedIds by favoriteViewModel.favoritedFileIds.collectAsState()

    LaunchedEffect(reasoningId, fileId) {
        viewModel.load(reasoningId, fileId)
        favoriteViewModel.loadFavorites()
    }

    val listState = rememberLazyListState()
    // The file name moves into the top bar once the in-page title has scrolled away
    val showTitleInBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val success = uiState as? FileDetailUiState.Success

    val onDownload: (StudyFile) -> Unit = { file ->
        downloader.downloadFile("${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}", file.fileName)
        Toast.makeText(context, "Downloading ${file.fileName}...", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (success != null) {
                        val isFavorited = favoritedIds.contains(success.file.id)
                        IconButton(onClick = { favoriteViewModel.toggleFavorite(success.file.id) }) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDownload(success.file) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
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
                is FileDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is FileDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(reasoningId, fileId) }) { Text("Retry") }
                    }
                }
                is FileDetailUiState.Success -> FileDetailContent(
                    state = state,
                    listState = listState,
                    onRetryPreview = viewModel::retryPreview,
                    onPreviewCorrupted = viewModel::onPreviewCorrupted,
                    onDownload = { onDownload(state.file) }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.FileDetailContent(
    state: FileDetailUiState.Success,
    listState: LazyListState,
    onRetryPreview: () -> Unit,
    onPreviewCorrupted: () -> Unit,
    onDownload: () -> Unit
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

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            FileHeader(file = state.file, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        item(key = "reasoning") {
            ReasoningCard(reasoning = state.reasoning, modifier = Modifier.padding(horizontal = 16.dp))
        }

        when (preview) {
            is PreviewState.Pdf -> if (renderer != null) {
                item(key = "preview-header") {
                    PreviewSectionHeader(
                        pageCount = renderer.pageCount,
                        onFullScreen = { fullScreen = true },
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
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
            is PreviewState.Image -> item(key = "image") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PreviewSectionHeader(pageCount = null, onFullScreen = { fullScreen = true })
                    AsyncImage(
                        model = preview.url,
                        contentDescription = state.file.fileName,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clickable { fullScreen = true }
                    )
                }
            }
            PreviewState.Loading -> item(key = "preview-loading") {
                PreviewMessage(
                    title = "Preparing preview…",
                    body = "Office documents can take a few seconds the first time.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                )
            }
            PreviewState.NotSupported -> item(key = "preview-unsupported") {
                val extension = fileExtension(state.file.fileName)
                PreviewMessage(
                    title = if (extension.isNotEmpty()) "No preview for .$extension files" else "No preview for this file",
                    body = "Previews are available for PDF, Word, PowerPoint and Excel files. Download the file to open it.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(32.dp)) },
                    action = { Button(onClick = onDownload) { Text("Download") } }
                )
            }
            is PreviewState.Unavailable -> item(key = "preview-unavailable") {
                PreviewMessage(
                    title = "Preview unavailable",
                    body = preview.message,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leading = { Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(32.dp)) },
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDownload) { Text("Download") }
                            Button(onClick = onRetryPreview) { Text("Retry") }
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
private fun FileHeader(file: StudyFile, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = fileNameWithoutExtension(file.fileName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        val meta = fileMetaLine(file.fileName, file.fileType, file.fileSizeBytes, file.uploadedAt)
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Clamped to a few lines so a long description never pushes the preview off a small screen
@Composable
private fun ReasoningCard(reasoning: Reasoning, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = "From reasoning",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
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
                    Text(if (expanded) "Show less" else "Show more")
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
                null -> "Preview"
                1 -> "Preview · 1 page"
                else -> "Preview · $pageCount pages"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onFullScreen) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Open full screen")
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfDocumentRenderer,
    index: Int,
    renderWidthPx: Int,
    placeholderRatio: Float,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(renderer, index, renderWidthPx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(renderer, index, renderWidthPx) {
        bitmap = renderer.renderPage(index, renderWidthPx)
    }
    val ratio = bitmap?.let { it.width.toFloat() / it.height } ?: placeholderRatio

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio),
        shape = RoundedCornerShape(4.dp),
        // PDF pages are white paper regardless of the app theme
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PreviewMessage(
    title: String,
    body: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    OutlinedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            leading()
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
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
            shape = RoundedCornerShape(50),
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

/**
 * Full-screen viewer with pinch-to-zoom. One finger keeps scrolling the pages; two fingers zoom and pan.
 * Double-tap toggles zoom.
 */
@Composable
private fun FullScreenViewer(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        fun clampOffset(value: Offset, currentScale: Float): Offset {
            val maxX = size.width * (currentScale - 1f) / 2f
            val maxY = size.height * (currentScale - 1f) / 2f
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        offset = clampOffset(offset, scale)
                    })
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            // Initial pass: see the gesture before the page list scrolls with it
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pan = event.calculatePan()
                            if (event.changes.size > 1) {
                                scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                                offset = clampOffset(offset + pan, scale)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (scale > 1f) {
                                // Sideways pan while zoomed; vertical movement still scrolls the list
                                offset = clampOffset(offset.copy(x = offset.x + pan.x), scale)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                content()
            }

            FilledTonalIconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close full screen")
            }
            if (scale > 1f) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(scale)}×",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
