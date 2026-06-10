package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import javax.inject.Inject

class DeleteAlarmUseCase @Inject constructor(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) = repository.delete(alarm)
}
