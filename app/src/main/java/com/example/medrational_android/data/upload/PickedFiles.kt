package com.example.medrational_android.data.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Matches the backend's suggestions.attachments.max-size
const val MAX_SUGGESTION_ATTACHMENT_BYTES = 10L * 1024 * 1024

/** A file chosen in the system picker, described without reading its contents. */
data class PickedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    // Null when the provider doesn't report a size
    val sizeBytes: Long?
)

/** Reads the display name, size and type of a picked file. Throws IOException or SecurityException. */
suspend fun describePickedFile(context: Context, uri: Uri): PickedFile = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    var name: String? = null
    var size: Long? = null
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex != -1) name = cursor.getString(nameIndex)
            if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    // Without a name, derive an extension from the MIME type so the backend can still recognise the file
    val fallbackName = "upload_${System.currentTimeMillis()}." +
        (MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin")
    PickedFile(uri, name ?: fallbackName, mimeType, size)
}

/** Reads the file into a multipart part named [partName]. Throws IOException or SecurityException. */
suspend fun PickedFile.toPart(context: Context, partName: String = "file"): MultipartBody.Part =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Couldn't open $name")
        MultipartBody.Part.createFormData(partName, name, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
    }

fun PickedFile.exceedsLimit(maxBytes: Long = MAX_SUGGESTION_ATTACHMENT_BYTES): Boolean =
    sizeBytes != null && sizeBytes > maxBytes
