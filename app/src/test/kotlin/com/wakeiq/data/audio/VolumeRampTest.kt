package com.wakeiq.data.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VolumeRampTest {

    @Test
    fun `5 minute default reproduces the original 120s and 180s phases`() {
        assertEquals(120_000L to 180_000L, VolumeRamp.computeRampPhases(300_000L))
    }

    @Test
    fun `1 minute ramp splits 40-60 without the floor binding`() {
        assertEquals(24_000L to 36_000L, VolumeRamp.computeRampPhases(60_000L))
    }

    @Test
    fun `15 minute ramp splits 40-60`() {
        assertEquals(360_000L to 540_000L, VolumeRamp.computeRampPhases(900_000L))
    }

    @Test
    fun `very short ramp raises the whisper phase to the floor`() {
        assertEquals(20_000L to 20_000L, VolumeRamp.computeRampPhases(40_000L))
    }

    @Test
    fun `extremely short ramp still leaves a non-negative escalation`() {
        val (whisper, escalation) = VolumeRamp.computeRampPhases(5_000L)
        assertTrue(escalation >= 0L, "escalation must never go negative")
        assertEquals(5_000L, whisper + escalation, "phases must still sum to the total")
    }

    @Test
    fun `phases always sum to the total duration`() {
        listOf(40_000L, 60_000L, 300_000L, 900_000L, 123_456L).forEach { total ->
            val (whisper, escalation) = VolumeRamp.computeRampPhases(total)
            assertEquals(total, whisper + escalation, "phases must sum to total for $total")
        }
    }

    @Test
    fun `volumeSteps starts at from and produces exactly STEPS values`() {
        val steps = VolumeRamp.volumeSteps(VolumeRamp.START_VOLUME, VolumeRamp.WHISPER_VOLUME)
        assertEquals(VolumeRamp.STEPS, steps.size)
        assertEquals(VolumeRamp.START_VOLUME, steps.first())
    }

    @Test
    fun `volumeSteps increases monotonically when ramping up`() {
        val steps = VolumeRamp.volumeSteps(0.05f, 0.15f)
        steps.zipWithNext().forEach { (a, b) -> assertTrue(b >= a, "expected non-decreasing, got $a then $b") }
    }

    @Test
    fun `volumeSteps clamps every value into the 0 to 1 range`() {
        val steps = VolumeRamp.volumeSteps(0.9f, 5f)
        assertTrue(steps.all { it in 0f..1f }, "all steps must be clamped to 0..1")
        assertEquals(1f, steps.last(), "an over-range target clamps to full volume")
    }
}
