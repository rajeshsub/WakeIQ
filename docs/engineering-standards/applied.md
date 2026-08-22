# Engineering standards - applied selection

Repo: WakeIQ (Android/Kotlin app, Gradle Kotlin DSL). Posture: personal
project, developer is sole author and standards authority.

## Selected groups (2026-08-22)

All seven selectable groups adopted:

- **G1 Testing discipline** - tiers: unit + instrumented (matches existing
  JVM unit tests + Espresso/Hilt instrumented tests).
- **G2 Coverage enforcement** - gating model: diff-gated, 80% floor on the
  Kover `logic` variant.
- **G3 Toolchain, gates and CI** - commit gate split: fast checks
  (ktlint/detekt/lint) at pre-commit, full unit suite at pre-push.
- **G5 Logging and observability** - depth: level hierarchy on device logs
  (Timber to logcat), not full structured JSON records (single offline app,
  no deployed-service log aggregation).
- **G6 Docs, ADRs, architecture, README** - adopted as-is.
- **G7 Interfaces and design** - adopted as-is.
- **G8 Instrumentation and performance** - scope: test suite only (no
  runtime-path instrumentation; app has no identified latency-sensitive
  path).

Not offered / always bind (security, credentials, AI-never-commits,
independent review, etc.) - see SKILL.md, not repeated here.

## Rule 16 gate verification (2026-08-22)

Verified against the actual local hook mechanism (`.pre-commit-config.yaml`,
Gradle-invoked, `pass_filenames: false` - hooks always scan real Gradle
source sets, not arbitrary matched files):

- **Green on clean code**: `pre-commit run --all-files` - all hooks passed
  (hygiene, ktlint, detekt, android-lint at pre-commit stage; observed
  separately for pre-push).
- **Blocks on violation**: a scratch file
  (`app/src/test/kotlin/com/wakeiq/ZzzGateVerificationScratchTest.kt`, never
  committed) with a deliberate ktlint/detekt style violation and an
  unresolved-reference compile error was created, hook chain run once per
  stage:
  - Pre-commit stage: `ktlintTestSourceSetCheck` and `detekt` both failed
    with the exact injected violations (`SpacingAroundParens`,
    `SpacingAroundOperators`, `Indentation`), correctly rejecting the commit.
  - Pre-push stage (`unit-tests` hook, `testFullDebugUnitTest`):
    `compileFullDebugUnitTestKotlin` failed on the injected error, correctly
    blocking before any test could run.
  - Scratch file deleted; `pre-commit run --all-files` re-run clean
    afterward, working tree confirmed matching pre-verification state plus
    only the intended changes.
- **CI/local parity**: `.github/workflows/ci.yml`'s `quality` job runs
  `pre-commit/action` (same `.pre-commit-config.yaml`) plus
  `testFullDebugUnitTest` explicitly - same gate list as pre-commit + the
  pre-push hook, from the one config file.

**Re-verified 2026-08-23** after `.pre-commit-config.yaml` changed (adding
`buildSrc` to the ktlint/detekt hook commands, needed once `buildSrc` gained
real source - see `gaps.md`). Confirmed the bare `ktlintCheck`/`detekt`
task names silently do NOT reach `buildSrc` (only `:app`) by running them
directly and grepping the task list; fixed by adding explicit
`:buildSrc:ktlintCheck`/`:buildSrc:detekt` to the hook commands; re-ran the
same inject-violation-in-buildSrc / confirm-rejection-via-the-real-hook /
delete / confirm-green cycle as the original verification. This is the
scenario rule 16 warns about directly: "green alone proves gate runs on
clean code, not [that it] stops bad code" - `pre-commit run --all-files` had
been reporting green on buildSrc changes for one full round without
buildSrc's lint ever actually running.

Hook config hash / CI config hash not separately recorded (no CI mechanism
in this repo for that yet); re-verify this section if `.pre-commit-config.yaml`,
the CI workflow files, or the pinned ktlint/detekt/Kover versions change.

## Gap audit history

See `gaps.md` in this directory for the per-gap record from the 2026-08-22
audit. All 10 gaps found were closed the same session; `gaps.md` now tracks
only the one open follow-up item that surfaced during independent review.
