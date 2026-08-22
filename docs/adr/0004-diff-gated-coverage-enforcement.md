# 4. Enforce coverage on changed lines via Kover XML + diff-cover

Date: 2026-08-22

## Status

Accepted

## Context

Kover is wired (ADR 0001, ADR 0002) and CI already generates `koverXmlReportLogic`
on every push and PR, but nothing consumes that report to fail the build.
Coverage is measured, never gated: a PR can drop the `logic` variant's coverage
to zero and CI stays green.

An engineering-standards audit (2026-08-22) flagged this as a gap and asked for
diff-gated enforcement specifically: fail CI only when *changed* lines in the
`logic` variant fall under an 80% floor, not the whole codebase at once. A
project-wide floor was considered and rejected: the existing "logic" coverage
number has never been measured against 80% before, and gating the whole tree
immediately would likely fail on legacy files with no relation to the change
under review, forcing an unplanned retrofit pass before this ADR could land.

Kover has no native diff-coverage mode; its `verify {}` block only supports
whole-report thresholds. `diff-cover` (Bitovi/dtchepak, on PyPI) reads a
Cobertura-format XML report and compares it against a git diff, which is
exactly what Kover's `koverXmlReportLogic` output already produces. Python and
pip are already a documented prerequisite for this repo (ADR 0003, for the
`pre-commit` framework itself), so adding a Python-distributed CLI tool
introduces no new toolchain dependency, only a new pip package.

## Decision

Add a CI-only step, after `koverXmlReportLogic` runs, that:

1. Installs `diff-cover` via pip (same Python environment `pre-commit`
   already requires).
2. Runs `diff-cover app/build/reports/kover/reportLogic.xml --compare-branch=origin/<base_ref> --fail-under=80`,
   where `<base_ref>` is the PR's actual base branch (`github.base_ref`), not
   hardcoded to `main` - a PR targeting a release branch diffs against that
   branch, not `main`.
3. Runs only on `pull_request` events (a diff against `origin/main` on a
   direct push to `main` is a no-op comparison and would either always pass
   or always fail depending on ordering; PRs are where this standard has
   teeth).

Not run locally at pre-commit or pre-push: it needs the PR's diff against
`main`, which a local branch may not have fetched, and Kover's XML report is
only generated as part of the existing CI step. CI stays the single point of
truth for this gate, same as the instrumented-test job.

Threshold: 80%, matching the standard's floor. Scope: the `logic` variant
XML report only (excludes generated code, UI, DI wiring per ADR 0002) so the
80% number reflects testable business logic, not Compose screens or Hilt
glue that were never expected to hit that bar.

## Consequences

- New PRs that add or change logic-layer code must bring changed lines to
  80% coverage or CI fails; legacy files untouched by a PR are never
  penalized retroactively.
- One new CI-only dependency (`diff-cover`, pip-installed at job runtime, not
  committed to the repo's own dependency graph).
- Coverage regression on a genuinely hard-to-test change becomes a visible,
  blocking CI failure instead of a silent report nobody reads.
- Reversible: removing the step reverts to measure-only; raising or lowering
  `--fail-under` is a one-line change.
