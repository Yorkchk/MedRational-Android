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

fun fileTypeIcon(fileName: String?, fileType: String?): ImageVector = when {
    isImageFile(fileName, fileType) -> Icons.Default.Image
    else -> when (fileExtension(fileName)) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "ppt", "pptx" -> Icons.Default.Slideshow
        "xls", "xlsx", "csv" -> Icons.Default.TableChart
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
