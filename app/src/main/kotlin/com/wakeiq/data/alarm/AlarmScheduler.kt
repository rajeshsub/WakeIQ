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

        val smartWindowStart = triggerAt.minusMinutes(alarm.smartWindowMinutes.toLong())
        val scheduledAt = if (smartWindowStart.isAfter(ZonedDateTime.now())) {
            smartWindowStart
        } else {
            triggerAt
        }

        val intent = buildAlarmIntent(alarm.id)
        val clockInfo = AlarmManager.AlarmClockInfo(
            scheduledAt.toInstant().toEpochMilli(),
            intent,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Timber.w("Exact alarm permission not granted for alarm ${alarm.id}")
            return
        }

        alarmManager.setAlarmClock(clockInfo, intent)
        Timber.i("Scheduled alarm ${alarm.id} at $scheduledAt (target: $triggerAt)")
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(buildAlarmIntent(alarmId))
        Timber.i("Cancelled alarm $alarmId")
    }

    private fun buildAlarmIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
