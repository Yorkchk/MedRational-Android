package com.example.medrational_android.data.reminders

import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.parseIsoLocalDateTime

const val REMINDER_LEAD_MILLIS = 10 * 60 * 1000L

enum class ReminderKind { OPENING, CLOSING }

data class WorkshopReminder(
    val projectId: Long,
    val title: String,
    val kind: ReminderKind,
    val triggerAtMillis: Long
) {
    val requestCode: Int get() = reminderRequestCode(projectId, kind)
}

// One alarm slot per project and kind, so scheduling again replaces the alarm instead of adding one
fun reminderRequestCode(projectId: Long, kind: ReminderKind): Int = (projectId * 2 + kind.ordinal).toInt()

/**
 * The reminders still ahead for [project]: 10 minutes before registration opens (while UPCOMING)
 * and 10 minutes before it closes (while UPCOMING or OPEN). Reminders already due are dropped.
 */
fun reminderTimes(project: WorkshopProject, nowMillis: Long): List<WorkshopReminder> {
    if (project.status == ProjectStatus.CLOSED) return emptyList()
    val opening = if (project.status == ProjectStatus.UPCOMING) {
        parseIsoLocalDateTime(project.registrationOpensAt)?.let {
            WorkshopReminder(project.id, project.title, ReminderKind.OPENING, it - REMINDER_LEAD_MILLIS)
        }
    } else {
        null
    }
    val closing = parseIsoLocalDateTime(project.registrationClosesAt)?.let {
        WorkshopReminder(project.id, project.title, ReminderKind.CLOSING, it - REMINDER_LEAD_MILLIS)
    }
    return listOfNotNull(opening, closing).filter { it.triggerAtMillis > nowMillis }
}

fun reminderTitle(kind: ReminderKind): String = when (kind) {
    ReminderKind.OPENING -> "Registration opens in 10 minutes"
    ReminderKind.CLOSING -> "Registration closes in 10 minutes"
}

fun reminderText(projectTitle: String, kind: ReminderKind): String = when (kind) {
    ReminderKind.OPENING -> "Spots are limited! Registration for \"$projectTitle\" opens in 10 minutes."
    ReminderKind.CLOSING -> "Registration for \"$projectTitle\" closes in 10 minutes. Sign up before the cutoff."
}

/** Local notifications 10 minutes before a workshop's registration opens or closes. */
interface WorkshopReminderScheduler {
    /** Schedules again the reminders of [projects]; closed projects lose theirs. */
    fun schedule(projects: List<WorkshopProject>)

    /** Like [schedule], and also cancels the reminders of every project not in [projects]. */
    fun replaceAll(projects: List<WorkshopProject>)

    fun cancel(projectId: Long)

    /** On sign-out. */
    fun cancelAll()

    object None : WorkshopReminderScheduler {
        override fun schedule(projects: List<WorkshopProject>) = Unit
        override fun replaceAll(projects: List<WorkshopProject>) = Unit
        override fun cancel(projectId: Long) = Unit
        override fun cancelAll() = Unit
    }
}
