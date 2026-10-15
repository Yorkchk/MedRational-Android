package com.example.medrational_android.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medrational_android.R
import com.example.medrational_android.data.model.fileExtension
import com.example.medrational_android.data.preview.PdfDocumentRenderer
import com.example.medrational_android.ui.text.UiText
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.PreviewState
import java.util.Locale

private val THUMBNAIL_WIDTH = 96.dp
private val THUMBNAIL_HEIGHT = 128.dp
private const val A4_RATIO = 1f / 1.414f

/**
 * An attached document (project documents, suggestion files): a thumbnail of its first page or image,
 * its name and a Download button. Tapping a card whose preview is ready opens it full screen.
 */
@Composable
fun AttachmentPreviewCard(
    fileName: String,
    // null until the screen asked for it
    preview: PreviewState?,
    onDownloadClick: () -> Unit,
    onRetryPreviewClick: () -> Unit,
    onPreviewCorrupted: () -> Unit,
    modifier: Modifier = Modifier,
    // Admin only; null hides the delete button
    onDeleteClick: (() -> Unit)? = null,
    deleteEnabled: Boolean = true
) {
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
    val image = preview as? PreviewState.Image
    var imageFailed by remember(image) { mutableStateOf(false) }
    var fullScreen by rememberSaveable(fileName) { mutableStateOf(false) }
    val canOpen = renderer != null || (image != null && !imageFailed)

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = canOpen) { fullScreen = true }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = THUMBNAIL_WIDTH, height = THUMBNAIL_HEIGHT)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    renderer != null && renderer.pageCount > 0 -> PdfPage(
                        renderer = renderer,
                        index = 0,
                        renderWidthPx = with(LocalDensity.current) { (THUMBNAIL_WIDTH * 2).roundToPx() },
                        placeholderRatio = A4_RATIO
                    )
                    image != null && !imageFailed -> AsyncImage(
                        model = image.url,
                        contentDescription = fileName,
                        contentScale = ContentScale.Crop,
                        onError = { imageFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    preview == null || preview == PreviewState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                    preview is PreviewState.Unavailable -> Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Icon(
                        imageVector = fileTypeIcon(fileName, null),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attachmentMeta(fileName, preview, renderer?.pageCount, imageFailed).asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (preview is PreviewState.Unavailable) {
                    TextButton(onClick = onRetryPreviewClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(R.string.preview_retry))
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onDownloadClick,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.common_download))
                    }
                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDeleteClick, enabled = deleteEnabled) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.attachment_remove_named, fileName))
                        }
                    }
                }
            }
        }
    }

    if (fullScreen && canOpen) {
        val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
        FullScreenViewer(onDismissRequest = { fullScreen = false }) {
            when {
                renderer != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = renderer.pageCount, key = { it }) { index ->
                        PdfPage(
                            renderer = renderer,
                            index = index,
                            // Sharper bitmaps so text stays readable when zoomed in
                            renderWidthPx = screenWidthPx * 2,
                            placeholderRatio = A4_RATIO
                        )
                    }
                }
                image != null -> AsyncImage(
                    model = image.url,
                    contentDescription = fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// "PDF · 3 pages", "DOCX · 2 pages", "Image", "ZIP · No preview"
internal fun attachmentMeta(fileName: String, preview: PreviewState?, pageCount: Int?, imageFailed: Boolean): UiText {
    val type = fileExtension(fileName).uppercase(Locale.ROOT)
    val detail = when {
        preview is PreviewState.Image -> return UiText.Res(if (imageFailed) R.string.attachment_image_failed else R.string.attachment_image)
        pageCount != null -> UiText.Plural(R.plurals.attachment_pages, pageCount)
        preview == null || preview == PreviewState.Loading -> UiText.Res(R.string.preview_preparing)
        preview is PreviewState.Unavailable -> UiText.Res(R.string.preview_unavailable)
        preview == PreviewState.NotSupported -> UiText.Res(R.string.attachment_no_preview)
        else -> null
    }
    return when {
        detail == null -> UiText.Res(R.string.attachment_type, listOf(type))
        type.isEmpty() -> detail
        else -> UiText.Res(R.string.attachment_meta, listOf(type, detail))
    }
}
