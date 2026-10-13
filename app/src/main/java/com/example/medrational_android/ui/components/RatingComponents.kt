package com.example.medrational_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.ui.theme.RatingStar
import java.util.Locale

// "4.3" with one decimal whatever the device locale, to match the backend's rounding
fun formatAverage(avgRating: Double): String = String.format(Locale.US, "%.1f", avgRating)

@Composable
fun ratingCountLabel(totalRatings: Int): String = pluralStringResource(R.plurals.rating_count, totalRatings, totalRatings)

/** Compact "★ 4.3 (12)" for list rows; "No ratings yet" until someone rates the file. */
@Composable
fun RatingBadge(
    avgRating: Double,
    totalRatings: Int,
    modifier: Modifier = Modifier
) {
    val description = if (totalRatings == 0) stringResource(R.string.rating_none)
    else stringResource(R.string.rating_description, formatAverage(avgRating), ratingCountLabel(totalRatings))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (totalRatings == 0) {
            Icon(
                imageVector = Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.rating_none),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = RatingStar,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = formatAverage(avgRating),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "($totalRatings)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** [RatingBadge] fed from the shared store's entry when there is one, else from the list item. */
@Composable
fun RatingBadge(
    known: FileRatingSummary?,
    avgRating: Double,
    totalRatings: Int,
    modifier: Modifier = Modifier
) {
    RatingBadge(
        avgRating = known?.avgRating ?: avgRating,
        totalRatings = known?.totalRatings ?: totalRatings,
        modifier = modifier
    )
}

/** Read-only row of five stars, half stars included, for an average. */
@Composable
fun AverageStars(
    avgRating: Double,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (star in 1..5) {
            val empty = avgRating < star - 0.75
            Icon(
                imageVector = when {
                    avgRating >= star - 0.25 -> Icons.Filled.Star
                    !empty -> Icons.AutoMirrored.Filled.StarHalf
                    else -> Icons.Filled.StarBorder
                },
                contentDescription = null,
                tint = if (empty) MaterialTheme.colorScheme.outline else RatingStar,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

/** Five tappable stars showing [userRating]; tapping star n rates the file n. */
@Composable
fun StarRatingInput(
    userRating: Int?,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (star in 1..5) {
            val filled = userRating != null && star <= userRating
            IconButton(onClick = { onRate(star) }, enabled = enabled) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = pluralStringResource(R.plurals.rating_rate_stars, star, star),
                    tint = if (filled) RatingStar else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
