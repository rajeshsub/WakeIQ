package com.wakeiq.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wakeiq.core.InstrumentedOnly
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
@InstrumentedOnly
class MotionDetector @Inject constructor(@ApplicationContext private val context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _lightSleepDetected = Channel<Unit>(Channel.CONFLATED)
    val lightSleepDetected: Flow<Unit> = _lightSleepDetected.receiveAsFlow()

    private companion object {
        const val SENSOR_SAMPLING_US = 200_000
    }
    private var listener: SensorEventListener? = null
    private var window = MotionWindow(MotionSensitivity.MEDIUM.threshold)

    fun startDetection(sensitivity: MotionSensitivity) {
        window = MotionWindow(sensitivity.threshold)
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val mag = sqrt(
                    event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2),
                ) - SensorManager.GRAVITY_EARTH
                if (window.add(abs(mag))) {
                    Timber.d("Light sleep detected (sensitivity=$sensitivity)")
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
        window.clear()
        Timber.d("Motion detection stopped")
    }
}
