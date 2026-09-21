package com.example.medrational_android.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.FileSearchResult
import com.example.medrational_android.viewmodel.SearchUiState
import com.example.medrational_android.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToReasoning: (categoryId: Long, reasoningId: Long) -> Unit,
    onBackClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }

    val uiState by searchViewModel.uiState.collectAsState()
    val categories by searchViewModel.categories.collectAsState()
    val query by searchViewModel.searchQuery.collectAsState()
    val selectedCategory by searchViewModel.selectedCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover & Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = query,
                onValueChange = { searchViewModel.onQueryChanged(it) },
                placeholder = { Text("Search files by category, reasoning") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category Filter Filter Chips (Instagram/Facebook style horizontal bar)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { searchViewModel.onCategorySelected(null) },
                        label = { Text("All Categories") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory?.id == category.id,
                        onClick = {
                            if (selectedCategory?.id == category.id) searchViewModel.onCategorySelected(null)
                            else searchViewModel.onCategorySelected(category)
                        },
                        label = { Text(category.name) }
                    )
                }
            }

            Divider()

            // Results Section
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Type a query or pick a category above to find files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is SearchUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is SearchUiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchUiState.Success -> {
                        if (state.files.isEmpty()) {
                            Text("No matching files found.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.files, key = { it.id }) { file ->
                                    SearchResultCard(
                                        file = file,
                                        onHashtagClick = { tag -> searchViewModel.onQueryChanged(tag) },
                                        onCardClick = {
                                            if (file.categoryId != null && file.reasoningId != null) {
                                                onNavigateToReasoning(file.categoryId, file.reasoningId)
                                            }
                                        },
                                        onDownload = {
                                            val url = "${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}"
                                            downloader.downloadFile(url, file.fileName)
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
}

@Composable
fun SearchResultCard(
    file: FileSearchResult,
    onHashtagClick: (String) -> Unit,
    onCardClick: () -> Unit,
    onDownload: () -> Unit
) {
    val isImage = file.fileType?.startsWith("image/") == true ||
            file.fileName.matches(Regex(".*\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isImage) {
                    AsyncImage(
                        model = file.publicUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(file.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${file.categoryName} • ${file.reasoningTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Hashtags (Instagram style clickable tags)
            if (file.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(file.hashtags) { tag ->
                        SuggestionChip(
                            onClick = { onHashtagClick("#$tag") },
                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}