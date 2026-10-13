package com.example.medrational_android.data.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    // Students in Morocco get French until they pick English in their profile
    @Test
    fun `french is the default`() {
        assertEquals(AppLanguage.FRENCH, languageFor(""))
        assertEquals(AppLanguage.FRENCH, languageFor("fr"))
        assertEquals(AppLanguage.FRENCH, languageFor("fr-MA"))
    }

    @Test
    fun `english when it was picked`() {
        assertEquals(AppLanguage.ENGLISH, languageFor("en"))
        assertEquals(AppLanguage.ENGLISH, languageFor("en-US"))
        assertEquals(AppLanguage.ENGLISH, languageFor("en,fr"))
    }

    // The app is only translated into French and English
    @Test
    fun `other languages fall back to french`() {
        assertEquals(AppLanguage.FRENCH, languageFor("ar-MA"))
        assertEquals(AppLanguage.FRENCH, languageFor("fr,en"))
    }

    @Test
    fun `languages are named in their own language`() {
        assertEquals("Français", AppLanguage.FRENCH.nativeName)
        assertEquals("English", AppLanguage.ENGLISH.nativeName)
    }
}
