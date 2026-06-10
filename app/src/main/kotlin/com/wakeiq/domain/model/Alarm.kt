package com.wakeiq.domain.model

import java.time.DayOfWeek

data class Alarm(
    val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val isEnabled: Boolean = true,
    val soundConfig: SoundConfig = SoundConfig(),
    val smartWindowMinutes: Int = 20,
    val rampDurationMinutes: Int = 15,
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val snoozeMinutes: Int = 9,
    val label: String = "",
    val useSmartWake: Boolean = true,
) {
    val isRecurring: Boolean get() = daysOfWeek.isNotEmpty()

    val timeString: String get() = "%02d:%02d".format(hour, minute)
}
