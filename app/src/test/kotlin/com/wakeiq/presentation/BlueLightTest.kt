package com.wakeiq.presentation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlueLightTest {

    @Test
    fun `inactive when disabled regardless of hour`() {
        (0..23).forEach { hour -> assertFalse(BlueLight.isActive(enabled = false, hour = hour)) }
    }

    @Test
    fun `active in the evening when enabled`() {
        assertTrue(BlueLight.isActive(enabled = true, hour = BlueLight.ON_HOUR))
        assertTrue(BlueLight.isActive(enabled = true, hour = 23))
    }

    @Test
    fun `active in the early morning when enabled`() {
        assertTrue(BlueLight.isActive(enabled = true, hour = 0))
        assertTrue(BlueLight.isActive(enabled = true, hour = BlueLight.OFF_HOUR - 1))
    }

    @Test
    fun `inactive during the day when enabled`() {
        (BlueLight.OFF_HOUR until BlueLight.ON_HOUR).forEach { hour ->
            assertFalse(BlueLight.isActive(enabled = true, hour = hour), "hour $hour is daytime")
        }
    }

    @Test
    fun `band boundaries are off at OFF_HOUR and on at ON_HOUR`() {
        assertFalse(BlueLight.isActive(enabled = true, hour = BlueLight.OFF_HOUR), "06:00 is daytime")
        assertTrue(BlueLight.isActive(enabled = true, hour = BlueLight.ON_HOUR), "18:00 is evening")
    }
}
