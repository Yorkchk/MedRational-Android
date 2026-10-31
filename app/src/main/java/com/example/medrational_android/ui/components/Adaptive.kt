package com.example.medrational_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medrational_android.ui.theme.Spacing

// Tablets and large screens: Material's width breakpoints, read from the window so split-screen follows too

/** Widest the content of a screen gets; wider windows center it. */
object ContentWidth {
    // Single-column pages (file, project, profile, statistics)
    val Reading = 720.dp
    // Two-column lists and the home grid
    val Grid = 1200.dp
    // Forms (sign-in, profile changes, new suggestion or project)
    val Form = 560.dp
}

private const val MEDIUM_WIDTH_DP = 600
private const val EXPANDED_WIDTH_DP = 840

/**
 * Card lists show [maxColumns] columns from a small tablet up, one on phones. Never more columns
 * than cards ([itemCount]), so a lone card isn't left in half the width.
 */
fun listColumnsFor(widthDp: Int, maxColumns: Int = 2, itemCount: Int? = null): Int {
    val columns = if (widthDp >= MEDIUM_WIDTH_DP) maxColumns else 1
    return if (itemCount == null) columns else columns.coerceAtMost(itemCount).coerceAtLeast(1)
}

/** A phone's width (below a small tablet). */
fun isCompactWidth(widthDp: Int): Boolean = widthDp < MEDIUM_WIDTH_DP

/** Wide enough to put a file's details next to its preview (a tablet in landscape). */
fun isTwoPane(widthDp: Int): Boolean = widthDp >= EXPANDED_WIDTH_DP

/** Side padding that centers [maxContentDp] of content, never less than [minPaddingDp]. */
fun centeredPaddingDp(widthDp: Int, maxContentDp: Int, minPaddingDp: Int): Int =
    maxOf(minPaddingDp, (widthDp - maxContentDp) / 2)

@Composable
fun windowWidthDp(): Int = LocalConfiguration.current.screenWidthDp

/** Side padding for a full-width list, so its content is centered and the margins still scroll it. */
@Composable
fun adaptiveHorizontalPadding(maxContent: Dp = ContentWidth.Reading, minPadding: Dp = Spacing.screen): Dp =
    centeredPaddingDp(windowWidthDp(), maxContent.value.toInt(), minPadding.value.toInt()).dp

/** Side padding of a card list: one column is centered at the reading width, more at the grid width. */
@Composable
fun adaptiveListPadding(columns: Int, minPadding: Dp = Spacing.screen): Dp =
    adaptiveHorizontalPadding(if (columns == 1) ContentWidth.Reading else ContentWidth.Grid, minPadding)

/** Centers a non-scrolling block and caps its width. */
fun Modifier.centeredContent(maxWidth: Dp): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = maxWidth)

/**
 * A list of cards: one column on phones, two from a small tablet up. Staggered, so cards of
 * different heights pack together. Headers and footers use `StaggeredGridItemSpan.FullLine`.
 * Lists of long cards of very different heights (reasonings) pass [maxColumns] 1, since two
 * ragged columns read badly; one column is centered at the reading width.
 */
@Composable
fun AdaptiveCardList(
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    top: Dp = Spacing.xs,
    bottom: Dp = Spacing.lg,
    spacing: Dp = Spacing.md,
    minPadding: Dp = Spacing.screen,
    maxColumns: Int = 2,
    // How many cards there are, so one card isn't left in half the width
    itemCount: Int? = null,
    content: LazyStaggeredGridScope.() -> Unit
) {
    val columns = listColumnsFor(windowWidthDp(), maxColumns, itemCount)
    val side = adaptiveListPadding(columns, minPadding)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = side, end = side, top = top, bottom = bottom),
        verticalItemSpacing = spacing,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}
