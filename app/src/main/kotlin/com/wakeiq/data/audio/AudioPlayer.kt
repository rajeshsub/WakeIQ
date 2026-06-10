package com.wakeiq.data.audio

import android.content.Context
import android.net.Uri
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

    fun prepare(soundConfig: SoundConfig) {
        release()
        val uri = resolveUri(soundConfig)
        player = ExoPlayer.Builder(context).build().also { exo ->
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.repeatMode = Player.REPEAT_MODE_ALL
            exo.volume = 0f
            exo.prepare()
        }
        Timber.d("Audio prepared: $uri")
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

    // Phase 1: whisper ramp (0 → WHISPER_VOLUME over WHISPER_PHASE_MS).
    // Phase 2: escalation ramp (WHISPER_VOLUME → targetVolume over ESCALATION_PHASE_MS).
    // Phase 3: holds at targetVolume. Caller responsible for sound cycling after this returns.
    suspend fun escalateVolume(targetVolume: Float) {
        rampVolumeBetween(0f, WHISPER_VOLUME, WHISPER_PHASE_MS)
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

    fun release() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun resolveUri(soundConfig: SoundConfig): Uri = when (soundConfig.type) {
        SoundType.CUSTOM ->
            soundConfig.customUri ?: rawUri(BundledSound.BIRDS_CHIRPING.resourceName)
        SoundType.BUNDLED -> rawUri(soundConfig.bundledSound.resourceName)
    }

    private fun rawUri(resourceName: String): Uri {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    companion object {
        private const val VOLUME_RAMP_STEPS = 100
        const val WHISPER_VOLUME = 0.15f
        const val WHISPER_PHASE_MS = 120_000L
        const val ESCALATION_PHASE_MS = 180_000L
    }
}
