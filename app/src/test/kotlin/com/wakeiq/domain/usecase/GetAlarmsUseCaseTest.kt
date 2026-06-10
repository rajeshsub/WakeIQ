package com.wakeiq.domain.usecase

import app.cash.turbine.test
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAlarmsUseCaseTest {

    private val repository = mockk<AlarmRepository>()
    private val useCase = GetAlarmsUseCase(repository)

    @Test
    fun `emits alarm list from repository`() = runTest {
        val alarm = Alarm(id = 1L, hour = 7, minute = 30)
        every { repository.observeAll() } returns flowOf(listOf(alarm))

        useCase().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(alarm, items.first())
            awaitComplete()
        }
    }

    @Test
    fun `emits empty list when no alarms`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(emptyList<Alarm>(), awaitItem())
            awaitComplete()
        }
    }
}
