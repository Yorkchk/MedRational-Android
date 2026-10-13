package com.example.medrational_android.ui.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeDrawerEntriesTest {

    private fun destinations(isAdmin: Boolean, isLoggedIn: Boolean) =
        homeDrawerEntries(isAdmin, isLoggedIn).map { it.destination }

    private fun label(destination: DrawerDestination, isAdmin: Boolean) =
        homeDrawerEntries(isAdmin, isLoggedIn = true).single { it.destination == destination }.label

    @Test
    fun `signed-in user gets every section with home first and sign out last`() {
        assertEquals(
            listOf(
                DrawerDestination.HOME,
                DrawerDestination.SEARCH,
                DrawerDestination.NOVELTIES,
                DrawerDestination.WORKSHOPS,
                DrawerDestination.FAVORITES,
                DrawerDestination.DOWNLOADS,
                DrawerDestination.SUGGESTIONS,
                DrawerDestination.PROFILE,
                DrawerDestination.SIGN_OUT
            ),
            destinations(isAdmin = false, isLoggedIn = true)
        )
    }

    @Test
    fun `workshops are listed as Projects`() {
        assertEquals("Projects", label(DrawerDestination.WORKSHOPS, isAdmin = false))
    }

    @Test
    fun `admins see the suggestions inbox`() {
        assertEquals("Suggestions inbox", label(DrawerDestination.SUGGESTIONS, isAdmin = true))
        assertEquals("Suggestions", label(DrawerDestination.SUGGESTIONS, isAdmin = false))
    }

    @Test
    fun `signed-out session hides account sections`() {
        val entries = destinations(isAdmin = false, isLoggedIn = false)

        assertFalse(DrawerDestination.NOVELTIES in entries)
        // The workshops API rejects anonymous calls
        assertFalse(DrawerDestination.WORKSHOPS in entries)
        assertFalse(DrawerDestination.SUGGESTIONS in entries)
        // The profile is the signed-in account
        assertFalse(DrawerDestination.PROFILE in entries)
        assertFalse(DrawerDestination.SIGN_OUT in entries)
        assertEquals(DrawerDestination.HOME, entries.first())
    }

    @Test
    fun `admins get the same sections as users`() {
        assertEquals(destinations(isAdmin = false, isLoggedIn = true), destinations(isAdmin = true, isLoggedIn = true))
    }

    @Test
    fun `badge count is capped at 99+`() {
        assertEquals("7", formatBadgeCount(7))
        assertEquals("99", formatBadgeCount(99))
        assertEquals("99+", formatBadgeCount(100))
    }
}
