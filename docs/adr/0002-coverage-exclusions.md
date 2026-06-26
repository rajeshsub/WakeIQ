# 2. Exclude thin Android adapters from logic coverage via @InstrumentedOnly

Date: 2026-06-26

## Status

Accepted

## Context

ADR-0001 introduced a measure-only Kover "logic" report meant to reflect coverage
of meaningful logic. Triage of the first real numbers showed the logic report was
dominated by classes that wrap an Android framework API (AlarmManager, ExoPlayer,
SensorManager, DataStore, Room, the permission/notification managers). Such code
cannot be exercised by JVM unit tests without mocking the framework, which the
project deliberately avoids: the established house pattern is to extract the pure
decision into a separate unit and unit-test that, leaving a thin adapter.

Two of these adapters already followed the pattern (`computeRampPhases`,
`planTriggers` lived as pure internal functions with tests). The rest mixed pure
logic with framework calls in one class, so the logic could not be counted without
also counting untestable plumbing.

## Decision

Introduce a `@InstrumentedOnly` annotation (`com.wakeiq.core.InstrumentedOnly`) and
exclude annotated classes and functions from the Kover logic report via
`annotatedBy`. Apply it only to thin adapters whose sole meaningful behaviour is
the framework call.

Before annotating, extract the pure logic each adapter contained into its own
tested unit, so coverage of that logic is kept, not lost:

- `AudioPlayer` -> `VolumeRamp` (ramp phase split, volume step curve)
- `AlarmScheduler` -> `AlarmSchedulePlanner` (trigger planning, request codes)
- `MotionDetector` -> `MotionWindow` (sliding-window standard-deviation decision)
- `AppStateViewModel` -> `BlueLight` (evening/night active rule)
- `EditAlarmViewModel` -> `NapRule` (under-90-minute nap detection, clock injected)

Adapters annotated `@InstrumentedOnly` after extraction:
`AudioPlayer`, `AlarmScheduler`, `MotionDetector`, `AppStateViewModel`,
`PermissionsViewModel` (and `areCriticalPermissionsGranted`), `AlarmRepositoryImpl`
(pure DAO passthrough), `AppPreferences` (DataStore passthrough). The file-level
`AppPreferencesKt` facade (the `dataStore` property delegate) is pure wiring and is
excluded by name in the Kover filter, since an annotation cannot target it.

## Consequences

- The logic report now measures logic that JVM tests can actually reach. The number
  reflects test effort, not the unavoidable Android surface area.
- Excluding code from coverage does not lower its quality bar: ktlint, detekt and
  Android Lint still apply, and instrumented tests (`AlarmFlowTest`) exercise the
  real flows through these adapters.
- The annotation is a deliberate, auditable marker. Adding it to a class that still
  holds real logic would be a misuse caught in review; the rule is "extract the
  logic first, then annotate what remains".
- `PermissionsViewModel` builds its list purely from Android system services and
  `Build.VERSION`, so it is treated as an adapter rather than tested via reflection
  mocking, which would be fragile and against house style.
