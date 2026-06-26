package com.wakeiq.presentation

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlarmPaletteTest {

    @Test
    fun `the default palette is not custom`() {
        assertFalse(AlarmPalettes[0].isCustom, "index 0 is the untinted default")
    }

    @Test
    fun `a tinted palette is custom`() {
        assertTrue(AlarmPalettes[1].isCustom)
    }

    @Test
    fun `isCustom is false only for the unspecified background`() {
        assertFalse(AlarmPalette(Color.Unspecified, Color.Unspecified).isCustom)
        assertTrue(AlarmPalette(Color(0xFF112233), Color.Unspecified).isCustom)
    }

    @Test
    fun `paletteForIndex returns the palette at a valid index`() {
        assertSame(AlarmPalettes[3], paletteForIndex(3))
    }

    @Test
    fun `paletteForIndex falls back to the default for an out-of-range index`() {
        assertSame(AlarmPalettes[0], paletteForIndex(AlarmPalettes.size))
        assertSame(AlarmPalettes[0], paletteForIndex(-1))
    }

    @Test
    fun `every palette index maps to a palette without throwing`() {
        AlarmPalettes.indices.forEach { i -> assertEquals(AlarmPalettes[i], paletteForIndex(i)) }
    }
}
