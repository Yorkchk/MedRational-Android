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
                DrawerDestination.FAVORITES,
                DrawerDestination.DOWNLOADS,
                DrawerDestination.SUGGESTIONS,
                DrawerDestination.SIGN_OUT
            ),
            destinations(isAdmin = false, isLoggedIn = true)
        )
    }

    @Test
    fun `admins see the suggestions inbox`() {
        assertEquals("Suggestions inbox", label(DrawerDestination.SUGGESTIONS, isAdmin = true))
        assertEquals("Suggestions", label(DrawerDestination.SUGGESTIONS, isAdmin = false))
    }

    @Test
    fun `signed-out session hides suggestions and sign out`() {
        val entries = destinations(isAdmin = false, isLoggedIn = false)

        assertFalse(DrawerDestination.SUGGESTIONS in entries)
        assertFalse(DrawerDestination.SIGN_OUT in entries)
        assertEquals(DrawerDestination.HOME, entries.first())
    }

    @Test
    fun `admins get the same sections as users`() {
        assertEquals(destinations(isAdmin = false, isLoggedIn = true), destinations(isAdmin = true, isLoggedIn = true))
    }
}
