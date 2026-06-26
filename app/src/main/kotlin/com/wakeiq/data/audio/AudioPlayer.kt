package com.wakeiq.data.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.wakeiq.core.InstrumentedOnly
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

@InstrumentedOnly
class AudioPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: ExoPlayer? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var savedAlarmVolume: Int? = null

    fun prepare(soundConfig: SoundConfig) {
        release()
        forceAlarmStreamAudible()
        val uri = resolveUri(soundConfig)
        player = ExoPlayer.Builder(context).build().also { exo ->
            // Surface playback errors: ExoPlayer otherwise fails silently, which once hid an alarm
            // crash for a long time.
            exo.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e(error, "Alarm playback error: ${error.errorCodeName}")
                    }
                },
            )
            // handleAudioFocus MUST be false for USAGE_ALARM: media3 throws
            // IllegalArgumentException ("Automatic handling of audio focus is only available for
            // USAGE_MEDIA and USAGE_GAME") otherwise, which crashed the app on every alarm. An alarm
            // should ring regardless of focus anyway, so we never want media3 managing focus here.
            exo.setAudioAttributes(alarmAudioAttributes(), false)
            routeToBuiltInSpeaker(exo)
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.repeatMode = Player.REPEAT_MODE_ALL
            exo.volume = VolumeRamp.START_VOLUME
            exo.prepare()
        }
        Timber.d("Audio prepared: $uri")
    }

    // The alarm must ring even if the user left the system alarm stream at zero. Raise STREAM_ALARM
    // to its maximum at fire time and restore the user's level on release. The actual loudness curve
    // is still controlled by the gentle exo.volume ramp, so this only guarantees audibility.
    private fun forceAlarmStreamAudible() {
        runCatching {
            if (savedAlarmVolume == null) {
                savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        }.onFailure { Timber.w(it, "Could not raise alarm stream volume") }
    }

    private fun restoreAlarmStreamVolume() {
        val saved = savedAlarmVolume ?: return
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0) }
            .onFailure { Timber.w(it, "Could not restore alarm stream volume") }
        savedAlarmVolume = null
    }

    // Pin output to the phone's built-in speaker so the alarm is never diverted to a connected
    // Bluetooth headset or speaker that the sleeper cannot hear.
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun routeToBuiltInSpeaker(exo: ExoPlayer) {
        runCatching {
            val speaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            exo.setPreferredAudioDevice(speaker)
        }.onFailure { Timber.w(it, "Could not pin alarm output to the built-in speaker") }
    }

    fun play() {
        player?.play()
        Timber.d("Audio play started")
    }

    fun switchSound(soundConfig: SoundConfig) {
        val uri = resolveUri(soundConfig)
        player?.run {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
        Timber.d("Sound switched to: ${soundConfig.bundledSound}")
    }

    // Phase 1: whisper ramp (START_VOLUME to WHISPER_VOLUME).
    // Phase 2: escalation ramp (WHISPER_VOLUME to targetVolume).
    // Phase 3: holds at targetVolume. Caller responsible for sound cycling after this returns.
    // The two phases sum to rampDurationMs, split via VolumeRamp.computeRampPhases, so the audio
    // ramp stays in step with the brightness ramp at whatever duration the alarm is configured for.
    suspend fun escalateVolume(targetVolume: Float, rampDurationMs: Long) {
        val (whisperMs, escalationMs) = VolumeRamp.computeRampPhases(rampDurationMs)
        rampVolumeBetween(VolumeRamp.START_VOLUME, VolumeRamp.WHISPER_VOLUME, whisperMs)
        rampVolumeBetween(VolumeRamp.WHISPER_VOLUME, targetVolume, escalationMs)
        player?.volume = targetVolume.coerceIn(0f, 1f)
        Timber.d("Escalation complete over ${rampDurationMs}ms: full volume $targetVolume")
    }

    private suspend fun rampVolumeBetween(from: Float, to: Float, durationMs: Long) {
        val exo = player ?: return
        val stepDelayMs = durationMs / VolumeRamp.STEPS
        VolumeRamp.volumeSteps(from, to).forEach { vol ->
            exo.volume = vol
            delay(stepDelayMs)
        }
        exo.volume = to.coerceIn(0f, 1f)
    }

    fun playPreview(soundConfig: SoundConfig) {
        release()
        val uri = resolveUri(soundConfig)
        player = ExoPlayer.Builder(context).build().also { exo ->
            exo.setAudioAttributes(previewAudioAttributes(), true)
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.repeatMode = Player.REPEAT_MODE_OFF
            exo.volume = 1f
            exo.prepare()
            exo.play()
        }
    }

    fun release() {
        restoreAlarmStreamVolume()
        player?.stop()
        player?.release()
        player = null
    }

    private fun resolveUri(soundConfig: SoundConfig): Uri = when (soundConfig.type) {
        SoundType.CUSTOM ->
            soundConfig.customUri ?: assetUri(BundledSound.BIRDS_LIGHT_RAIN.assetFile)
        SoundType.BUNDLED -> assetUri(soundConfig.bundledSound.assetFile)
    }

    private fun assetUri(assetFile: String): Uri = Uri.parse("asset:///sounds/$assetFile")

    private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_ALARM)
        .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
        .build()

    private fun previewAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
}
