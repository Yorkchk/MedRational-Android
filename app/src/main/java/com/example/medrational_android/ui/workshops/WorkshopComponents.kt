package com.example.medrational_android.ui.workshops

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.medrational_android.R
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.formatIsoLocalDateTime
import com.example.medrational_android.ui.text.UiText
import java.util.Locale

@StringRes
fun ProjectStatus.label(): Int = when (this) {
    ProjectStatus.UPCOMING -> R.string.workshop_status_upcoming
    ProjectStatus.OPEN -> R.string.workshop_status_open
    ProjectStatus.CLOSED -> R.string.workshop_status_closed
}

// The admin menu action that sets this status
@StringRes
fun ProjectStatus.markAsLabel(): Int = when (this) {
    ProjectStatus.UPCOMING -> R.string.workshop_mark_upcoming
    ProjectStatus.OPEN -> R.string.workshop_mark_open
    ProjectStatus.CLOSED -> R.string.workshop_mark_closed
}

@StringRes
fun ProjectType.label(): Int = when (this) {
    ProjectType.FIRST_COME_FIRST_SERVED -> R.string.workshop_type_fcfs
    ProjectType.MOTIVATION -> R.string.workshop_type_motivation
}

// What students read under the "Project type" line on the project page; the selection itself
// happens on the external form
@StringRes
fun ProjectType.description(): Int = when (this) {
    ProjectType.FIRST_COME_FIRST_SERVED -> R.string.workshop_type_fcfs_description
    ProjectType.MOTIVATION -> R.string.workshop_type_motivation_description
}

// Workshop times are always 24-hour, e.g. "20 Oct 2026, 19:30" ("20 oct. 2026, 19:30" in French),
// whatever the phone's setting
fun WorkshopProject.opensLabel(locale: Locale): String = formatIsoLocalDateTime(registrationOpensAt, locale)

fun WorkshopProject.closesLabel(locale: Locale): String = formatIsoLocalDateTime(registrationClosesAt, locale)

fun WorkshopProject.capacityLabel(): UiText? = capacity?.let { UiText.Plural(R.plurals.workshop_spots, it) }

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
    SuggestionChip(onClick = {}, modifier = modifier, label = { Text(stringResource(status.label())) }, colors = colors)
}
