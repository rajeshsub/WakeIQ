package com.wakeiq.data.repository

import com.wakeiq.data.db.AlarmDao
import com.wakeiq.data.db.toEntity
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AlarmRepositoryImpl @Inject constructor(private val dao: AlarmDao) : AlarmRepository {

    override fun observeAll(): Flow<List<Alarm>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    override suspend fun save(alarm: Alarm): Long = dao.insert(alarm.toEntity())

    override suspend fun delete(alarm: Alarm) = dao.delete(alarm.toEntity())

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
