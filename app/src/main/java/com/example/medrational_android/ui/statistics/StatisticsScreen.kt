package com.example.medrational_android.ui.statistics

import androidx.annotation.PluralsRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.model.AdminStatistics
import com.example.medrational_android.data.model.ContentStats
import com.example.medrational_android.data.model.EngagementStats
import com.example.medrational_android.data.model.FileMetric
import com.example.medrational_android.data.model.formatIsoLocalDateTime
import com.example.medrational_android.ui.components.ContentWidth
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.components.SectionHeader
import com.example.medrational_android.ui.components.SwisraTopBar
import com.example.medrational_android.ui.components.adaptiveHorizontalPadding
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.TrendUp
import com.example.medrational_android.viewmodel.StatRange
import com.example.medrational_android.viewmodel.StatisticsUiState
import com.example.medrational_android.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val refreshError = (uiState as? StatisticsUiState.Success)?.refreshError

    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            snackbarHostState.showSnackbar(refreshError.asString(context))
            viewModel.refreshErrorShown()
        }
    }

    Scaffold(
        topBar = {
            SwisraTopBar(
                title = stringResource(R.string.stats_title),
                onBackClick = onBackClick,
                actions = {
                    val refreshing = (uiState as? StatisticsUiState.Success)?.refreshing == true
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(Spacing.md).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = viewModel::refresh, enabled = uiState !is StatisticsUiState.Loading) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.stats_refresh))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> LoadingState()
                is StatisticsUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::retry)
                is StatisticsUiState.Success -> StatisticsContent(
                    stats = state.stats,
                    range = state.range,
                    content = state.content,
                    onRangeSelected = viewModel::selectRange,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

// Three questions, top to bottom: are students using the app, what do they use, what needs a look
@Composable
private fun StatisticsContent(
    stats: AdminStatistics,
    range: StatRange,
    content: ContentStats,
    onRangeSelected: (StatRange) -> Unit,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = appLocale()
    val users = stats.users
    val usage = stats.usage

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding(ContentWidth.Reading), vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        stats.generatedAt?.let { generatedAt ->
            item {
                Text(
                    stringResource(R.string.stats_updated_at, formatIsoLocalDateTime(generatedAt, locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // This week
        item {
            Column {
                SectionHeader(stringResource(R.string.stats_section_week))
                Text(
                    stringResource(R.string.stats_week_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            TileRow(
                left = {
                    StatTile(
                        label = stringResource(R.string.stats_active_students),
                        value = formatCount(users.weeklyActiveStudents, locale),
                        delta = weekDelta(users.weeklyActiveStudents, users.weeklyActiveStudentsPreviousWeek),
                        caption = stringResource(
                            R.string.stats_active_share,
                            percentOf(users.weeklyActiveStudents, users.totalStudents),
                            formatCount(users.totalStudents, locale)
                        ),
                        modifier = it
                    )
                },
                right = {
                    StatTile(
                        label = stringResource(R.string.stats_new_students),
                        value = formatCount(users.newStudentsThisWeek, locale),
                        delta = weekDelta(users.newStudentsThisWeek, users.newStudentsPreviousWeek),
                        caption = countText(R.plurals.stats_registered_total, users.totalStudents),
                        modifier = it
                    )
                }
            )
        }
        item {
            TileRow(
                left = {
                    StatTile(
                        label = stringResource(R.string.stats_legend_downloads),
                        value = formatCount(usage.downloadsThisWeek, locale),
                        delta = weekDelta(usage.downloadsThisWeek, usage.downloadsPreviousWeek),
                        modifier = it
                    )
                },
                right = {
                    StatTile(
                        label = stringResource(R.string.stats_legend_views),
                        value = formatCount(usage.viewsThisWeek, locale),
                        delta = weekDelta(usage.viewsThisWeek, usage.viewsPreviousWeek),
                        modifier = it
                    )
                }
            )
        }
        item {
            StatCard {
                LineChart(
                    title = stringResource(R.string.stats_chart_activity),
                    series = listOf(
                        ChartSeries(
                            stringResource(R.string.stats_legend_downloads),
                            usage.dailyDownloads,
                            MaterialTheme.colorScheme.primary
                        ),
                        ChartSeries(
                            stringResource(R.string.stats_legend_views),
                            usage.dailyViews,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
            }
        }

        // Most popular
        item { SectionHeader(stringResource(R.string.stats_section_popular)) }
        item { RangeSelector(range = range, onRangeSelected = onRangeSelected) }
        item { TopFiles(files = content.topDownloaded.take(TOP_LIMIT), onFileClick = onFileClick) }
        item { TopCategories(shares = topCategoryShares(content.categories)) }

        // Needs attention
        item { SectionHeader(stringResource(R.string.stats_section_attention)) }
        item { AttentionCard(engagement = stats.engagement) }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) { content() }
    }
}

// Two tiles side by side with the same height
@Composable
private fun TileRow(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        left(Modifier.weight(1f).fillMaxHeight())
        right(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    delta: WeekDelta,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            DeltaChip(delta, modifier = Modifier.padding(top = Spacing.xs))
            if (caption != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeltaChip(delta: WeekDelta, modifier: Modifier = Modifier) {
    val (text, color) = when (delta) {
        is WeekDelta.Up -> stringResource(R.string.stats_delta_up, delta.percent) to TrendUp
        is WeekDelta.Down -> stringResource(R.string.stats_delta_down, delta.percent) to MaterialTheme.colorScheme.error
        WeekDelta.Flat -> stringResource(R.string.stats_delta_flat) to MaterialTheme.colorScheme.onSurfaceVariant
        WeekDelta.New -> stringResource(R.string.stats_delta_new) to TrendUp
        WeekDelta.None -> return
    }
    val description = stringResource(R.string.stats_delta_description, text)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(range: StatRange, onRangeSelected: (StatRange) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(StatRange.WEEK to R.string.stats_range_week, StatRange.ALL_TIME to R.string.stats_range_all)
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = range == option,
                onClick = { onRangeSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun TopFiles(
    files: List<FileMetric>,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    StatCard(modifier = modifier) {
        Text(stringResource(R.string.stats_top_files), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        if (files.isEmpty()) {
            EmptyText()
        }
        files.forEachIndexed { index, file ->
            if (index > 0) HorizontalDivider()
            val reasoningId = file.reasoningId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (reasoningId != null) Modifier.clickable { onFileClick(reasoningId, file.fileId) } else Modifier)
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(Spacing.xl)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        file.categoryName ?: stringResource(R.string.stats_unassigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    countText(R.plurals.stats_downloads_count, file.metricValue),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TopCategories(shares: List<CategoryShare>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val barShape = MaterialTheme.shapes.extraSmall

    StatCard(modifier = modifier) {
        Text(stringResource(R.string.stats_top_categories), style = MaterialTheme.typography.titleSmall)
        if (shares.isEmpty()) {
            EmptyText()
        }
        shares.forEach { share ->
            Column(modifier = Modifier.padding(top = Spacing.md)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        share.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.stats_share, share.percent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.sm)
                        .background(trackColor, barShape)
                ) {
                    if (share.fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(share.fraction)
                                .fillMaxHeight()
                                .background(barColor, barShape)
                        )
                    }
                }
            }
        }
    }
}

// Only what an admin can act on; a row with nothing to report is left out
@Composable
private fun AttentionCard(engagement: EngagementStats, modifier: Modifier = Modifier) {
    val locale = appLocale()
    // Label, count, and the share of all files where that helps
    val rows = buildList<Triple<Int, Long, Int?>> {
        if (engagement.unreadSuggestions > 0) {
            add(Triple(R.string.stats_unread_suggestions, engagement.unreadSuggestions, null))
        }
        if (engagement.openWorkshops > 0) {
            add(Triple(R.string.stats_open_projects, engagement.openWorkshops, null))
        }
        if (engagement.filesWithoutDownloads > 0) {
            add(
                Triple(
                    R.string.stats_never_downloaded,
                    engagement.filesWithoutDownloads,
                    percentOf(engagement.filesWithoutDownloads, engagement.totalFiles)
                )
            )
        }
    }

    StatCard(modifier = modifier) {
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.stats_all_clear),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        rows.forEachIndexed { index, (labelId, count, percent) ->
            if (index > 0) HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(labelId), style = MaterialTheme.typography.bodyMedium)
                    if (percent != null) {
                        Text(
                            stringResource(R.string.stats_never_downloaded_caption, percent),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    formatCount(count, locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyText() {
    Text(
        stringResource(R.string.stats_list_empty),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.sm)
    )
}

// "1 234 téléchargements": the plural form follows the count, the number the screen's locale
@Composable
private fun countText(@PluralsRes id: Int, count: Long): String =
    pluralStringResource(id, count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatCount(count, appLocale()))
