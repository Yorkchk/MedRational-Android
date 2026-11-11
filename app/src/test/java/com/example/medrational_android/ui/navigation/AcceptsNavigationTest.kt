package com.example.medrational_android.ui.navigation

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptsNavigationTest {

    @Test
    fun `a settled screen accepts taps`() {
        assertTrue(acceptsNavigation(Lifecycle.State.RESUMED))
    }

    @Test
    fun `a screen still transitioning in ignores taps`() {
        // Navigation Compose keeps the incoming screen STARTED until its transition ends
        assertFalse(acceptsNavigation(Lifecycle.State.STARTED))
    }

    @Test
    fun `no screen or a stopped one ignores taps`() {
        assertFalse(acceptsNavigation(null))
        assertFalse(acceptsNavigation(Lifecycle.State.CREATED))
        assertFalse(acceptsNavigation(Lifecycle.State.DESTROYED))
    }
}
