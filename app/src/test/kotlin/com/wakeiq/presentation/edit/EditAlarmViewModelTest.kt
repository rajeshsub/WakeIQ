package com.wakeiq.presentation.edit

import androidx.lifecycle.SavedStateHandle
import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.data.audio.AudioPlayer
import com.wakeiq.data.preferences.AppPreferences
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.model.BundledSound
import com.wakeiq.domain.model.MotionSensitivity
import com.wakeiq.domain.model.SoundType
import com.wakeiq.domain.repository.AlarmRepository
import com.wakeiq.domain.usecase.DeleteAlarmUseCase
import com.wakeiq.domain.usecase.SaveAlarmUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EditAlarmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<AlarmRepository>(relaxed = true)
    private val saveAlarm = mockk<SaveAlarmUseCase>()
    private val deleteAlarm = mockk<DeleteAlarmUseCase>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)
    private val audioPlayer = mockk<AudioPlayer>(relaxed = true)
    private val prefs = mockk<AppPreferences>(relaxed = true) {
        every { use24HourClock } returns flowOf(false)
        every { defaultMotionSensitivity } returns flowOf(MotionSensitivity.MEDIUM)
        every { defaultSnoozeMinutes } returns flowOf(9)
        every { defaultSmartWindowMinutes } returns flowOf(20)
        every { defaultRampDurationMinutes } returns flowOf(15)
    }

    @BeforeEach
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(id: Long? = null): EditAlarmViewModel {
        val handle = if (id == null) SavedStateHandle() else SavedStateHandle(mapOf("id" to id))
        return EditAlarmViewModel(handle, repository, saveAlarm, deleteAlarm, scheduler, prefs, audioPlayer)
    }

    @Test
    fun `a new alarm finishes loading in Once mode with prefs applied`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertFalse(state.isRepeatMode, "a new alarm must default to Once mode")
        assertTrue(state.daysOfWeek.isEmpty())
        assertEquals(9, state.snoozeMinutes)
    }

    @Test
    fun `switching from Once to Repeat mode pre-selects today`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setRepeatMode(true)

        val state = viewModel.uiState.value
        assertTrue(state.isRepeatMode)
        assertEquals(setOf(LocalDate.now().dayOfWeek), state.daysOfWeek)
    }

    @Test
    fun `switching from Repeat to Once clears days`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setRepeatMode(true)
        viewModel.setRepeatMode(false)

        val state = viewModel.uiState.value
        assertFalse(state.isRepeatMode)
        assertTrue(state.daysOfWeek.isEmpty())
    }

    @Test
    fun `saving in Once mode persists an empty daysOfWeek`() = runTest {
        val saved = slot<Alarm>()
        coEvery { saveAlarm(capture(saved)) } returns 200L
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved.captured.daysOfWeek.isEmpty())
    }

    @Test
    fun `editing an existing recurring alarm surfaces as Repeat mode`() = runTest {
        val stored = Alarm(id = 12L, hour = 6, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        coEvery { repository.getById(12L) } returns stored
        val viewModel = newViewModel(id = 12L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isRepeatMode)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), state.daysOfWeek)
    }

    @Test
    fun `editing an existing one-off alarm surfaces as Once mode`() = runTest {
        val stored = Alarm(id = 13L, hour = 6, minute = 0, daysOfWeek = emptySet())
        coEvery { repository.getById(13L) } returns stored
        val viewModel = newViewModel(id = 13L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRepeatMode)
        assertTrue(state.daysOfWeek.isEmpty())
    }

    @Test
    fun `nap rule still forces smart wake off while in Once mode`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRepeatMode, "new alarms start in Once mode")

        val soon = ZonedDateTime.now().plusMinutes(30)
        viewModel.setTime(soon.hour, soon.minute)
        assumeTrue(viewModel.uiState.value.isNapDuration, "skip near midnight when 30 min ahead is not a nap")

        assertFalse(viewModel.uiState.value.useSmartWake, "a nap cannot use smart wake, even in Once mode")
        viewModel.setUseSmartWake(true)
        assertFalse(viewModel.uiState.value.useSmartWake, "a nap cannot enable smart wake, even in Once mode")
    }

    @Test
    fun `toggleDay adds then removes a day`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now().dayOfWeek
        val day = DayOfWeek.entries.first { it != today }
        viewModel.toggleDay(day)
        assertTrue(day in viewModel.uiState.value.daysOfWeek)
        viewModel.toggleDay(day)
        assertFalse(day in viewModel.uiState.value.daysOfWeek)
    }

    @Test
    fun `toggleAllDays selects all seven then clears`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleAllDays()
        assertEquals(7, viewModel.uiState.value.daysOfWeek.size)
        viewModel.toggleAllDays()
        assertTrue(viewModel.uiState.value.daysOfWeek.isEmpty())
    }

    @Test
    fun `setSound updates the config and starts a preview`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSound(BundledSound.PIANO)

        val config = viewModel.uiState.value.soundConfig
        assertEquals(SoundType.BUNDLED, config.type)
        assertEquals(BundledSound.PIANO, config.bundledSound)
        verify { audioPlayer.playPreview(any()) }
    }

    @Test
    fun `saving applies the global motion sensitivity, ignoring the stored per-alarm value`() = runTest {
        every { prefs.defaultMotionSensitivity } returns flowOf(MotionSensitivity.LOW)
        val stored = Alarm(id = 9L, hour = 8, minute = 15, motionSensitivity = MotionSensitivity.HIGH)
        coEvery { repository.getById(9L) } returns stored
        val saved = slot<Alarm>()
        coEvery { saveAlarm(capture(saved)) } returns 9L
        val viewModel = newViewModel(id = 9L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MotionSensitivity.LOW, saved.captured.motionSensitivity)
    }

    @Test
    fun `saving a non-nap alarm keeps smart wake and the configured ramp, then schedules`() = runTest {
        val saved = slot<Alarm>()
        coEvery { saveAlarm(capture(saved)) } returns 100L
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val future = ZonedDateTime.now().plusHours(4)
        viewModel.setTime(future.hour, future.minute)
        assertFalse(viewModel.uiState.value.isNapDuration, "four hours ahead must not be a nap")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved.captured.useSmartWake)
        assertEquals(15, saved.captured.rampDurationMinutes)
        assertEquals(20, saved.captured.smartWindowMinutes)
        coVerify(exactly = 1) { scheduler.schedule(any()) }
        assertTrue(viewModel.uiState.value.savedOrDeleted)
    }

    @Test
    fun `saving a nap alarm forces smart wake off and no ramp`() = runTest {
        val saved = slot<Alarm>()
        coEvery { saveAlarm(capture(saved)) } returns 101L
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val soon = ZonedDateTime.now().plusMinutes(30)
        viewModel.setTime(soon.hour, soon.minute)
        assumeTrue(viewModel.uiState.value.isNapDuration, "skip near midnight when 30 min ahead is not a nap")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(saved.captured.useSmartWake)
        assertEquals(0, saved.captured.rampDurationMinutes)
        assertEquals(0, saved.captured.smartWindowMinutes)
    }

    @Test
    fun `setUseSmartWake is ignored while the alarm is a nap`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val soon = ZonedDateTime.now().plusMinutes(30)
        viewModel.setTime(soon.hour, soon.minute)
        assumeTrue(viewModel.uiState.value.isNapDuration, "skip near midnight when 30 min ahead is not a nap")

        viewModel.setUseSmartWake(true)
        assertFalse(viewModel.uiState.value.useSmartWake, "a nap cannot enable smart wake")
    }

    @Test
    fun `editing an existing alarm loads its values into state`() = runTest {
        val stored = Alarm(
            id = 7L,
            hour = 8,
            minute = 15,
            label = "Standup",
            snoozeMinutes = 11,
            colorIndex = 4,
        )
        coEvery { repository.getById(7L) } returns stored
        val viewModel = newViewModel(id = 7L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isNew)
        assertEquals(8, state.hour)
        assertEquals(15, state.minute)
        assertEquals("Standup", state.label)
        assertEquals(11, state.snoozeMinutes)
        assertEquals(4, state.colorIndex)
    }

    @Test
    fun `setUseSmartWake toggles the choice for a non-nap alarm`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val future = ZonedDateTime.now().plusHours(4)
        viewModel.setTime(future.hour, future.minute)
        assertFalse(viewModel.uiState.value.isNapDuration, "four hours ahead must not be a nap")

        viewModel.setUseSmartWake(false)
        assertFalse(viewModel.uiState.value.useSmartWake)
        viewModel.setUseSmartWake(true)
        assertTrue(viewModel.uiState.value.useSmartWake)
    }

    @Test
    fun `deleting a brand-new alarm does nothing`() = runTest {
        val viewModel = newViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.delete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { scheduler.cancel(any()) }
        coVerify(exactly = 0) { deleteAlarm(any()) }
    }

    @Test
    fun `delete cancels the schedule and removes the stored alarm`() = runTest {
        val alarm = Alarm(id = 3L, hour = 7, minute = 0, label = "Work")
        coEvery { repository.getById(3L) } returns alarm
        val viewModel = newViewModel(id = 3L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.delete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { scheduler.cancel(3L) }
        coVerify(exactly = 1) { deleteAlarm(alarm) }
        assertTrue(viewModel.uiState.value.savedOrDeleted)
    }
}
