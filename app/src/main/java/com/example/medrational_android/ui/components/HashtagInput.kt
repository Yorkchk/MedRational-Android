package com.example.medrational_android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.medrational_android.data.model.HashtagUsage
import com.example.medrational_android.viewmodel.parseHashtags
import com.example.medrational_android.viewmodel.suggestHashtags

/**
 * Picks hashtags as chips. Typing a space or comma (or pressing Done) turns the text into a chip;
 * tapping a suggestion adds it directly. [suggestionsFor] receives the text being typed and the
 * tags already chosen, so the caller decides what to suggest.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashtagInput(
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    // Hoisted so the dialog's confirm button can include a tag that is still being typed
    text: String,
    onTextChange: (String) -> Unit,
    suggestionsFor: (query: String, selected: List<String>) -> List<HashtagUsage>,
    modifier: Modifier = Modifier,
    // Rename mode: a new pick replaces the current one
    singleTag: Boolean = false,
    supportingText: String? = null
) {
    fun commit(names: List<String>) {
        if (names.isEmpty()) return
        onSelectedChange(if (singleTag) names.takeLast(1) else (selected + names).distinct())
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { name ->
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text("#$name") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove #$name",
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable { onSelectedChange(selected - name) }
                            )
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                val lastSeparator = value.indexOfLast { it == ',' || it.isWhitespace() }
                if (lastSeparator >= 0) {
                    commit(parseHashtags(value.substring(0, lastSeparator)))
                    onTextChange(value.substring(lastSeparator + 1))
                } else {
                    onTextChange(value)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("#") },
            singleLine = true,
            placeholder = { Text(if (singleTag) "New name" else "Type a tag") },
            supportingText = supportingText?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                commit(parseHashtags(text))
                onTextChange("")
            })
        )

        val suggestions = suggestionsFor(text, selected)
        if (suggestions.isNotEmpty()) {
            Text(
                text = if (text.isBlank()) "Most used" else "Existing tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { usage ->
                    SuggestionChip(
                        onClick = {
                            commit(listOf(usage.hashtag))
                            onTextChange("")
                        },
                        label = { Text("#${usage.hashtag} · ${usage.attachedFilesCount}") }
                    )
                }
            }
        }
    }
}

/** Chosen chips plus whatever is still being typed, so a pending tag isn't lost on confirm. */
fun pendingHashtags(selected: List<String>, typed: String, singleTag: Boolean = false): List<String> {
    val all = (selected + parseHashtags(typed)).distinct()
    return if (singleTag) all.takeLast(1) else all
}

/**
 * Dialog wrapping [HashtagInput]. [onConfirm] gets the sanitised tags, including any still typed.
 * [header] is shown above the input (e.g. the name of the file being uploaded).
 */
@Composable
fun HashtagPickerDialog(
    title: String,
    confirmLabel: String,
    catalog: List<HashtagUsage>,
    onConfirm: (List<String>) -> Unit,
    onDismissRequest: () -> Unit,
    // Tags never worth suggesting, e.g. the ones the file already has
    exclude: Set<String> = emptySet(),
    initialSelected: List<String> = emptyList(),
    singleTag: Boolean = false,
    supportingText: String? = null,
    // Lets the confirm button stay enabled with no tags (uploading without tags is fine)
    allowEmpty: Boolean = false,
    isValid: (List<String>) -> Boolean = { allowEmpty || it.isNotEmpty() },
    header: (@Composable () -> Unit)? = null
) {
    var selected by rememberSaveable { mutableStateOf(initialSelected) }
    var text by rememberSaveable { mutableStateOf("") }
    val result = pendingHashtags(selected, text, singleTag)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                header?.invoke()
                HashtagInput(
                    selected = selected,
                    onSelectedChange = { selected = it },
                    text = text,
                    onTextChange = { text = it },
                    suggestionsFor = { query, chosen ->
                        suggestHashtags(catalog, query, exclude + chosen)
                    },
                    singleTag = singleTag,
                    supportingText = supportingText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(result) }, enabled = isValid(result)) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}
