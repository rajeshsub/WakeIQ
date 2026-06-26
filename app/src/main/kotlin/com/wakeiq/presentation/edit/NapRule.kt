package com.wakeiq.presentation.edit

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Pure nap-detection rule, isolated from the clock so it can be unit-tested.
 *
 * A nap is any alarm whose next occurrence is under one full sleep cycle ([NAP_THRESHOLD_MINUTES])
 * away. Smart Wake assumes a full sleep cycle, so it is disabled for naps. The next occurrence is the
 * selected time today if it is still ahead, otherwise the same time tomorrow.
 */
object NapRule {
    const val NAP_THRESHOLD_MINUTES = 90L

    fun isNap(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val next = if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
        return Duration.between(now, next).toMinutes() < NAP_THRESHOLD_MINUTES
    }
}
