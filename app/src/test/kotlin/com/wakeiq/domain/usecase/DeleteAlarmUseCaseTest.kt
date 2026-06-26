package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DeleteAlarmUseCaseTest {

    private val repository = mockk<AlarmRepository>(relaxed = true)
    private val useCase = DeleteAlarmUseCase(repository)

    @Test
    fun `delegates to repository delete with the given alarm`() = runTest {
        val alarm = Alarm(id = 9L, hour = 6, minute = 15)
        useCase(alarm)
        coVerify(exactly = 1) { repository.delete(alarm) }
    }
}
