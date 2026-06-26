package com.wakeiq.data.alarm

import com.wakeiq.domain.model.Alarm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

// Time-resolution cases (one-shot, recurring, disabled) live in GetNextAlarmTimeUseCaseTest with a
// fixed clock. This suite covers the pure trigger planning in AlarmSchedulePlanner.
class AlarmSchedulerTest {

    @Test
    fun `smart wake alarm plans a monitor trigger and a ring trigger`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusHours(1)
        val alarm = Alarm(hour = 7, minute = 0, useSmartWake = true, smartWindowMinutes = 20)

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)

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

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(AlarmReceiver.PHASE_RING, triggers.single().phase)
    }

    @Test
    fun `smart wake alarm whose window already passed plans only a ring trigger`() {
        val now = ZonedDateTime.now()
        val triggerAt = now.plusMinutes(10)
        val alarm = Alarm(hour = 7, minute = 0, useSmartWake = true, smartWindowMinutes = 20)

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)

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

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)
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

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)

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

        val triggers = AlarmSchedulePlanner.planTriggers(alarm, triggerAt, now)

        assertEquals(1, triggers.size)
        assertEquals(triggerAt.toInstant().toEpochMilli(), triggers.single().triggerAtMillis)
    }
}
