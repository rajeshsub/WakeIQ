package com.wakeiq.data.alarm

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.usecase.GetNextAlarmTimeUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `smart wake alarm plans a monitor trigger and a ring trigger`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusHours(1)
        val alarm = Alarm(hour = 7, minute = 0, useSmartWake = true, smartWindowMinutes = 20)

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)

        assertEquals(2, triggers.size)
        assertEquals(setOf(AlarmReceiver.PHASE_MONITOR, AlarmReceiver.PHASE_RING), triggers.map { it.phase }.toSet())
        val monitor = triggers.first { it.phase == AlarmReceiver.PHASE_MONITOR }
        val ring = triggers.first { it.phase == AlarmReceiver.PHASE_RING }
        assertTrue(monitor.requestCode != ring.requestCode, "monitor and ring must use distinct request codes")
        assertTrue(monitor.triggerAtMillis < ring.triggerAtMillis, "monitor must precede the ring")
    }

    @Test
    fun `non-smart alarm plans a single ring trigger`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusHours(1)
        val alarm = Alarm(hour = 7, minute = 0, useSmartWake = false, smartWindowMinutes = 20)

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(AlarmReceiver.PHASE_RING, triggers.single().phase)
    }

    @Test
    fun `smart wake alarm whose window already passed plans only a ring trigger`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusMinutes(10)
        val alarm = Alarm(hour = 7, minute = 0, useSmartWake = true, smartWindowMinutes = 20)

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(AlarmReceiver.PHASE_RING, triggers.single().phase)
    }

    @Test
    fun `ring trigger fires at the ramp start, not the target time`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusHours(2)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            useSmartWake = true,
            smartWindowMinutes = 30,
            rampDurationMinutes = 5,
        )

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)
        val ring = triggers.first { it.phase == AlarmReceiver.PHASE_RING }

        val expectedRampStart = triggerAt.minusMinutes(5).toInstant().toEpochMilli()
        assertEquals(expectedRampStart, ring.triggerAtMillis, "ring must fire at T minus the ramp duration")
    }

    @Test
    fun `monitor trigger is skipped when the smart window is no wider than the ramp`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusHours(1)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            useSmartWake = true,
            smartWindowMinutes = 5,
            rampDurationMinutes = 10,
        )

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(AlarmReceiver.PHASE_RING, triggers.single().phase)
    }

    @Test
    fun `very short alarm falls back to ringing at the target time`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusMinutes(3)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            useSmartWake = false,
            rampDurationMinutes = 5,
        )

        val triggers = AlarmScheduler.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(triggerAt.toInstant().toEpochMilli(), triggers.single().triggerAtMillis)
    }
}
