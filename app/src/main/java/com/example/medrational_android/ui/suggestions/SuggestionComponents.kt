package com.example.medrational_android.ui.suggestions

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.medrational_android.data.model.SuggestionAttachment
import com.example.medrational_android.data.model.SuggestionStatus
import com.example.medrational_android.data.model.formatFileSize
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.ui.files.fileTypeIcon

private fun sizeLabel(bytes: Long?): String = bytes?.takeIf { it > 0 }?.let { " · ${formatFileSize(it)}" }.orEmpty()

/** A file already sent with a suggestion or reply; tapping downloads it. */
@Composable
fun AttachmentChip(
    attachment: SuggestionAttachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = attachment.fileName + sizeLabel(attachment.sizeBytes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = fileTypeIcon(attachment.fileName, attachment.contentType),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

/** A file picked but not sent yet, with an ✕ to drop it. */
@Composable
fun PickedFileChip(
    file: PickedFile,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    InputChip(
        selected = false,
        onClick = onRemoveClick,
        enabled = enabled,
        modifier = modifier,
        label = { Text(file.name + sizeLabel(file.sizeBytes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                imageVector = fileTypeIcon(file.name, file.mimeType),
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove ${file.name}",
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        }
    )
}

@Composable
fun SuggestionStatusChip(status: SuggestionStatus, modifier: Modifier = Modifier) {
    val closed = status == SuggestionStatus.CLOSED
    SuggestionChip(
        onClick = {},
        modifier = modifier,
        label = { Text(if (closed) "Closed" else "Pending") },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (closed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = if (closed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}
