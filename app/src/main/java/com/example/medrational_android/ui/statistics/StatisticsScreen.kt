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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
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
import com.example.medrational_android.data.model.CategoryActivity
import com.example.medrational_android.data.model.ContentStats
import com.example.medrational_android.data.model.FileMetric
import com.example.medrational_android.data.model.HashtagActivity
import com.example.medrational_android.data.model.TrendingItem
import com.example.medrational_android.data.model.formatIsoLocalDateTime
import com.example.medrational_android.ui.components.formatAverage
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
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
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    val refreshing = (uiState as? StatisticsUiState.Success)?.refreshing == true
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = viewModel::refresh, enabled = uiState !is StatisticsUiState.Loading) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.stats_refresh))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is StatisticsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::retry) { Text(stringResource(R.string.common_retry)) }
                    }
                }
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
    val engagement = stats.engagement
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                stats.generatedAt?.let {
                    Text(
                        stringResource(R.string.stats_updated_at, formatIsoLocalDateTime(it, locale)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(R.string.stats_scope_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Students
        item { SectionTitle(stringResource(R.string.stats_section_students)) }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_total_students),
                        value = formatCount(users.totalStudents, locale),
                        caption = countText(R.plurals.stats_new_students, users.newStudentsThisWeek),
                        delta = weekDelta(users.newStudentsThisWeek, users.newStudentsPreviousWeek),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_weekly_active),
                        value = formatCount(users.weeklyActiveStudents, locale),
                        caption = countText(R.plurals.stats_active_today, users.dailyActiveStudents),
                        delta = weekDelta(users.weeklyActiveStudents, users.weeklyActiveStudentsPreviousWeek),
                        modifier = it
                    )
                }
            )
        }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_stickiness),
                        value = stringResource(
                            R.string.stats_percent,
                            percentOf(users.dailyActiveStudents, users.weeklyActiveStudents)
                        ),
                        caption = stringResource(R.string.stats_stickiness_caption),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_activation),
                        value = stringResource(R.string.stats_percent, percentOf(users.activatedStudents, users.totalStudents)),
                        caption = stringResource(R.string.stats_activation_caption),
                        modifier = it
                    )
                }
            )
        }
        item {
            StatCard {
                BarChart(
                    title = stringResource(R.string.stats_chart_signups),
                    series = ChartSeries(stringResource(R.string.stats_chart_signups), users.weeklySignUps, primary)
                )
            }
        }
        item {
            StatCard {
                BarChart(
                    title = stringResource(R.string.stats_chart_weekly_active),
                    series = ChartSeries(stringResource(R.string.stats_chart_weekly_active), users.weeklyActiveSeries, tertiary)
                )
            }
        }

        // Usage
        item { SectionTitle(stringResource(R.string.stats_section_usage)) }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_downloads_week),
                        value = formatCount(usage.downloadsThisWeek, locale),
                        caption = stringResource(R.string.stats_total_caption, formatCount(usage.totalDownloads, locale)),
                        delta = weekDelta(usage.downloadsThisWeek, usage.downloadsPreviousWeek),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_views_week),
                        value = formatCount(usage.viewsThisWeek, locale),
                        caption = stringResource(R.string.stats_total_caption, formatCount(usage.totalViews, locale)),
                        delta = weekDelta(usage.viewsThisWeek, usage.viewsPreviousWeek),
                        modifier = it
                    )
                }
            )
        }
        item {
            StatCard {
                LineChart(
                    title = stringResource(R.string.stats_chart_daily_usage),
                    series = listOf(
                        ChartSeries(stringResource(R.string.stats_legend_downloads), usage.dailyDownloads, primary),
                        ChartSeries(stringResource(R.string.stats_legend_views), usage.dailyViews, tertiary)
                    )
                )
            }
        }

        // Trending
        item { SectionTitle(stringResource(R.string.stats_section_trending)) }
        item {
            StatCard {
                Text(
                    stringResource(R.string.stats_trending_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                TrendingList(
                    title = stringResource(R.string.stats_trending_files),
                    items = stats.trends.files,
                    onItemClick = { item -> item.reasoningId?.let { onFileClick(it, item.id) } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TrendingList(
                    title = stringResource(R.string.stats_trending_categories),
                    items = stats.trends.categories,
                    onItemClick = null
                )
            }
        }

        // Content
        item { SectionTitle(stringResource(R.string.stats_section_content)) }
        item { RangeSelector(range = range, onRangeSelected = onRangeSelected) }
        item {
            Leaderboard(
                title = stringResource(R.string.stats_top_downloaded),
                items = content.topDownloaded,
                metric = { countText(R.plurals.stats_downloads_count, it.metricValue) },
                onFileClick = onFileClick
            )
        }
        item {
            Leaderboard(
                title = stringResource(R.string.stats_top_viewed),
                items = content.topViewed,
                metric = { countText(R.plurals.stats_views_count, it.metricValue) },
                onFileClick = onFileClick
            )
        }
        item {
            Leaderboard(
                title = stringResource(R.string.stats_most_favorited),
                items = content.mostFavorited,
                metric = { countText(R.plurals.stats_favorites_count, it.metricValue) },
                onFileClick = onFileClick
            )
        }
        item { CategoryBreakdown(categories = content.categories) }
        item { HashtagList(hashtags = content.topHashtags) }

        // Engagement
        item { SectionTitle(stringResource(R.string.stats_section_engagement)) }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_favorites_week),
                        value = formatCount(engagement.favoritesThisWeek, locale),
                        caption = stringResource(R.string.stats_total_caption, formatCount(engagement.totalFavorites, locale)),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_ratings_week),
                        value = formatCount(engagement.ratingsThisWeek, locale),
                        caption = stringResource(R.string.stats_total_caption, formatCount(engagement.totalRatings, locale)),
                        modifier = it
                    )
                }
            )
        }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_average_rating),
                        value = engagement.averageRating?.let { "${formatAverage(it)} ★" }
                            ?: stringResource(R.string.stats_no_value),
                        caption = countText(R.plurals.stats_ratings_count, engagement.totalRatings),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_new_files),
                        value = formatCount(engagement.newFilesThisWeek, locale),
                        caption = countText(R.plurals.stats_files_total, engagement.totalFiles),
                        modifier = it
                    )
                }
            )
        }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_cold_files),
                        value = formatCount(engagement.filesWithoutDownloads, locale),
                        caption = stringResource(
                            R.string.stats_cold_caption,
                            percentOf(engagement.filesWithoutDownloads, engagement.totalFiles)
                        ),
                        modifier = it
                    )
                },
                right = null
            )
        }
        item {
            Leaderboard(
                title = stringResource(R.string.stats_highest_rated),
                items = engagement.highestRated,
                metric = { file ->
                    val average = file.avgRating?.let { "${formatAverage(it)} ★" } ?: stringResource(R.string.stats_no_value)
                    "$average · ${countText(R.plurals.stats_ratings_count, (file.totalRatings ?: 0).toLong())}"
                },
                onFileClick = onFileClick
            )
        }

        // Community
        item { SectionTitle(stringResource(R.string.stats_section_community)) }
        item {
            KpiRow(
                left = {
                    KpiCard(
                        label = stringResource(R.string.stats_open_suggestions),
                        value = formatCount(engagement.openSuggestions, locale),
                        caption = countText(R.plurals.stats_unread, engagement.unreadSuggestions),
                        modifier = it
                    )
                },
                right = {
                    KpiCard(
                        label = stringResource(R.string.stats_open_workshops),
                        value = formatCount(engagement.openWorkshops, locale),
                        caption = stringResource(
                            R.string.stats_workshops_caption,
                            formatCount(engagement.upcomingWorkshops, locale),
                            formatCount(engagement.closedWorkshops, locale)
                        ),
                        modifier = it
                    )
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp)
    )
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

// Two cards side by side with the same height; a missing right card leaves its half empty
@Composable
private fun KpiRow(
    left: @Composable (Modifier) -> Unit,
    right: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        left(Modifier.weight(1f).fillMaxHeight())
        if (right != null) {
            right(Modifier.weight(1f).fillMaxHeight())
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    delta: WeekDelta = WeekDelta.None
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                DeltaChip(delta)
            }
            if (caption != null) {
                Spacer(modifier = Modifier.height(4.dp))
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
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
private fun Leaderboard(
    title: String,
    items: List<FileMetric>,
    metric: @Composable (FileMetric) -> String,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    StatCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        if (items.isEmpty()) {
            EmptyText()
        }
        items.forEachIndexed { index, file ->
            if (index > 0) HorizontalDivider()
            val reasoningId = file.reasoningId
            RankedRow(
                rank = index + 1,
                title = file.fileName,
                subtitle = listOfNotNull(file.categoryName, file.reasoningTitle).joinToString(" · ")
                    .ifEmpty { stringResource(R.string.stats_unassigned) },
                trailing = metric(file),
                onClick = reasoningId?.let { { onFileClick(it, file.fileId) } }
            )
        }
    }
}

@Composable
private fun TrendingList(
    title: String,
    items: List<TrendingItem>,
    onItemClick: ((TrendingItem) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val locale = appLocale()
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (items.isEmpty()) {
            EmptyText(R.string.stats_trending_empty)
        }
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            val delta = weekDelta(item.thisWeek, item.previousWeek)
            RankedRow(
                rank = index + 1,
                title = item.name,
                subtitle = stringResource(
                    R.string.stats_trending_counts,
                    formatCount(item.thisWeek, locale),
                    formatCount(item.previousWeek, locale)
                ),
                trailing = null,
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = TrendUp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        DeltaChip(delta)
                    }
                },
                onClick = if (onItemClick != null && item.reasoningId != null) {
                    { onItemClick(item) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun RankedRow(
    rank: Int,
    title: String,
    subtitle: String,
    trailing: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun CategoryBreakdown(categories: List<CategoryActivity>, modifier: Modifier = Modifier) {
    val totals = categories.map { it.downloads + it.views + it.favorites }
    val sum = totals.sum()
    val busiest = totals.maxOrNull() ?: 0L
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    StatCard(modifier = modifier) {
        Text(stringResource(R.string.stats_categories), style = MaterialTheme.typography.titleSmall)
        if (categories.isEmpty()) {
            EmptyText()
        }
        categories.forEachIndexed { index, category ->
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        category.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.stats_share, percentOf(totals[index], sum)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(trackColor, RoundedCornerShape(4.dp))
                ) {
                    val fraction = chartFractions(listOf(totals[index]), busiest).first()
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(barColor, RoundedCornerShape(4.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    listOf(
                        countText(R.plurals.stats_downloads_count, category.downloads),
                        countText(R.plurals.stats_views_count, category.views),
                        countText(R.plurals.stats_favorites_count, category.favorites)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HashtagList(hashtags: List<HashtagActivity>, modifier: Modifier = Modifier) {
    StatCard(modifier = modifier) {
        Text(stringResource(R.string.stats_hashtags), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        if (hashtags.isEmpty()) {
            EmptyText()
        }
        hashtags.forEachIndexed { index, hashtag ->
            if (index > 0) HorizontalDivider()
            RankedRow(
                rank = index + 1,
                title = "#${hashtag.name}",
                subtitle = countText(R.plurals.stats_downloads_count, hashtag.downloads),
                trailing = null,
                onClick = null
            )
        }
    }
}

@Composable
private fun EmptyText(textId: Int = R.string.stats_list_empty) {
    Text(
        stringResource(textId),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

// "1 234 téléchargements": the plural form follows the count, the number the screen's locale
@Composable
private fun countText(@PluralsRes id: Int, count: Long): String =
    pluralStringResource(id, count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatCount(count, appLocale()))
