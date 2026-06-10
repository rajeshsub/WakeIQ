package com.wakeiq.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wakeiq.R
import com.wakeiq.data.audio.AudioPlayer
import com.wakeiq.data.sensor.MotionDetector
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.SoundType
import com.wakeiq.domain.repository.AlarmRepository
import com.wakeiq.presentation.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var alarmRepository: AlarmRepository

    @Inject lateinit var motionDetector: MotionDetector

    @Inject lateinit var audioPlayer: AudioPlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentAlarmId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId == -1L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentAlarmId = alarmId
                startForeground(NOTIFICATION_ID, buildLoadingNotification())
                loadAndStartAlarm(alarmId)
            }
            ACTION_DISMISS -> dismissAlarm()
            ACTION_SNOOZE -> snoozeAlarm()
        }
        return START_NOT_STICKY
    }

    private fun loadAndStartAlarm(alarmId: Long) {
        scope.launch {
            val alarm = alarmRepository.getById(alarmId) ?: run {
                Timber.e("Alarm $alarmId not found")
                stopSelf()
                return@launch
            }
            startSmartWindow(alarm)
        }
    }

    private fun startSmartWindow(alarm: Alarm) {
        scope.launch {
            val windowMs = alarm.smartWindowMinutes * MILLIS_PER_MINUTE

            updateNotification(alarm)

            motionDetector.startDetection(alarm.motionSensitivity)

            val motionJob = launch {
                motionDetector.lightSleepDetected.collect {
                    Timber.i("Light sleep detected, starting escalation early")
                    triggerEscalation(alarm)
                    cancel()
                }
            }

            delay(windowMs)
            motionJob.cancel()
            motionDetector.stopDetection()
            triggerEscalation(alarm)
        }
    }

    private fun triggerEscalation(alarm: Alarm) {
        motionDetector.stopDetection()
        Timber.i("Triggering alarm escalation for alarm ${alarm.id}")

        audioPlayer.prepare(alarm.soundConfig)
        audioPlayer.play()

        // Gradual escalation: whisper for 2 min, ramp to full over next 3 min
        scope.launch(Dispatchers.IO) {
            audioPlayer.escalateVolume(alarm.soundConfig.peakVolume)
        }

        // After reaching full volume, cycle sounds every 2 min to break through deep sleep
        scope.launch {
            delay(SOUND_SWITCH_START_MS)
            cycleSounds(alarm)
        }

        showAlarmActivity(alarm)
    }

    private suspend fun cycleSounds(alarm: Alarm) {
        FALLBACK_SOUND_ROTATION
            .filter { it != alarm.soundConfig.bundledSound }
            .forEach { sound ->
                delay(SOUND_SWITCH_INTERVAL_MS)
                audioPlayer.switchSound(
                    alarm.soundConfig.copy(
                        type = SoundType.BUNDLED,
                        bundledSound = sound,
                    ),
                )
                Timber.i("Auto-switched alarm sound to $sound")
            }
    }

    private fun showAlarmActivity(alarm: Alarm) {
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmActivity.EXTRA_RAMP_DURATION_MS, alarm.rampDurationMinutes * MILLIS_PER_MINUTE)
        }
        startActivity(alarmIntent)
    }

    private fun dismissAlarm() {
        Timber.i("Alarm dismissed")
        motionDetector.stopDetection()
        audioPlayer.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snoozeAlarm() {
        Timber.i("Alarm snoozed for alarm $currentAlarmId")
        motionDetector.stopDetection()
        audioPlayer.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        motionDetector.stopDetection()
        audioPlayer.release()
        super.onDestroy()
    }

    private fun buildLoadingNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.app_name))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .build()

    private fun updateNotification(alarm: Alarm) {
        val label = alarm.label.ifEmpty { alarm.timeString }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_alarm_title))
            .setContentText(label)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(buildFullScreenIntent(alarm.id), true)
            .addAction(buildDismissAction())
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildFullScreenIntent(alarmId: Long): PendingIntent {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildDismissAction(): NotificationCompat.Action {
        val dismissIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_DISMISS
        }
        val pi = PendingIntent.getService(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, getString(R.string.notif_alarm_dismiss), pi)
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.wakeiq.action.START_ALARM"
        const val ACTION_DISMISS = "com.wakeiq.action.DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.wakeiq.action.SNOOZE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val MILLIS_PER_MINUTE = 60_000L

        // Total escalation time = WHISPER_PHASE_MS + ESCALATION_PHASE_MS from AudioPlayer
        private const val SOUND_SWITCH_START_MS = 300_000L
        private const val SOUND_SWITCH_INTERVAL_MS = 120_000L

        private val FALLBACK_SOUND_ROTATION = listOf(
            BundledSound.ROOSTER,
            BundledSound.THUNDERSTORM,
            BundledSound.SINGING_BOWL,
            BundledSound.TRAIN_STATION,
        )

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun dismiss(context: Context) {
            context.startService(
                Intent(context, AlarmForegroundService::class.java).apply {
                    action = ACTION_DISMISS
                },
            )
        }

        fun snooze(context: Context) {
            context.startService(
                Intent(context, AlarmForegroundService::class.java).apply {
                    action = ACTION_SNOOZE
                },
            )
        }
    }
}
