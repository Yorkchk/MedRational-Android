package com.example.medrational_android.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Light or dark look, picked in the profile. Light is the default, whatever the phone's setting.
 *
 * AppCompat's night mode carries the choice everywhere: Compose's isSystemInDarkTheme(), the
 * values-night window and splash colors, system dialogs and the status bar icons. AppCompat doesn't
 * keep it between launches, so it's stored here and applied at every start.
 */
enum class AppTheme(val nightMode: Int) {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        private const val PREFS = "settings"
        private const val KEY = "theme"

        /** The stored theme, or light when none was picked yet. */
        fun current(context: Context): AppTheme =
            themeFor(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

        /** Stores the choice and applies it, which recreates the activity. */
        fun set(context: Context, theme: AppTheme) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, theme.name).apply()
            AppCompatDelegate.setDefaultNightMode(theme.nightMode)
        }

        /** Applies the stored theme. Call in MainActivity.onCreate before super.onCreate. */
        fun apply(context: Context) {
            AppCompatDelegate.setDefaultNightMode(current(context).nightMode)
        }
    }
}

/** The theme for a stored name, light for nothing or an unknown value. */
fun themeFor(stored: String?): AppTheme = AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.LIGHT
