package com.example.medrational_android.ui.suggestions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.data.model.SuggestionStatus
import com.example.medrational_android.data.model.SuggestionSummary
import com.example.medrational_android.data.model.formatIsoDateTime
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.SuggestionListUiState
import com.example.medrational_android.viewmodel.SuggestionListViewModel

/** Admins get the inbox of every user's suggestions; users get their own, plus a button to send one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionListScreen(
    viewModel: SuggestionListViewModel,
    isAdmin: Boolean,
    onSuggestionClick: (Long) -> Unit,
    onNewSuggestionClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tab by viewModel.tab.collectAsState()

    // Reloads whenever the screen is shown again, e.g. back from a conversation
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) stringResource(R.string.drawer_suggestions_inbox) else stringResource(R.string.suggestion_my_suggestions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onNewSuggestionClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.suggestion_new)) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = if (tab == SuggestionStatus.PENDING) 0 else 1) {
                Tab(
                    selected = tab == SuggestionStatus.PENDING,
                    onClick = { viewModel.selectTab(SuggestionStatus.PENDING) },
                    text = { Text(stringResource(R.string.suggestion_tab_pending)) }
                )
                Tab(
                    selected = tab == SuggestionStatus.CLOSED,
                    onClick = { viewModel.selectTab(SuggestionStatus.CLOSED) },
                    text = { Text(stringResource(R.string.suggestion_tab_archived)) }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is SuggestionListUiState.Loading -> LoadingState()
                    is SuggestionListUiState.Error -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
                    }
                    is SuggestionListUiState.Success -> if (state.suggestions.isEmpty()) {
                        EmptySuggestions(
                            archived = tab == SuggestionStatus.CLOSED,
                            isAdmin = isAdmin,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            // Bottom padding keeps the last card clear of the FAB
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.suggestions, key = { it.id }) { suggestion ->
                                SuggestionCard(
                                    suggestion = suggestion,
                                    showSender = isAdmin,
                                    onClick = { onSuggestionClick(suggestion.id) }
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
private fun SuggestionCard(
    suggestion: SuggestionSummary,
    showSender: Boolean,
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
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (suggestion.unread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showSender) {
                    Text(
                        text = suggestion.senderName ?: suggestion.senderEmail.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val replies = when (suggestion.messageCount) {
                    0 -> stringResource(R.string.suggestion_no_replies)
                    else -> pluralStringResource(R.plurals.suggestion_reply_count, suggestion.messageCount, suggestion.messageCount)
                }
                val activity = suggestion.lastActivityAt?.let { formatIsoDateTime(it, appLocale()) }
                Text(
                    text = listOfNotNull(replies, activity).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (suggestion.unread) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun EmptySuggestions(archived: Boolean, isAdmin: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (archived) Icons.Default.Archive else Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                archived -> stringResource(R.string.suggestion_empty_archived)
                isAdmin -> stringResource(R.string.suggestion_empty_pending)
                else -> stringResource(R.string.suggestion_empty)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                archived -> stringResource(R.string.suggestion_empty_archived_hint)
                isAdmin -> stringResource(R.string.suggestion_empty_admin_hint)
                else -> stringResource(R.string.suggestion_empty_user_hint)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
