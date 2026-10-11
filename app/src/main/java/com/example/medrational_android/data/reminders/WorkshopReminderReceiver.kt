package com.example.medrational_android.data.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medrational_android.MainActivity
import com.example.medrational_android.R

/** Posts the notification for a reminder set by [AlarmWorkshopReminderScheduler]. */
class WorkshopReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { name -> ReminderKind.entries.find { it.name == name } }
        if (projectId < 0 || title == null || kind == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)

        val code = reminderRequestCode(projectId, kind)
        val openApp = PendingIntent.getActivity(
            context,
            code,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = reminderText(title, kind)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminderTitle(kind))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(code, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Workshop reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts 10 minutes before a workshop's registration opens or closes"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "workshop_reminders"
        private const val EXTRA_PROJECT_ID = "projectId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_KIND = "kind"

        fun intent(context: Context, reminder: WorkshopReminder): Intent =
            Intent(context, WorkshopReminderReceiver::class.java)
                .putExtra(EXTRA_PROJECT_ID, reminder.projectId)
                .putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_KIND, reminder.kind.name)
    }
}
