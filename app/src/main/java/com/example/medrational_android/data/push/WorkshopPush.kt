package com.example.medrational_android.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Workshop reminders arrive as Firebase Cloud Messaging pushes. The backend scheduler publishes
 * them to one topic, and every signed-in phone subscribes to it, so the server never needs to
 * know which phones exist or who is signed in on them.
 */
object WorkshopPush {

    // Must match PushNotificationService.WORKSHOPS_TOPIC on the backend
    const val TOPIC = "workshops"

    // Must match FcmPushNotificationService.ANDROID_CHANNEL_ID on the backend
    const val CHANNEL_ID = "workshop_reminders"

    // Data key carrying the project a push is about; Android copies it into the launch intent's
    // extras when the user taps a push shown while the app was closed
    const val EXTRA_PROJECT_ID = "projectId"

    private const val TAG = "WorkshopPush"

    // False in builds without app/google-services.json
    private fun available(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /** Safe to call repeatedly; FCM keeps the subscription across app restarts and token changes. */
    fun subscribe(context: Context) {
        if (!available(context)) return
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
            .addOnFailureListener { Log.w(TAG, "Couldn't subscribe to $TOPIC", it) }
    }

    /** On sign-out: this phone stops receiving workshop pushes. */
    fun unsubscribe(context: Context) {
        if (!available(context)) return
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC)
            .addOnFailureListener { Log.w(TAG, "Couldn't unsubscribe from $TOPIC", it) }
    }

    /** The channel pushes are shown on; it must exist before the first push arrives. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Workshop reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "When a workshop's registration opens and closes, with a reminder 10 minutes before each"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** The project a push is about, or null when it doesn't name a valid one. */
fun projectIdFromPush(data: Map<String, String?>): Long? =
    data[WorkshopPush.EXTRA_PROJECT_ID]?.trim()?.toLongOrNull()?.takeIf { it > 0 }
