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
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.formatIsoLocalDateTime

fun ProjectStatus.label(): String = when (this) {
    ProjectStatus.UPCOMING -> "Upcoming"
    ProjectStatus.OPEN -> "Open"
    ProjectStatus.CLOSED -> "Closed"
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
