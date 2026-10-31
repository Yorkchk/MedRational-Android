package com.example.medrational_android.ui.reasonings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSelectionTest {

    @Test
    fun `the Select button opens the selection with nothing picked`() {
        val selection = FileSelection().start()

        assertTrue(selection.active)
        assertEquals(emptyList<Long>(), selection.ids)
    }

    @Test
    fun `a long-press opens the selection with that file picked`() {
        val selection = FileSelection().toggle(7)

        assertTrue(selection.active)
        assertEquals(listOf(7L), selection.ids)
    }

    @Test
    fun `tapping a file picks it, and tapping it again unpicks it`() {
        val picked = FileSelection().start().toggle(1).toggle(2)
        assertEquals(listOf(1L, 2L), picked.ids)

        assertEquals(listOf(2L), picked.toggle(1).ids)
    }

    @Test
    fun `unpicking the last file keeps the selection open`() {
        val selection = FileSelection().start().toggle(1).toggle(1)

        assertTrue(selection.active)
        assertEquals(emptyList<Long>(), selection.ids)
    }

    @Test
    fun `clearing closes the selection`() {
        val selection = FileSelection().start().toggle(1).clear()

        assertFalse(selection.active)
        assertEquals(emptyList<Long>(), selection.ids)
    }
}
