package com.wakeiq.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class AlarmTest {

    @Test
    fun `timeString zero-pads hour and minute`() {
        assertEquals("07:05", Alarm(hour = 7, minute = 5).timeString)
    }

    @Test
    fun `formattedTime in 24 hour mode matches timeString`() {
        val alarm = Alarm(hour = 18, minute = 9)
        assertEquals("18:09", alarm.formattedTime(is24Hour = true))
    }

    @Test
    fun `formattedTime renders midnight as 12 AM in 12 hour mode`() {
        assertEquals("12:00 AM", Alarm(hour = 0, minute = 0).formattedTime(is24Hour = false))
    }

    @Test
    fun `formattedTime renders noon as 12 PM in 12 hour mode`() {
        assertEquals("12:00 PM", Alarm(hour = 12, minute = 0).formattedTime(is24Hour = false))
    }

    @Test
    fun `formattedTime renders afternoon hours with PM`() {
        assertEquals("1:30 PM", Alarm(hour = 13, minute = 30).formattedTime(is24Hour = false))
        assertEquals("11:45 PM", Alarm(hour = 23, minute = 45).formattedTime(is24Hour = false))
    }

    @Test
    fun `formattedTime renders morning hours with AM`() {
        assertEquals("9:00 AM", Alarm(hour = 9, minute = 0).formattedTime(is24Hour = false))
    }

    @Test
    fun `isRecurring is false without days and true with days`() {
        assertFalse(Alarm(hour = 7, minute = 0).isRecurring)
        assertTrue(Alarm(hour = 7, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY)).isRecurring)
    }

    @Test
    fun `default ramp duration is 5 minutes`() {
        assertEquals(5, Alarm(hour = 7, minute = 0).rampDurationMinutes)
    }
}
