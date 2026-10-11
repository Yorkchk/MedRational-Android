package com.example.medrational_android.data.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class DatesTest {

    private val originalZone = TimeZone.getDefault()

    // Not UTC, so a mix-up between UTC and local time shows
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Casablanca"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `backend LocalDateTime round-trips through epoch millis`() {
        val millis = parseIsoLocalDateTime("2026-10-12T14:30:00")!!

        assertEquals("2026-10-12T14:30:00", toIsoLocalDateTime(millis))
    }

    @Test
    fun `fractional seconds and minute precision are accepted`() {
        val expected = parseIsoLocalDateTime("2026-10-12T14:30:00")

        assertEquals(expected, parseIsoLocalDateTime("2026-10-12T14:30:00.123456"))
        assertEquals(expected, parseIsoLocalDateTime("2026-10-12T14:30"))
    }

    @Test
    fun `garbage is null`() {
        assertNull(parseIsoLocalDateTime("soon"))
        assertNull(parseIsoLocalDateTime("2026-13-45T99:99:00"))
    }

    @Test
    fun `picked day and time become that local wall-clock time`() {
        // What a Material DatePicker returns for 12 Oct 2026: midnight UTC
        val pickedDay = 1_791_763_200_000L

        val millis = combineDateAndTime(pickedDay, hour = 9, minute = 5)

        assertEquals("2026-10-12T09:05:00", toIsoLocalDateTime(millis))
        assertEquals("12 Oct 2026, 09:05", formatMillisDateTime(millis))
    }
}
