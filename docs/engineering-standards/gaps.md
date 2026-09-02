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

## 2026-08-23 - CI failure: NVD_API_KEY turned out to be required, not optional

First two pushes both failed `ci.yml`'s "Dependency vulnerability scan
(OWASP)" step: `NvdApiException: Invalid API Key, length of 0`. The original
ADR 0006 / code comments said an unset key just meant slower anonymous NVD
access - that was wrong. This plugin version's NVD API 2.0 client has no
anonymous fallback at all; a blank key throws outright, it doesn't degrade.
Fixed by making both `ci.yml` and `dependency-scan.yml` skip the OWASP step
(with a `::warning::` annotation) when `secrets.NVD_API_KEY` is unset, so CI
passes green - but the scan itself does not actually run until the key is
added. ADR 0006 and the build.gradle.kts comment corrected to say
"required," not "optional." **Closed** (CI green), but functionally the
dependency vulnerability scan is a no-op until the developer adds the key -
this is not the same as the gap being fully resolved end-to-end.

## 2026-09-02 - CI failure: NVD retry burst had no backoff, exhausted budget on a transient 503

`ci.yml`'s "Dependency vulnerability scan (OWASP)" step failed after 28
minutes: `NvdApiException: NVD Returned Status Code: 503`, despite a valid
`NVD_API_KEY` being set (this is a different failure from the 2026-08-23
entry below - the key was present and accepted; NVD's own API was
unavailable/throttling during the update window). The plugin's own log
line even said "Unable to update 1 or more Cached Web DataSource, using
local data instead" immediately before failing anyway, rather than
actually falling back. Root cause: the plugin's defaults are 30 retries
with `nvd.delay = 0`ms between them - a rapid-fire burst against an
already-503'ing endpoint, not a backoff. Fixed by setting `nvd.delay =
6000` (6s) in `build.gradle.kts`, so a transient NVD outage has 6s x 30 =
up to 3 minutes to clear instead of the retry budget burning in seconds.
Verified against the installed `dependency-check-gradle:13.0.0` jar
(`NvdExtension.class`) that `nvd.delay`/`nvd.maxRetryCount`/
`nvd.validForHours` are real settable properties before changing config,
and against `dependency-check-core:13.0.0`'s bundled
`dependencycheck.properties` that the as-shipped defaults are
`nvd.api.max.retry.count=30` / `nvd.api.delay=0`. ADR 0006 updated
(Consequences). **Closed** for the zero-delay bug; does not guarantee a
build survives a sustained (not transient) NVD outage - that is an
accepted risk of depending on a third-party service for this gate, not a
config value to keep raising.

Deferred by developer request (2026-08-22), not forgotten:

- `NVD_API_KEY` GitHub secret - **now confirmed required for the OWASP scan
  to run at all**, not merely "recommended for speed" as first stated. Get a
  free key at https://nvd.nist.gov/developers/request-an-api-key and add it
  as a repo secret; until then the scan step is skipped, not degraded.
- GPG tag signing setup - README already documents `git tag -s`/`git tag -v`
  going forward; needs the developer's signing key configured
  (`git config user.signingkey`, `tag.gpgSign`).
