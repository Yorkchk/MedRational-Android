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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.data.model.RecentDownload
import com.example.medrational_android.data.model.formatIsoDate
import com.example.medrational_android.ui.files.fileTypeIcon
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

    // Reloads each time the screen is shown, so files downloaded meanwhile appear at the top
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent downloads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                is RecentDownloadsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is RecentDownloadsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::load) { Text("Retry") }
                    }
                }
                is RecentDownloadsUiState.Success -> {
                    if (state.downloads.isEmpty()) {
                        EmptyDownloads(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // The ViewModel only keeps entries with both ids
                            items(state.downloads, key = { it.fileId!! }) { download ->
                                RecentDownloadCard(
                                    download = download,
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
                    text = download.fileName ?: "Untitled file",
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
                download.downloadedAt?.let(::formatIsoDate)?.let { date ->
                    Text(
                        text = "Downloaded $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Text("No downloads yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Files you download will appear here so you can open them again quickly.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
