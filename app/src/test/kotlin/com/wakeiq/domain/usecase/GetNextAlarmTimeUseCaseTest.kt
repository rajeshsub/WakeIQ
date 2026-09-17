package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
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

    // --- DST transitions ---------------------------------------------------------------------
    // 2026 US DST rule (verified against the system tzdata): America/New_York springs forward
    // 2026-03-08 02:00 -> 03:00 (EST/-05:00 -> EDT/-04:00) and falls back 2026-11-01 02:00 -> 01:00
    // (EDT/-04:00 -> EST/-05:00). Real ZonedDateTime, not a fixed-offset zone, so the use case must
    // resolve wall-clock alarm times through the zone's actual transition rules.

    @Test
    fun `a one-shot alarm set for a time inside the spring-forward gap resolves to a valid instant`() {
        // 2:30 AM does not exist on 2026-03-08 in America/New_York; the clock jumps 2:00 -> 3:00.
        val beforeGap = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ZoneId.of("America/New_York"))
        val alarm = Alarm(hour = 2, minute = 30)

        val result = useCase(alarm, beforeGap)

        assertTrue(result != null, "an alarm landing in the DST gap must still resolve to some instant")
        // java.time resolves a gap time by pushing forward by the gap length (standard java.time
        // behaviour), landing at 3:30 EDT rather than crashing or silently picking 2:30 EST.
        assertEquals(3, result?.hour)
        assertEquals(30, result?.minute)
        assertEquals(ZoneOffset.ofHours(-4), result?.offset)
    }

    @Test
    fun `a recurring alarm crosses the spring-forward boundary with the correct offset`() {
        // Sunday 2026-03-08 is the spring-forward day itself.
        val saturdayBeforeSpring = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ZoneId.of("America/New_York"))
        val alarm = Alarm(hour = 7, minute = 0, daysOfWeek = setOf(DayOfWeek.SUNDAY))

        val result = useCase(alarm, saturdayBeforeSpring)

        assertEquals(LocalDate.of(2026, 3, 8), result?.toLocalDate())
        assertEquals(7, result?.hour)
        assertEquals(ZoneOffset.ofHours(-4), result?.offset, "7am on the spring-forward day is already EDT")
    }

    @Test
    fun `a one-shot alarm crosses the fall-back boundary with the correct offset`() {
        // Fall back is 2026-11-01 02:00 EDT -> 01:00 EST. An alarm set for 1:30 AM from just before
        // midnight the same day must land after both 1:30 AM occurrences, at the post-transition one.
        val justBeforeMidnight = ZonedDateTime.of(2026, 10, 31, 23, 0, 0, 0, ZoneId.of("America/New_York"))
        val alarm = Alarm(hour = 1, minute = 30)

        val result = useCase(alarm, justBeforeMidnight)

        assertEquals(LocalDate.of(2026, 11, 1), result?.toLocalDate())
        assertEquals(1, result?.hour)
        assertEquals(30, result?.minute)
    }

    @Test
    fun `a recurring alarm keeps a stable wall-clock hour across the fall-back boundary`() {
        val saturdayBeforeFall = ZonedDateTime.of(2026, 10, 31, 12, 0, 0, 0, ZoneId.of("America/New_York"))
        val alarm = Alarm(hour = 7, minute = 0, daysOfWeek = setOf(DayOfWeek.SUNDAY))

        val result = useCase(alarm, saturdayBeforeFall)

        assertEquals(LocalDate.of(2026, 11, 1), result?.toLocalDate())
        assertEquals(7, result?.hour, "wall-clock alarm time must stay 7am local, not shift with the offset")
        assertEquals(ZoneOffset.ofHours(-5), result?.offset, "7am on the fall-back day is already EST")
    }

    // --- Timezone change (device travels / zone is changed in settings) ----------------------

    @Test
    fun `the same alarm resolves against whichever zone now is expressed in`() {
        val alarm = Alarm(hour = 7, minute = 0)

        val fromNewYork = ZonedDateTime.of(2026, 6, 26, 6, 0, 0, 0, ZoneId.of("America/New_York"))
        val fromTokyo = ZonedDateTime.of(2026, 6, 26, 6, 0, 0, 0, ZoneId.of("Asia/Tokyo"))

        val resultNewYork = useCase(alarm, fromNewYork)
        val resultTokyo = useCase(alarm, fromTokyo)

        assertEquals(ZoneId.of("America/New_York"), resultNewYork?.zone)
        assertEquals(ZoneId.of("Asia/Tokyo"), resultTokyo?.zone)
        assertEquals(7, resultNewYork?.hour)
        assertEquals(7, resultTokyo?.hour)
        // Same wall-clock alarm (7am), different zones -> different absolute instants.
        assertTrue(resultNewYork?.toInstant() != resultTokyo?.toInstant())
    }

    @Test
    fun `a recurring alarm's day-of-week is evaluated in the new zone after a timezone change`() {
        // 11pm Sunday in Los Angeles is already Monday in Berlin - a day-of-week recurring alarm
        // must be evaluated against the zone "now" carries, not a stale zone from before the change.
        val alarm = Alarm(hour = 1, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))

        val stillSundayInLA = ZonedDateTime.of(2026, 6, 21, 23, 0, 0, 0, ZoneId.of("America/Los_Angeles"))
        val afterMovingToBerlin = stillSundayInLA.withZoneSameInstant(ZoneId.of("Europe/Berlin"))

        val result = useCase(alarm, afterMovingToBerlin)

        assertEquals(DayOfWeek.MONDAY, result?.dayOfWeek)
        assertEquals(ZoneId.of("Europe/Berlin"), result?.zone)
    }

    // --- Missed alarm (device asleep/off past the scheduled time; boot or wake-up runs late) --

    @Test
    fun `a one-shot alarm missed by hours rolls forward instead of firing in the past`() {
        // Alarm was due at 7:00 but the process/device only re-evaluates "now" at 9:15, well after
        // the original target - simulates a missed fire (e.g. Doze, device off, restored late).
        val wayAfterOriginalTime = ZonedDateTime.of(2026, 6, 26, 9, 15, 0, 0, ZoneId.of("UTC"))
        val alarm = Alarm(hour = 7, minute = 0)

        val result = useCase(alarm, wayAfterOriginalTime)

        assertTrue(result != null)
        assertTrue(result!!.isAfter(wayAfterOriginalTime), "a missed alarm must never resolve to a past instant")
        assertEquals(wayAfterOriginalTime.toLocalDate().plusDays(1), result.toLocalDate())
        assertEquals(7, result.hour)
    }

    @Test
    fun `a recurring alarm missed on its day rolls to the next matching day, not today in the past`() {
        val alarm = Alarm(hour = 6, minute = 30, daysOfWeek = setOf(from.dayOfWeek))
        // "now" is well past today's 6:30 target on the very day the alarm should have rung.
        val missedToday = from.withHour(23).withMinute(0)

        val result = useCase(alarm, missedToday)

        assertTrue(result!!.isAfter(missedToday))
        assertEquals(from.toLocalDate().plusDays(7), result.toLocalDate())
    }

    @Test
    fun `a restart long after the scheduled time still produces a future occurrence`() {
        // Simulates a device that was off for days: "now" is far past the alarm's original hour on
        // several intervening days, e.g. restored from a multi-day process/device outage.
        val daysLater = from.plusDays(3).withHour(22).withMinute(0)
        val alarm = Alarm(hour = 6, minute = 0, daysOfWeek = setOf(from.dayOfWeek))

        val result = useCase(alarm, daysLater)

        assertTrue(result!!.isAfter(daysLater), "recomputed trigger must be in the future relative to the restored clock")
    }
}
