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
(Consequences).

**Correction, same day:** the `nvd.delay` fix above was necessary but not
sufficient. The very next CI run, already carrying that fix, still hung on
the same step for over an hour (job started 07:29, still `in_progress`
past 08:47, cancelled manually) rather than failing fast. A 6-second
inter-retry delay cannot on its own explain an hour-plus run (30 retries x
6s is 3 minutes); the actual missing piece was that **no `timeout-minutes`
existed anywhere in `ci.yml`**, so a step stuck on a slow/hanging network
call (as opposed to a fast 503-and-fail) had GitHub's own default of 360
minutes to work with, not the few minutes a healthy run needs. Fixed by
adding `timeout-minutes: 20` to the `quality` job and `30` to
`instrumented`, sized against real historical run times (8m3s and 7m36s
respectively, from run 32615221004) rather than guessed. **Closed**: a
hung external call (NVD or otherwise) now surfaces as a clear timeout
failure within a bounded window instead of silently occupying a runner
for up to six hours.

## 2026-09-04 - CI wall-clock blowup: NVD cache never saved, retry delay multiplied it

CI stopped finishing. Timeline of the `quality` job, from `gh run list`:
8m (32615221004, 2026-08-23) -> 33m -> 115m (33603477386) -> 32m ->
timeout at the newly-added 20m cap. Step-level timings showed the whole
delta in one step, "Dependency vulnerability scan (OWASP)": 138s -> 1612s
-> 6571s. Every other step in the job was flat (~5.5 min combined).

Two compounding root causes, both introduced by earlier fixes in this file:

1. **The NVD cache never saved.** The key was
   `owasp-nvd-data-${{ github.run_id }}` (added in 61219a9).
   `actions/cache` only writes an entry when the primary key *misses*, and
   a run-id key is unique per run, so it missed every time and re-saved a
   fresh copy that the next run could never name. Confirmed empirically:
   `gh api /actions/caches` listed 21 cache entries, **zero** matching
   `owasp-nvd-data-*`, and every run's log says `Cache not found for input
   keys: owasp-nvd-data-<id>, owasp-nvd-data-`. So each run paid a full
   cold NVD feed sync, and the cost grew as the feed did.
2. **`nvd.delay` is per-request, not per-retry.** The 2026-09-02 entry set
   `delay = 6000` reasoning "worst case 30 retries x 6s = 3 minutes." That
   is wrong: the delay applies between every NVD API page request, and a
   cold sync is thousands of pages. Against cause (1)'s permanently-cold
   cache, 6s/page is what turned a ~2 minute scan into 109 minutes.

Fixed on three axes:

- **Moved the OWASP scan out of `ci.yml` entirely.** It now runs only in
  the weekly `dependency-scan.yml`. Lock files freeze dependency versions,
  so scanning on every push re-scans an unchanged graph - the scheduled run
  is the one that adds information. A slow or hung NVD can no longer block
  a merge.
- **Fixed the cache key** to a UTC date stamp
  (`owasp-nvd-data-YYYY-MM-DD`) with `restore-keys: owasp-nvd-data-`, so
  the entry rotates once a day and warm runs actually restore it.
- **Lowered `nvd.delay` to 2000ms**, still within NVD's documented rate
  limit for keyed access (50 requests / 30s), with a comment recording that
  this value is a per-request multiplier so the next person does not
  re-raise it.

`quality` timeout tightened 20 -> 15 min now that nothing in it talks to a
throttling third party; `dependency-scan.yml` gets its own
`timeout-minutes: 45`, sized for a legitimately slow cold sync.
**Closed**: PR-path CI is back to its pre-regression shape (lint + tests +
build only). Note the scan's own first scheduled run after this change is
still a cold sync and will be slow once; subsequent ones restore the cache.


Deferred by developer request (2026-08-22), not forgotten:

- `NVD_API_KEY` GitHub secret - **now confirmed required for the OWASP scan
  to run at all**, not merely "recommended for speed" as first stated. Get a
  free key at https://nvd.nist.gov/developers/request-an-api-key and add it
  as a repo secret; until then the scan step is skipped, not degraded.
- GPG tag signing setup - README already documents `git tag -s`/`git tag -v`
  going forward; needs the developer's signing key configured
  (`git config user.signingkey`, `tag.gpgSign`).
