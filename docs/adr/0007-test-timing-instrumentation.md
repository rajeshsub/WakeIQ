# 7. Test-suite timing instrumentation via JUnit XML, trended in CI job summary

Date: 2026-08-22

## Status

Accepted

## Context

The standard (rule 25) requires test-suite execution timing to be captured
via idiomatic tooling and trended over time in CI, for Service/API-and-above
projects. This app has no latency-sensitive runtime paths (fully offline,
`AlarmManager`/`WorkManager` scheduling with no network or heavy compute), so
the agreed scope (Step A, G8) is test-suite timing only, not runtime-path
instrumentation.

Gradle's JUnit XML test reports (`app/build/test-results/**/*.xml`,
produced by both `testFullDebugUnitTest` and `connectedFullDebugAndroidTest`)
already carry a `time` attribute per test case and per suite - the data this
rule wants already exists on every CI run today, unused. Two approaches were
considered:

- **Parse the existing JUnit XML** (chosen): a small script extracts total
  and per-class durations already present in Gradle's own output, writes
  them as JSON into `benchmark-results/`, and a CI step renders a trend
  chart into the job summary from the history of those JSON files. No new
  test dependency, no changed test-execution behavior.
- **Adopt a dedicated JVM benchmark library** (e.g. JMH): gives
  nanosecond-precision micro-benchmarks, but that's built for measuring
  algorithmic hot paths, not "how long did the suite take" - a much heavier
  tool than this scope calls for, and this app has no identified hot path to
  benchmark.

## Decision

Add a Gradle task (`recordTestTiming`) that runs after
`testFullDebugUnitTest`, parses `app/build/test-results/testFullDebugUnitTest/**/*.xml`
for aggregate suite duration (`totalSeconds`) and test count (`testCount`),
and writes a single `benchmark-results/unit-test-timing.json` (git-ignored
locally; per-run identity comes from the `commit` field and the CI artifact
that wraps it, not from the filename). Per-class duration is not captured:
aggregate suite time is the signal rule 25 cares about (regression trend),
and per-class breakdown would need to survive in the same file across a
history of runs to be useful, which the single-file-per-run shape doesn't
give it - a scoped follow-up if per-class trend is ever actually wanted.

The `quality` job uploads that JSON as a `test-timing` artifact on every run
(90-day retention). A CI step then uses `gh run list`/`gh run download`
(`actions: read` permission) to pull the JSON from the last 10 successful
runs on `main`, combines them with the current run's, and renders a
duration-over-time table into `$GITHUB_STEP_SUMMARY`. This reuses GitHub's
own artifact storage as the history store instead of a `gh-pages` branch or
committed file, so there's nothing extra to maintain.

Scope stays test-suite-only per the agreed selection: no instrumentation is
added to alarm-scheduling or audio-ramp runtime code paths.

**Test coverage of `recordTestTiming` itself:** the task's parsing/aggregation
logic (XML parse, sum, JSON write) is real, non-trivial logic per rule 15,
but it lives inline in `app/build.gradle.kts` rather than in an extractable,
independently testable unit (would need a `buildSrc` precompiled script
plugin to house a testable class - a new build-logic module this project
doesn't otherwise have). Flagged as an open item rather than silently
exempted; whether to extract it is a developer call, not decided here.

## Consequences

- Every CI run now produces a small timing JSON artifact alongside existing
  lint/test/coverage artifacts; a slow-growing suite becomes visible as a
  trend instead of only being noticed anecdotally.
- No new test dependency and no change to how tests execute; the source data
  is Gradle's own existing JUnit XML output.
- If the suite's growth ever demands hot-path-level runtime profiling later,
  that's a separate, scoped addition (a JMH benchmark module) rather than a
  revision of this decision.
