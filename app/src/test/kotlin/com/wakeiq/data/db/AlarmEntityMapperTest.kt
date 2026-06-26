package com.wakeiq.data.db

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.MotionSensitivity
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class AlarmEntityMapperTest {

    private fun entity(bundledSoundName: String = BundledSound.OCEAN_COAST.name, daysOfWeek: String = "") = AlarmEntity(
        id = 1L,
        hour = 7,
        minute = 30,
        daysOfWeek = daysOfWeek,
        isEnabled = true,
        soundType = SoundType.BUNDLED.name,
        bundledSoundName = bundledSoundName,
        customUri = null,
        peakVolume = 0.7f,
        smartWindowMinutes = 20,
        rampDurationMinutes = 10,
        motionSensitivity = MotionSensitivity.HIGH.name,
        snoozeMinutes = 5,
        label = "Morning",
        useSmartWake = true,
        colorIndex = 2,
    )

    @Test
    fun `domain to entity to domain round-trips a bundled alarm`() {
        val alarm = Alarm(
            id = 4L,
            hour = 6,
            minute = 45,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            soundConfig = SoundConfig(type = SoundType.BUNDLED, bundledSound = BundledSound.PIANO, peakVolume = 0.6f),
            motionSensitivity = MotionSensitivity.LOW,
            snoozeMinutes = 7,
            label = "Gym",
            colorIndex = 4,
        )
        assertEquals(alarm, alarm.toEntity().toDomain())
    }

    @Test
    fun `days serialize and parse back to the same set`() {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)
        val alarm = Alarm(hour = 8, minute = 0, daysOfWeek = days)
        assertEquals(days, alarm.toEntity().toDomain().daysOfWeek)
    }

    @Test
    fun `an empty days string maps to an empty set, not a set with a blank day`() {
        assertTrue(entity(daysOfWeek = "").toDomain().daysOfWeek.isEmpty())
    }

    @Test
    fun `an unknown bundled sound name falls back to the default sound`() {
        val domain = entity(bundledSoundName = "NO_LONGER_EXISTS").toDomain()
        assertEquals(BundledSound.BIRDS_LIGHT_RAIN, domain.soundConfig.bundledSound)
    }

    @Test
    fun `a known bundled sound name is preserved`() {
        val domain = entity(bundledSoundName = BundledSound.MUSIC_BOX.name).toDomain()
        assertEquals(BundledSound.MUSIC_BOX, domain.soundConfig.bundledSound)
    }
}
