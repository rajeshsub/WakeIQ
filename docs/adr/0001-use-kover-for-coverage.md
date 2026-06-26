# 1. Use Kover to measure coverage (measure-only, split logic vs glue)

Date: 2026-06-26

## Status

Accepted

## Context

Our engineering standard requires "80% coverage of meaningful logic" and says not
to chase coverage on trivial getters or generated code. The project already has
unit tests (use cases, scheduler, motion detector, audio ramp, view models) and an
instrumented flow test, plus CI running ktlint, detekt, Android Lint, unit tests
and an emulator suite. What was missing was a way to measure coverage at all: with
no tool, the "80% of meaningful logic" claim is unverifiable.

Two tool choices exist for the JVM: JaCoCo and Kover. JaCoCo predates Kotlin and
mishandles inline functions and coroutine state machines, and reports noise on
synthetic code. Kover is the JetBrains-official, Kotlin-aware coverage engine and
is the idiomatic, right-sized choice for a Kotlin/Compose codebase.

A coverage gate (failing the build below a threshold) was considered and rejected
for now. This app is largely Android-framework glue (Compose screens, Activities,
receivers, a foreground service, Room DAOs, Hilt modules). A global hard gate would
force chasing coverage on exactly the code the standard says to skip, and invites
Goodhart's law: the number becomes the target and low-value tests get written to
hit it. We want honest visibility first.

## Decision

Add the Kover Gradle plugin (0.9.8), scoped to the `full` flavor to match the
`testFullDebugUnitTest` run that CI already exercises. Configure measure-only
reporting with no build-failing verification rule.

Produce two reports from the same `fullDebug` test execution:

- **Base** (`koverHtmlReportFullDebug`): all code minus generated machinery. The
  honest, glue-included line-coverage number.
- **Logic** (`koverHtmlReportLogic`): meaningful-logic coverage only. Additionally
  excludes untestable Android/Compose glue, identified by its wiring annotation
  (`@AndroidEntryPoint`, `@HiltAndroidApp`, `@Module`, `@Composable`) plus the
  theme constant files. Kover per-variant filters replace rather than merge the
  base filters, so the generated-code excludes are restated for this report.

Generated code is excluded from both reports (`*_Impl*`, Hilt/Dagger factories and
injectors, `hilt_aggregated_deps`, `ComposableSingletons`, `BuildConfig`). Kover
wildcards span the package separator, but a trailing `*` is needed to catch nested
synthetic classes (`AlarmDao_Impl$2`) and factory names without an `_Factory`
suffix (`Module_ProvideXFactory`); the first patterns missed both and leaked
generated code into the logic denominator. View models and small helpers with real
branching (for example `paletteForIndex`) stay in the logic report.

The `@Composable` annotation filter alone does not strip file-level composable
facades (`HomeScreenKt`), so UI is also excluded from the logic report by name
(`*ScreenKt`, the `theme` and `navigation` packages). Android adapters that wrap a
framework API behind a thin shell are excluded via `@InstrumentedOnly`; see
docs/adr/0002-coverage-exclusions.md.

CI generates both reports and logs the percentages; reports are uploaded as build
artifacts. No CI step fails on coverage.

## Consequences

- The "80% of meaningful logic" standard becomes measurable via the logic report.
- The base report will read lower than the logic report because it includes UI and
  Android glue; that gap is expected and informative, not a defect.
- Excluding glue from coverage does not lower its quality bar: ktlint, detekt and
  Android Lint still cover it, and instrumented tests exercise real flows.
- This is reversible. When the logic number is understood and stable, a changed
  code (diff) coverage gate or a measure-and-ratchet floor can be added in a
  follow-up ADR. We deliberately did not start with a hard global gate.
