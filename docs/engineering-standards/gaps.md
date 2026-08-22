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

## Open item (not closed - developer decision needed)

- **`recordTestTiming` (app/build.gradle.kts) has no test coverage.** It's
  real parsing/aggregation logic (JUnit XML parse, duration sum, JSON write)
  per rule 15's "every non-trivial logic unit has a runnable check," but it
  lives inline in a `doLast` closure, not in an extractable unit. Testing it
  properly would mean pulling the logic into a `buildSrc` precompiled script
  plugin - new build-logic infrastructure this project doesn't otherwise
  have. Left open rather than unilaterally adding that infrastructure or
  silently exempting it; noted in ADR 0007's consequences section too.
