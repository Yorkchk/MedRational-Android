package com.example.medrational_android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// SWISRA brand: the logo's red sphere and slate "M". Screens use MaterialTheme.colorScheme,
// never these directly, except to draw the logo itself
val SwisraRed = Color(0xFFE3212B)
val SwisraSlate = Color(0xFF25343C)

// Filled rating stars; reads on both light and dark surfaces
val RatingStar = Color(0xFFFFB300)

// A statistic that went up since the previous week; reads on both light and dark surfaces
val TrendUp = Color(0xFF2E9D4A)

// Clean clinical light theme: off-white background, white cards, red accent, slate text
internal val LightColors = lightColorScheme(
    primary = Color(0xFFC8102E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410006),
    inversePrimary = Color(0xFFFFB3AE),
    secondary = SwisraSlate,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4EE),
    onSecondaryContainer = Color(0xFF0E1D24),
    tertiary = Color(0xFF00796B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2EFE6),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF7F8FA),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFE1E5E9),
    onSurfaceVariant = Color(0xFF444B51),
    // Tonal elevation leans slate rather than pink
    surfaceTint = SwisraSlate,
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFEEF1F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF747C82),
    outlineVariant = Color(0xFFC9CED3),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7F8FA),
    surfaceDim = Color(0xFFD7DADD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFECEFF2),
    surfaceContainerHigh = Color(0xFFE6E9EC),
    surfaceContainerHighest = Color(0xFFE0E3E6)
)

// Dark slate surfaces with a softened red, easy on the eyes for night study
internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AE),
    onPrimary = Color(0xFF680010),
    primaryContainer = Color(0xFF93001E),
    onPrimaryContainer = Color(0xFFFFDAD7),
    inversePrimary = Color(0xFFC8102E),
    secondary = Color(0xFFB7C9D3),
    onSecondary = Color(0xFF21323A),
    secondaryContainer = Color(0xFF384951),
    onSecondaryContainer = Color(0xFFD3E4EE),
    tertiary = Color(0xFF80D5C9),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFF9DF2E5),
    background = Color(0xFF111518),
    onBackground = Color(0xFFE0E3E6),
    surface = Color(0xFF111518),
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C7CD),
    surfaceTint = Color(0xFFB7C9D3),
    inverseSurface = Color(0xFFE0E3E6),
    inverseOnSurface = Color(0xFF2C3134),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF363C40),
    surfaceDim = Color(0xFF111518),
    surfaceContainerLowest = Color(0xFF0C1013),
    surfaceContainerLow = Color(0xFF181E22),
    surfaceContainer = Color(0xFF1C2226),
    surfaceContainerHigh = Color(0xFF262C30),
    surfaceContainerHighest = Color(0xFF31373B)
)

/** A soft background and its content color, e.g. for a category's monogram tile. */
data class Tint(val container: Color, val content: Color)

// Calm, distinct tints so neighbouring category tiles don't look alike. Same order in both themes
internal val LightTints = listOf(
    Tint(Color(0xFFFFDAD7), Color(0xFF8C0019)),
    Tint(Color(0xFFD3E4EE), Color(0xFF1C3540)),
    Tint(Color(0xFFB2EFE6), Color(0xFF00504A)),
    Tint(Color(0xFFFFE0B2), Color(0xFF6D4100)),
    Tint(Color(0xFFE1DDFB), Color(0xFF3B3470)),
    Tint(Color(0xFFD7EFC9), Color(0xFF2D5016))
)

internal val DarkTints = listOf(
    Tint(Color(0xFF5C1219), Color(0xFFFFDAD7)),
    Tint(Color(0xFF2B3E48), Color(0xFFD3E4EE)),
    Tint(Color(0xFF0D3F3A), Color(0xFFB2EFE6)),
    Tint(Color(0xFF4A3510), Color(0xFFFFE0B2)),
    Tint(Color(0xFF353152), Color(0xFFE1DDFB)),
    Tint(Color(0xFF26391C), Color(0xFFD7EFC9))
)
