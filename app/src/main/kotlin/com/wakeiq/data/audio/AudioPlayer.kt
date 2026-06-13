package com.wakeiq.data.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class AudioPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: ExoPlayer? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var savedAlarmVolume: Int? = null

    fun prepare(soundConfig: SoundConfig) {
        release()
        forceAlarmStreamAudible()
        val uri = resolveUri(soundConfig)
        player = ExoPlayer.Builder(context).build().also { exo ->
            exo.setAudioAttributes(alarmAudioAttributes(), true)
            routeToBuiltInSpeaker(exo)
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.repeatMode = Player.REPEAT_MODE_ALL
            exo.volume = START_VOLUME
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

    // Phase 1: whisper ramp (START_VOLUME to WHISPER_VOLUME over WHISPER_PHASE_MS).
    // Phase 2: escalation ramp (WHISPER_VOLUME to targetVolume over ESCALATION_PHASE_MS).
    // Phase 3: holds at targetVolume. Caller responsible for sound cycling after this returns.
    // Total time to peak is WHISPER_PHASE_MS + ESCALATION_PHASE_MS (5 minutes).
    suspend fun escalateVolume(targetVolume: Float) {
        rampVolumeBetween(START_VOLUME, WHISPER_VOLUME, WHISPER_PHASE_MS)
        rampVolumeBetween(WHISPER_VOLUME, targetVolume, ESCALATION_PHASE_MS)
        player?.volume = targetVolume.coerceIn(0f, 1f)
        Timber.d("Escalation complete: full volume $targetVolume")
    }

    private suspend fun rampVolumeBetween(from: Float, to: Float, durationMs: Long) {
        val exo = player ?: return
        val stepDelayMs = durationMs / VOLUME_RAMP_STEPS
        val volStep = (to - from) / VOLUME_RAMP_STEPS
        var vol = from
        repeat(VOLUME_RAMP_STEPS) {
            exo.volume = vol.coerceIn(0f, 1f)
            vol += volStep
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

    companion object {
        private const val VOLUME_RAMP_STEPS = 100
        const val START_VOLUME = 0.05f
        const val WHISPER_VOLUME = 0.15f
        const val WHISPER_PHASE_MS = 120_000L
        const val ESCALATION_PHASE_MS = 180_000L
    }
}
