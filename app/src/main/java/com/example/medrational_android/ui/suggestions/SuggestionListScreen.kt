package com.example.medrational_android.ui.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.model.SuggestionSummary
import com.example.medrational_android.data.model.formatIsoDateTime
import com.example.medrational_android.ui.components.EmptyState
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.ListRowCard
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.components.SwisraTopBar
import com.example.medrational_android.ui.text.appLocale
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.SuggestionListUiState
import com.example.medrational_android.viewmodel.SuggestionListViewModel

/** Admin only: the inbox of every suggestion users sent. */
@Composable
fun SuggestionListScreen(
    viewModel: SuggestionListViewModel,
    onSuggestionClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reloads whenever the screen is shown again, e.g. back from a deleted suggestion
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            SwisraTopBar(
                title = stringResource(R.string.drawer_suggestions_inbox),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
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
                is SuggestionListUiState.Loading -> LoadingState()
                is SuggestionListUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::load)
                is SuggestionListUiState.Success -> if (state.suggestions.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.suggestion_empty_pending),
                        icon = Icons.Outlined.Lightbulb,
                        hint = stringResource(R.string.suggestion_empty_admin_hint)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(state.suggestions, key = { it.id }) { suggestion ->
                            SuggestionRow(suggestion = suggestion, onClick = { onSuggestionClick(suggestion.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: SuggestionSummary, onClick: () -> Unit) {
    val sender = suggestion.senderName?.takeIf { it.isNotBlank() } ?: suggestion.senderEmail.orEmpty()
    val sent = suggestion.createdAt?.let { formatIsoDateTime(it, appLocale()) }
    ListRowCard(
        title = suggestion.title,
        subtitle = listOfNotNull(sender.takeIf { it.isNotBlank() }, sent).joinToString(" · "),
        // Bold until an admin opens it
        titleWeight = if (suggestion.unread) FontWeight.Bold else FontWeight.SemiBold,
        trailing = if (suggestion.unread) {
            { Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
        } else {
            null
        },
        onClick = onClick
    )
}
