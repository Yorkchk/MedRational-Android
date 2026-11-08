package com.example.medrational_android.data.crash

import android.content.Context
import com.example.medrational_android.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Only installed uat and prod builds report crashes: never debug builds (Android Studio, tests),
 * and never the dev environment, whose release build CI makes on every PR.
 */
internal fun crashReportingEnabled(flavor: String, debug: Boolean): Boolean = !debug && flavor != "dev"

/**
 * The Profile screen's "Test crash reporting" button: only in the UAT release build, where reports
 * are on and testers can check that a crash reaches Crashlytics. Never in the Play Store app.
 */
internal fun crashTestAvailable(flavor: String, debug: Boolean): Boolean =
    flavor == "uat" && crashReportingEnabled(flavor, debug)

/** Thrown on purpose by the test button; its name makes it easy to spot in the console. */
class CrashReportingTestException : RuntimeException("Test crash from the UAT Profile screen")

/**
 * Crashes and ANRs go to Firebase Crashlytics (Firebase console → Crashlytics), with the stack
 * trace, app version and device, and nothing about the user: no ID, email or name. Collection is
 * off in the manifest until [start] decides, so nothing is sent before.
 */
object CrashReporting {

    val testCrashAvailable: Boolean = crashTestAvailable(BuildConfig.FLAVOR, BuildConfig.DEBUG)

    /** Crashes the app on purpose; Crashlytics sends the report the next time the app opens. */
    fun crashForTest(): Nothing = throw CrashReportingTestException()

    /** Called at every app start, before anything else can crash. */
    fun start(context: Context) {
        // False in builds without app/google-services.json, where the plugin isn't applied
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(crashReportingEnabled(BuildConfig.FLAVOR, BuildConfig.DEBUG))
    }
}
