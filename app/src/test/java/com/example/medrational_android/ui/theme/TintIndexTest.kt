package com.example.medrational_android.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TintIndexTest {

    @Test
    fun `the same key always gets the same tint`() {
        assertEquals(tintIndex(42, 6), tintIndex(42, 6))
    }

    @Test
    fun `consecutive ids get different tints`() {
        val indexes = (1L..6L).map { tintIndex(it, 6) }

        assertEquals(6, indexes.toSet().size)
    }

    @Test
    fun `every key stays inside the palette, negative ones included`() {
        listOf(Long.MIN_VALUE, -7L, -1L, 0L, 5L, Long.MAX_VALUE).forEach { key ->
            assertTrue(tintIndex(key, 6) in 0 until 6)
        }
    }

    @Test
    fun `both themes have the same number of tints`() {
        assertEquals(LightTints.size, DarkTints.size)
    }
}
