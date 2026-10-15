package com.example.medrational_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalTints = staticCompositionLocalOf { LightTints }

/**
 * The SWISRA look: brand colors in light and dark (following the phone), Plus Jakarta Sans and
 * Inter, and rounded shapes. Dynamic color is deliberately off so the brand stays recognisable.
 */
@Composable
fun SwisraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTints provides if (darkTheme) DarkTints else LightTints) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SwisraTypography,
            shapes = SwisraShapes,
            content = content
        )
    }
}

/** Soft tints for decorative tiles (category monograms), matching the current light or dark theme. */
object SwisraTints {
    val all: List<Tint>
        @Composable
        @ReadOnlyComposable
        get() = LocalTints.current

    /** The tint for an item, always the same one for the same [key]. */
    @Composable
    @ReadOnlyComposable
    fun forKey(key: Long): Tint = all[tintIndex(key, all.size)]
}

/** Index in a palette of [size] tints for [key]: stable, and spread over the palette. */
fun tintIndex(key: Long, size: Int): Int = Math.floorMod(key, size.toLong()).toInt()
