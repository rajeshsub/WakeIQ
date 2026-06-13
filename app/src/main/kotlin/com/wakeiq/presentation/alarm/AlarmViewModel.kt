package com.wakeiq.presentation.alarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(val label: String = "", val snoozeMinutes: Int = 9, val is24Hour: Boolean = false)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>("extra_alarm_id") ?: -1L

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val is24Hour = prefs.use24HourClock.first()
            _uiState.update { it.copy(is24Hour = is24Hour) }

            if (alarmId != -1L) {
                alarmRepository.getById(alarmId)?.let { alarm ->
                    _uiState.update { it.copy(label = alarm.label, snoozeMinutes = alarm.snoozeMinutes) }
                }
            }
        }
    }
}
