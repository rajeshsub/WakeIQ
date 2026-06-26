package com.wakeiq.presentation

/**
 * Pure rule for whether blue-light reduction should be active, isolated from the clock so it can be
 * unit-tested. Active only when enabled and within the evening/night band ([ON_HOUR]..23, 0..[OFF_HOUR]).
 */
object BlueLight {
    const val ON_HOUR = 18
    const val OFF_HOUR = 6

    fun isActive(enabled: Boolean, hour: Int): Boolean = enabled && (hour >= ON_HOUR || hour < OFF_HOUR)
}
