package com.wakeiq.domain.model

import com.wakeiq.R

enum class BundledSound(val assetFile: String, val displayNameRes: Int, val category: SoundCategory) {
    // Nature & Weather
    BIRDS_LIGHT_RAIN("nature_birds_light_rain.mp3", R.string.sound_birds_light_rain, SoundCategory.NATURE),
    FOREST_BIRDS("nature_forest_birds.mp3", R.string.sound_forest_birds, SoundCategory.NATURE),
    OCEAN_COAST("nature_ocean_coast.mp3", R.string.sound_ocean_coast, SoundCategory.NATURE),
    STREAM("nature_stream.mp3", R.string.sound_stream, SoundCategory.NATURE),
    CITY_RAIN("nature_city_rain.mp3", R.string.sound_city_rain, SoundCategory.NATURE),
    THUNDERSTORM("nature_thunderstorm.mp3", R.string.sound_thunderstorm, SoundCategory.NATURE),

    // Farm & Animals
    SHEEP_HORSES_DOGS("farm_sheep_horses_dogs.mp3", R.string.sound_sheep_horses_dogs, SoundCategory.FARM),
    CRICKETS_NIGHT("farm_crickets_night.mp3", R.string.sound_crickets_night, SoundCategory.FARM),
    MORNING_ROOSTERS("farm_morning_roosters.mp3", R.string.sound_morning_roosters, SoundCategory.FARM),
    COWS("farm_cows.mp3", R.string.sound_cows, SoundCategory.FARM),
    SUMMER_FIELD("farm_summer_field.mp3", R.string.sound_summer_field, SoundCategory.FARM),

    // Music & Melodies
    VAQUERO("music_vaquero.mp3", R.string.sound_vaquero, SoundCategory.MUSIC),
    MUSIC_BOX("music_music_box.mp3", R.string.sound_music_box, SoundCategory.MUSIC),
    PIANO("music_piano.mp3", R.string.sound_piano, SoundCategory.MUSIC),
    INDIAN_HARP("music_indian_harp.mp3", R.string.sound_indian_harp, SoundCategory.MUSIC),
    WIND_CHIMES("music_wind_chimes.mp3", R.string.sound_wind_chimes, SoundCategory.MUSIC),

    // Ambient Places
    CAFE("places_cafe.mp3", R.string.sound_cafe, SoundCategory.PLACES),
    AIRPORT("places_airport.mp3", R.string.sound_airport, SoundCategory.PLACES),
    BUSY_STREET("places_busy_street.mp3", R.string.sound_busy_street, SoundCategory.PLACES),
    OFFICE("places_office.mp3", R.string.sound_office, SoundCategory.PLACES),
    TRAIN_STATION("places_train_station.mp3", R.string.sound_train_station, SoundCategory.PLACES),
}

enum class SoundCategory(val displayNameRes: Int) {
    NATURE(R.string.sound_category_nature),
    FARM(R.string.sound_category_farm),
    MUSIC(R.string.sound_category_music),
    PLACES(R.string.sound_category_places),
}
