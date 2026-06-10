package com.wakeiq.data.alarm

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.usecase.GetNextAlarmTimeUseCase
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class AlarmSchedulerTest {

    private val getNextAlarmTime = GetNextAlarmTimeUseCase()

    @Test
    fun `one-shot alarm scheduled in future returns non-null time`() {
        val alarm = Alarm(hour = 23, minute = 59, isEnabled = true)
        val from = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        val result = getNextAlarmTime(alarm, from)
        assertNotNull(result)
    }

    @Test
    fun `recurring alarm on matching day returns correct time`() {
        val today = ZonedDateTime.now()
        val alarm = Alarm(
            hour = today.hour + 1,
            minute = 0,
            daysOfWeek = setOf(today.dayOfWeek),
            isEnabled = true,
        )
        val result = getNextAlarmTime(alarm, today)
        assertNotNull(result)
    }

    @Test
    fun `disabled alarm returns null`() {
        val alarm = Alarm(hour = 7, minute = 0, isEnabled = false)
        val result = getNextAlarmTime(alarm)
        assert(result == null)
    }

    @Test
    fun `past one-shot alarm scheduled for next day`() {
        val alarm = Alarm(hour = 0, minute = 1, isEnabled = true)
        val from = ZonedDateTime.now().withHour(6).withMinute(0)
        val result = getNextAlarmTime(alarm, from)
        assertNotNull(result)
        assert(result!!.isAfter(from))
    }
}
