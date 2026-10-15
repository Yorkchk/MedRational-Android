package com.example.medrational_android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.files.FileKind
import com.example.medrational_android.ui.files.fileKind
import com.example.medrational_android.ui.files.icon
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.SwisraTheme
import com.example.medrational_android.ui.theme.SwisraTints
import com.example.medrational_android.ui.theme.Tint

/** The app's top bar: a title, an optional back arrow, and actions on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwisraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
    )
}

/** A small heading above a group of content, with an optional action on the right ("See all"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

/**
 * A row in a list, as a white card: a leading visual (badge, thumbnail), a title, a subtitle line
 * and trailing content (rating, menu). [selected] outlines it for multi-selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRowCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    titleWeight: FontWeight = FontWeight.SemiBold,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = if (selected) CardDefaults.outlinedCardBorder() else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        val clickable = if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
        } else {
            Modifier
        }
        Row(
            modifier = clickable.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(Spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = titleWeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                footer?.invoke()
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                trailing()
            }
        }
    }
}

/** A tinted rounded tile with an icon or initials in it. */
@Composable
fun TintedTile(
    tint: Tint,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.small,
        color = tint.container,
        contentColor = tint.content
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** A file's type as a colored tile with its icon: red for PDFs, slate for documents, and so on. */
@Composable
fun FileTypeBadge(
    fileName: String?,
    fileType: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val kind = fileKind(fileName, fileType)
    TintedTile(tint = kind.tint(), modifier = modifier, size = size) {
        Icon(kind.icon(), contentDescription = null, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun FileKind.tint(): Tint {
    val tints = SwisraTints.all
    return when (this) {
        FileKind.PDF -> tints[0]
        FileKind.DOCUMENT -> tints[1]
        FileKind.IMAGE -> tints[2]
        FileKind.SLIDES -> tints[3]
        FileKind.SHEET -> tints[5]
        FileKind.OTHER -> tints[4]
    }
}

/** Initials of [name] on the tint picked for [key], or [fallback] when the name has no letters. */
@Composable
fun MonogramTile(
    name: String,
    key: Long,
    fallback: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    TintedTile(tint = SwisraTints.forKey(key), modifier = modifier, size = size) {
        val initials = monogram(name)
        if (initials != null) {
            Text(initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            Icon(fallback, contentDescription = null, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListRowCardPreview() {
    SwisraTheme {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            ListRowCard(
                title = "ECG - lecture systématique.pdf",
                subtitle = "PDF · 2,4 Mo",
                leading = { FileTypeBadge("ecg.pdf", "application/pdf") }
            )
            Spacer(modifier = Modifier.size(Spacing.sm))
            ListRowCard(
                title = "Cardiologie",
                subtitle = "12 raisonnements",
                leading = { MonogramTile("Cardiologie", 1, Icons.AutoMirrored.Filled.ArrowBack) },
                selected = true
            )
        }
    }
}
