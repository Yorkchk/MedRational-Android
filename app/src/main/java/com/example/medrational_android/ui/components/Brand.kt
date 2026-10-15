package com.example.medrational_android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.ui.theme.SwisraTheme

/** The SWISRA logo. Decorative: the name next to it is what screen readers announce. */
@Composable
fun SwisraLogo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Image(
        painter = painterResource(R.drawable.ic_swisra_logo),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

/** "SWISRA" in the heading font. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = stringResource(R.string.app_name),
        style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    )
}

/** Logo, name and what it stands for, stacked and centred: the top of the auth screens. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier, logoSize: Dp = 56.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SwisraLogo(size = logoSize)
        Spacer(modifier = Modifier.size(Spacing.sm))
        Wordmark()
        Text(
            text = stringResource(R.string.brand_tagline),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Logo and name side by side, e.g. in the side menu's header. */
@Composable
fun BrandRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        SwisraLogo(size = 40.dp)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column {
            Wordmark()
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF111518)
@Composable
private fun BrandPreview() {
    SwisraTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            BrandHeader()
            BrandRow()
        }
    }
}
