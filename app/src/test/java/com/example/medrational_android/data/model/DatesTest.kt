package com.example.medrational_android.data.model

import java.util.Locale
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
        assertEquals("12 Oct 2026, 09:05", formatMillisDateTime(millis, Locale.ENGLISH))
    }

    @Test
    fun `times follow the phone's 12 or 24-hour clock`() {
        val evening = parseIsoLocalDateTime("2026-10-11T19:53:00")!!

        assertEquals("11 Oct 2026, 19:53", formatMillisDateTime(evening, Locale.ENGLISH, is24Hour = true))
        assertEquals("11 Oct 2026, 7:53 PM", formatMillisDateTime(evening, Locale.ENGLISH, is24Hour = false))
        assertEquals("11 Oct 2026, 7:53 AM", formatIsoLocalDateTime("2026-10-11T07:53:00", Locale.ENGLISH, is24Hour = false))
        assertEquals("soon", formatIsoLocalDateTime("soon", Locale.ENGLISH, is24Hour = false))
    }

    // The app is in French by default
    @Test
    fun `month names follow the app's language`() {
        val evening = parseIsoLocalDateTime("2026-03-11T19:53:00")!!

        assertEquals("11 mars 2026, 19:53", formatMillisDateTime(evening, Locale.FRENCH))
        assertEquals("11 Mar 2026, 19:53", formatMillisDateTime(evening, Locale.ENGLISH))
    }

    @Test
    fun `editing opens the date picker on the stored day`() {
        // Just after midnight locally (UTC+1), which is still the previous day in UTC
        val stored = parseIsoLocalDateTime("2026-10-12T00:30:00")!!

        assertEquals(1_791_763_200_000L, toUtcDateMillis(stored))
        assertEquals(stored, combineDateAndTime(toUtcDateMillis(stored), hour = 0, minute = 30))
    }

    @Test
    fun `backend LocalDate is local midnight of that day`() {
        val millis = parseIsoLocalDate("2026-10-07")!!

        assertEquals("2026-10-07T00:00:00", toIsoLocalDateTime(millis))
        assertNull(parseIsoLocalDate("07/10/2026"))
        assertNull(parseIsoLocalDate(""))
    }

    @Test
    fun `chart dates are short and follow the app's language`() {
        val millis = parseIsoLocalDate("2026-10-07")!!

        assertEquals("7 Oct", formatMillisShortDate(millis, Locale.ENGLISH))
        assertEquals("7 oct.", formatMillisShortDate(millis, Locale.FRENCH))
    }
}
