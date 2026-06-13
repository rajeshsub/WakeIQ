package com.wakeiq.data.audio

import com.wakeiq.domain.model.Alarm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioPlayerRampTest {

    @Test
    fun `5 minute default reproduces the original 120s and 180s phases`() {
        assertEquals(120_000L to 180_000L, AudioPlayer.computeRampPhases(300_000L))
    }

    @Test
    fun `1 minute ramp splits 40-60 without the floor binding`() {
        assertEquals(24_000L to 36_000L, AudioPlayer.computeRampPhases(60_000L))
    }

    @Test
    fun `15 minute ramp splits 40-60`() {
        assertEquals(360_000L to 540_000L, AudioPlayer.computeRampPhases(900_000L))
    }

    @Test
    fun `very short ramp raises the whisper phase to the floor`() {
        assertEquals(20_000L to 20_000L, AudioPlayer.computeRampPhases(40_000L))
    }

    @Test
    fun `phases always sum to the total duration`() {
        listOf(40_000L, 60_000L, 300_000L, 900_000L, 123_456L).forEach { total ->
            val (whisper, escalation) = AudioPlayer.computeRampPhases(total)
            assertEquals(total, whisper + escalation, "phases must sum to total for $total")
        }
    }

    @Test
    fun `default alarm ramp duration is 5 minutes`() {
        assertEquals(5, Alarm(hour = 7, minute = 0).rampDurationMinutes)
    }
}
