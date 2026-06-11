package com.wakeiq.presentation.home

import app.cash.turbine.test
import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.usecase.DeleteAlarmUseCase
import com.wakeiq.domain.usecase.GetAlarmsUseCase
import com.wakeiq.domain.usecase.SaveAlarmUseCase
import com.wakeiq.domain.usecase.ToggleAlarmUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAlarms = mockk<GetAlarmsUseCase>()
    private val toggleAlarm = mockk<ToggleAlarmUseCase>(relaxed = true)
    private val deleteAlarm = mockk<DeleteAlarmUseCase>(relaxed = true)
    private val saveAlarm = mockk<SaveAlarmUseCase>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)
    private val prefs = mockk<AppPreferences>(relaxed = true) {
        every { defaultAlarmSeeded } returns flowOf(true)
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val alarm = Alarm(id = 1L, hour = 7, minute = 0)
        every { getAlarms() } returns flowOf(listOf(alarm))

        val viewModel = HomeViewModel(getAlarms, toggleAlarm, deleteAlarm, saveAlarm, scheduler, prefs)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as HomeUiState.Success
            assertEquals(1, success.alarms.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle calls scheduler schedule when enabling`() = runTest {
        val alarm = Alarm(id = 1L, hour = 7, minute = 0, isEnabled = false)
        every { getAlarms() } returns flowOf(emptyList())
        every { scheduler.canScheduleExactAlarms() } returns true

        val viewModel = HomeViewModel(getAlarms, toggleAlarm, deleteAlarm, saveAlarm, scheduler, prefs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggle(alarm, true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { scheduler.schedule(any()) }
    }

    @Test
    fun `toggle emits openExactAlarmSettings when permission denied`() = runTest {
        val alarm = Alarm(id = 3L, hour = 7, minute = 0, isEnabled = false)
        every { getAlarms() } returns flowOf(emptyList())
        every { scheduler.canScheduleExactAlarms() } returns false

        val viewModel = HomeViewModel(getAlarms, toggleAlarm, deleteAlarm, saveAlarm, scheduler, prefs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openExactAlarmSettings.test {
            viewModel.toggle(alarm, true)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `toggle calls scheduler cancel when disabling`() = runTest {
        val alarm = Alarm(id = 2L, hour = 8, minute = 30, isEnabled = true)
        every { getAlarms() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(getAlarms, toggleAlarm, deleteAlarm, saveAlarm, scheduler, prefs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggle(alarm, false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { scheduler.cancel(2L) }
    }
}
