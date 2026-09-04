package com.example.medrational_android.ui.categories

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational.data.model.Category
import com.example.medrational.viewmodel.CategoryUiState
import com.example.medrational.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    onCategoryClick: (categoryId: Long, categoryName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val downloader = AndroidDownloader(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Categories", fontWeight = FontWeight.Bold) },
                actions = {
                    // Download All Categories (Full Database ZIP)
                    IconButton(onClick = {
                        val fullDownloadUrl = "${ApiClient.BASE_URL}api/v1/downloads/all/zip"
                        downloader.downloadFile(fullDownloadUrl, "MedRational_Complete_Export.zip")
                        Toast.makeText(context, "Downloading all study materials...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download All Material")
                    }
                    IconButton(onClick = { viewModel.loadCategories() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is CategoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CategoryUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadCategories() }) { Text("Retry") }
                    }
                }
                is CategoryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.categories, key = { it.id }) { category ->
                            CategoryCard(
                                category = category,
                                onClick = { onCategoryClick(category.id, category.name) },
                                onDownloadCategoryZip = {
                                    val categoryZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/category/${category.id}/zip"
                                    downloader.downloadFile(categoryZipUrl, "${category.name}_Materials.zip")
                                    Toast.makeText(context, "Downloading ${category.name} ZIP...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    onDownloadCategoryZip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!category.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Category-level ZIP download button
            IconButton(onClick = onDownloadCategoryZip) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Category",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}