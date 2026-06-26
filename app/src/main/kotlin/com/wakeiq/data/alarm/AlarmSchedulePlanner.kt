package com.wakeiq.data.alarm

import com.wakeiq.domain.model.Alarm
import java.time.ZonedDateTime

/**
 * A single exact-alarm registration that backs one moment in the firing pipeline.
 * Smart Wake alarms produce two of these (monitor + ring); everything else produces one.
 */
internal data class ScheduledTrigger(val phase: String, val requestCode: Int, val triggerAtMillis: Long)

/**
 * Pure scheduling decisions, isolated from AlarmManager so they can be unit-tested.
 */
object AlarmSchedulePlanner {
    private const val MONITOR_REQUEST_CODE_OFFSET = 100_000

    fun ringRequestCode(alarmId: Long): Int = alarmId.toInt()

    fun monitorRequestCode(alarmId: Long): Int = (alarmId + MONITOR_REQUEST_CODE_OFFSET).toInt()

    /**
     * The ramp must finish by the target time T, so the ring trigger fires at T minus the ramp
     * duration. Smart Wake additionally schedules a monitor trigger at T minus the smart window,
     * giving motion detection a chance to start the ramp even earlier. If the smart window is no
     * wider than the ramp, the monitor would not precede the ring, so it is skipped. A ring start
     * already in the past (very short alarm) falls back to firing at T.
     */
    internal fun planTriggers(alarm: Alarm, triggerAt: ZonedDateTime, now: ZonedDateTime): List<ScheduledTrigger> {
        val rampStart = triggerAt.minusMinutes(alarm.rampDurationMinutes.toLong())
        val ringTriggerTime = if (rampStart.isAfter(now)) rampStart else triggerAt
        val ring = ScheduledTrigger(
            AlarmReceiver.PHASE_RING,
            ringRequestCode(alarm.id),
            ringTriggerTime.toInstant().toEpochMilli(),
        )
        val windowStart = triggerAt.minusMinutes(alarm.smartWindowMinutes.toLong())
        // The monitor window must open in the future and close before the ramp starts.
        val windowIsUsable = windowStart.isAfter(now) && windowStart.isBefore(rampStart)
        val useWindow = alarm.useSmartWake && alarm.smartWindowMinutes > 0 && windowIsUsable
        return if (useWindow) {
            listOf(
                ScheduledTrigger(
                    AlarmReceiver.PHASE_MONITOR,
                    monitorRequestCode(alarm.id),
                    windowStart.toInstant().toEpochMilli(),
                ),
                ring,
            )
        } else {
            listOf(ring)
        }
    }
}
