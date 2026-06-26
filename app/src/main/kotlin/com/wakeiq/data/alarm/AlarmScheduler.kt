package com.wakeiq.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeiq.core.InstrumentedOnly
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.usecase.GetNextAlarmTimeUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@InstrumentedOnly
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
        AlarmSchedulePlanner.planTriggers(alarm, triggerAt, ZonedDateTime.now())
            .forEach { setExact(alarm.id, it) }
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
            ScheduledTrigger(
                AlarmReceiver.PHASE_RING,
                AlarmSchedulePlanner.ringRequestCode(alarm.id),
                triggerAt.toInstant().toEpochMilli(),
            ),
        )
        Timber.i("Scheduled snooze for alarm ${alarm.id} at $triggerAt")
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(
            buildPendingIntent(alarmId, AlarmReceiver.PHASE_MONITOR, AlarmSchedulePlanner.monitorRequestCode(alarmId)),
        )
        alarmManager.cancel(
            buildPendingIntent(alarmId, AlarmReceiver.PHASE_RING, AlarmSchedulePlanner.ringRequestCode(alarmId)),
        )
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
}
