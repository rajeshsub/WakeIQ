# 5. Re-run quality checks inside the release workflow

Date: 2026-08-22

## Status

Accepted

## Context

`release.yml` triggers independently on `v*` tag pushes. It has no
dependency on `ci.yml` passing for the tagged commit: a tag can be pushed
against a commit that never ran CI, or against one where CI failed, and
`release.yml` will still build, sign, and publish release APKs to GitHub
Releases (and from there to the Play Store via `fastlane/`). This is exactly
the failure mode the standard calls out: a gate that exists (`ci.yml`) but
isn't wired to block what it's supposed to guard.

Three options were considered:

- **Re-run quality steps inside `release.yml`** (chosen): lint, detekt, and
  unit tests run again as steps in the release job before signing.
  Self-contained; the release workflow's pass/fail is the only signal that
  matters, with no dependency on a second workflow's event timing.
- **`workflow_run` trigger on `ci.yml` completion**, checking
  `github.event.workflow_run.conclusion == 'success'`. Avoids running tests
  twice, but only fires for the workflow run associated with the exact commit
  `ci.yml` last saw, which is fragile against tags pushed on commits `ci.yml`
  never ran against (e.g., a tag on an old commit, or a commit pushed without
  a matching PR/branch push event) - the workflow simply never triggers,
  silently, with no visible failure.
- **Branch protection** requiring a green `main` before a tag can be pushed.
  Pure process control, not enforceable by workflow code, and doesn't cover
  tags cut from a non-`main` ref.

Re-running the checks costs CI minutes on every release (an infrequent event)
in exchange for a release workflow that fails loudly and self-evidently
whenever the tagged commit doesn't pass quality gates, without relying on a
second workflow having already run against that exact commit.

## Decision

Add lint/format (`pre-commit run --all-files`) and unit test
(`testFullDebugUnitTest`) steps to `release.yml`, before the signing and
build steps, gating the rest of the job on their success (default GitHub
Actions step-failure behavior: any failing step stops the job before later
steps run). Instrumented (emulator) tests are not re-run here: they are the
slowest part of `ci.yml` and this is a build-integrity gate, not a full
regression re-run; a tag is only ever pushed from a commit that already
merged through a PR where `ci.yml`'s `instrumented` job ran.

## Consequences

- A tag pushed against a commit with a lint violation or a failing unit test
  no longer produces a signed release; the job fails before the signing step
  runs, and the keystore is never decoded.
- Release builds take longer (pays the lint+unit-test cost a second time),
  but this only affects the release job, cut infrequently.
- Reversible: replacing the re-run with a `workflow_run` trigger later, once
  the commit-coverage edge case is solved another way, is a scoped change to
  one workflow file.
