package com.example.medrational_android.ui.files

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.medrational_android.data.model.fileExtension
import com.example.medrational_android.data.model.isImageFile

/** What a file is, for its icon and badge color. */
enum class FileKind { PDF, DOCUMENT, SLIDES, SHEET, IMAGE, OTHER }

fun fileKind(fileName: String?, fileType: String?): FileKind = when {
    isImageFile(fileName, fileType) -> FileKind.IMAGE
    else -> when (fileExtension(fileName)) {
        "pdf" -> FileKind.PDF
        "doc", "docx" -> FileKind.DOCUMENT
        "ppt", "pptx" -> FileKind.SLIDES
        "xls", "xlsx", "csv" -> FileKind.SHEET
        else -> FileKind.OTHER
    }
}

fun FileKind.icon(): ImageVector = when (this) {
    FileKind.PDF -> Icons.Default.PictureAsPdf
    FileKind.DOCUMENT -> Icons.Default.Description
    FileKind.SLIDES -> Icons.Default.Slideshow
    FileKind.SHEET -> Icons.Default.TableChart
    FileKind.IMAGE -> Icons.Default.Image
    FileKind.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun fileTypeIcon(fileName: String?, fileType: String?): ImageVector = fileKind(fileName, fileType).icon()
