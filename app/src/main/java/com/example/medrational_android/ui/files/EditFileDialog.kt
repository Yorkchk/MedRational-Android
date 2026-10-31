package com.example.medrational_android.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.medrational_android.R
import com.example.medrational_android.data.model.MAX_FILE_DESCRIPTION_LENGTH
import com.example.medrational_android.data.model.MAX_FILE_TITLE_LENGTH
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.fileExtension
import com.example.medrational_android.data.model.fileNameWithoutExtension
import com.example.medrational_android.ui.theme.Spacing

/**
 * Lets an admin rename a study file and change its description. Only the name before the extension
 * is edited: the backend keeps the extension, so it's shown as a suffix.
 */
@Composable
fun EditFileDialog(
    file: StudyFile,
    isSaving: Boolean,
    onSave: (title: String, description: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var title by rememberSaveable(file.id) { mutableStateOf(fileNameWithoutExtension(file.fileName)) }
    var description by rememberSaveable(file.id) { mutableStateOf(file.description.orEmpty()) }
    val extension = fileExtension(file.fileName)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.file_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_FILE_TITLE_LENGTH) },
                    label = { Text(stringResource(R.string.file_edit_title_label)) },
                    suffix = if (extension.isEmpty()) null else { { Text(".$extension") } },
                    singleLine = true,
                    isError = title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(MAX_FILE_DESCRIPTION_LENGTH) },
                    label = { Text(stringResource(R.string.file_edit_description_label)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, description) }, enabled = title.isNotBlank() && !isSaving) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
