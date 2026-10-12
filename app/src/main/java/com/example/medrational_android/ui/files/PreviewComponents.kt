package com.example.medrational_android.ui.files

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medrational_android.data.preview.PdfDocumentRenderer

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
internal fun PdfPage(
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
internal fun PreviewMessage(
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

/**
 * Full-screen viewer with pinch-to-zoom. One finger keeps scrolling the pages; two fingers zoom and pan.
 * Double-tap toggles zoom.
 */
@Composable
internal fun FullScreenViewer(
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
