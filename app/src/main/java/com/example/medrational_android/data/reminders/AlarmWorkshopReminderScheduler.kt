package com.example.medrational_android.data.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medrational_android.data.model.WorkshopProject

/**
 * Schedules workshop reminders with [AlarmManager]. Exact alarms are used when the app may set them
 * (SCHEDULE_EXACT_ALARM, which Android 14+ leaves to the user); otherwise Android may deliver a
 * reminder a few minutes late while the phone dozes.
 * Alarms are lost on reboot until the app syncs them again on its next start.
 */
class AlarmWorkshopReminderScheduler(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) : WorkshopReminderScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    // Ids with reminders, so replaceAll can cancel those of projects that disappeared
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun schedule(projects: List<WorkshopProject>) {
        val now = clock()
        val scheduled = scheduledIds().toMutableSet()
        projects.forEach { project ->
            cancelAlarms(project.id)
            val reminders = reminderTimes(project, now)
            reminders.forEach(::setAlarm)
            if (reminders.isEmpty()) scheduled -= project.id else scheduled += project.id
        }
        saveScheduledIds(scheduled)
    }

    override fun replaceAll(projects: List<WorkshopProject>) {
        val keep = projects.map { it.id }.toSet()
        val stale = scheduledIds() - keep
        stale.forEach(::cancelAlarms)
        saveScheduledIds(scheduledIds() - stale)
        schedule(projects)
    }

    override fun cancel(projectId: Long) {
        cancelAlarms(projectId)
        saveScheduledIds(scheduledIds() - projectId)
    }

    override fun cancelAll() {
        scheduledIds().forEach(::cancelAlarms)
        saveScheduledIds(emptySet())
    }

    // Lint can't see the canScheduleExactAlarms() check
    @SuppressLint("MissingPermission")
    private fun setAlarm(reminder: WorkshopReminder) {
        val pending = PendingIntent.getBroadcast(
            appContext,
            reminder.requestCode,
            WorkshopReminderReceiver.intent(appContext, reminder),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
                return
            }
        } catch (e: SecurityException) {
            // Revoked between the check and the call: fall back to an inexact alarm
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
    }

    private fun cancelAlarms(projectId: Long) {
        ReminderKind.entries.forEach { kind ->
            // Extras don't take part in PendingIntent matching, so a bare intent finds the alarm
            val pending = PendingIntent.getBroadcast(
                appContext,
                reminderRequestCode(projectId, kind),
                Intent(appContext, WorkshopReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pending?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun scheduledIds(): Set<Long> =
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().mapNotNull(String::toLongOrNull).toSet()

    private fun saveScheduledIds(ids: Set<Long>) {
        prefs.edit().putStringSet(KEY_IDS, ids.map(Long::toString).toSet()).apply()
    }

    private companion object {
        const val PREFS_NAME = "workshop_reminders"
        const val KEY_IDS = "scheduled_project_ids"
    }
}
