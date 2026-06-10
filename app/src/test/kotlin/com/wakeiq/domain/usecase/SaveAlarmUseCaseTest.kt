package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaveAlarmUseCaseTest {

    private val repository = mockk<AlarmRepository>()
    private val useCase = SaveAlarmUseCase(repository)

    @Test
    fun `returns id from repository`() = runTest {
        val alarm = Alarm(hour = 7, minute = 0)
        coEvery { repository.save(alarm) } returns 42L

        val result = useCase(alarm)

        assertEquals(42L, result)
        coVerify(exactly = 1) { repository.save(alarm) }
    }
}
