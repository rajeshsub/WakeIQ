package com.wakeiq.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {

    private val _isBlueLightActive = MutableStateFlow(false)
    val isBlueLightActive: StateFlow<Boolean> = _isBlueLightActive.asStateFlow()

    val warmHueIndex: StateFlow<Int> = prefs.warmHueIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val use24HourClock: StateFlow<Boolean> = prefs.use24HourClock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            prefs.blueLightReductionEnabled.collect { refreshBlueLightState() }
        }
        viewModelScope.launch {
            while (true) {
                refreshBlueLightState()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshBlueLightState() {
        val enabled = prefs.blueLightReductionEnabled.first()
        val hour = LocalTime.now().hour
        _isBlueLightActive.value = enabled && (hour >= BLUE_LIGHT_ON_HOUR || hour < BLUE_LIGHT_OFF_HOUR)
    }

    private companion object {
        const val BLUE_LIGHT_ON_HOUR = 18
        const val BLUE_LIGHT_OFF_HOUR = 6
        const val POLL_INTERVAL_MS = 5 * 60 * 1000L
    }
}
