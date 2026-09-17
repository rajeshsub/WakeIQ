package com.wakeiq.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * [AlarmRuntimeTransition] is the single component that owns legal moves between
 * [AlarmRuntimeState] values. These tests are the executable spec for the acceptance criteria:
 * scheduled->ringing, ringing->snoozed, snoozed->ringing, ringing->dismissed, an illegal
 * transition being rejected, and interrupted-state handling.
 */
class AlarmRuntimeTransitionTest {

    private val alarmId = 42L

    @Test
    fun `scheduled to monitoring on StartMonitoring`() {
        val result = AlarmRuntimeTransition.next(null, AlarmRuntimeEvent.StartMonitoring(alarmId))
        assertEquals(AlarmRuntimeState.Monitoring(alarmId), result)
    }

    @Test
    fun `scheduled to ringing on StartRinging (no Smart Wake window)`() {
        val result = AlarmRuntimeTransition.next(null, AlarmRuntimeEvent.StartRinging(alarmId))
        assertEquals(AlarmRuntimeState.Ringing(alarmId), result)
    }

    @Test
    fun `monitoring to ringing when motion escalates early`() {
        val monitoring = AlarmRuntimeState.Monitoring(alarmId)
        val result = AlarmRuntimeTransition.next(monitoring, AlarmRuntimeEvent.StartRinging(alarmId))
        assertEquals(AlarmRuntimeState.Ringing(alarmId), result)
    }

    @Test
    fun `ringing to snoozed`() {
        val ringing = AlarmRuntimeState.Ringing(alarmId)
        val result = AlarmRuntimeTransition.next(ringing, AlarmRuntimeEvent.Snooze(alarmId))
        assertEquals(AlarmRuntimeState.Snoozed(alarmId), result)
    }

    @Test
    fun `ringing to dismissed`() {
        val ringing = AlarmRuntimeState.Ringing(alarmId)
        val result = AlarmRuntimeTransition.next(ringing, AlarmRuntimeEvent.Dismiss(alarmId))
        assertEquals(AlarmRuntimeState.Dismissed(alarmId), result)
    }

    @Test
    fun `snoozed to ringing when the snooze trigger re-fires`() {
        val snoozed = AlarmRuntimeState.Snoozed(alarmId)
        val result = AlarmRuntimeTransition.next(snoozed, AlarmRuntimeEvent.StartRinging(alarmId))
        assertEquals(AlarmRuntimeState.Ringing(alarmId), result)
    }

    @Test
    fun `monitoring to interrupted when the service is torn down early`() {
        val monitoring = AlarmRuntimeState.Monitoring(alarmId)
        val result = AlarmRuntimeTransition.next(monitoring, AlarmRuntimeEvent.Interrupt(alarmId))
        assertEquals(AlarmRuntimeState.Interrupted(alarmId), result)
    }

    @Test
    fun `ringing to interrupted when the service is torn down early`() {
        val ringing = AlarmRuntimeState.Ringing(alarmId)
        val result = AlarmRuntimeTransition.next(ringing, AlarmRuntimeEvent.Interrupt(alarmId))
        assertEquals(AlarmRuntimeState.Interrupted(alarmId), result)
    }

    @Test
    fun `illegal transition snoozed to scheduled (StartMonitoring) is rejected`() {
        val snoozed = AlarmRuntimeState.Snoozed(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(snoozed, AlarmRuntimeEvent.StartMonitoring(alarmId))
        }
    }

    @Test
    fun `illegal transition dismissed to ringing is rejected (dismissed is terminal)`() {
        val dismissed = AlarmRuntimeState.Dismissed(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(dismissed, AlarmRuntimeEvent.StartRinging(alarmId))
        }
    }

    @Test
    fun `illegal transition snoozed to snoozed again is rejected`() {
        val snoozed = AlarmRuntimeState.Snoozed(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(snoozed, AlarmRuntimeEvent.Snooze(alarmId))
        }
    }

    @Test
    fun `illegal transition monitoring directly to snoozed is rejected`() {
        val monitoring = AlarmRuntimeState.Monitoring(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(monitoring, AlarmRuntimeEvent.Snooze(alarmId))
        }
    }

    @Test
    fun `illegal transition monitoring directly to dismissed is rejected`() {
        val monitoring = AlarmRuntimeState.Monitoring(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(monitoring, AlarmRuntimeEvent.Dismiss(alarmId))
        }
    }

    @Test
    fun `illegal transition scheduled directly to snoozed is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(null, AlarmRuntimeEvent.Snooze(alarmId))
        }
    }

    @Test
    fun `illegal transition scheduled directly to dismissed is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(null, AlarmRuntimeEvent.Dismiss(alarmId))
        }
    }

    @Test
    fun `illegal transition already-monitoring cannot start monitoring again`() {
        val monitoring = AlarmRuntimeState.Monitoring(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(monitoring, AlarmRuntimeEvent.StartMonitoring(alarmId))
        }
    }

    @Test
    fun `event for a different alarm id than the in-flight state is rejected`() {
        val ringing = AlarmRuntimeState.Ringing(alarmId)
        assertThrows(IllegalStateException::class.java) {
            AlarmRuntimeTransition.next(ringing, AlarmRuntimeEvent.Dismiss(alarmId + 1))
        }
    }
}
