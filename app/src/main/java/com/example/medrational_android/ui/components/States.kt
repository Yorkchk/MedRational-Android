package com.example.medrational_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.text.UiText
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.SwisraTheme

/** A spinner centred in the available space, while a screen loads. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** A screen that couldn't load: what went wrong and a Retry button. */
@Composable
fun ErrorState(
    message: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageState(
        icon = Icons.Outlined.CloudOff,
        title = message.asString(),
        hint = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier
    ) {
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

/** Nothing to show yet, with a hint on what to do and an optional action. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    hint: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    MessageState(
        icon = icon,
        title = title,
        hint = hint,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
        action = action
    )
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    hint: String?,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.12f)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(Spacing.lg).size(32.dp))
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (hint != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                action()
            }
        }
    }
}

enum class BannerKind { Error, Info }

/** An inline message inside a form or list, e.g. a sign-in error. Always has an icon, so errors don't rely on red alone. */
@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.Error
) {
    val (container, content, icon) = when (kind) {
        BannerKind.Error -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Outlined.ErrorOutline)
        BannerKind.Info -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.Info)
    }
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun StatesPreview() {
    SwisraTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(Spacing.lg)) {
            StatusBanner("Code invalide ou expiré.")
            StatusBanner("Un code a été envoyé.", kind = BannerKind.Info)
            Box(modifier = Modifier.height(260.dp)) {
                EmptyState(title = "Aucun favori", hint = "Touchez le cœur d'un fichier pour le retrouver ici.")
            }
        }
    }
}
