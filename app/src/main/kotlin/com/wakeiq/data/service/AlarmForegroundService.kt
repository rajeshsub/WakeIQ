package com.wakeiq.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wakeiq.R
import com.wakeiq.data.alarm.AlarmReceiver
import com.wakeiq.data.alarm.AlarmScheduler
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class AlarmForegroundService : Service() {

    @Inject lateinit var alarmRepository: AlarmRepository

    @Inject lateinit var motionDetector: MotionDetector

    @Inject lateinit var audioPlayer: AudioPlayer

    @Inject lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentAlarmId: Long = -1L
    private var currentAlarm: Alarm? = null
    private var monitorJob: Job? = null
    private var escalated = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId == -1L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val phase = intent.getStringExtra(EXTRA_PHASE) ?: AlarmReceiver.PHASE_RING
                currentAlarmId = alarmId
                acquireWakeLock()
                postInitialForeground(alarmId, phase)
                loadAndStartAlarm(alarmId, phase)
            }
            ACTION_DISMISS -> dismissAlarm()
            ACTION_SNOOZE -> snoozeAlarm()
        }
        return START_NOT_STICKY
    }

    // The very first foreground notification already carries the full-screen intent and
    // CATEGORY_ALARM, so the alarm can take over the screen at t=0 of the service rather than
    // after the async DB load. Monitor-phase starts only show a waiting notification (no ring yet).
    private fun postInitialForeground(alarmId: Long, phase: String) {
        if (phase == AlarmReceiver.PHASE_RING) {
            startForeground(NOTIFICATION_ID_ESCALATION, buildEscalationNotification(alarmId, null))
        } else {
            startForeground(NOTIFICATION_ID, buildWaitingNotification(null))
        }
    }

    private fun loadAndStartAlarm(alarmId: Long, phase: String) {
        scope.launch {
            val alarm = alarmRepository.getById(alarmId) ?: run {
                Timber.e("Alarm $alarmId not found")
                stopSelf()
                return@launch
            }
            currentAlarm = alarm
            if (phase == AlarmReceiver.PHASE_MONITOR) {
                startMonitoring(alarm)
            } else {
                triggerEscalation(alarm)
            }
        }
    }

    private fun startMonitoring(alarm: Alarm) {
        updateWaitingNotification(alarm)
        motionDetector.startDetection(alarm.motionSensitivity)
        monitorJob = scope.launch {
            motionDetector.lightSleepDetected.collect {
                Timber.i("Light sleep detected, starting escalation early")
                triggerEscalation(alarm)
            }
        }
    }

    private fun triggerEscalation(alarm: Alarm) {
        if (escalated) return
        escalated = true
        monitorJob?.cancel()
        motionDetector.stopDetection()
        // Clear any sibling pending alarm for this id (e.g. the ring alarm when motion fired early).
        alarmScheduler.cancel(alarm.id)
        Timber.i("Triggering alarm escalation for alarm ${alarm.id}")

        postEscalationNotification(alarm)
        launchAlarmActivity(alarm)

        audioPlayer.prepare(alarm.soundConfig)
        audioPlayer.play()
        val rampDurationMs = alarm.rampDurationMinutes * MILLIS_PER_MINUTE
        scope.launch(Dispatchers.IO) {
            audioPlayer.escalateVolume(alarm.soundConfig.peakVolume, rampDurationMs)
        }
        scope.launch {
            delay(rampDurationMs)
            cycleSounds(alarm)
        }
    }

    // Direct launch within the alarm's background-activity-start exemption window. AlarmActivity has
    // showWhenLocked + turnScreenOn, so this turns the screen on and shows over the lock screen. The
    // FSI on the notification remains the fallback for OEMs that block the direct start.
    private fun launchAlarmActivity(alarm: Alarm) {
        runCatching {
            startActivity(buildAlarmActivityIntent(alarm.id, alarm.rampDurationMinutes * MILLIS_PER_MINUTE))
        }.onFailure { Timber.w(it, "Direct AlarmActivity start blocked; relying on full-screen intent") }
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

    private fun dismissAlarm() {
        Timber.i("Alarm dismissed")
        stopAlarmWork()
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_ESCALATION)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snoozeAlarm() {
        val alarm = currentAlarm
        Timber.i("Alarm snoozed for alarm $currentAlarmId")
        stopAlarmWork()
        if (alarm != null) {
            alarmScheduler.scheduleSnooze(alarm)
        }
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_ESCALATION)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAlarmWork() {
        monitorJob?.cancel()
        motionDetector.stopDetection()
        audioPlayer.release()
        releaseWakeLock()
    }

    override fun onDestroy() {
        scope.cancel()
        motionDetector.stopDetection()
        audioPlayer.release()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildWaitingNotification(alarm: Alarm?): Notification {
        val label = alarm?.let { it.label.ifEmpty { it.timeString } } ?: getString(R.string.app_name)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_alarm_title))
            .setContentText(label)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateWaitingNotification(alarm: Alarm) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildWaitingNotification(alarm))
    }

    private fun buildEscalationNotification(alarmId: Long, alarm: Alarm?): Notification {
        val label = alarm?.let { it.label.ifEmpty { it.timeString } } ?: getString(R.string.notif_alarm_title)
        val rampMs = (alarm?.rampDurationMinutes?.toLong() ?: DEFAULT_RAMP_MINUTES) * MILLIS_PER_MINUTE
        val activityPi = buildAlarmActivityPendingIntent(alarmId, rampMs)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_alarm_title))
            .setContentText(label)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPi)
            .setFullScreenIntent(activityPi, true)
            .addAction(buildSnoozeAction())
            .addAction(buildDismissAction())
            .build()
    }

    private fun postEscalationNotification(alarm: Alarm) {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !nm.canUseFullScreenIntent()) {
            Timber.w("USE_FULL_SCREEN_INTENT not granted - alarm may not launch full-screen")
        }
        // Single post on NOTIFICATION_ID_ESCALATION (the same id the initial RING post used).
        startForeground(NOTIFICATION_ID_ESCALATION, buildEscalationNotification(alarm.id, alarm))
        nm.cancel(NOTIFICATION_ID)
    }

    private fun buildAlarmActivityIntent(alarmId: Long, rampDurationMs: Long): Intent =
        Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_RAMP_DURATION_MS, rampDurationMs)
        }

    private fun buildAlarmActivityPendingIntent(alarmId: Long, rampDurationMs: Long): PendingIntent =
        PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            buildAlarmActivityIntent(alarmId, rampDurationMs),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun buildDismissAction(): NotificationCompat.Action {
        val pi = PendingIntent.getService(
            this,
            REQUEST_CODE_DISMISS,
            Intent(this, AlarmForegroundService::class.java).apply { action = ACTION_DISMISS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, getString(R.string.notif_alarm_dismiss), pi)
    }

    private fun buildSnoozeAction(): NotificationCompat.Action {
        val pi = PendingIntent.getService(
            this,
            REQUEST_CODE_SNOOZE,
            Intent(this, AlarmForegroundService::class.java).apply { action = ACTION_SNOOZE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, getString(R.string.notif_alarm_snooze), pi)
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel_v2"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_ESCALATION = 1002
        const val ACTION_START = "com.wakeiq.action.START_ALARM"
        const val ACTION_DISMISS = "com.wakeiq.action.DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.wakeiq.action.SNOOZE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PHASE = "extra_phase"
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val DEFAULT_RAMP_MINUTES = 5L
        private const val SOUND_SWITCH_INTERVAL_MS = 120_000L
        private const val REQUEST_CODE_DISMISS = 2001
        private const val REQUEST_CODE_SNOOZE = 2002
        private const val WAKE_LOCK_TAG = "WakeIQ:alarm"
        private const val WAKE_LOCK_TIMEOUT_MS = 60_000L

        private val FALLBACK_SOUND_ROTATION = listOf(
            BundledSound.MORNING_ROOSTERS,
            BundledSound.THUNDERSTORM,
            BundledSound.INDIAN_HARP,
            BundledSound.TRAIN_STATION,
        )

        fun start(context: Context, alarmId: Long, phase: String = AlarmReceiver.PHASE_RING) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_PHASE, phase)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun dismiss(context: Context) {
            context.startService(
                Intent(context, AlarmForegroundService::class.java).apply { action = ACTION_DISMISS },
            )
        }

        fun snooze(context: Context) {
            context.startService(
                Intent(context, AlarmForegroundService::class.java).apply { action = ACTION_SNOOZE },
            )
        }
    }
}
