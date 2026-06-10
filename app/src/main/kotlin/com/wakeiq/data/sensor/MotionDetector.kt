package com.wakeiq.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wakeiq.domain.model.MotionSensitivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class MotionDetector @Inject constructor(@ApplicationContext private val context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _lightSleepDetected = Channel<Unit>(Channel.CONFLATED)
    val lightSleepDetected: Flow<Unit> = _lightSleepDetected.receiveAsFlow()

    private val windowSize = 20
    private val readings = ArrayDeque<Float>(windowSize + 1)

    companion object {
        private const val MIN_DETECTION_READINGS = 10
        private const val SENSOR_SAMPLING_US = 200_000
    }
    private var listener: SensorEventListener? = null
    private var threshold: Float = MotionSensitivity.MEDIUM.threshold

    fun startDetection(sensitivity: MotionSensitivity) {
        threshold = sensitivity.threshold
        readings.clear()
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val mag = sqrt(
                    event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2),
                ) - SensorManager.GRAVITY_EARTH
                readings.addLast(abs(mag))
                if (readings.size > windowSize) readings.removeFirst()

                if (readings.size >= MIN_DETECTION_READINGS && readings.stdDev() > threshold) {
                    Timber.d("Light sleep detected (stdDev=${readings.stdDev()}, threshold=$threshold)")
                    _lightSleepDetected.trySend(Unit)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SENSOR_SAMPLING_US)
        Timber.d("Motion detection started (sensitivity=$sensitivity)")
    }

    fun stopDetection() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        readings.clear()
        Timber.d("Motion detection stopped")
    }

    private fun ArrayDeque<Float>.stdDev(): Float {
        if (size < 2) return 0f
        val mean = sum() / size
        val variance = sumOf { (it - mean).toDouble().pow(2) } / size
        return sqrt(variance).toFloat()
    }
}
