package com.wakeiq.domain.model

/**
 * The runtime state of one in-flight alarm firing, i.e. what [com.wakeiq.data.service.AlarmForegroundService]
 * is currently doing for a given alarm id, as distinct from the alarm's persisted configuration
 * ([Alarm]/[com.wakeiq.data.db.AlarmEntity], which never records this).
 *
 * There is no `Scheduled` variant here: "scheduled" is the absence of a runtime state (no service
 * instance is tracking the alarm), so it is represented by there being no [AlarmRuntimeState]
 * rather than by a dedicated object. [AlarmRuntimeTransition] treats "no current state" as the
 * implicit starting point.
 */
sealed interface AlarmRuntimeState {
    val alarmId: Long

    /** Smart Wake motion-detection window is open; audio has not started. */
    data class Monitoring(override val alarmId: Long) : AlarmRuntimeState

    /** Audio is escalating/playing and the full-screen alarm UI is up. */
    data class Ringing(override val alarmId: Long) : AlarmRuntimeState

    /** User snoozed; a new ring trigger is scheduled for later. Terminal for this firing. */
    data class Snoozed(override val alarmId: Long) : AlarmRuntimeState

    /** User dismissed. Terminal for this firing. */
    data class Dismissed(override val alarmId: Long) : AlarmRuntimeState

    /** The firing ended without an explicit dismiss or snooze (e.g. service destroyed early). */
    data class Interrupted(override val alarmId: Long) : AlarmRuntimeState
}
