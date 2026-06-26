package com.wakeiq.domain.usecase

import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ToggleAlarmUseCaseTest {

    private val repository = mockk<AlarmRepository>(relaxed = true)
    private val useCase = ToggleAlarmUseCase(repository)

    @Test
    fun `enabling delegates to repository setEnabled`() = runTest {
        useCase(7L, enabled = true)
        coVerify(exactly = 1) { repository.setEnabled(7L, true) }
    }

    @Test
    fun `disabling delegates to repository setEnabled`() = runTest {
        useCase(7L, enabled = false)
        coVerify(exactly = 1) { repository.setEnabled(7L, false) }
    }
}
