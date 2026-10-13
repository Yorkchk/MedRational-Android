package com.example.medrational_android.data.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The app's language. French is the default, whatever the phone's language, because the app is
 * for students in Morocco; English can be picked in the profile. Only the app's own text is
 * translated: what admins type (titles, descriptions, hashtags...) is shown as written.
 *
 * AppCompat stores the choice (on Android 12 and older through the AppLocalesMetadataHolderService
 * in the manifest) and recreates the activity when it changes. [nativeName] is always shown in
 * its own language, so anyone can find theirs.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English");

    companion object {
        /** The stored language, or French when none was picked yet. */
        fun current(): AppLanguage = languageFor(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        fun set(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }

        /** Starts the app in French until the user picks a language. Call after `super.onCreate`. */
        fun ensureDefault() {
            if (AppCompatDelegate.getApplicationLocales().isEmpty) set(FRENCH)
        }
    }
}

/** The language for stored locale tags like "en-US,fr": English only when it comes first. */
fun languageFor(languageTags: String): AppLanguage =
    if (languageTags.substringBefore(',').substringBefore('-').trim().equals("en", ignoreCase = true)) {
        AppLanguage.ENGLISH
    } else {
        AppLanguage.FRENCH
    }
