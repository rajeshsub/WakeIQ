package com.wakeiq.domain.usecase

import com.wakeiq.data.alarm.AlarmScheduler
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * Runs when an alarm finishes firing (dismissed or escalated), so it either arms its next
 * occurrence or expires. A recurring alarm is rescheduled against the same [Alarm], letting
 * [AlarmScheduler] find the next matching day. A one-off alarm (empty `daysOfWeek`) is disabled
 * instead, since it has no next occurrence. An already-disabled alarm is left untouched.
 */
class CompleteAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) {
    suspend operator fun invoke(alarm: Alarm) {
        if (!alarm.isEnabled) return
        if (alarm.isRecurring) {
            scheduler.schedule(alarm)
        } else {
            repository.setEnabled(alarm.id, false)
        }
    }
}
