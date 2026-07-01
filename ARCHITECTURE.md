# Alarm Execution Architecture

## Overview

WakeIQ uses `AlarmManager.setAlarmClock()` for exact alarm delivery and a foreground
service for audio playback. This document covers the scheduling, firing, and recovery
path for both standard and Smart Wake alarms.

## Components

| Component | Role |
|---|---|
| `AlarmScheduler` | Registers/cancels `PendingIntent`s with `AlarmManager` |
| `AlarmSchedulePlanner` | Calculates trigger times for monitor and ring phases |
| `AlarmReceiver` | `BroadcastReceiver` that receives the AlarmManager callback |
| `AlarmForegroundService` | Plays audio, drives the UI, manages WakeLock |
| `RescheduleAlarmsWorker` | Re-registers all enabled alarms after device reboot |

## Scheduling

`AlarmScheduler.schedule()` calls `AlarmManager.setAlarmClock()` for each trigger.
`setAlarmClock()` is chosen over `setExact()` / `setExactAndAllowWhileIdle()` because:

- It is exempt from Doze and app-standby restrictions on all API levels.
- It surfaces the next alarm time in the system clock UI.
- It grants the receiver a background-activity-start exemption used to launch
  `AlarmActivity` directly from the foreground service.

On API 31+ (Android 12), `AlarmManager.canScheduleExactAlarms()` is checked before
scheduling. If the permission has been revoked, the alarm is not registered and a
warning is logged. The app shows a banner prompting the user to re-grant the permission.

## Two-Phase Execution (Smart Wake)

When Smart Wake is enabled, `AlarmSchedulePlanner` registers two separate alarms:

```
[window start]          [target time]
     |                       |
  PHASE_MONITOR ---------- PHASE_RING
  (motion detection)      (hard ring)
```

- **PHASE_MONITOR**: fires at `targetTime - smartWindowMinutes`. The foreground service
  starts in monitoring mode, activating the accelerometer via `MotionDetector`. If light
  sleep is detected, the service immediately transitions to PHASE_RING and cancels the
  pending hard-ring alarm.
- **PHASE_RING**: fires at the configured target time unconditionally. The sibling monitor
  alarm is cancelled if still pending.

Non-smart alarms and snoozes register only a PHASE_RING trigger.

## Firing Path

```
AlarmManager fires PendingIntent
    → AlarmReceiver.onReceive()
        → AlarmForegroundService.start()  (via ContextCompat.startForegroundService)
            → acquireWakeLock()
            → postInitialForeground()     (immediate foreground post to avoid ANR)
            → loadAndStartAlarm()         (async DB load)
                → startMonitoring()       (PHASE_MONITOR)
                  or triggerEscalation()  (PHASE_RING)
```

`postInitialForeground()` posts the foreground notification synchronously before the
async DB load so the service is promoted to foreground before the system's 5-second
window expires.

## Foreground Service Type

On API 29+ (Android 10), the foreground service declares
`FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`. The `setAlarmClock()` callback grants a
temporary exemption allowing this type to start under Doze. `systemExempted` is
deliberately not used: it requires a narrow platform allowlist this app does not qualify
for and throws `SecurityException` at `startForeground()` on devices where the app is
not on that list.

On API 34+ (Android 14), `NotificationManager.canUseFullScreenIntent()` is checked. If
the `USE_FULL_SCREEN_INTENT` permission has been revoked, the service logs a warning and
falls back to the `AlarmActivity` direct-start path (which still works within the
background-activity-start exemption granted by `setAlarmClock()`).

## WakeLock

`PARTIAL_WAKE_LOCK` is acquired when the service starts and released on dismiss, snooze,
or `onDestroy()`. A 60-second timeout cap prevents a stuck service from draining the
battery indefinitely.

## Boot Recovery

`AlarmManager` registrations do not survive a device reboot. A `BootReceiver`
(registered for `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED`) enqueues
`RescheduleAlarmsWorker` via WorkManager. The worker reads all enabled alarms from Room
and re-registers each with `AlarmScheduler`. WorkManager's retry-on-failure guarantee
ensures rescheduling completes even if the first attempt fails during a slow boot.

## SDK Boundaries Summary

| API level | Behaviour change |
|---|---|
| 29 (Android 10) | `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` required |
| 31 (Android 12) | `canScheduleExactAlarms()` permission gate added |
| 34 (Android 14) | `canUseFullScreenIntent()` permission gate added |
