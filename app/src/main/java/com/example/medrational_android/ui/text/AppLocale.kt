package com.example.medrational_android.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/** The language the screen is shown in, for dates and sizes ("12 mars 2026", "2,5 Mo"). */
@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]
