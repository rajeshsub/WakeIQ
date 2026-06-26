package com.wakeiq.data.sensor

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Sliding window of acceleration magnitudes that flags light sleep when recent variation (the
 * population standard deviation across the window) exceeds the sensitivity threshold. Pure and free
 * of Android, so the detection decision can be unit-tested independently of the sensor plumbing.
 */
class MotionWindow(
    private val threshold: Float,
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    private val minReadings: Int = DEFAULT_MIN_READINGS,
) {
    private val readings = ArrayDeque<Float>(windowSize + 1)

    /** Adds a magnitude reading and returns true when the window now signals light sleep. */
    fun add(magnitude: Float): Boolean {
        readings.addLast(magnitude)
        if (readings.size > windowSize) readings.removeFirst()
        return readings.size >= minReadings && stdDev() > threshold
    }

    fun clear() = readings.clear()

    private fun stdDev(): Float {
        if (readings.size < 2) return 0f
        val mean = readings.sum() / readings.size
        val variance = readings.sumOf { (it - mean).toDouble().pow(2) } / readings.size
        return sqrt(variance).toFloat()
    }

    companion object {
        const val DEFAULT_WINDOW_SIZE = 20
        const val DEFAULT_MIN_READINGS = 10
    }
}
