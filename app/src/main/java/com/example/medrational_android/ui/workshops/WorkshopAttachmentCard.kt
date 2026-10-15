package com.example.medrational_android.ui.workshops

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.medrational_android.data.model.WorkshopAttachment
import com.example.medrational_android.ui.files.AttachmentPreviewCard
import com.example.medrational_android.viewmodel.PreviewState

/** A project document; see [AttachmentPreviewCard]. */
@Composable
fun WorkshopAttachmentCard(
    attachment: WorkshopAttachment,
    // null until the project page asked for it
    preview: PreviewState?,
    onDownloadClick: () -> Unit,
    onRetryPreviewClick: () -> Unit,
    onPreviewCorrupted: () -> Unit,
    modifier: Modifier = Modifier,
    // Admin only; null hides the delete button
    onDeleteClick: (() -> Unit)? = null,
    deleteEnabled: Boolean = true
) {
    AttachmentPreviewCard(
        fileName = attachment.fileName,
        preview = preview,
        onDownloadClick = onDownloadClick,
        onRetryPreviewClick = onRetryPreviewClick,
        onPreviewCorrupted = onPreviewCorrupted,
        modifier = modifier,
        onDeleteClick = onDeleteClick,
        deleteEnabled = deleteEnabled
    )
}
