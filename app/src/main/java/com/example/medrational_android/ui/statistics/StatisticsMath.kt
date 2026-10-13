package com.example.medrational_android.ui.statistics

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** How a weekly figure moved against the previous week. */
sealed interface WeekDelta {
    data class Up(val percent: Int) : WeekDelta
    data class Down(val percent: Int) : WeekDelta
    data object Flat : WeekDelta
    // Something this week after nothing the week before, where a percentage means nothing
    data object New : WeekDelta
    // Nothing either week
    data object None : WeekDelta
}

fun weekDelta(current: Long, previous: Long): WeekDelta = when {
    current == previous -> if (current == 0L) WeekDelta.None else WeekDelta.Flat
    previous == 0L -> WeekDelta.New
    else -> {
        val percent = ((current - previous) * 100.0 / previous).roundToInt()
        when {
            percent > 0 -> WeekDelta.Up(percent)
            percent < 0 -> WeekDelta.Down(-percent)
            // A change too small to show as a percentage
            else -> WeekDelta.Flat
        }
    }
}

/** [part] as a whole percentage of [total], 0 when there is no total. */
fun percentOf(part: Long, total: Long): Int =
    if (total <= 0L) 0 else (part * 100.0 / total).roundToInt().coerceIn(0, 100)

/**
 * Top of a chart's value axis: [max] rounded up to 1, 2 or 5 times a power of ten, so the label
 * reads well ("50" rather than "43"). At least 1, so an empty chart still has a scale.
 */
fun chartCeiling(max: Long): Long {
    if (max <= 1L) return 1L
    var magnitude = 1L
    while (magnitude * 10 < max) magnitude *= 10
    for (step in longArrayOf(1, 2, 5, 10)) {
        if (step * magnitude >= max) return step * magnitude
    }
    return 10 * magnitude
}

/** Each value as a fraction of [ceiling], between 0 and 1. */
fun chartFractions(values: List<Long>, ceiling: Long): List<Float> =
    values.map { if (ceiling <= 0L) 0f else (it.toFloat() / ceiling).coerceIn(0f, 1f) }

/** "1 234" in French, "1,234" in English. */
fun formatCount(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)
