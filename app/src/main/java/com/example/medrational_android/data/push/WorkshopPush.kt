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
 * Workshop reminders and new study files arrive as Firebase Cloud Messaging pushes. The backend
 * publishes them to one topic, and every phone with the app subscribes to it at startup, signed in
 * or not, so the server never needs to know which phones exist or who is signed in on them.
 */
object WorkshopPush {

    // The topic of the backend this build talks to, picked by its environment flavor
    val TOPIC: String = workshopsTopicFor(BuildConfig.FLAVOR)

    // The channels a push names; must match PushMessage.WORKSHOP_CHANNEL and NEW_FILES_CHANNEL on
    // the backend. Separate so users can mute new files and keep the workshop reminders
    const val CHANNEL_ID = "workshop_reminders"
    const val NEW_FILES_CHANNEL_ID = "new_files"

    // Data keys saying what a push is about; Android copies them into the launch intent's extras
    // when the user taps a push shown while the app was closed
    const val EXTRA_PROJECT_ID = "projectId"
    const val EXTRA_FILE_ID = "fileId"
    const val EXTRA_REASONING_ID = "reasoningId"
    const val EXTRA_TYPE = "type"
    val DATA_KEYS = listOf(EXTRA_PROJECT_ID, EXTRA_FILE_ID, EXTRA_REASONING_ID, EXTRA_TYPE)

    // The type of a push about new study files (NotifType.NEW_FILES on the backend)
    const val TYPE_NEW_FILES = "NEW_FILES"

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
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
            .addOnFailureListener { Log.w(TAG, "Couldn't subscribe to $TOPIC", it) }
    }

    /** The channels pushes are shown on; they must exist before the first push arrives. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Called at every start, so the names follow the app's language
        val workshops = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.push_channel_description) }
        // New files aren't urgent: they make a sound but don't pop up over other apps
        val files = NotificationChannel(
            NEW_FILES_CHANNEL_ID, context.getString(R.string.push_files_channel_name), NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.push_files_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(workshops, files))
    }
}

/**
 * The workshops topic of an environment flavor (app/build.gradle.kts). Must match push.workshops-topic
 * in the backend's application-{dev,uat,prod}.yaml. Each environment is its own app (application ID),
 * so it can never be subscribed to another environment's topic.
 */
fun workshopsTopicFor(env: String): String = when (env) {
    "dev" -> "workshops-dev"
    "uat" -> "workshops-uat"
    "prod" -> "workshops"
    // Never fall back to prod: a new flavor must not notify real students by accident
    else -> throw IllegalArgumentException("No workshops topic for environment '$env'")
}

/**
 * The screen a push opens, or null when it doesn't name a valid one: its workshop project, a single
 * new file's page, or What's new for several new files.
 */
fun pushRoute(data: Map<String, String?>): String? {
    fun id(key: String) = data[key]?.trim()?.toLongOrNull()?.takeIf { it > 0 }
    id(WorkshopPush.EXTRA_PROJECT_ID)?.let { return workshopRoute(it) }
    if (data[WorkshopPush.EXTRA_TYPE] != WorkshopPush.TYPE_NEW_FILES) return null
    val fileId = id(WorkshopPush.EXTRA_FILE_ID)
    val reasoningId = id(WorkshopPush.EXTRA_REASONING_ID)
    return if (fileId != null && reasoningId != null) "file/$reasoningId/$fileId" else "novelties"
}

fun workshopRoute(projectId: Long): String = "workshops/$projectId"

/**
 * Where a tapped push leads: its [route], or the login screen when signed out (the app's API needs
 * a signed-in user). The route then opens after login.
 */
fun routeForPushTap(route: String, isLoggedIn: Boolean): String =
    if (isLoggedIn) route else "login"

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
