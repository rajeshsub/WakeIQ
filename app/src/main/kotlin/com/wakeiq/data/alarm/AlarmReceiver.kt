package com.wakeiq.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakeiq.data.service.AlarmForegroundService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) {
            Timber.e("AlarmReceiver received intent with no alarm ID")
            return
        }
        Timber.d("Alarm receiver fired for alarm $alarmId")
        AlarmForegroundService.start(context, alarmId)
    }

    companion object {
        const val ACTION_FIRE = "com.wakeiq.ACTION_FIRE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
