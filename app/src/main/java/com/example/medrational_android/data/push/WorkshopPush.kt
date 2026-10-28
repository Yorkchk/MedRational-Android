package com.example.medrational_android.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.medrational_android.BuildConfig
import com.example.medrational_android.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.IllegalFormatException

/**
 * Workshop reminders arrive as Firebase Cloud Messaging pushes. The backend scheduler publishes
 * them to one topic, and every phone with the app subscribes to it at startup, signed in or not,
 * so the server never needs to know which phones exist or who is signed in on them.
 */
object WorkshopPush {

    // Must match push.workshops-topic of the backend this build talks to; set per build type in
    // app/build.gradle.kts (workshops-dev for debug, workshops for release)
    val TOPIC: String = BuildConfig.WORKSHOPS_TOPIC

    // Every environment's topic, so a phone that switched builds stops hearing the old backend
    val ENVIRONMENT_TOPICS = listOf("workshops", "workshops-uat", "workshops-dev")

    // Must match FcmPushNotificationService.ANDROID_CHANNEL_ID on the backend
    const val CHANNEL_ID = "workshop_reminders"

    // Data key carrying the project a push is about; Android copies it into the launch intent's
    // extras when the user taps a push shown while the app was closed
    const val EXTRA_PROJECT_ID = "projectId"

    private const val TAG = "WorkshopPush"

    // False in builds without app/google-services.json
    private fun available(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /**
     * Called at every app start. Safe to call repeatedly; FCM keeps the subscription across app
     * restarts and token changes. Signing out doesn't undo it: only uninstalling the app or turning
     * off its notifications stops the pushes.
     */
    fun subscribe(context: Context) {
        if (!available(context)) return
        val messaging = FirebaseMessaging.getInstance()
        messaging.subscribeToTopic(TOPIC)
            .addOnFailureListener { Log.w(TAG, "Couldn't subscribe to $TOPIC", it) }
        topicsToLeave(TOPIC).forEach { topic ->
            messaging.unsubscribeFromTopic(topic)
                .addOnFailureListener { Log.w(TAG, "Couldn't unsubscribe from $topic", it) }
        }
    }

    /** The channel pushes are shown on; it must exist before the first push arrives. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Called at every start, so the name follows the app's language
        val name = context.getString(R.string.push_channel_name)
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.push_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** The other environments' topics, which a phone listening to [topic] must not stay subscribed to. */
fun topicsToLeave(topic: String): List<String> = WorkshopPush.ENVIRONMENT_TOPICS - topic

/** The project a push is about, or null when it doesn't name a valid one. */
fun projectIdFromPush(data: Map<String, String?>): Long? =
    data[WorkshopPush.EXTRA_PROJECT_ID]?.trim()?.toLongOrNull()?.takeIf { it > 0 }

fun workshopRoute(projectId: Long): String = "workshops/$projectId"

/**
 * Where a tapped push leads: its project, or the login screen when signed out (the workshops API
 * needs a signed-in user). The project then opens after login.
 */
fun routeForPushTap(projectId: Long, isLoggedIn: Boolean): String =
    if (isLoggedIn) workshopRoute(projectId) else "login"

/**
 * A push's title or body in the app's language: the app string named [key] (sent by the backend
 * as title_loc_key/body_loc_key) filled with [args], or [fallback], the backend's English text, when
 * there is no key or the app doesn't know it. [template] looks a string up by name.
 */
fun localizedPushText(key: String?, args: List<String>, fallback: String?, template: (String) -> String?): String? {
    val format = key?.let(template) ?: return fallback
    return try {
        String.format(format, *args.toTypedArray())
    } catch (e: IllegalFormatException) {
        fallback
    }
}
