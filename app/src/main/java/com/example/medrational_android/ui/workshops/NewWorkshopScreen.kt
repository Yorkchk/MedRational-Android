package com.example.medrational_android.ui.workshops

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.medrational_android.data.model.combineDateAndTime
import com.example.medrational_android.data.model.formatMillisDateTime
import com.example.medrational_android.viewmodel.NewWorkshopUiState
import com.example.medrational_android.viewmodel.NewWorkshopViewModel
import com.example.medrational_android.viewmodel.WorkshopForm

private enum class DateField { OPENS, CLOSES }

/** Admin form for a first-come, first-served workshop project. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkshopScreen(
    viewModel: NewWorkshopViewModel,
    onCreated: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var formLink by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("") }
    var opensAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var closesAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }

    val sending = uiState == NewWorkshopUiState.Sending

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is NewWorkshopUiState.Created -> onCreated(state.projectId)
            is NewWorkshopUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New project", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                text = "First come, first served. Students see the registration link only while registration is " +
                    "open, and everyone is notified 10 minutes before it opens and closes. You can add documents " +
                    "once the project is created.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                enabled = !sending
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                label = { Text("Description") },
                enabled = !sending,
                minLines = 5
            )
            OutlinedTextField(
                value = formLink,
                onValueChange = { formLink = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Registration link") },
                placeholder = { Text("https://forms.gle/…") },
                singleLine = true,
                enabled = !sending,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = capacity,
                onValueChange = { value -> capacity = value.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Capacity (optional)") },
                supportingText = { Text("Leave empty for no limit") },
                singleLine = true,
                enabled = !sending,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            DateTimeButton(
                label = "Registration opens",
                millis = opensAt,
                enabled = !sending,
                onClick = { picking = DateField.OPENS }
            )
            DateTimeButton(
                label = "Registration closes",
                millis = closesAt,
                enabled = !sending,
                onClick = { picking = DateField.CLOSES }
            )
            Button(
                onClick = {
                    viewModel.submit(WorkshopForm(title, description, formLink, capacity, opensAt, closesAt))
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create project")
                }
            }
        }
    }

    picking?.let { field ->
        DateTimePickerDialog(
            onDismissRequest = { picking = null },
            onPicked = { millis ->
                when (field) {
                    DateField.OPENS -> opensAt = millis
                    DateField.CLOSES -> closesAt = millis
                }
                picking = null
            }
        )
    }
}

@Composable
private fun DateTimeButton(label: String, millis: Long?, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  $label: ${millis?.let(::formatMillisDateTime) ?: "pick a date and time"}")
    }
}

// A date, then a time
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(onDismissRequest: () -> Unit, onPicked: (Long) -> Unit) {
    var pickedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    val date = pickedDate

    if (date == null) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = { pickedDate = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(is24Hour = true)
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Pick a time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = { onPicked(combineDateAndTime(date, timeState.hour, timeState.minute)) }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
            }
        )
    }
}
