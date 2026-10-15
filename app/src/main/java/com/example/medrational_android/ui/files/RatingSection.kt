package com.example.medrational_android.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.AverageStars
import com.example.medrational_android.ui.components.StarRatingInput
import com.example.medrational_android.ui.components.formatAverage
import com.example.medrational_android.ui.components.ratingCountLabel
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.FileRatingUiState

/** The file's average and count, and the user's own stars to rate, change or remove their rating. */
@Composable
fun RatingSection(
    state: FileRatingUiState,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Column {
            Text(text = stringResource(R.string.rating_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            when (state) {
                FileRatingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is FileRatingUiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.message.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
                }
                is FileRatingUiState.Success -> {
                    val summary = state.summary
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (summary.totalRatings > 0) {
                            Text(
                                text = formatAverage(summary.avgRating),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            AverageStars(avgRating = summary.avgRating)
                        }
                        Text(
                            text = if (summary.totalRatings == 0) stringResource(R.string.rating_none) else ratingCountLabel(summary.totalRatings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (summary.userRating == null) stringResource(R.string.rating_tap_star) else stringResource(R.string.rating_yours),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (summary.userRating != null) {
                            TextButton(onClick = onRemoveRating, enabled = !state.isSubmitting) {
                                Text(stringResource(R.string.rating_remove))
                            }
                        }
                    }
                    StarRatingInput(
                        userRating = summary.userRating,
                        onRate = onRate,
                        enabled = !state.isSubmitting
                    )
                }
            }
        }
    }
}
