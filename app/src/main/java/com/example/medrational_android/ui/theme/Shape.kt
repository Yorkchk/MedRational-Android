package com.example.medrational_android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Use MaterialTheme.shapes in screens rather than RoundedCornerShape literals:
// extraSmall badges and chips, small inputs and thumbnails, medium cards and buttons,
// large sheets and hero cards, extraLarge dialogs
val SwisraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
