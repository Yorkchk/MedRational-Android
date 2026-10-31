package com.example.medrational_android.data.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "heif", "avif")

// Limits of the backend's study file fields
const val MAX_FILE_TITLE_LENGTH = 200
const val MAX_FILE_DESCRIPTION_LENGTH = 2000

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

// "2.5 MB", or "2,5 Mo" in French, where bytes are "octets"
fun formatFileSize(bytes: Long, locale: Locale): String {
    val french = locale.language == "fr"
    return when {
        bytes < 1024 -> "$bytes ${if (french) "o" else "B"}"
        bytes < 1024 * 1024 -> String.format(locale, if (french) "%.0f Ko" else "%.0f KB", bytes / 1024.0)
        else -> String.format(locale, if (french) "%.1f Mo" else "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

// "DOCX · 2.3 MB · 12 Mar 2026"; parts that are missing are left out
fun fileMetaLine(fileName: String?, fileType: String?, sizeBytes: Long?, uploadedAt: String?, locale: Locale): String {
    val type = fileExtension(fileName).uppercase(Locale.ROOT)
        .ifEmpty { fileType?.substringAfter('/')?.uppercase(Locale.ROOT).orEmpty() }
    return listOfNotNull(
        type.ifEmpty { null },
        sizeBytes?.takeIf { it > 0 }?.let { formatFileSize(it, locale) },
        uploadedAt?.let { formatIsoDate(it, locale) }
    ).joinToString(" · ")
}

// "12 Mar 2026, 10:15" from an ISO LocalDateTime; falls back to the date alone when there is no time
fun formatIsoDateTime(isoDateTime: String, locale: Locale): String? {
    val date = formatIsoDate(isoDateTime, locale) ?: return null
    val time = isoDateTime.substringAfter('T', "").take(5)
    return if (time.length == 5 && time[2] == ':') "$date, $time" else date
}

// Backend sends ISO LocalDateTime ("2026-03-12T10:15:30"); only the date part is shown ("12 Mar 2026",
// "12 mars 2026" in French)
fun formatIsoDate(isoDateTime: String, locale: Locale): String? = try {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDateTime.take(10))
    date?.let { SimpleDateFormat("d MMM yyyy", locale).format(it) }
} catch (e: ParseException) {
    null
}
