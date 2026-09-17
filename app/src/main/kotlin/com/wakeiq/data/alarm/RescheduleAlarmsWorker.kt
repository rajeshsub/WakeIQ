package com.wakeiq.data.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wakeiq.domain.model.Alarm
import com.wakeiq.domain.repository.AlarmRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Reads all enabled alarms from Room and re-registers them with AlarmManager after a device reboot.
 *
 * Runs as a Hilt-injected CoroutineWorker so that:
 *   - Repository dependencies are injected without a BroadcastReceiver lifecycle
 *   - WorkManager retries on failure and survives process death
 *   - No foreground service is ever started from this context (FGS starts only from AlarmReceiver)
 */
@HiltWorker
class RescheduleAlarmsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) : CoroutineWorker(context, params) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        val alarms = alarmRepository.observeAll().first()
        val enabled = alarmsToReschedule(alarms)
        enabled.forEach { alarm ->
            alarmScheduler.schedule(alarm)
            Timber.d("Rescheduled alarm ${alarm.id} after boot")
        }
        Timber.i("Boot reschedule complete: ${enabled.size} alarm(s) registered")
        Result.success()
    } catch (e: Exception) {
        // Broad catch is intentional: any unhandled exception at this WorkManager boundary would
        // be silently discarded by the framework with no retry. Logging + Result.retry() ensures
        // failures surface and alarm rescheduling is guaranteed to complete eventually.
        Timber.e(e, "Boot reschedule failed, will retry")
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "reschedule_alarms_on_boot"

        // Pure selection extracted out of doWork so the restart/restoration decision is
        // unit-testable without constructing a real CoroutineWorker (see ADR 0002: this worker is
        // a thin WorkManager adapter, and framework-object mocking is deliberately avoided here).
        internal fun alarmsToReschedule(alarms: List<Alarm>): List<Alarm> = alarms.filter { it.isEnabled }
    }
}
