package com.wakeiq.domain.usecase

import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class CompleteAlarmUseCaseTest {

    private val repository = mockk<AlarmRepository>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)
    private val useCase = CompleteAlarmUseCase(repository, scheduler)

    @Test
    fun `one-off alarm disables itself in the repository`() = runTest {
        val alarm = Alarm(id = 5L, hour = 6, minute = 30, daysOfWeek = emptySet())

        useCase(alarm)

        coVerify(exactly = 1) { repository.setEnabled(5L, false) }
    }

    @Test
    fun `one-off alarm does not get rescheduled`() = runTest {
        val alarm = Alarm(id = 5L, hour = 6, minute = 30, daysOfWeek = emptySet())

        useCase(alarm)

        verify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `recurring alarm reschedules itself to arm the next occurrence`() = runTest {
        val alarm = Alarm(id = 6L, hour = 6, minute = 30, daysOfWeek = setOf(DayOfWeek.MONDAY))

        useCase(alarm)

        verify(exactly = 1) { scheduler.schedule(alarm) }
    }

    @Test
    fun `recurring alarm stays enabled with no repository write`() = runTest {
        val alarm = Alarm(id = 6L, hour = 6, minute = 30, daysOfWeek = setOf(DayOfWeek.MONDAY))

        useCase(alarm)

        coVerify(exactly = 0) { repository.setEnabled(any(), any()) }
    }

    @Test
    fun `an already-disabled alarm is a no-op`() = runTest {
        val alarm = Alarm(id = 7L, hour = 6, minute = 30, daysOfWeek = emptySet(), isEnabled = false)

        useCase(alarm)

        coVerify(exactly = 0) { repository.setEnabled(any(), any()) }
        verify(exactly = 0) { scheduler.schedule(any()) }
    }
}
