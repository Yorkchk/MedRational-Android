package com.example.medrational_android.data.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val ISO_LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss"

/**
 * Epoch millis of an ISO LocalDateTime from the backend ("2026-03-12T10:15:30", optionally with
 * fractional seconds), read in the device's time zone, or null when it can't be parsed.
 * The backend has no zone, so this assumes the device and the server share one.
 */
fun parseIsoLocalDateTime(isoDateTime: String): Long? {
    val value = isoDateTime.substringBefore('.')
    val pattern = if (value.length == 16) "yyyy-MM-dd'T'HH:mm" else ISO_LOCAL_DATE_TIME
    return try {
        SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)?.time
    } catch (e: ParseException) {
        null
    }
}

/** The ISO LocalDateTime the backend expects for [millis], in the device's time zone. */
fun toIsoLocalDateTime(millis: Long): String = SimpleDateFormat(ISO_LOCAL_DATE_TIME, Locale.US).format(Date(millis))

/**
 * Local epoch millis for the day picked in a Material DatePicker (UTC midnight of that day) at
 * [hour]:[minute] on the device's clock.
 */
fun combineDateAndTime(utcDateMillis: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDateMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
    }.timeInMillis
}

/** The inverse of [combineDateAndTime]: UTC midnight of [localMillis]'s local day, as a DatePicker expects. */
fun toUtcDateMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Local midnight of an ISO LocalDate from the backend ("2026-10-07"), or null when it can't be parsed. */
fun parseIsoLocalDate(isoDate: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(isoDate)?.time
} catch (e: ParseException) {
    null
}

/** "7 Oct" for local epoch millis; month names follow [locale] ("7 oct." in French). */
fun formatMillisShortDate(millis: Long, locale: Locale): String = SimpleDateFormat("d MMM", locale).format(Date(millis))

/**
 * "12 Mar 2026, 19:15", or "12 Mar 2026, 7:15 PM" when [is24Hour] is false, for local epoch millis.
 * Month names follow [locale], the app's language: "12 mars 2026, 19:15" in French.
 */
fun formatMillisDateTime(millis: Long, locale: Locale, is24Hour: Boolean = true): String =
    SimpleDateFormat(if (is24Hour) "d MMM yyyy, HH:mm" else "d MMM yyyy, h:mm a", locale).format(Date(millis))

/** A backend LocalDateTime formatted like [formatMillisDateTime], or the raw value if it can't be read. */
fun formatIsoLocalDateTime(isoDateTime: String, locale: Locale, is24Hour: Boolean = true): String =
    parseIsoLocalDateTime(isoDateTime)?.let { formatMillisDateTime(it, locale, is24Hour) } ?: isoDateTime
