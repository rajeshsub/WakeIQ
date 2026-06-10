package com.wakeiq.domain.model

import android.net.Uri

data class SoundConfig(
    val type: SoundType = SoundType.BUNDLED,
    val bundledSound: BundledSound = BundledSound.BIRDS_CHIRPING,
    val customUri: Uri? = null,
    val peakVolume: Float = 0.8f,
)

enum class SoundType { BUNDLED, CUSTOM }
