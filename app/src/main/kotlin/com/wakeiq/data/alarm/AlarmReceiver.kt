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
        val phase = intent.getStringExtra(EXTRA_PHASE) ?: PHASE_RING
        Timber.d("Alarm receiver fired for alarm $alarmId (phase=$phase)")
        AlarmForegroundService.start(context, alarmId, phase)
    }

    companion object {
        const val ACTION_FIRE = "com.wakeiq.ACTION_FIRE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PHASE = "extra_phase"

        // Smart Wake fires a monitor alarm at the window start (motion detection only) and a
        // separate ring alarm at the hard target time. Non-smart alarms and snoozes use PHASE_RING.
        const val PHASE_MONITOR = "monitor"
        const val PHASE_RING = "ring"
    }
}
