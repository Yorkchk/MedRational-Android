package com.example.medrational_android.ui.reasonings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.download.AndroidDownloader
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.viewmodel.ReasoningUiState
import com.example.medrational_android.viewmodel.ReasoningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningDetailScreen(
    categoryId: Long,
    categoryName: String,
    viewModel: ReasoningViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val downloader = remember { AndroidDownloader(context) }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewPdfUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categoryId) {
        viewModel.loadReasoningsForCategory(categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = categoryName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Download Entire Category
                    IconButton(onClick = {
                        val categoryZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/category/$categoryId/zip"
                        downloader.downloadFile(categoryZipUrl, "${categoryName}_Materials.zip")
                        Toast.makeText(context, "Downloading $categoryName ZIP...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download Category ZIP")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ReasoningUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ReasoningUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadReasoningsForCategory(categoryId) }) { Text("Retry") }
                    }
                }
                is ReasoningUiState.Success -> {
                    if (state.reasonings.isEmpty()) {
                        Text(
                            text = "No reasonings added for this category.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.reasonings, key = { it.id }) { reasoning ->
                                ReasoningItemCard(
                                    reasoning = reasoning,
                                    onImageClick = { url -> previewImageUrl = url },
                                    onPdfClick = { url -> previewPdfUrl = url },
                                    onDownloadReasoningZip = {
                                        val reasoningZipUrl = "${ApiClient.BASE_URL}api/v1/downloads/reasoning/${reasoning.id}/zip"
                                        downloader.downloadFile(reasoningZipUrl, "${reasoning.title}_Files.zip")
                                        Toast.makeText(context, "Downloading ${reasoning.title} files...", Toast.LENGTH_SHORT).show()
                                    },
                                    onDownloadSingleFile = { file ->
                                        val singleFileUrl = "${ApiClient.BASE_URL}api/v1/downloads/file/${file.id}"
                                        downloader.downloadFile(singleFileUrl, file.fileName)
                                        Toast.makeText(context, "Downloading ${file.fileName}...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Image Preview Dialog
    previewImageUrl?.let { url ->
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun ReasoningItemCard(
    reasoning: Reasoning,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit,
    onDownloadReasoningZip: () -> Unit,
    onDownloadSingleFile: (StudyFile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reasoning.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (!reasoning.files.isNullOrEmpty()) {
                    IconButton(onClick = onDownloadReasoningZip) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download all files for this reasoning",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!reasoning.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = reasoning.content, style = MaterialTheme.typography.bodyMedium)
            }

            if (!reasoning.files.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Attached Files (${reasoning.files.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                reasoning.files.forEach { file ->
                    StudyFileRow(
                        file = file,
                        onImageClick = onImageClick,
                        onPdfClick = onPdfClick,
                        onDownloadFile = { onDownloadSingleFile(file) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun StudyFileRow(
    file: StudyFile,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit,
    onDownloadFile: () -> Unit
) {
    val isImage = file.fileType?.startsWith("image/") == true || file.fileName.matches(Regex(".*\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE))
    val isPdf = file.fileType == "application/pdf" || file.fileName.endsWith(".pdf", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isImage) onImageClick(file.publicUrl)
                else if (isPdf) onPdfClick(file.publicUrl)
            }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isImage) {
                AsyncImage(
                    model = file.publicUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isImage) "Tap to preview image" else if (isPdf) "Tap to preview PDF" else "Document",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDownloadFile) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}