package com.example.medrational_android.ui.downloads

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
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.medrational_android.data.model.RecentDownload
import com.example.medrational_android.data.model.formatIsoDate
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.ui.components.RatingBadge
import com.example.medrational_android.ui.files.fileTypeIcon
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.RecentDownloadsUiState
import com.example.medrational_android.viewmodel.RecentDownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDownloadsScreen(
    viewModel: RecentDownloadsViewModel,
    onFileClick: (reasoningId: Long, fileId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ratings by FileRatingStore.shared.ratings.collectAsState()

    // Reloads each time the screen is shown, so files downloaded meanwhile appear at the top
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title), fontWeight = FontWeight.Bold) },
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
                is RecentDownloadsUiState.Loading -> LoadingState()
                is RecentDownloadsUiState.Error -> {
                    ErrorState(message = state.message, onRetry = viewModel::load)
                }
                is RecentDownloadsUiState.Success -> {
                    if (state.downloads.isEmpty()) {
                        EmptyDownloads(modifier = Modifier.align(Alignment.Center))
                    } else {
                        AdaptiveCardList(top = 16.dp, bottom = 16.dp, spacing = 12.dp, minPadding = 16.dp) {
                            // The ViewModel only keeps entries with both ids
                            items(state.downloads, key = { it.fileId!! }) { download ->
                                RecentDownloadCard(
                                    download = download,
                                    knownRating = ratings[download.fileId],
                                    onClick = { onFileClick(download.reasoningId!!, download.fileId!!) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDownloadCard(
    download: RecentDownload,
    knownRating: FileRatingSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = fileTypeIcon(download.fileName, download.fileType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.fileName ?: stringResource(R.string.file_untitled),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val location = listOfNotNull(download.categoryName, download.reasoningTitle).joinToString(" • ")
                if (location.isNotEmpty()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                download.downloadedAt?.let { formatIsoDate(it, appLocale()) }?.let { date ->
                    Text(
                        text = stringResource(R.string.downloads_downloaded_on, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RatingBadge(
                    known = knownRating,
                    avgRating = download.avgRating,
                    totalRatings = download.totalRatings,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloads(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.downloads_empty), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.downloads_empty_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
