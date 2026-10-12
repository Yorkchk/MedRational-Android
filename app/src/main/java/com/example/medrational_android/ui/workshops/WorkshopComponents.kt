package com.example.medrational_android.ui.workshops

import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.formatIsoLocalDateTime

fun ProjectStatus.label(): String = when (this) {
    ProjectStatus.UPCOMING -> "Upcoming"
    ProjectStatus.OPEN -> "Open"
    ProjectStatus.CLOSED -> "Closed"
}

fun ProjectType.label(): String = when (this) {
    ProjectType.FIRST_COME_FIRST_SERVED -> "First come, first served"
    ProjectType.MOTIVATION -> "Motivation letter"
}

// What students read under the "Project type" line on the project page; the selection itself
// happens on the external form
fun ProjectType.description(): String = when (this) {
    ProjectType.FIRST_COME_FIRST_SERVED -> "Register as soon as registration opens: spots go to the first to sign up."
    ProjectType.MOTIVATION ->
        "Apply through the registration link with a motivation letter. The committee decides who is accepted."
}

fun WorkshopProject.opensLabel(is24Hour: Boolean = true): String = formatIsoLocalDateTime(registrationOpensAt, is24Hour)

fun WorkshopProject.closesLabel(is24Hour: Boolean = true): String = formatIsoLocalDateTime(registrationClosesAt, is24Hour)

/** The phone's 12/24-hour setting, which every workshop time follows. */
@Composable
fun is24HourClock(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

fun WorkshopProject.capacityLabel(): String? = capacity?.let { if (it == 1) "1 spot" else "$it spots" }

@Composable
fun WorkshopStatusChip(status: ProjectStatus, modifier: Modifier = Modifier) {
    val colors = when (status) {
        ProjectStatus.UPCOMING -> SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        ProjectStatus.OPEN -> SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ProjectStatus.CLOSED -> SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    SuggestionChip(onClick = {}, modifier = modifier, label = { Text(status.label()) }, colors = colors)
}
