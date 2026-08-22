# Engineering standards - gap audit findings

## 2026-08-22 audit - all 10 gaps closed

1. Coverage measured but not gated -> diff-gated Kover+diff-cover, ADR 0004. **Closed.**
2. CI workflow missing `permissions:` block -> added least-privilege blocks. **Closed.**
3. Third-party Actions pinned to mutable tags -> SHA-pinned, version in comment. **Closed.**
4. Release not gated on CI success -> re-run lint+test in `release.yml`, ADR 0005. **Closed.**
5. Release workflow missing `concurrency:` group -> added. **Closed.**
6. No dependency lockfile / vuln scanning -> Gradle locking + OWASP dependency-check, ADR 0006. **Closed.**
7. No CI badge in README -> CI/Release/Dependency-Scan badges added. **Closed.**
8. No release integrity artifacts (checksums, signed tags) -> SHA-256 checksums generated in `release.yml`, tag-signing practice documented in README (existing v0.1.0-v0.1.2 tags noted as predating it, not retroactively claimed signed). **Closed.**
9. No `docs/DATA-CLASSIFICATION.md` -> added, field table cross-checked against `AlarmEntity.kt` / `AppPreferences.kt`. **Closed.**
10. No test-suite timing / CI trend -> `recordTestTiming` Gradle task + CI trend step in job summary, ADR 0007. **Closed.**

Independent review (fresh-context subagent, 2026-08-22) additionally found
and fixed:

- **Blocking**: release checksum files hashed the build-output subdirectory
  path instead of the bare filename, so `sha256sum -c` would have failed for
  anyone following the README's own verification instructions. Fixed.
- Missing NVD CVE database cache across CI runs (ADR 0006 implied one
  existed; it didn't). Fixed: explicit `data.directory`, `actions/cache` step
  in both `ci.yml` and `dependency-scan.yml`.
- ADR 0004 hardcoded `origin/main` in its decision text where the actual
  implementation correctly uses `origin/${{ github.base_ref }}`. Doc fixed
  to match implementation.
- ADR 0006 referenced the plugin's generic `suppression.xml` name instead of
  this repo's actual `owasp-suppressions.xml`. Doc fixed.
- ADR 0007 promised per-run-named files and per-class duration breakdown
  that the implementation doesn't provide (single aggregate file instead).
  Doc rewritten to match what was actually built.
- Kover `xml { xmlFile = ... }` was resolving the path eagerly at
  configuration time; switched to the lazy `Provider<RegularFile>` form.

## 2026-08-23 - open item closed

- **`recordTestTiming` test coverage** (open item above): extracted the
  parse/aggregate logic into `buildSrc` (`TestTimingParser`), added 5 unit
  tests (single file, multiple files, missing `time` attr, malformed `tests`
  attr, empty list) - all pass, all assert actual totals, not just
  no-throw. `app/build.gradle.kts`'s task now just orchestrates. Wiring this
  surfaced a real gate gap along the way: the pre-commit `ktlint-check`/
  `detekt` hooks called bare `./gradlew ktlintCheck`/`detekt`, which only
  reaches the `:app` module - buildSrc's new Kotlin source would have been
  unlinted by the real hook despite `pre-commit run --all-files` looking
  green (it only looked green because I was invoking `:buildSrc:ktlintCheck`
  manually, not through the hook). Fixed by adding `:buildSrc:ktlintCheck`/
  `:buildSrc:detekt` to `.pre-commit-config.yaml`, then proved it with the
  same rule-16 method as the original audit: injected a deliberate ktlint
  violation into buildSrc source, ran `pre-commit run --all-files` (the real
  hook entrypoint, not a manual task call), confirmed rejection, reverted.
  **Closed.**

Deferred by developer request (2026-08-22), not forgotten:

- `NVD_API_KEY` GitHub secret - optional, recommended (unset = slower NVD
  lookups, anonymous rate limits).
- GPG tag signing setup - README already documents `git tag -s`/`git tag -v`
  going forward; needs the developer's signing key configured
  (`git config user.signingkey`, `tag.gpgSign`).
