package com.wakeiq.domain.model

import com.wakeiq.R

enum class BundledSound(val resourceName: String, val displayNameRes: Int, val category: SoundCategory) {
    BIRDS_CHIRPING("birds_chirping", R.string.sound_birds_chirping, SoundCategory.NATURE),
    RAIN("rain", R.string.sound_rain, SoundCategory.NATURE),
    THUNDERSTORM("thunderstorm", R.string.sound_thunderstorm, SoundCategory.NATURE),
    OCEAN_WAVES("ocean_waves", R.string.sound_ocean_waves, SoundCategory.NATURE),
    FARM_ANIMALS("farm_animals", R.string.sound_farm_animals, SoundCategory.NATURE),
    ROOSTER("rooster", R.string.sound_rooster, SoundCategory.NATURE),
    TRAIN_STATION("train_station", R.string.sound_train_station, SoundCategory.AMBIENT),
    AIRPORT("airport", R.string.sound_airport, SoundCategory.AMBIENT),
    CAFE("cafe", R.string.sound_cafe, SoundCategory.AMBIENT),
    OFFICE("office", R.string.sound_office, SoundCategory.AMBIENT),
    PIANO_MELODY("piano_melody", R.string.sound_piano_melody, SoundCategory.AMBIENT),
    SINGING_BOWL("singing_bowl", R.string.sound_singing_bowl, SoundCategory.AMBIENT),
}

enum class SoundCategory { NATURE, AMBIENT }
