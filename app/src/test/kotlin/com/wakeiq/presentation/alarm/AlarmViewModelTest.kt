package com.wakeiq.presentation.alarm

import androidx.lifecycle.SavedStateHandle
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import io.mockk.coEvery
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
class AlarmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<AlarmRepository>(relaxed = true)
    private val prefs = mockk<AppPreferences>(relaxed = true) {
        every { use24HourClock } returns flowOf(true)
    }

    @BeforeEach
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads label and snooze from the stored alarm`() = runTest {
        coEvery { repository.getById(5L) } returns
            Alarm(id = 5L, hour = 7, minute = 0, label = "Work", snoozeMinutes = 12)
        val viewModel = AlarmViewModel(SavedStateHandle(mapOf("extra_alarm_id" to 5L)), repository, prefs)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Work", state.label)
        assertEquals(12, state.snoozeMinutes)
        assertEquals(true, state.is24Hour)
    }

    @Test
    fun `without an alarm id it keeps defaults and does not query the repository`() = runTest {
        val viewModel = AlarmViewModel(SavedStateHandle(), repository, prefs)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.label)
        coVerify(exactly = 0) { repository.getById(any()) }
    }
}
