package com.example.medrational_android.ui.categories

import com.example.medrational_android.R
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
        assertEquals(R.string.drawer_projects, label(DrawerDestination.WORKSHOPS, isAdmin = false))
    }

    @Test
    fun `admins see the suggestions inbox`() {
        assertEquals(R.string.drawer_suggestions_inbox, label(DrawerDestination.SUGGESTIONS, isAdmin = true))
        assertEquals(R.string.drawer_suggestions, label(DrawerDestination.SUGGESTIONS, isAdmin = false))
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
    fun `admins also get statistics, before their profile`() {
        val userSections = destinations(isAdmin = false, isLoggedIn = true)
        val adminSections = destinations(isAdmin = true, isLoggedIn = true)

        assertEquals(userSections, adminSections - DrawerDestination.STATISTICS)
        assertEquals(DrawerDestination.STATISTICS, adminSections[adminSections.indexOf(DrawerDestination.PROFILE) - 1])
        assertEquals(R.string.drawer_statistics, label(DrawerDestination.STATISTICS, isAdmin = true))
    }

    // Its endpoint answers 403 to anyone else, which signs them out
    @Test
    fun `statistics are hidden from users`() {
        assertFalse(DrawerDestination.STATISTICS in destinations(isAdmin = false, isLoggedIn = true))
        assertFalse(DrawerDestination.STATISTICS in destinations(isAdmin = false, isLoggedIn = false))
    }

    @Test
    fun `badge count is capped at 99+`() {
        assertEquals("7", formatBadgeCount(7))
        assertEquals("99", formatBadgeCount(99))
        assertEquals("99+", formatBadgeCount(100))
    }
}
