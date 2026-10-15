package com.example.medrational_android.ui.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.PrimaryButton
import com.example.medrational_android.ui.components.SecondaryButton
import com.example.medrational_android.ui.components.SwisraLogo
import com.example.medrational_android.ui.components.Wordmark
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.SwisraTheme

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                LatitudeArcs(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize())
                SwisraLogo(size = 132.dp)
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            Wordmark(large = true)
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                ValuePoint(Icons.Outlined.MenuBook, stringResource(R.string.welcome_point_browse))
                ValuePoint(Icons.Outlined.BookmarkBorder, stringResource(R.string.welcome_point_keep))
                ValuePoint(Icons.Outlined.Event, stringResource(R.string.welcome_point_projects))
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            PrimaryButton(text = stringResource(R.string.signup_title), onClick = onNavigateToSignUp)
            Spacer(modifier = Modifier.height(Spacing.md))
            SecondaryButton(text = stringResource(R.string.auth_log_in), onClick = onNavigateToLogin)
        }
    }
}

@Composable
private fun ValuePoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(Spacing.sm).size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// Faint arcs around the logo, echoing the sphere's stripes
@Composable
private fun LatitudeArcs(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        listOf(1f, 0.82f, 0.64f).forEachIndexed { index, scale ->
            val w = size.width * scale
            val h = size.height * scale * 0.55f
            val alpha = 0.08f + index * 0.05f
            // Upper and lower halves of a flattened ring, like parallels on a globe
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset((size.width - w) / 2, (size.height - h) / 2 - size.height * 0.18f),
                size = Size(w, h),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset((size.width - w) / 2, (size.height - h) / 2 + size.height * 0.18f),
                size = Size(w, h),
                style = Stroke(width = stroke)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 820)
@Preview(showBackground = true, heightDp = 820, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomePreview() {
    SwisraTheme { WelcomeScreen(onNavigateToLogin = {}, onNavigateToSignUp = {}) }
}
