package com.wakeiq.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber

/**
 * Re-schedules all enabled alarms after device boot.
 *
 * On Android 15 (API 35) a mediaPlayback FGS cannot be launched from a BOOT_COMPLETED context.
 * This receiver therefore does zero async work itself: it enqueues a WorkManager job and returns
 * immediately. WorkManager provides guaranteed execution and automatic retry on process death.
 *
 * The actual FGS start happens exclusively from AlarmReceiver, which fires in the AlarmManager
 * setAlarmClock() callback context - an explicit system exemption for mediaPlayback FGS on API 34+.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Timber.i("Boot complete, enqueueing alarm reschedule worker")
        WorkManager.getInstance(context).enqueueUniqueWork(
            RescheduleAlarmsWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>().build(),
        )
    }
}
