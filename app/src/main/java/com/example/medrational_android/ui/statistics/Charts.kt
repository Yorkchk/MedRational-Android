package com.example.medrational_android.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.model.StatPoint
import com.example.medrational_android.data.model.formatMillisShortDate
import com.example.medrational_android.data.model.parseIsoLocalDate
import com.example.medrational_android.ui.text.appLocale
import java.util.Locale

/** One line or set of bars of a chart. */
data class ChartSeries(val label: String, val points: List<StatPoint>, val color: Color)

private val ChartHeight = 140.dp

/** Bars for a single series, e.g. sign-ups per week. */
@Composable
fun BarChart(
    title: String,
    series: ChartSeries,
    modifier: Modifier = Modifier
) {
    val values = series.points.map { it.value }
    val ceiling = chartCeiling(values.maxOrNull() ?: 0L)
    val fractions = chartFractions(values, ceiling)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    ChartFrame(title = title, series = listOf(series), ceiling = ceiling, modifier = modifier) { canvasModifier ->
        Canvas(modifier = canvasModifier) {
            drawGrid(gridColor)
            if (fractions.isEmpty()) return@Canvas
            val slot = size.width / fractions.size
            val barWidth = slot * 0.6f
            fractions.forEachIndexed { index, fraction ->
                val barHeight = size.height * fraction
                if (barHeight <= 0f) return@forEachIndexed
                drawRoundRect(
                    color = series.color,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}

/** Lines sharing one scale, e.g. daily downloads and views. Series must have the same periods. */
@Composable
fun LineChart(
    title: String,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier
) {
    val ceiling = chartCeiling(series.flatMap { line -> line.points.map { it.value } }.maxOrNull() ?: 0L)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    ChartFrame(title = title, series = series, ceiling = ceiling, modifier = modifier) { canvasModifier ->
        Canvas(modifier = canvasModifier) {
            drawGrid(gridColor)
            series.forEach { line ->
                val fractions = chartFractions(line.points.map { it.value }, ceiling)
                if (fractions.size < 2) return@forEach
                val step = size.width / (fractions.size - 1)
                val path = Path()
                fractions.forEachIndexed { index, fraction ->
                    val x = index * step
                    val y = size.height * (1 - fraction)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = line.color,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Today's value stands out at the right end
                drawCircle(
                    color = line.color,
                    radius = 3.5.dp.toPx(),
                    center = Offset(size.width, size.height * (1 - fractions.last()))
                )
            }
        }
    }
}

// Title, the scale's top value, the chart, the first and last period, then the legend
@Composable
private fun ChartFrame(
    title: String,
    series: List<ChartSeries>,
    ceiling: Long,
    modifier: Modifier = Modifier,
    chart: @Composable (Modifier) -> Unit
) {
    val locale = appLocale()
    val points = series.firstOrNull()?.points.orEmpty()
    val first = points.firstOrNull()?.let { periodLabel(it.periodStart, locale) }
    val last = points.lastOrNull()?.let { periodLabel(it.periodStart, locale) }
    val peak = formatCount(series.flatMap { line -> line.points.map { it.value } }.maxOrNull() ?: 0L, locale)
    val summary = if (first != null && last != null) {
        stringResource(R.string.stats_chart_summary, title, first, last, peak)
    } else {
        title
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatCount(ceiling, locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        chart(
            Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .padding(top = 4.dp)
                .semantics { contentDescription = summary }
        )
        if (first != null && last != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(last, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (series.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                series.forEach { line -> LegendItem(line.label, line.color) }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// Baseline and the line of the scale's top value
private fun DrawScope.drawGrid(color: Color) {
    val stroke = 1.dp.toPx()
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke)
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
}

// "7 oct." for a period starting on 2026-10-07, or the raw value if it can't be read
private fun periodLabel(isoDate: String, locale: Locale): String =
    parseIsoLocalDate(isoDate)?.let { formatMillisShortDate(it, locale) } ?: isoDate
