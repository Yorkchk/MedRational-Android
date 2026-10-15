package com.example.medrational_android.ui.suggestions

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.medrational_android.R
import com.example.medrational_android.data.model.formatFileSize
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.ui.files.fileTypeIcon
import com.example.medrational_android.ui.text.appLocale

@Composable
private fun sizeLabel(bytes: Long?): String = bytes?.takeIf { it > 0 }?.let { " · ${formatFileSize(it, appLocale())}" }.orEmpty()

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
                contentDescription = stringResource(R.string.attachment_remove_named, file.name),
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        }
    )
}
