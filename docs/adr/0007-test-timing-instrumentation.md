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
for aggregate suite duration (`totalSeconds`), test count (`testCount`), and
per-test-case duration (`testCases`: class, name, seconds - sorted slowest
first), and writes a single `benchmark-results/unit-test-timing.json`
(git-ignored locally; per-run identity comes from the `commit` field and the
CI artifact that wraps it, not from the filename). Per-test-case duration
*is* captured (revised from this ADR's original text, which said it wasn't -
see 2026-08-23 addendum below); it's read straight from the JUnit XML's own
`<testcase time="...">` attribute, no separate timer.

The `quality` job uploads that JSON as a `test-timing` artifact on every run
(90-day retention). A CI step then:
- uses `gh run list`/`gh run download` (`actions: read` permission) to pull
  the JSON from the last 10 successful runs on `main`, combines them with
  the current run's, and renders a duration-over-time table (one row per
  run) into `$GITHUB_STEP_SUMMARY` - this reuses GitHub's own artifact
  storage as the history store instead of a `gh-pages` branch or committed
  file, so there's nothing extra to maintain;
- renders a second table, per-test-case durations for the current run only
  (slowest first), also into `$GITHUB_STEP_SUMMARY` - not trended across
  runs, since a per-class history table across 10 runs would need its own
  storage shape (see Consequences);
- posts the run-level aggregate as a `::notice::` annotation too, since the
  commit/PR check link lands on the job page, one navigation short of where
  `$GITHUB_STEP_SUMMARY` renders (the run-overview page).

Scope stays test-suite-only per the agreed selection: no instrumentation is
added to alarm-scheduling or audio-ramp runtime code paths.

**Test coverage of `recordTestTiming` itself:** the XML parse/aggregate logic
is real, non-trivial logic per rule 15. It's extracted into
`buildSrc/src/main/kotlin/com/wakeiq/buildlogic/TestTimingParser.kt`
(a plain `object`, no Gradle API dependency) with unit tests in
`buildSrc/src/test/kotlin/.../TestTimingParserTest.kt` covering: single file,
multiple files summed, missing `time` attribute, malformed `tests`
attribute, an empty file list, per-testcase extraction sorted slowest-first,
and a testcase missing its own `time` attribute - each asserting the
resulting totals/list, not just that parsing doesn't throw.
`app/build.gradle.kts`'s `recordTestTiming`
task now only orchestrates (locate XML files, call the parser, resolve the
commit, write JSON); JSON serialization stays untested as trivial string
formatting. `buildSrc` also has `ktlint`/`detekt` wired (versions repeated
from `gradle/libs.versions.toml` since buildSrc is a standalone build with no
catalog access), and `.pre-commit-config.yaml`'s `ktlint-check`/`detekt`
hooks were updated to explicitly include `:buildSrc:ktlintCheck` /
`:buildSrc:detekt` - the bare `ktlintCheck`/`detekt` task names only reach
the `:app` module, so without this buildSrc code would have been unlinted.

## Consequences

- Every CI run now produces a small timing JSON artifact alongside existing
  lint/test/coverage artifacts; a slow-growing suite becomes visible as a
  trend instead of only being noticed anecdotally.
- No new test dependency and no change to how tests execute; the source data
  is Gradle's own existing JUnit XML output.
- Per-test-case duration only trends within a single run (slowest-this-run
  table), not across runs like the aggregate does - JUnit XML doesn't
  distinguish a single-unit test from a scenario/use-case test that chains
  several pieces of production code in sequence, so a scenario test (e.g.
  one exercising a multi-step alarm-scheduling flow) shows up in the same
  per-test-case table as any other, no separate mechanism needed for that.
- If the suite's growth ever demands hot-path-level runtime profiling later,
  that's a separate, scoped addition (a JMH benchmark module) rather than a
  revision of this decision.
