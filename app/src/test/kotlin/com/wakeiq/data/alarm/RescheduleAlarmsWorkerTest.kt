package com.wakeiq.data.alarm

import com.wakeiq.domain.model.Alarm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// RescheduleAlarmsWorker runs after a device reboot or process death to restore every enabled
// alarm's AlarmManager registration (both are lost when the process/OS state is torn down). The
// CoroutineWorker itself is a thin WorkManager/Hilt adapter (see ADR 0002) so, following the same
// extract-the-pure-decision pattern as AlarmSchedulePlanner, the "which alarms survive a restart"
// selection is exposed as a pure function here and unit-tested directly.
class RescheduleAlarmsWorkerTest {

    @Test
    fun `only enabled alarms are restored after a restart`() {
        val enabled = Alarm(id = 1L, hour = 7, minute = 0, isEnabled = true)
        val disabled = Alarm(id = 2L, hour = 8, minute = 0, isEnabled = false)

        val result = RescheduleAlarmsWorker.alarmsToReschedule(listOf(enabled, disabled))

        assertEquals(listOf(enabled), result)
    }

    @Test
    fun `all alarms are restored when all are enabled`() {
        val alarms = listOf(
            Alarm(id = 1L, hour = 7, minute = 0, isEnabled = true),
            Alarm(id = 2L, hour = 8, minute = 0, isEnabled = true),
            Alarm(id = 3L, hour = 9, minute = 0, isEnabled = true),
        )

        val result = RescheduleAlarmsWorker.alarmsToReschedule(alarms)

        assertEquals(alarms, result)
    }

    @Test
    fun `no alarms are restored when none are enabled`() {
        val alarms = listOf(
            Alarm(id = 1L, hour = 7, minute = 0, isEnabled = false),
            Alarm(id = 2L, hour = 8, minute = 0, isEnabled = false),
        )

        val result = RescheduleAlarmsWorker.alarmsToReschedule(alarms)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `an empty alarm list restores nothing`() {
        val result = RescheduleAlarmsWorker.alarmsToReschedule(emptyList())

        assertTrue(result.isEmpty())
    }
}
