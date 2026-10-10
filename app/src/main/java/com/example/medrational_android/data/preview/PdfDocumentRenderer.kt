package com.example.medrational_android.data.preview

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/**
 * Renders the pages of a local PDF to bitmaps. PdfRenderer allows only one open page at a time,
 * so every call goes through a mutex.
 */
class PdfDocumentRenderer(file: File) : Closeable {

    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()

    @Volatile
    private var closed = false
    private var released = false

    // Keeps a few pages around so scrolling back and forth doesn't re-render them
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    val pageCount: Int = renderer.pageCount

    /** Page width / height, used to reserve the right space before the bitmap is ready. */
    suspend fun pageAspectRatio(index: Int): Float =
        withOpenRenderer { r -> r.openPage(index).use { page -> page.width.toFloat() / page.height } } ?: A4_RATIO

    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? {
        val width = targetWidthPx.coerceIn(1, MAX_PAGE_WIDTH_PX)
        val key = "$index@$width"
        cache.get(key)?.let { return it }

        return withOpenRenderer { r ->
            r.openPage(index).use { page ->
                val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // Pages without a background would otherwise render transparent (black in dark mode)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(key, bitmap)
                bitmap
            }
        }
    }

    // Returns null once the document is closed
    private suspend fun <T> withOpenRenderer(block: (PdfRenderer) -> T): T? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (closed) null else block(renderer)
            } finally {
                if (closed) release()
            }
        }
    }

    // Safe from the main thread: if a page is being rendered, that render releases the document when done
    override fun close() {
        closed = true
        if (mutex.tryLock()) {
            try {
                release()
            } finally {
                mutex.unlock()
            }
        }
        cache.evictAll()
    }

    private fun release() {
        if (released) return
        released = true
        renderer.close()
        descriptor.close()
    }

    companion object {
        private const val MAX_PAGE_WIDTH_PX = 1440
        private const val CACHE_BYTES = 48 * 1024 * 1024
        private const val A4_RATIO = 1f / 1.414f
    }
}
