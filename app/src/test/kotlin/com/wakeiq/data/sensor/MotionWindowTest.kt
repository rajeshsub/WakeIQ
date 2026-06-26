package com.wakeiq.data.sensor

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MotionWindowTest {

    @Test
    fun `does not signal before reaching the minimum number of readings`() {
        val window = MotionWindow(threshold = 0f, windowSize = 5, minReadings = 3)
        assertFalse(window.add(0f), "one reading is below the minimum")
        assertFalse(window.add(10f), "two readings are still below the minimum")
    }

    @Test
    fun `signals once variation exceeds the threshold and enough readings exist`() {
        val window = MotionWindow(threshold = 1f, windowSize = 5, minReadings = 3)
        window.add(0f)
        window.add(5f)
        assertTrue(window.add(0f), "alternating 0 and 5 has a standard deviation above 1")
    }

    @Test
    fun `a still sleeper with constant readings never signals`() {
        val window = MotionWindow(threshold = 0.5f, windowSize = 5, minReadings = 3)
        repeat(10) { assertFalse(window.add(3f), "constant readings have zero variation") }
    }

    @Test
    fun `a high threshold suppresses signalling despite movement`() {
        val window = MotionWindow(threshold = 100f, windowSize = 5, minReadings = 3)
        var signalled = false
        repeat(10) { i -> if (window.add(if (i % 2 == 0) 0f else 10f)) signalled = true }
        assertFalse(signalled, "variation of ~5 must not cross a threshold of 100")
    }

    @Test
    fun `clear resets the window so detection must build up again`() {
        val window = MotionWindow(threshold = 1f, windowSize = 5, minReadings = 3)
        window.add(0f)
        window.add(5f)
        window.add(0f)
        window.clear()
        assertFalse(window.add(0f), "after clear the window is below the minimum again")
        assertFalse(window.add(5f), "still below the minimum after clear")
    }
}
