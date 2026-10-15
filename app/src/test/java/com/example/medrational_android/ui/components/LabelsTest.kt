package com.example.medrational_android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelsTest {

    @Test
    fun `monogram takes the first letter of the first two words`() {
        assertEquals("C", monogram("Cardiologie"))
        assertEquals("MI", monogram("Médecine interne"))
        assertEquals("GO", monogram("Gynécologie obstétrique et néonatologie"))
    }

    @Test
    fun `accents are kept and the initials are upper case`() {
        assertEquals("ÉU", monogram("éthique urgences"))
    }

    @Test
    fun `symbols and emoji are skipped`() {
        assertEquals("CP", monogram("❤️ Cardio  - pédiatrie"))
        assertEquals("N", monogram("(Neurologie)"))
    }

    @Test
    fun `digits count as initials`() {
        assertEquals("2A", monogram("2e année"))
    }

    @Test
    fun `nothing usable gives no monogram`() {
        assertNull(monogram(null))
        assertNull(monogram("   "))
        assertNull(monogram("🩺 ✨"))
    }

    @Test
    fun `greeting uses the first word of the stored name`() {
        assertEquals("Salma", greetingName("Salma Benali"))
        assertEquals("Youssef", greetingName("  Youssef   Cherkaoui "))
    }

    @Test
    fun `greeting falls back when there is no real name`() {
        assertNull(greetingName(null))
        assertNull(greetingName(""))
        assertNull(greetingName("   "))
        // Some accounts only stored their email as the name
        assertNull(greetingName("salma@example.com"))
    }
}
