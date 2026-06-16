package com.wakeiq.presentation

import androidx.compose.ui.graphics.Color

/** A card colour option for an alarm. Index 0 is the default (untinted) card. */
data class AlarmPalette(val background: Color, val onBackground: Color) {
    val isCustom get() = background != Color.Unspecified
}

/** Ordered palette list; an alarm stores its choice as the index into this list. */
val AlarmPalettes = listOf(
    AlarmPalette(Color.Unspecified, Color.Unspecified),
    AlarmPalette(Color(0xFFFFFBEB), Color(0xFF92400E)),
    AlarmPalette(Color(0xFFFFF1F2), Color(0xFF9F1239)),
    AlarmPalette(Color(0xFFECFDF5), Color(0xFF065F46)),
    AlarmPalette(Color(0xFFF0F9FF), Color(0xFF075985)),
    AlarmPalette(Color(0xFFF5F3FF), Color(0xFF5B21B6)),
)

fun paletteForIndex(index: Int): AlarmPalette = AlarmPalettes.getOrElse(index) { AlarmPalettes[0] }
