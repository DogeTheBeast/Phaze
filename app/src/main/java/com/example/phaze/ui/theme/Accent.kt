package com.example.phaze.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Selectable accent colors (mockup settings.html "Accent color" dots).
 * The chosen accent becomes the Material primary + container tones.
 */
enum class Accent(val key: String, val label: String, val primary: Color) {
    BLUE("blue", "Blue", Color(0xFF36A3D9)),
    GREEN("green", "Green", Color(0xFFB8CC52)),
    CYAN("cyan", "Cyan", Color(0xFF95E5CB)),
    YELLOW("yellow", "Yellow", Color(0xFFE6C446)),
    ORANGE("orange", "Orange", Color(0xFFF19618)),
    ;

    companion object {
        fun fromKey(key: String?): Accent = entries.firstOrNull { it.key == key } ?: BLUE
    }
}
