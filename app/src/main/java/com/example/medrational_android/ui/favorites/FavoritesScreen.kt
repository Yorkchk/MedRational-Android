package com.example.medrational_android.ui.favorites

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.FavoriteResponse
import com.example.medrational_android.viewmodel.FavoriteUiState
import com.example.medrational_android.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoriteViewModel,
    onNavigateToReasoning: (categoryId: Long, reasoningId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Saved Files", fontWeight = FontWeight.Bold) },
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
                is FavoriteUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is FavoriteUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadFavorites() }) { Text("Retry") }
                    }
                }
                is FavoriteUiState.Success -> {
                    if (state.favorites.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No saved files yet.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap the heart icon on any clinical study file to bookmark it here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.favorites, key = { it.favoriteId }) { fav ->
                                val file = fav.file
                                FavoriteItemCard(
                                    favorite = fav,
                                    onCardClick = {
                                        if (file.categoryId != null && file.reasoningId != null) {
                                            onNavigateToReasoning(file.categoryId, file.reasoningId)
                                        }
                                    },
                                    onRemoveFavorite = {
                                        viewModel.toggleFavorite(file.id)
                                        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                    },
                                    onDownload = {
                                        val fileName = file.fileName ?: "file_${file.id}"
                                        val url = "${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}"
                                        downloader.downloadFile(url, fileName)
                                        Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
                                    }
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
fun FavoriteItemCard(
    favorite: FavoriteResponse,
    onCardClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    val file = favorite.file
    val isImage = file.fileType?.startsWith("image/") == true ||
            file.fileName?.matches(Regex(".*\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE)) == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isImage && !file.publicUrl.isNullOrBlank()) {
                AsyncImage(
                    model = file.publicUrl,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName ?: "Untitled File",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${file.categoryName ?: "General"} • ${file.reasoningTitle ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onRemoveFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "Remove Favorite", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}