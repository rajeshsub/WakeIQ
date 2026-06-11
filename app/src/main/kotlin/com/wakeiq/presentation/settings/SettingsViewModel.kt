package com.wakeiq.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.MotionSensitivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultSmartWindowMinutes: Int = 20,
    val defaultRampDurationMinutes: Int = 15,
    val defaultMotionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val defaultSnoozeMinutes: Int = 9,
    val blueLightReductionEnabled: Boolean = true,
    val warmHueIndex: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.defaultSmartWindowMinutes,
        prefs.defaultRampDurationMinutes,
        prefs.defaultMotionSensitivity,
        prefs.defaultSnoozeMinutes,
    ) { smartWindow, ramp, sensitivity, snooze ->
        SettingsUiState(
            defaultSmartWindowMinutes = smartWindow,
            defaultRampDurationMinutes = ramp,
            defaultMotionSensitivity = sensitivity,
            defaultSnoozeMinutes = snooze,
        )
    }.combine(prefs.blueLightReductionEnabled) { state, blue ->
        state.copy(blueLightReductionEnabled = blue)
    }.combine(prefs.warmHueIndex) { state, hue ->
        state.copy(warmHueIndex = hue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDefaultSmartWindow(minutes: Int) = viewModelScope.launch {
        prefs.setDefaultSmartWindow(minutes)
    }

    fun setDefaultRampDuration(minutes: Int) = viewModelScope.launch {
        prefs.setDefaultRampDuration(minutes)
    }

    fun setDefaultSensitivity(s: MotionSensitivity) = viewModelScope.launch {
        prefs.setDefaultMotionSensitivity(s)
    }

    fun setBlueLightReduction(enabled: Boolean) = viewModelScope.launch {
        prefs.setBlueLightReduction(enabled)
    }

    fun setWarmHueIndex(index: Int) = viewModelScope.launch {
        prefs.setWarmHueIndex(index)
    }
}
