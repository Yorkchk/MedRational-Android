package com.example.medrational_android.ui.suggestions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.MAX_SUGGESTION_TITLE_LENGTH
import com.example.medrational_android.viewmodel.NewSuggestionUiState
import com.example.medrational_android.viewmodel.NewSuggestionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSuggestionScreen(
    viewModel: NewSuggestionViewModel,
    onSent: (suggestionId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    // Not saveable: the picker's Uri permission doesn't survive process death anyway
    var attachment by remember { mutableStateOf<PickedFile?>(null) }
    val pickAttachment = rememberAttachmentPicker { attachment = it }

    val sending = uiState == NewSuggestionUiState.Sending

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is NewSuggestionUiState.Sent -> onSent(state.suggestionId)
            is NewSuggestionUiState.Error -> {
                Toast.makeText(context, state.message.asString(context), Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.suggestion_new), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.suggestion_new_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MAX_SUGGESTION_TITLE_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.suggestion_title_label)) },
                singleLine = true,
                enabled = !sending,
                supportingText = { Text("${title.length} / $MAX_SUGGESTION_TITLE_LENGTH") }
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                label = { Text(stringResource(R.string.suggestion_description_label)) },
                enabled = !sending,
                minLines = 6
            )

            val picked = attachment
            if (picked != null) {
                PickedFileChip(file = picked, onRemoveClick = { attachment = null }, enabled = !sending)
            } else {
                OutlinedButton(onClick = pickAttachment, enabled = !sending) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.suggestion_attach_optional))
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        readAttachment(context, attachment).onSuccess { part ->
                            viewModel.submit(title, description, part)
                        }
                    }
                },
                enabled = !sending && viewModel.canSubmit(title, description),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.suggestion_send))
                }
            }
        }
    }
}
