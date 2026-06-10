package com.wakeiq.presentation.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.MotionSensitivity
import com.wakeiq.domain.model.SoundConfig
import com.wakeiq.domain.model.SoundType
import com.wakeiq.domain.repository.AlarmRepository
import com.wakeiq.domain.usecase.DeleteAlarmUseCase
import com.wakeiq.domain.usecase.SaveAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class EditAlarmUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val hour: Int = 7,
    val minute: Int = 0,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val soundConfig: SoundConfig = SoundConfig(),
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val snoozeMinutes: Int = 9,
    val useSmartWake: Boolean = true,
    val isSaving: Boolean = false,
    val savedOrDeleted: Boolean = false,
)

@HiltViewModel
class EditAlarmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val saveAlarm: SaveAlarmUseCase,
    private val deleteAlarm: DeleteAlarmUseCase,
    private val scheduler: AlarmScheduler,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(EditAlarmUiState())
    val uiState: StateFlow<EditAlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (alarmId == -1L) {
                val sensitivity = prefs.defaultMotionSensitivity.first()
                val snooze = prefs.defaultSnoozeMinutes.first()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        motionSensitivity = sensitivity,
                        snoozeMinutes = snooze,
                    )
                }
            } else {
                val alarm = alarmRepository.getById(alarmId)
                if (alarm != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNew = false,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            daysOfWeek = alarm.daysOfWeek,
                            label = alarm.label,
                            soundConfig = alarm.soundConfig,
                            motionSensitivity = alarm.motionSensitivity,
                            snoozeMinutes = alarm.snoozeMinutes,
                            useSmartWake = alarm.useSmartWake,
                        )
                    }
                }
            }
        }
    }

    fun setTime(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }

    fun toggleDay(day: DayOfWeek) = _uiState.update {
        val days = it.daysOfWeek.toMutableSet()
        if (day in days) days.remove(day) else days.add(day)
        it.copy(daysOfWeek = days)
    }

    fun toggleAllDays() = _uiState.update {
        val allDays = DayOfWeek.entries.toSet()
        it.copy(daysOfWeek = if (it.daysOfWeek.size == 7) emptySet() else allDays)
    }

    fun setLabel(label: String) = _uiState.update { it.copy(label = label) }

    fun setSound(sound: BundledSound) = _uiState.update {
        it.copy(soundConfig = it.soundConfig.copy(type = SoundType.BUNDLED, bundledSound = sound))
    }

    fun setCustomSound(uri: android.net.Uri) = _uiState.update {
        it.copy(soundConfig = it.soundConfig.copy(type = SoundType.CUSTOM, customUri = uri))
    }

    fun setSensitivity(s: MotionSensitivity) = _uiState.update { it.copy(motionSensitivity = s) }

    fun setUseSmartWake(enabled: Boolean) = _uiState.update { it.copy(useSmartWake = enabled) }

    fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val smartWindow = prefs.defaultSmartWindowMinutes.first()
            val ramp = prefs.defaultRampDurationMinutes.first()
            val alarm = Alarm(
                id = if (state.isNew) 0L else alarmId,
                hour = state.hour,
                minute = state.minute,
                daysOfWeek = state.daysOfWeek,
                isEnabled = true,
                soundConfig = state.soundConfig,
                smartWindowMinutes = if (state.useSmartWake) smartWindow else 0,
                rampDurationMinutes = ramp,
                motionSensitivity = state.motionSensitivity,
                snoozeMinutes = state.snoozeMinutes,
                label = state.label,
                useSmartWake = state.useSmartWake,
            )
            val savedId = saveAlarm(alarm)
            scheduler.schedule(alarm.copy(id = savedId))
            _uiState.update { it.copy(isSaving = false, savedOrDeleted = true) }
        }
    }

    fun delete() {
        if (alarmId == -1L) return
        viewModelScope.launch {
            val alarm = alarmRepository.getById(alarmId) ?: return@launch
            scheduler.cancel(alarmId)
            deleteAlarm(alarm)
            _uiState.update { it.copy(savedOrDeleted = true) }
        }
    }
}
