package com.wakeiq.domain.usecase

import com.wakeiq.domain.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.inject.Inject

class GetNextAlarmTimeUseCase @Inject constructor() {

    private companion object {
        const val DAYS_TO_SEARCH = 8
        const val DAYS_IN_WEEK = 7L
    }
    operator fun invoke(alarm: Alarm, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.isEnabled) return null

        val candidate = from.toLocalDateTime()
            .withHour(alarm.hour)
            .withMinute(alarm.minute)
            .withSecond(0)
            .withNano(0)

        return if (alarm.isRecurring) {
            nextRecurringTime(alarm.daysOfWeek, candidate, from)
        } else {
            val next = if (candidate.isAfter(from.toLocalDateTime())) candidate else candidate.plusDays(1)
            from.withDayOfYear(next.dayOfYear).withYear(next.year)
                .withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
        }
    }

    private fun nextRecurringTime(days: Set<DayOfWeek>, candidate: LocalDateTime, from: ZonedDateTime): ZonedDateTime {
        var check = candidate
        repeat(DAYS_TO_SEARCH) {
            if (check.dayOfWeek in days && check.isAfter(from.toLocalDateTime())) {
                return from.withDayOfYear(check.dayOfYear).withYear(check.year)
                    .withHour(check.hour).withMinute(check.minute).withSecond(0).withNano(0)
            }
            check = check.plusDays(1)
        }
        return from.plusDays(DAYS_IN_WEEK)
    }
}
