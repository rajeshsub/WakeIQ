package com.wakeiq.presentation.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.data.audio.AudioPlayer
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
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
    val isNapDuration: Boolean = false,
    val is24Hour: Boolean = false,
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
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private var previewJob: Job? = null

    // Tracks the smart-wake choice the user actually made, so it can be restored when the alarm time
    // moves back out of nap range. The displayed useSmartWake is forced off while in nap range.
    private var smartWakeUserChoice: Boolean = true

    private val alarmId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(EditAlarmUiState())
    val uiState: StateFlow<EditAlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val is24Hour = prefs.use24HourClock.first()
            if (alarmId == -1L) {
                val sensitivity = prefs.defaultMotionSensitivity.first()
                val snooze = prefs.defaultSnoozeMinutes.first()
                val nap = isNapDuration(_uiState.value.hour, _uiState.value.minute)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        motionSensitivity = sensitivity,
                        snoozeMinutes = snooze,
                        isNapDuration = nap,
                        useSmartWake = !nap && smartWakeUserChoice,
                        is24Hour = is24Hour,
                    )
                }
            } else {
                val alarm = alarmRepository.getById(alarmId)
                if (alarm != null) {
                    smartWakeUserChoice = alarm.useSmartWake
                    val nap = isNapDuration(alarm.hour, alarm.minute)
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
                            isNapDuration = nap,
                            useSmartWake = !nap && alarm.useSmartWake,
                            is24Hour = is24Hour,
                        )
                    }
                }
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        val nap = isNapDuration(hour, minute)
        _uiState.update {
            it.copy(
                hour = hour,
                minute = minute,
                isNapDuration = nap,
                useSmartWake = !nap && smartWakeUserChoice,
            )
        }
    }

    // A nap is any alarm whose next occurrence is under one full sleep cycle (90 min) away. Smart
    // wake assumes a full sleep cycle, so it is disabled for naps. Next occurrence is the selected
    // time today if still ahead, otherwise the same time tomorrow.
    private fun isNapDuration(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val next = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
        return Duration.between(now, next).toMinutes() < NAP_THRESHOLD_MINUTES
    }

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

    fun setSound(sound: BundledSound) {
        _uiState.update { it.copy(soundConfig = it.soundConfig.copy(type = SoundType.BUNDLED, bundledSound = sound)) }
        previewJob?.cancel()
        audioPlayer.playPreview(SoundConfig(type = SoundType.BUNDLED, bundledSound = sound))
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DURATION_MS)
            audioPlayer.release()
        }
    }

    fun setCustomSound(uri: android.net.Uri) = _uiState.update {
        it.copy(soundConfig = it.soundConfig.copy(type = SoundType.CUSTOM, customUri = uri))
    }

    fun setSensitivity(s: MotionSensitivity) = _uiState.update { it.copy(motionSensitivity = s) }

    fun setUseSmartWake(enabled: Boolean) {
        if (_uiState.value.isNapDuration) return
        smartWakeUserChoice = enabled
        _uiState.update { it.copy(useSmartWake = enabled) }
    }

    fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val smartWindow = prefs.defaultSmartWindowMinutes.first()
            val ramp = prefs.defaultRampDurationMinutes.first()
            // Final guard: a nap alarm is never saved with smart wake, regardless of stale state.
            val effectiveSmartWake = state.useSmartWake && !state.isNapDuration
            val alarm = Alarm(
                id = if (state.isNew) 0L else alarmId,
                hour = state.hour,
                minute = state.minute,
                daysOfWeek = state.daysOfWeek,
                isEnabled = true,
                soundConfig = state.soundConfig,
                smartWindowMinutes = if (effectiveSmartWake) smartWindow else 0,
                rampDurationMinutes = ramp,
                motionSensitivity = state.motionSensitivity,
                snoozeMinutes = state.snoozeMinutes,
                label = state.label,
                useSmartWake = effectiveSmartWake,
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

    override fun onCleared() {
        super.onCleared()
        previewJob?.cancel()
        audioPlayer.release()
    }

    companion object {
        private const val PREVIEW_DURATION_MS = 6_000L
        private const val NAP_THRESHOLD_MINUTES = 90
    }
}
