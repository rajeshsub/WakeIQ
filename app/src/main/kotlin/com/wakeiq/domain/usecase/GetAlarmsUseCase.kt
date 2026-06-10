package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlarmsUseCase @Inject constructor(private val repository: AlarmRepository) {
    operator fun invoke(): Flow<List<Alarm>> = repository.observeAll()
}
