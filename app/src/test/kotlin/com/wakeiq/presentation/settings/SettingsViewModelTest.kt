package com.wakeiq.presentation.settings

import app.cash.turbine.test
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.MotionSensitivity
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefs = mockk<AppPreferences>(relaxed = true)

    @BeforeEach
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState reflects every preference flow`() = runTest {
        every { prefs.defaultSmartWindowMinutes } returns flowOf(25)
        every { prefs.defaultRampDurationMinutes } returns flowOf(12)
        every { prefs.defaultMotionSensitivity } returns flowOf(MotionSensitivity.HIGH)
        every { prefs.defaultSnoozeMinutes } returns flowOf(8)
        every { prefs.blueLightReductionEnabled } returns flowOf(false)
        every { prefs.use24HourClock } returns flowOf(true)

        val viewModel = SettingsViewModel(prefs)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(25, state.defaultSmartWindowMinutes)
            assertEquals(12, state.defaultRampDurationMinutes)
            assertEquals(MotionSensitivity.HIGH, state.defaultMotionSensitivity)
            assertEquals(8, state.defaultSnoozeMinutes)
            assertEquals(false, state.blueLightReductionEnabled)
            assertEquals(true, state.use24HourClock)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setters delegate to preferences`() = runTest {
        val viewModel = SettingsViewModel(prefs)

        viewModel.setDefaultSmartWindow(40)
        viewModel.setDefaultRampDuration(11)
        viewModel.setDefaultSensitivity(MotionSensitivity.LOW)
        viewModel.setBlueLightReduction(false)
        viewModel.setUse24HourClock(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { prefs.setDefaultSmartWindow(40) }
        coVerify(exactly = 1) { prefs.setDefaultRampDuration(11) }
        coVerify(exactly = 1) { prefs.setDefaultMotionSensitivity(MotionSensitivity.LOW) }
        coVerify(exactly = 1) { prefs.setBlueLightReduction(false) }
        coVerify(exactly = 1) { prefs.setUse24HourClock(true) }
    }
}
