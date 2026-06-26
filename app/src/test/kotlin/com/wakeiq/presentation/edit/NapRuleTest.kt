package com.wakeiq.presentation.edit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NapRuleTest {

    private fun at(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 6, 26, hour, minute, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `a time half an hour away is a nap`() {
        assertTrue(NapRule.isNap(hour = 12, minute = 30, now = at(12, 0)))
    }

    @Test
    fun `a time two hours away is not a nap`() {
        assertFalse(NapRule.isNap(hour = 14, minute = 0, now = at(12, 0)))
    }

    @Test
    fun `a time earlier today rolls to tomorrow and is not a nap`() {
        assertFalse(NapRule.isNap(hour = 11, minute = 0, now = at(12, 0)))
    }

    @Test
    fun `just under the threshold is a nap`() {
        assertTrue(NapRule.isNap(hour = 13, minute = 29, now = at(12, 0)), "89 minutes is a nap")
    }

    @Test
    fun `exactly the threshold is not a nap`() {
        assertFalse(NapRule.isNap(hour = 13, minute = 30, now = at(12, 0)), "90 minutes is not under the cycle")
    }

    @Test
    fun `a nap is detected across midnight`() {
        assertTrue(NapRule.isNap(hour = 0, minute = 20, now = at(23, 50)), "30 minutes spanning midnight is a nap")
    }
}
