package com.wakeiq.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.MotionSensitivity
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String,
    val isEnabled: Boolean,
    val soundType: String,
    val bundledSoundName: String,
    val customUri: String?,
    val peakVolume: Float,
    val smartWindowMinutes: Int,
    val rampDurationMinutes: Int,
    val motionSensitivity: String,
    val snoozeMinutes: Int,
    val label: String,
    val useSmartWake: Boolean = true,
) {
    fun toDomain(): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek.split(",")
            .filter { it.isNotEmpty() }
            .map { DayOfWeek.valueOf(it) }
            .toSet(),
        isEnabled = isEnabled,
        soundConfig = SoundConfig(
            type = SoundType.valueOf(soundType),
            bundledSound = BundledSound.valueOf(bundledSoundName),
            customUri = customUri?.let { android.net.Uri.parse(it) },
            peakVolume = peakVolume,
        ),
        smartWindowMinutes = smartWindowMinutes,
        rampDurationMinutes = rampDurationMinutes,
        motionSensitivity = MotionSensitivity.valueOf(motionSensitivity),
        snoozeMinutes = snoozeMinutes,
        label = label,
        useSmartWake = useSmartWake,
    )
}

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    daysOfWeek = daysOfWeek.joinToString(",") { it.name },
    isEnabled = isEnabled,
    soundType = soundConfig.type.name,
    bundledSoundName = soundConfig.bundledSound.name,
    customUri = soundConfig.customUri?.toString(),
    peakVolume = soundConfig.peakVolume,
    smartWindowMinutes = smartWindowMinutes,
    rampDurationMinutes = rampDurationMinutes,
    motionSensitivity = motionSensitivity.name,
    snoozeMinutes = snoozeMinutes,
    label = label,
    useSmartWake = useSmartWake,
)
