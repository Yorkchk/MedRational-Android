package com.example.medrational_android.ui.workshops

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medrational_android.data.model.combineDateAndTime
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.formatMillisDateTime
import com.example.medrational_android.data.model.toUtcDateMillis
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.ui.suggestions.PickedFileChip
import com.example.medrational_android.ui.suggestions.readAttachment
import com.example.medrational_android.ui.suggestions.rememberAttachmentPicker
import com.example.medrational_android.viewmodel.EditWorkshopUiState
import com.example.medrational_android.viewmodel.EditWorkshopViewModel
import com.example.medrational_android.viewmodel.NewWorkshopUiState
import com.example.medrational_android.viewmodel.NewWorkshopViewModel
import com.example.medrational_android.viewmodel.PendingAttachment
import com.example.medrational_android.viewmodel.WorkshopForm
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class DateField { OPENS, CLOSES }

/** Admin form for a new project of either type, with optional documents. */
@Composable
fun NewWorkshopScreen(
    viewModel: NewWorkshopViewModel,
    onCreated: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    // Not saveable: the picker's Uri permission doesn't survive process death anyway
    val attachments = remember { mutableStateListOf<PickedFile>() }
    val pickAttachment = rememberAttachmentPicker { picked ->
        if (attachments.none { it.uri == picked.uri }) attachments += picked
    }
    val sending = uiState == NewWorkshopUiState.Sending

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is NewWorkshopUiState.Created -> {
                if (state.failedFiles.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Project created, but ${state.failedFiles.joinToString()} couldn't be attached. " +
                            "Add it from the project page.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onCreated(state.projectId)
            }
            is NewWorkshopUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
            else -> Unit
        }
    }

    WorkshopFormScaffold(title = "New project", onBackClick = onBackClick) {
        WorkshopFormFields(
            initial = WorkshopForm(),
            intro = "Students see the registration link only while registration is open, and everyone is " +
                "notified 10 minutes before it opens and closes.",
            submitLabel = "Create project",
            showTypePicker = true,
            busy = sending,
            onSubmit = { form ->
                scope.launch {
                    // Read every file first, so a file that can't be read stops the whole submission
                    val pending = attachments.map { file ->
                        val part = readAttachment(context, file).getOrNull() ?: return@launch
                        PendingAttachment(file.name, part)
                    }
                    viewModel.submit(form, pending)
                }
            }
        ) {
            Text("Documents (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            attachments.forEach { file ->
                PickedFileChip(file = file, onRemoveClick = { attachments.remove(file) }, enabled = !sending)
            }
            OutlinedButton(onClick = pickAttachment, enabled = !sending) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (attachments.isEmpty()) "  Attach a file (max 10 MB)" else "  Attach another file")
            }
        }
    }
}

