package com.example.medrational_android.data.settings

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {

    @Test
    fun `light is the default`() {
        assertEquals(AppTheme.LIGHT, themeFor(null))
    }

    @Test
    fun `stored choices are read back`() {
        assertEquals(AppTheme.DARK, themeFor("DARK"))
        assertEquals(AppTheme.SYSTEM, themeFor("SYSTEM"))
        assertEquals(AppTheme.LIGHT, themeFor("LIGHT"))
    }

    @Test
    fun `an unknown value falls back to light`() {
        assertEquals(AppTheme.LIGHT, themeFor("dark"))
        assertEquals(AppTheme.LIGHT, themeFor(""))
    }

    @Test
    fun `each theme sets the matching AppCompat night mode`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppTheme.LIGHT.nightMode)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppTheme.DARK.nightMode)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppTheme.SYSTEM.nightMode)
    }
}
