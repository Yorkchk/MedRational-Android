package com.example.medrational_android.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatisticsMathTest {

    @Test
    fun `growth and decline are rounded percentages`() {
        assertEquals(WeekDelta.Up(50), weekDelta(current = 6, previous = 4))
        assertEquals(WeekDelta.Down(33), weekDelta(current = 2, previous = 3))
        assertEquals(WeekDelta.Up(200), weekDelta(current = 3, previous = 1))
    }

    @Test
    fun `nothing the week before is new rather than an infinite percentage`() {
        assertEquals(WeekDelta.New, weekDelta(current = 5, previous = 0))
    }

    @Test
    fun `same figure is flat, and zero both weeks shows nothing`() {
        assertEquals(WeekDelta.Flat, weekDelta(current = 4, previous = 4))
        assertEquals(WeekDelta.None, weekDelta(current = 0, previous = 0))
        // Below half a percent
        assertEquals(WeekDelta.Flat, weekDelta(current = 1001, previous = 1000))
    }

    @Test
    fun `a drop to zero is a full decline`() {
        assertEquals(WeekDelta.Down(100), weekDelta(current = 0, previous = 7))
    }

    @Test
    fun `percentages are rounded and safe without a total`() {
        assertEquals(33, percentOf(1, 3))
        assertEquals(67, percentOf(2, 3))
        assertEquals(0, percentOf(5, 0))
        assertEquals(100, percentOf(5, 5))
    }

    @Test
    fun `chart ceiling rounds up to 1, 2 or 5 times a power of ten`() {
        assertEquals(1L, chartCeiling(0))
        assertEquals(1L, chartCeiling(1))
        assertEquals(2L, chartCeiling(2))
        assertEquals(5L, chartCeiling(3))
        assertEquals(10L, chartCeiling(7))
        assertEquals(10L, chartCeiling(10))
        assertEquals(20L, chartCeiling(11))
        assertEquals(50L, chartCeiling(43))
        assertEquals(100L, chartCeiling(51))
        assertEquals(2000L, chartCeiling(1234))
    }

    @Test
    fun `chart fractions are relative to the ceiling and clamped`() {
        assertEquals(listOf(0f, 0.5f, 1f, 1f), chartFractions(listOf(0, 5, 10, 12), ceiling = 10))
        assertEquals(listOf(0f), chartFractions(listOf(3), ceiling = 0))
    }

    @Test
    fun `counts are grouped the way each language writes them`() {
        assertEquals("1,234", formatCount(1234, Locale.ENGLISH))
        assertEquals("1234", formatCount(1234, Locale.FRENCH).filter { it.isDigit() })
        assertEquals(5, formatCount(1234, Locale.FRENCH).length)
    }
}
