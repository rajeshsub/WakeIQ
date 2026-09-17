package com.wakeiq.domain.model

/**
 * The single place that decides whether moving from one [AlarmRuntimeState] to another is legal.
 *
 * [AlarmForegroundService][com.wakeiq.data.service.AlarmForegroundService] is the only caller: it
 * used to track "have I escalated yet" and "which alarm/phase am I on" via separate fields
 * (a boolean, a nullable [Alarm], a raw phase string). Those fields are collapsed into calls to
 * [next], so every legal move between states is expressed here and nowhere else, and an illegal
 * move throws instead of silently corrupting service-local booleans.
 *
 * `current == null` means "scheduled": no runtime state exists yet for this firing.
 */
object AlarmRuntimeTransition {

    /**
     * Returns the state reached by applying [event] to [current] (null = scheduled/no state yet).
     *
     * @throws IllegalStateException if [event] is not legal from [current].
     */
    fun next(current: AlarmRuntimeState?, event: AlarmRuntimeEvent): AlarmRuntimeState {
        val alarmId = current?.alarmId ?: event.alarmId
        check(current == null || current.alarmId == event.alarmId) {
            "Event for alarm ${event.alarmId} does not match in-flight alarm ${current?.alarmId}"
        }
        return when (event) {
            is AlarmRuntimeEvent.StartMonitoring -> when (current) {
                null -> AlarmRuntimeState.Monitoring(alarmId)
                else -> illegal(current, event)
            }
            is AlarmRuntimeEvent.StartRinging -> when (current) {
                null, is AlarmRuntimeState.Monitoring, is AlarmRuntimeState.Snoozed ->
                    AlarmRuntimeState.Ringing(alarmId)
                else -> illegal(current, event)
            }
            is AlarmRuntimeEvent.Snooze -> when (current) {
                is AlarmRuntimeState.Ringing -> AlarmRuntimeState.Snoozed(alarmId)
                else -> illegal(current, event)
            }
            is AlarmRuntimeEvent.Dismiss -> when (current) {
                is AlarmRuntimeState.Ringing -> AlarmRuntimeState.Dismissed(alarmId)
                else -> illegal(current, event)
            }
            is AlarmRuntimeEvent.Interrupt -> when (current) {
                is AlarmRuntimeState.Monitoring, is AlarmRuntimeState.Ringing ->
                    AlarmRuntimeState.Interrupted(alarmId)
                else -> illegal(current, event)
            }
        }
    }

    private fun illegal(current: AlarmRuntimeState?, event: AlarmRuntimeEvent): Nothing =
        error("Illegal alarm runtime transition: $event from ${current ?: "Scheduled"}")
}

/** Inputs to [AlarmRuntimeTransition.next]; one per action the service can take or receive. */
sealed interface AlarmRuntimeEvent {
    val alarmId: Long

    data class StartMonitoring(override val alarmId: Long) : AlarmRuntimeEvent

    data class StartRinging(override val alarmId: Long) : AlarmRuntimeEvent

    data class Snooze(override val alarmId: Long) : AlarmRuntimeEvent

    data class Dismiss(override val alarmId: Long) : AlarmRuntimeEvent

    data class Interrupt(override val alarmId: Long) : AlarmRuntimeEvent
}
