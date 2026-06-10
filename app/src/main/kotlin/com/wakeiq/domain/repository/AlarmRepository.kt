package com.wakeiq.domain.repository

import com.wakeiq.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun observeAll(): Flow<List<Alarm>>
    suspend fun getById(id: Long): Alarm?
    suspend fun save(alarm: Alarm): Long
    suspend fun delete(alarm: Alarm)
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
