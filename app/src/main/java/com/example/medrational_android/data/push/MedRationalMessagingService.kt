package com.example.medrational_android.data.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medrational_android.MainActivity
import com.example.medrational_android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes while the app is in the foreground. When it's in the background or closed,
 * Android shows the push's notification part by itself, on the channel the backend names.
 */
class MedRationalMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body.orEmpty()
        val projectId = projectIdFromPush(message.data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        WorkshopPush.ensureChannel(this)

        // Tapping opens the project, as it does for a push shown while the app was closed
        val open = PendingIntent.getActivity(
            this,
            projectId?.toInt() ?: 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { projectId?.let { putExtra(WorkshopPush.EXTRA_PROJECT_ID, it.toString()) } },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, WorkshopPush.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), notification)
    }

    // Topic subscriptions follow token changes on their own, so there's nothing to send anywhere
    override fun onNewToken(token: String) = Unit
}
