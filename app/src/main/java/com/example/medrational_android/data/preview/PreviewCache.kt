package com.example.medrational_android.data.preview

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException

/**
 * Saves a downloaded preview as [target]. Writes to a temp file first so an interrupted download
 * never leaves a broken cached PDF.
 */
suspend fun saveToCache(body: ResponseBody, target: File, ioDispatcher: CoroutineDispatcher): File =
    withContext(ioDispatcher) {
        val dir = requireNotNull(target.parentFile)
        dir.mkdirs()
        val temp = File(dir, "${target.name}.part")
        try {
            body.use { source ->
                temp.outputStream().use { sink -> source.byteStream().copyTo(sink) }
            }
            if (!temp.renameTo(target)) throw IOException("Couldn't save the preview.")
        } finally {
            temp.delete()
        }
        target
    }
