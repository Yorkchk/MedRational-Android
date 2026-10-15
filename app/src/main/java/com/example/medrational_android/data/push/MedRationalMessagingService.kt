package com.example.medrational_android.data.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
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
        val push = message.notification ?: return
        // The backend's English text is only a fallback for an app that doesn't know the key
        val strings = appLanguageContext()
        // The backend names the string, so it can only be looked up by name
        @SuppressLint("DiscouragedApi")
        val template = { name: String ->
            strings.resources.getIdentifier(name, "string", packageName).takeIf { it != 0 }?.let(strings::getString)
        }
        val title = localizedPushText(push.titleLocalizationKey, push.titleLocalizationArgs.orEmpty().toList(), push.title, template)
            ?: return
        val body = localizedPushText(push.bodyLocalizationKey, push.bodyLocalizationArgs.orEmpty().toList(), push.body, template)
            .orEmpty()
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
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), notification)
    }

    // Android 13+ gives services the language picked in the profile; older versions only give it to
    // activities, so it's applied here (the app is open, as this only runs in the foreground)
    private fun appLanguageContext(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return this
        val config = Configuration(resources.configuration).apply { setLocales(LocaleList.forLanguageTags(locales.toLanguageTags())) }
        return createConfigurationContext(config)
    }

    // Topic subscriptions follow token changes on their own, so there's nothing to send anywhere
    override fun onNewToken(token: String) = Unit
}
