package com.example.medrational_android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTest {

    @Test
    fun `phones show one column of cards, tablets two`() {
        assertEquals(1, listColumnsFor(360))
        assertEquals(1, listColumnsFor(599))
        assertEquals(2, listColumnsFor(600))
        assertEquals(2, listColumnsFor(800))
        assertEquals(2, listColumnsFor(1280))
    }

    @Test
    fun `lists of long cards can stay in one column on tablets`() {
        assertEquals(1, listColumnsFor(360, maxColumns = 1))
        assertEquals(1, listColumnsFor(800, maxColumns = 1))
        assertEquals(2, listColumnsFor(800))
    }

    @Test
    fun `the file page splits in two only from a tablet in landscape`() {
        assertFalse(isTwoPane(411))
        assertFalse(isTwoPane(800))
        assertTrue(isTwoPane(840))
        assertTrue(isTwoPane(1280))
    }

    @Test
    fun `phones keep the usual side padding`() {
        assertEquals(20, centeredPaddingDp(widthDp = 360, maxContentDp = 720, minPaddingDp = 20))
        assertEquals(20, centeredPaddingDp(widthDp = 740, maxContentDp = 720, minPaddingDp = 20))
    }

    @Test
    fun `wide screens center the content at its max width`() {
        assertEquals(280, centeredPaddingDp(widthDp = 1280, maxContentDp = 720, minPaddingDp = 20))
        assertEquals(40, centeredPaddingDp(widthDp = 1280, maxContentDp = 1200, minPaddingDp = 20))
    }
}