/** Admin form to change a project's details and registration dates. */
@Composable
fun EditWorkshopScreen(
    projectId: Long,
    viewModel: EditWorkshopViewModel,
    onSaved: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(uiState) {
        if (uiState == EditWorkshopUiState.Saved) onSaved()
    }

    WorkshopFormScaffold(title = "Edit project", onBackClick = onBackClick) {
        when (val state = uiState) {
            is EditWorkshopUiState.Loading, EditWorkshopUiState.Saved -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is EditWorkshopUiState.LoadError -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.load(projectId) }) { Text("Retry") }
            }
            is EditWorkshopUiState.Editing -> WorkshopFormFields(
                initial = state.initial,
                intro = "Changing the dates updates the status to match them and sends the 10-minute reminders " +
                    "again. If the dates stay the same, a status you set by hand is kept.",
                submitLabel = "Save changes",
                busy = state.saving,
                onSubmit = viewModel::save
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkshopFormScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

/**
 * The project's fields, seeded from [initial]. [extraContent] goes between the dates and the submit
 * button.
 */
@Composable
private fun WorkshopFormFields(
    initial: WorkshopForm,
    intro: String,
    submitLabel: String,
    busy: Boolean,
    onSubmit: (WorkshopForm) -> Unit,
    // Only when creating: an existing project keeps its type
    showTypePicker: Boolean = false,
    extraContent: @Composable () -> Unit = {}
) {
    var title by rememberSaveable(initial) { mutableStateOf(initial.title) }
    var description by rememberSaveable(initial) { mutableStateOf(initial.description) }
    var formLink by rememberSaveable(initial) { mutableStateOf(initial.formLink) }
    var capacity by rememberSaveable(initial) { mutableStateOf(initial.capacity) }
    var opensAt by rememberSaveable(initial) { mutableStateOf(initial.opensAtMillis) }
    var closesAt by rememberSaveable(initial) { mutableStateOf(initial.closesAtMillis) }
    var projectType by rememberSaveable(initial) { mutableStateOf(initial.projectType) }
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }

    Text(
        text = intro,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (showTypePicker) {
        ProjectTypePicker(selected = projectType, enabled = !busy, onSelect = { projectType = it })
    }
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Title") },
        singleLine = true,
        enabled = !busy
    )
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        label = { Text("Description") },
        enabled = !busy,
        minLines = 5
    )
    OutlinedTextField(
        value = formLink,
        onValueChange = { formLink = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Registration link") },
        placeholder = { Text("https://forms.gle/…") },
        singleLine = true,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    OutlinedTextField(
        value = capacity,
        onValueChange = { value -> capacity = value.filter(Char::isDigit).take(6) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Capacity (optional)") },
        supportingText = { Text("Leave empty for no limit") },
        singleLine = true,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    DateTimeButton(label = "Registration opens", millis = opensAt, enabled = !busy, onClick = { picking = DateField.OPENS })
    DateTimeButton(label = "Registration closes", millis = closesAt, enabled = !busy, onClick = { picking = DateField.CLOSES })
    extraContent()
    Button(
        onClick = { onSubmit(WorkshopForm(title, description, formLink, capacity, opensAt, closesAt, projectType)) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(submitLabel)
        }
    }

    picking?.let { field ->
        DateTimePickerDialog(
            initialMillis = if (field == DateField.OPENS) opensAt else closesAt,
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

// Both types work the same in the app: the difference is what students do on the external form
private fun ProjectType.formHint(): String = when (this) {
    ProjectType.FIRST_COME_FIRST_SERVED -> "Students register on the link as soon as registration opens, until it's full."
    ProjectType.MOTIVATION -> "Students apply on the link with a motivation letter; the committee decides who is accepted."
}

@Composable
private fun ProjectTypePicker(selected: ProjectType, enabled: Boolean, onSelect: (ProjectType) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        Text("Project type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ProjectType.entries.sortedBy { it != ProjectType.FIRST_COME_FIRST_SERVED }.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = type == selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(type) }
                    )
                    .padding(vertical = 4.dp)
            ) {
                // The row handles the click, so TalkBack reads it as one radio button
                RadioButton(selected = type == selected, onClick = null, enabled = enabled)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(type.label(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = type.formHint(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimeButton(label: String, millis: Long?, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
        val is24Hour = is24HourClock()
        Text("  $label: ${millis?.let { formatMillisDateTime(it, is24Hour) } ?: "pick a date and time"}")
    }
}

// A date, then a time; both start on [initialMillis] when there is one
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(initialMillis: Long?, onDismissRequest: () -> Unit, onPicked: (Long) -> Unit) {
    var pickedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    val date = pickedDate

    if (date == null) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis?.let(::toUtcDateMillis))
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
        val initial = initialMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
        val timeState = rememberTimePickerState(
            initialHour = initial?.get(Calendar.HOUR_OF_DAY) ?: 0,
            initialMinute = initial?.get(Calendar.MINUTE) ?: 0,
            // A forced 24-hour dial reads "7" as 07:00; follow the phone so AM/PM shows when it uses it
            is24Hour = is24HourClock()
        )
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
