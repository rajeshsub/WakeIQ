# 3. Consolidate git hooks on the pre-commit framework, add a one-step bootstrap

Date: 2026-07-24

## Status

Accepted

## Context

Three independent gate lists had drifted apart:

- `.githooks/pre-commit` (activated via `git config core.hooksPath .githooks`):
  ktlint, detekt, and `testFullDebugUnitTest`, gated on staged `.kt`/`.kts` files
  under source paths.
- `.pre-commit-config.yaml` (activated via `pre-commit install`): generic
  hygiene checks (trailing-whitespace, end-of-file-fixer, check-yaml,
  check-merge-conflict, check-added-large-files, mixed-line-ending), ktlint,
  detekt, Android Lint, and a no-println guard. No test hook at all.
- `.github/workflows/ci.yml`: its own explicit step list (ktlint, detekt,
  Android Lint, unit tests, Kover reports, `assembleFullDebug`).

Neither local mechanism was wired by default: both required a separate manual
command after clone, violating the one-step-bootstrap standard. Worse, the two
local mechanisms overlapped (ktlint/detekt duplicated) while diverging
elsewhere (only `.githooks` ran tests; only `pre-commit` ran Android Lint and
hygiene checks), and both diverged from CI's own list. This is exactly the
drift the "local hooks match CI, from one source" standard exists to prevent.

Tests currently run at commit time (moved there from pre-push in a prior
change), making every commit pay the full unit-test cost. Instrumented
(emulator) tests have never run locally in either mechanism; they require a
configured emulator and stay CI-only.

`pre-commit` itself is a Python tool, so wiring it requires Python/pip as a
prerequisite alongside the JDK and Android SDK already documented in the
README. A bootstrap step needs to work identically on Windows, macOS, and
Linux; the Gradle wrapper already ships both `gradlew` and `gradlew.bat` for
exactly this reason, whereas `make` is not available on Windows by default.

## Decision

**Canonical mechanism:** the `pre-commit` framework. Delete `.githooks/`
entirely. Nothing it uniquely provided is lost: `testFullDebugUnitTest` moves
to a new `pre-push`-staged hook in `.pre-commit-config.yaml`, and the
ktlint/detekt hooks' `files:` pattern widens from `\.kt$` to `\.kts?$` so
Gradle Kotlin-DSL build scripts keep the same coverage `.githooks` gave them.

**Pre-commit stage:** hygiene checks, ktlint, detekt, Android Lint, no-println
- fast, no test execution.

**Pre-push stage:** `testFullDebugUnitTest`, gated on staged `\.kts?$` files.
Full suite, not a hand-rolled affected-test selection: no dependency-graph-aware
test-selection tool exists for this Gradle/Kotlin stack, and the standard
explicitly forbids filename-matching selection as a substitute. Instrumented
tests stay CI-only; requiring a local emulator on every push is disproportionate
and nothing today runs them locally anyway.

**Bootstrap:** a Gradle task (`bootstrap`), not a Makefile. `make` is not
available on Windows without a separate install, which would reintroduce a
manual pre-step for exactly the platform that most needs a one-command
bootstrap. The Gradle wrapper is already cross-platform (`gradlew` /
`gradlew.bat`), so `./gradlew bootstrap` / `gradlew.bat bootstrap` is a single
command with no new tooling to install on any OS. The task: unsets
`core.hooksPath` (so a clone that previously followed the old
`.githooks`-activation instructions doesn't leave git pointed at a
now-deleted directory, which would silently disable every hook), ensures
`pre-commit` is installed, then runs
`pre-commit install --hook-type pre-commit --hook-type pre-push`.

**CI:** the quality job replaces its hand-listed lint/format/hygiene steps
with a single `pre-commit run --all-files`, so local and CI read the same
gate definition instead of a third hand-maintained list. Unit tests, Kover
reporting, and `assembleFullDebug` stay explicit CI steps since they aren't
hook concerns.

## Consequences

- One file (`.pre-commit-config.yaml`) is now the source of truth for every
  lint/format/hygiene gate, read by pre-commit, pre-push (tests only), and CI
  alike.
- Commits get faster (no test run); pushes carry the full unit-test cost
  instead, with CI as the final backstop re-running everything regardless.
- New contributors and fresh clones run one command
  (`./gradlew bootstrap`/`gradlew.bat bootstrap`) with no platform-specific
  instructions and no extra tooling installs beyond Python/pip, which is now
  stated as a prerequisite in the README alongside the JDK and Android SDK.
- Reversible: reintroducing `.githooks` or reverting CI's step list is a
  small, local change if `pre-commit` ever stops fitting.
