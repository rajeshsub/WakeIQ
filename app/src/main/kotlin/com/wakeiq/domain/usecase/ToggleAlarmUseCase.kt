package com.wakeiq.domain.usecase

import com.wakeiq.domain.repository.AlarmRepository
import javax.inject.Inject

class ToggleAlarmUseCase @Inject constructor(private val repository: AlarmRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean) = repository.setEnabled(id, enabled)
}
