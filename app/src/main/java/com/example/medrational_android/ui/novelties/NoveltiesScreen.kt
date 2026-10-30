package com.example.medrational_android.ui.novelties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.AdaptiveCardList
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.NoveltyItem
import com.example.medrational_android.data.model.fileMetaLine
import com.example.medrational_android.data.model.formatIsoDateTime
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.ui.components.RatingBadge
import com.example.medrational_android.ui.files.fileTypeIcon
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.NoveltiesUiState
import com.example.medrational_android.viewmodel.NoveltiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoveltiesScreen(
    viewModel: NoveltiesViewModel,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    // The ViewModel loads once, not this screen: reloading after the feed was marked seen would
    // drop the highlight when coming back from a file
    val uiState by viewModel.uiState.collectAsState()
    val ratings by FileRatingStore.shared.ratings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novelties_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is NoveltiesUiState.Loading -> LoadingState()
                is NoveltiesUiState.Error -> {
                    ErrorState(message = state.message, onRetry = viewModel::retry)
                }
                is NoveltiesUiState.Success -> {
                    if (state.items.isEmpty()) {
                        EmptyNovelties(modifier = Modifier.align(Alignment.Center))
                    } else {
                        NoveltiesList(
                            state = state,
                            ratings = ratings,
                            onFileClick = onFileClick,
                            onLoadMore = viewModel::loadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoveltiesList(
    state: NoveltiesUiState.Success,
    ratings: Map<Long, FileRatingSummary>,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyStaggeredGridState()
    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    // A failed page waits for its Retry button instead of being re-requested in a loop
    LaunchedEffect(nearEnd, state.canLoadMore, state.loadMoreFailed) {
        if (nearEnd && state.canLoadMore && !state.loadMoreFailed) onLoadMore()
    }

    AdaptiveCardList(
        state = listState,
        modifier = modifier,
        top = 16.dp,
        bottom = 16.dp,
        spacing = 12.dp,
        minPadding = 16.dp
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) {
            Text(
                text = when (state.newCount) {
                    0L -> stringResource(R.string.novelties_caught_up_latest)
                    else -> pluralStringResource(R.plurals.novelties_new_count, state.newCount.toInt(), state.newCount.toInt())
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (state.newCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        // The ViewModel only keeps files with a reasoning
        items(state.items, key = { it.fileId }) { item ->
            NoveltyCard(
                item = item,
                knownRating = ratings[item.fileId],
                onClick = { onFileClick(item.reasoningId!!, item.fileId) }
            )
        }
        if (state.loadingMore || state.loadMoreFailed) {
            item(key = "footer", span = StaggeredGridItemSpan.FullLine) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    } else {
                        TextButton(onClick = onLoadMore) { Text(stringResource(R.string.novelties_load_more_failed)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoveltyCard(
    item: NoveltyItem,
    knownRating: FileRatingSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isNew) colors.primaryContainer else colors.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = fileTypeIcon(item.fileName, item.fileType),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.fileName ?: stringResource(R.string.file_untitled),
                        fontWeight = if (item.isNew) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isNew) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                            Text(stringResource(R.string.novelties_new_badge))
                        }
                    }
                }
                val location = listOfNotNull(item.categoryName, item.reasoningTitle).joinToString(" • ")
                if (location.isNotEmpty()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (item.isNew) FontWeight.SemiBold else FontWeight.Normal,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.uploadedAt?.let { formatIsoDateTime(it, appLocale()) }?.let { date ->
                    Text(
                        text = stringResource(R.string.novelties_uploaded_on, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                // Type and size only; the upload date is shown above with its time
                val meta = fileMetaLine(item.fileName, item.fileType, item.fileSizeBytes, uploadedAt = null, locale = appLocale())
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                RatingBadge(
                    known = knownRating,
                    avgRating = item.avgRating,
                    totalRatings = item.totalRatings,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNovelties(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.NewReleases,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.novelties_caught_up), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.novelties_empty_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
