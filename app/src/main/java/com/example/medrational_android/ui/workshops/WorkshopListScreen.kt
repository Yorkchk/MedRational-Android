package com.example.medrational_android.ui.workshops

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.viewmodel.WorkshopListUiState
import com.example.medrational_android.viewmodel.WorkshopListViewModel

private val TABS: List<ProjectStatus?> = listOf(null, ProjectStatus.UPCOMING, ProjectStatus.OPEN, ProjectStatus.CLOSED)

/** Workshop projects, filterable by status. Admins also get a button to create one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopListScreen(
    viewModel: WorkshopListViewModel,
    isAdmin: Boolean,
    onProjectClick: (Long) -> Unit,
    onNewProjectClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val context = LocalContext.current

    // Reminders are notifications, which need the user's consent on Android 13+
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Reloads whenever the screen is shown again, e.g. back from a project
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onNewProjectClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New project") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(selectedTabIndex = TABS.indexOf(tab), edgePadding = 16.dp) {
                TABS.forEach { status ->
                    Tab(
                        selected = tab == status,
                        onClick = { viewModel.selectTab(status) },
                        text = { Text(status?.label() ?: "All") }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is WorkshopListUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is WorkshopListUiState.Error -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::load) { Text("Retry") }
                    }
                    is WorkshopListUiState.Success -> if (state.projects.isEmpty()) {
                        EmptyProjects(status = tab, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            // Bottom padding keeps the last card clear of the FAB
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.projects, key = { it.id }) { project ->
                                ProjectCard(project = project, onClick = { onProjectClick(project.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: WorkshopProject, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                WorkshopStatusChip(status = project.status)
            }
            val dates = when (project.status) {
                ProjectStatus.UPCOMING -> "Registration opens ${project.opensLabel()}"
                ProjectStatus.OPEN -> "Registration closes ${project.closesLabel()}"
                ProjectStatus.CLOSED -> "Registration closed ${project.closesLabel()}"
            }
            Text(
                text = listOfNotNull(dates, project.capacityLabel()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyProjects(status: ProjectStatus?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (status) {
                null -> "No projects yet."
                ProjectStatus.UPCOMING -> "No upcoming projects."
                ProjectStatus.OPEN -> "No project is open for registration."
                ProjectStatus.CLOSED -> "No closed projects."
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "You'll get a notification 10 minutes before a project's registration opens and closes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
