package com.example.phaze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Where the light "source" of a screen gradient sits. */
enum class GradientDirection { TOP_LEFT, BOTTOM_RIGHT, TOP_BOTTOM }

/**
 * Smooth light screen gradient derived from the color scheme: an accent-tinted
 * highlight fading through a lighter surface into the dark background. The
 * direction differs per screen so pages feel a little distinct.
 */
@Composable
fun screenBackgroundBrush(direction: GradientDirection): Brush {
    val highlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val mid = MaterialTheme.colorScheme.surfaceBright
    val base = MaterialTheme.colorScheme.background
    return when (direction) {
        GradientDirection.TOP_LEFT ->
            Brush.linearGradient(
                listOf(highlight, mid, base),
                start = Offset(0f, 0f),
                end = Offset.Infinite,
            )
        GradientDirection.BOTTOM_RIGHT ->
            Brush.linearGradient(
                listOf(base, mid, highlight),
                start = Offset(0f, 0f),
                end = Offset.Infinite,
            )
        GradientDirection.TOP_BOTTOM ->
            Brush.verticalGradient(listOf(highlight, mid, base))
    }
}
