package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GetNextAlarmTimeUseCaseTest {

    private val useCase = GetNextAlarmTimeUseCase()

    // A fixed reference instant so every recurring branch is deterministic.
    private val from: ZonedDateTime = ZonedDateTime.of(2026, 6, 26, 12, 0, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `a disabled alarm has no next time`() {
        assertNull(useCase(Alarm(hour = 7, minute = 0, isEnabled = false), from))
    }

    @Test
    fun `a one-shot later today fires today`() {
        val result = useCase(Alarm(hour = 14, minute = 0), from)
        assertEquals(from.toLocalDate(), result?.toLocalDate())
        assertEquals(14, result?.hour)
    }

    @Test
    fun `a one-shot whose time has passed rolls to tomorrow`() {
        val result = useCase(Alarm(hour = 9, minute = 0), from)
        assertEquals(from.toLocalDate().plusDays(1), result?.toLocalDate())
    }

    @Test
    fun `a recurring alarm on today with time ahead fires today`() {
        val alarm = Alarm(hour = 14, minute = 0, daysOfWeek = setOf(from.dayOfWeek))
        val result = useCase(alarm, from)
        assertEquals(from.toLocalDate(), result?.toLocalDate())
        assertEquals(14, result?.hour)
    }

    @Test
    fun `a recurring alarm on today whose time has passed rolls to the same day next week`() {
        val alarm = Alarm(hour = 9, minute = 0, daysOfWeek = setOf(from.dayOfWeek))
        val result = useCase(alarm, from)
        assertEquals(from.toLocalDate().plusDays(7), result?.toLocalDate())
        assertEquals(9, result?.hour)
    }

    @Test
    fun `a recurring alarm skips non-matching days to the next matching day`() {
        val alarm = Alarm(hour = 8, minute = 0, daysOfWeek = setOf(from.plusDays(2).dayOfWeek))
        val result = useCase(alarm, from)
        assertEquals(from.toLocalDate().plusDays(2), result?.toLocalDate())
    }

    @Test
    fun `a recurring alarm with several days picks the nearest upcoming one`() {
        val alarm = Alarm(
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(from.plusDays(1).dayOfWeek, from.plusDays(4).dayOfWeek),
        )
        val result = useCase(alarm, from)
        assertEquals(from.toLocalDate().plusDays(1), result?.toLocalDate())
    }
}
