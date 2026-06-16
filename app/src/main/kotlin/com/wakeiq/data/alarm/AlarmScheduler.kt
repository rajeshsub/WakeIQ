package com.wakeiq.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.usecase.GetNextAlarmTimeUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single exact-alarm registration that backs one moment in the firing pipeline.
 * Smart Wake alarms produce two of these (monitor + ring); everything else produces one.
 */
internal data class ScheduledTrigger(val phase: String, val requestCode: Int, val triggerAtMillis: Long)

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getNextAlarmTime: GetNextAlarmTimeUseCase,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerAt = getNextAlarmTime(alarm) ?: return
        if (!canScheduleExactAlarms()) {
            Timber.w("Exact alarm permission not granted for alarm ${alarm.id}")
            return
        }
        planTriggers(alarm, triggerAt, ZonedDateTime.now()).forEach { setExact(alarm.id, it) }
        Timber.i("Scheduled alarm ${alarm.id} for target $triggerAt")
    }

    fun scheduleSnooze(alarm: Alarm) {
        if (!canScheduleExactAlarms()) {
            Timber.w("Exact alarm permission not granted, cannot schedule snooze for alarm ${alarm.id}")
            return
        }
        val triggerAt = ZonedDateTime.now().plusMinutes(alarm.snoozeMinutes.toLong())
        setExact(
            alarm.id,
            ScheduledTrigger(AlarmReceiver.PHASE_RING, ringRequestCode(alarm.id), triggerAt.toInstant().toEpochMilli()),
        )
        Timber.i("Scheduled snooze for alarm ${alarm.id} at $triggerAt")
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(buildPendingIntent(alarmId, AlarmReceiver.PHASE_MONITOR, monitorRequestCode(alarmId)))
        alarmManager.cancel(buildPendingIntent(alarmId, AlarmReceiver.PHASE_RING, ringRequestCode(alarmId)))
        Timber.i("Cancelled alarm $alarmId")
    }

    private fun setExact(alarmId: Long, trigger: ScheduledTrigger) {
        val pi = buildPendingIntent(alarmId, trigger.phase, trigger.requestCode)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger.triggerAtMillis, pi), pi)
    }

    private fun buildPendingIntent(alarmId: Long, phase: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_PHASE, phase)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val MONITOR_REQUEST_CODE_OFFSET = 100_000

        private fun ringRequestCode(alarmId: Long): Int = alarmId.toInt()

        private fun monitorRequestCode(alarmId: Long): Int = (alarmId + MONITOR_REQUEST_CODE_OFFSET).toInt()

        /**
         * Pure scheduling decision, isolated from Android so it can be unit-tested.
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
}
