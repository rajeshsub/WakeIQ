package com.wakeiq.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wakeiq.domain.model.MotionSensitivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val store = context.dataStore

    val defaultSmartWindowMinutes: Flow<Int> =
        store.data.map { it[KEY_SMART_WINDOW] ?: DEFAULT_SMART_WINDOW_MINUTES }

    val defaultRampDurationMinutes: Flow<Int> =
        store.data.map { it[KEY_RAMP_DURATION] ?: DEFAULT_RAMP_DURATION_MINUTES }

    val defaultMotionSensitivity: Flow<MotionSensitivity> =
        store.data.map {
            MotionSensitivity.entries.getOrElse(it[KEY_MOTION_SENSITIVITY] ?: 1) {
                MotionSensitivity.MEDIUM
            }
        }

    val defaultSnoozeMinutes: Flow<Int> =
        store.data.map { it[KEY_SNOOZE_DURATION] ?: DEFAULT_SNOOZE_MINUTES }

    val blueLightReductionEnabled: Flow<Boolean> =
        store.data.map { it[KEY_BLUE_LIGHT_REDUCTION] ?: true }

    val warmHueIndex: Flow<Int> =
        store.data.map { it[KEY_WARM_HUE_INDEX] ?: 0 }

    val defaultAlarmSeeded: Flow<Boolean> =
        store.data.map { it[KEY_DEFAULT_ALARM_SEEDED] ?: false }

    suspend fun setDefaultSmartWindow(minutes: Int) {
        store.edit { it[KEY_SMART_WINDOW] = minutes }
    }

    suspend fun setDefaultRampDuration(minutes: Int) {
        store.edit { it[KEY_RAMP_DURATION] = minutes }
    }

    suspend fun setDefaultMotionSensitivity(sensitivity: MotionSensitivity) {
        store.edit { it[KEY_MOTION_SENSITIVITY] = sensitivity.ordinal }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        store.edit { it[KEY_SNOOZE_DURATION] = minutes }
    }

    suspend fun setBlueLightReduction(enabled: Boolean) {
        store.edit { it[KEY_BLUE_LIGHT_REDUCTION] = enabled }
    }

    suspend fun setWarmHueIndex(index: Int) {
        store.edit { it[KEY_WARM_HUE_INDEX] = index }
    }

    suspend fun markDefaultAlarmSeeded() {
        store.edit { it[KEY_DEFAULT_ALARM_SEEDED] = true }
    }

    private companion object {
        const val DEFAULT_SMART_WINDOW_MINUTES = 20
        const val DEFAULT_RAMP_DURATION_MINUTES = 15
        const val DEFAULT_SNOOZE_MINUTES = 9

        val KEY_SMART_WINDOW = intPreferencesKey("smart_window_minutes")
        val KEY_RAMP_DURATION = intPreferencesKey("ramp_duration_minutes")
        val KEY_MOTION_SENSITIVITY = intPreferencesKey("motion_sensitivity")
        val KEY_SNOOZE_DURATION = intPreferencesKey("snooze_duration_minutes")
        val KEY_BLUE_LIGHT_REDUCTION = booleanPreferencesKey("blue_light_reduction_enabled")
        val KEY_WARM_HUE_INDEX = intPreferencesKey("warm_hue_index")
        val KEY_DEFAULT_ALARM_SEEDED = booleanPreferencesKey("default_alarm_seeded")
    }
}
