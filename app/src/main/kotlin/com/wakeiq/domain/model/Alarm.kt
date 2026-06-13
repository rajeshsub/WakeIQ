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
    val rampDurationMinutes: Int = 5,
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val snoozeMinutes: Int = 9,
    val label: String = "",
    val useSmartWake: Boolean = true,
) {
    val isRecurring: Boolean get() = daysOfWeek.isNotEmpty()

    val timeString: String get() = "%02d:%02d".format(hour, minute)

    fun formattedTime(is24Hour: Boolean): String = if (is24Hour) {
        timeString
    } else {
        val h12 = when {
            hour == 0 -> HOURS_IN_HALF_DAY
            hour > HOURS_IN_HALF_DAY -> hour - HOURS_IN_HALF_DAY
            else -> hour
        }
        val amPm = if (hour < HOURS_IN_HALF_DAY) "AM" else "PM"
        "%d:%02d %s".format(h12, minute, amPm)
    }

    private companion object {
        const val HOURS_IN_HALF_DAY = 12
    }
}
