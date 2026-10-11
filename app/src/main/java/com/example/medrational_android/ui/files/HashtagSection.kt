package com.example.medrational_android.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medrational_android.data.model.Hashtag
import com.example.medrational_android.data.model.HashtagUsage
import com.example.medrational_android.ui.components.HashtagPickerDialog
import com.example.medrational_android.viewmodel.HashtagsUiState

/**
 * The file's hashtags as chips. Admins can tap a chip to rename it, use its ✕ to remove it, and
 * add new ones; everyone else only sees the chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashtagSection(
    state: HashtagsUiState,
    isAdmin: Boolean,
    onAddHashtags: (String) -> Unit,
    onRenameHashtag: (Hashtag, String) -> Unit,
    onRemoveHashtag: (Hashtag) -> Unit,
    modifier: Modifier = Modifier,
    // Existing tags for autocomplete (admins only); refreshed each time a dialog opens
    catalog: List<HashtagUsage> = emptyList(),
    onRefreshCatalog: () -> Unit = {}
) {
    // Loading and load errors stay quiet: hashtags are secondary to the file itself
    val success = state as? HashtagsUiState.Success ?: return
    if (success.hashtags.isEmpty() && !isAdmin) return

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // Ids rather than Hashtag objects so the open dialog survives rotation
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var removingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = success.hashtags.firstOrNull { it.id == editingId }
    val removing = success.hashtags.firstOrNull { it.id == removingId }

    Column(modifier = modifier) {
        Text(
            text = "Hashtags",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            success.hashtags.forEach { hashtag ->
                if (isAdmin) {
                    InputChip(
                        selected = false,
                        enabled = !success.busy,
                        onClick = {
                            onRefreshCatalog()
                            editingId = hashtag.id
                        },
                        label = { Text("#${hashtag.name}") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove #${hashtag.name}",
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable(enabled = !success.busy) { removingId = hashtag.id }
                            )
                        }
                    )
                } else {
                    SuggestionChip(onClick = {}, enabled = true, label = { Text("#${hashtag.name}") })
                }
            }
            if (isAdmin) {
                AssistChip(
                    onClick = {
                        onRefreshCatalog()
                        showAddDialog = true
                    },
                    enabled = !success.busy,
                    label = { Text("Add") },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    }
                )
            }
        }
    }

    val onFile = success.hashtags.map { it.name }.toSet()

    if (showAddDialog) {
        HashtagPickerDialog(
            title = "Add hashtags",
            confirmLabel = "Add",
            catalog = catalog,
            exclude = onFile,
            supportingText = "Separate several tags with spaces or commas",
            onConfirm = { names ->
                onAddHashtags(names.joinToString(" "))
                showAddDialog = false
            },
            onDismissRequest = { showAddDialog = false }
        )
    }

    editing?.let { hashtag ->
        HashtagPickerDialog(
            title = "Rename #${hashtag.name}",
            confirmLabel = "Save",
            catalog = catalog,
            exclude = onFile,
            initialSelected = listOf(hashtag.name),
            singleTag = true,
            // Picking an existing tag merges this one into it on this file
            supportingText = "Renames the tag on this file only",
            isValid = { it.size == 1 && it.first() != hashtag.name },
            onConfirm = { names ->
                onRenameHashtag(hashtag, names.first())
                editingId = null
            },
            onDismissRequest = { editingId = null }
        )
    }

    removing?.let { hashtag ->
        AlertDialog(
            onDismissRequest = { removingId = null },
            title = { Text("Remove #${hashtag.name}?") },
            text = { Text("It will be removed from this file, and deleted entirely if no other file uses it.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveHashtag(hashtag)
                    removingId = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { removingId = null }) { Text("Cancel") }
            }
        )
    }
}
