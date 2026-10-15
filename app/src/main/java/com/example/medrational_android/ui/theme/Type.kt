package com.example.medrational_android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.medrational_android.R

// Both fonts are bundled (SIL Open Font License, see docs/brand/README.md), so they work offline
// and without Google Play services

/** Headings: Plus Jakarta Sans, friendly and confident. */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

/** Body text: Inter, built for screen reading. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    // No bold file: titles in Inter use SemiBold, which keeps long lists calm
    Font(R.font.inter_semibold, FontWeight.Bold)
)

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.Bold, spacing: Double = 0.0) = TextStyle(
    fontFamily = JakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

// Material 3 scale; body lines are a little taller than the default for long study sessions
val SwisraTypography = Typography(
    displayLarge = heading(54, 62, spacing = -0.5),
    displayMedium = heading(44, 52, spacing = -0.25),
    displaySmall = heading(36, 44),
    headlineLarge = heading(30, 38),
    headlineMedium = heading(26, 34),
    headlineSmall = heading(22, 30),
    titleLarge = heading(20, 28, FontWeight.SemiBold),
    titleMedium = text(16, 24, FontWeight.SemiBold, 0.1),
    titleSmall = text(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = text(16, 26, spacing = 0.15),
    bodyMedium = text(14, 22, spacing = 0.2),
    bodySmall = text(12, 18, spacing = 0.3),
    labelLarge = text(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = text(12, 16, FontWeight.Medium, 0.4),
    labelSmall = text(11, 16, FontWeight.Medium, 0.5)
)
