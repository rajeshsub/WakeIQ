package com.wakeiq.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakeiq.domain.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmRepository: AlarmRepository

    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        Timber.i("Device booted, re-registering alarms")

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val alarms = alarmRepository.observeAll().first()
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    alarmScheduler.schedule(alarm)
                    Timber.d("Re-scheduled alarm ${alarm.id}")
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
        )
    }
}
