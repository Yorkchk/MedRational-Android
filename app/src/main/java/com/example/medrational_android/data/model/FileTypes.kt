package com.example.medrational_android.data.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")

// Lowercase extension without the dot, or "" when the name has none
fun fileExtension(fileName: String?): String {
    val name = fileName ?: return ""
    val dot = name.lastIndexOf('.')
    return if (dot in 1 until name.length - 1) name.substring(dot + 1).lowercase(Locale.ROOT) else ""
}

fun isImageFile(fileName: String?, fileType: String?): Boolean =
    fileType?.startsWith("image/") == true || fileExtension(fileName) in IMAGE_EXTENSIONS

fun fileNameWithoutExtension(fileName: String): String {
    val dot = fileName.lastIndexOf('.')
    return if (dot > 0) fileName.substring(0, dot) else fileName
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

// "DOCX · 2.3 MB · 12 Mar 2026"; parts that are missing are left out
fun fileMetaLine(fileName: String?, fileType: String?, sizeBytes: Long?, uploadedAt: String?): String {
    val type = fileExtension(fileName).uppercase(Locale.ROOT)
        .ifEmpty { fileType?.substringAfter('/')?.uppercase(Locale.ROOT).orEmpty() }
    return listOfNotNull(
        type.ifEmpty { null },
        sizeBytes?.takeIf { it > 0 }?.let(::formatFileSize),
        uploadedAt?.let(::formatUploadDate)
    ).joinToString(" · ")
}

// Backend sends ISO LocalDateTime ("2026-03-12T10:15:30"); only the date part is shown
private fun formatUploadDate(isoDateTime: String): String? = try {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDateTime.take(10))
    date?.let { SimpleDateFormat("d MMM yyyy", Locale.US).format(it) }
} catch (e: ParseException) {
    null
}
