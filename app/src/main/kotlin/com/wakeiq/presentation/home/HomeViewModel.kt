package com.wakeiq.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import com.wakeiq.domain.usecase.DeleteAlarmUseCase
import com.wakeiq.domain.usecase.GetAlarmsUseCase
import com.wakeiq.domain.usecase.SaveAlarmUseCase
import com.wakeiq.domain.usecase.ToggleAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val alarms: List<Alarm>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAlarms: GetAlarmsUseCase,
    private val toggleAlarm: ToggleAlarmUseCase,
    private val deleteAlarm: DeleteAlarmUseCase,
    private val saveAlarm: SaveAlarmUseCase,
    private val scheduler: AlarmScheduler,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _openExactAlarmSettings = MutableSharedFlow<Unit>()
    val openExactAlarmSettings: SharedFlow<Unit> = _openExactAlarmSettings.asSharedFlow()

    init {
        viewModelScope.launch { seedDefaultAlarmIfNeeded() }
        viewModelScope.launch {
            getAlarms()
                .catch { e ->
                    Timber.e(e, "Failed to load alarms")
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                .collect { alarms ->
                    _uiState.value = HomeUiState.Success(alarms)
                }
        }
    }

    private suspend fun seedDefaultAlarmIfNeeded() {
        if (prefs.defaultAlarmSeeded.first()) return
        val existing = getAlarms().first()
        if (existing.isEmpty()) {
            val defaultSound = SoundConfig(type = SoundType.BUNDLED, bundledSound = BundledSound.BIRDS_LIGHT_RAIN)
            saveAlarm(
                Alarm(
                    hour = 6,
                    minute = 0,
                    daysOfWeek = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    ),
                    isEnabled = false,
                    soundConfig = defaultSound,
                    label = "Weekdays",
                    useSmartWake = true,
                    colorIndex = 3,
                ),
            )
            saveAlarm(
                Alarm(
                    hour = 7,
                    minute = 0,
                    daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                    isEnabled = false,
                    soundConfig = defaultSound,
                    label = "Weekends",
                    useSmartWake = true,
                    colorIndex = 2,
                ),
            )
        }
        prefs.markDefaultAlarmSeeded()
    }

    fun toggle(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !scheduler.canScheduleExactAlarms()) {
                _openExactAlarmSettings.emit(Unit)
                return@launch
            }
            toggleAlarm(alarm.id, enabled)
            if (enabled) {
                scheduler.schedule(alarm.copy(isEnabled = true))
            } else {
                scheduler.cancel(alarm.id)
            }
        }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            deleteAlarm(alarm)
        }
    }
}
