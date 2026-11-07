# Migration report — Zulfi Portal + ZakaaDash into one TAF framework

Date: 2026-10-20. Implements `MULTI_SYSTEM_FRAMEWORK_HANDOFF.md` with the choices confirmed by the user:
**new combined repository**, **shared core with ZakaaDash adapted onto it**, **core runner** (no dashboard,
schedules, Word/Excel export, or Docker).

Source repositories were only read, never modified:

| | Zulfi Portal | ZakaaDash |
|---|---|---|
| Source | `D:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation` (working tree, incl. uncommitted work) | `D:\Auto_Test_ZakaaDash_DV\Selenium_Framework` (working tree; `.backup/` excluded) |
| Before | 231 Java files, `com.automationexercices` (TAF core) + `com.zulfi.portal`, Selenium 4.35, TestNG 7.11, Allure 2.29.1, ~40 suite XMLs | 65 Java files, all under `src/test`, `com.automation`, Selenium 4.49, TestNG 7.12, Allure 3.0, Owner config, own driver/waits/listeners, 10 suite XMLs |
| Auth | env `ZULFI_USER` / `ZULFI_PASSWORD` | JVM prop or env `ZAKAADASH_EMAIL` / `ZAKAADASH_PASSWORD` |
| After | `com.taf.systems.zulfi` (main + test) | `com.taf.systems.zakaadash` (pages/flows/config/utils in main, base/tests in test) |

Not copied: build output, `test-execution-reports/` history (it stays as evidence in the Zulfi repo), ZakaaDash
`.backup/`, `allure-results/`, `qa-summary/`, `test-output/`. ZakaaDash docs and phase logs are under `docs/zakaadash/`.

## Architecture decisions

* **Shared core** = Zulfi's TAF core renamed to `com.taf.core` (drivers, `ElementActions`, waits, pacing, validations,
  media, Allure, logs). New in core: `OutputPaths`, `SystemProfile`, per-system `JsonReader`, `SystemReportingHooks` SPI.
* **Listener decoupling:** the core `TestNGListeners` previously imported 10 Zulfi reporting classes. It is now
  system-neutral; Zulfi's reporting is `ZulfiReportingHooks` (same calls, same order: soft-assert → pacing diagnostics →
  final screenshot → failure diagnostics → video → failure grouping/bug report → context clear), invoked only for Zulfi
  tests and, at execution level, only when `active.system=zulfi`. ZakaaDash's `TestListener`/`QaSummaryListener` only act
  on ZakaaDash classes.
* **Separate browser holders:** Zulfi keeps `BaseUiTest` (per-test `GUIDriver`); ZakaaDash keeps a ZakaaDash-only
  `DriverManager` (`ThreadLocal<GUIDriver>`). Both browsers come from the core factories (Local/LocalHeadless/Remote Grid).
* **Config:** `PropertyReader` loads base resources + `systems/<active.system>.properties`; each system reads its own profile
  through `SystemProfile` (`ZulfiConfig`, `ZakaaDashConfig`) so it works regardless of `active.system`.
* **No API clients / BaseApiClient:** neither system had API clients (Rest Assured was an unused Zulfi dependency, kept).
  Per the ZakaaDash "no over-engineering" rule nothing speculative was added; the runner preflight therefore checks URL
  reachability only and explicitly reports authentication as *not verified*.
* **Credentials:** env-only by both systems' contracts. `login-data.json` per system documents the env var names (no
  values). The runner passes form credentials as child environment variables, never as `-D` flags, and never persists them.

## Behaviour changes (review these)

1. Packages: `com.automationexercices` → `com.taf.core`; `com.zulfi.portal` → `com.taf.systems.zulfi`;
   `com.automation` → `com.taf.systems.zakaadash` (`listeners` → `reporting`, `pages.zakaadash` → `pages`,
   `flows.zakaadash` → `flows`, `BaseTest` → `ZakaaDashBaseTest`).
2. Versions: Zulfi Selenium 4.35 → 4.49 and TestNG 7.11 → 7.12; ZakaaDash Allure 3.0 → 2.29.1 (8 `AttachmentOptions`
   calls ported to `Allure.addAttachment(..., ".txt|.png")`); Owner dependency removed.
3. ZakaaDash browser creation now uses the TAF Chrome/Edge options (notifications/popups disabled, start maximized,
   download dir, unhandled prompts ignored). Its original NORMAL page-load strategy and page-load timeout are kept.
4. ZakaaDash keys are namespaced: `zakaadash.baseUrl` (legacy `zakaadash.base.url` still accepted),
   `zakaadash.explicit.wait`, `zakaadash.page.load.timeout`, `zakaadash.human.delay.*`, `zakaadash.environment.name`.
   `-Dbrowser` / `-Dheadless` are replaced by core `-DbrowserType` / `-DexecutionType=LocalHeadless`.
5. Zulfi URL key `zulfi.baseUrl` (legacy `-DbaseUrlWeb` and env `ZULFI_BASE_URL` still accepted, HTTPS still enforced).
   `analysis.fixture.index` and `portal.defaultClub.*` moved to `systems/zulfi.properties` (same key names).
6. **Property files are now read as UTF-8.** Previously `Properties.load(InputStream)` read the UTF-8 `webapp.properties`
   as ISO-8859-1, so the file default for `portal.defaultClub.label` (Arabic) was garbled whenever no env/-D override was
   set. This is a latent-bug fix that can change Zulfi Home-results outcomes on runs relying on the file default.
7. Chrome downloads: Zulfi used `src/test/resources/downloads`, Edge/Firefox `target/downloads`; all now use
   `<output.dir>/downloads` and `FileUtils.isFileExist` checks the same folder.
8. All artifacts follow `OutputPaths` (`-Doutput.dir`, default `target`): screenshots, logs (log4j2 `${sys:output.dir}`),
   recordings, bug reports, failure groups, ZakaaDash QA summary/defect candidates. Zulfi's run archive follows
   `-Dreports.dir` (default `test-execution-reports`). Maven defaults are unchanged.
9. Zulfi `target/analysis-fixtures/latest-manifest.json` intentionally stays project-scoped (discovery seeds later runs);
   the runner serializes Zulfi runs, so runs do not race on it.
10. Grid sessions set `LocalFileDetector` so ZakaaDash uploads work on remote nodes.
11. Suites moved: `src/test/resources/suites/zulfi/*.xml`, `src/test/resources/suites/zakaadash/*.xml`. Zulfi scripts
    and strategy/registry docs were updated; historical reports under `docs/recovery/` were left as written.
12. ZakaaDash test data moved to `src/test/resources/test-data/zakaadash/` (resource constants and `case.json` updated).

## Validation results (actually executed, 2026-10-20)

No `ZULFI_*` / `ZAKAADASH_*` credentials were available in this environment, so authenticated scenarios were not run.

| Check | Result |
|---|---|
| `mvn test-compile` (merged POM, offline) | BUILD SUCCESS |
| Zulfi `portal-unit-tests-suite.xml` (Maven) | 9/9 passed; artifacts in `target/`, Allure env shows `System=zulfi` |
| ZakaaDash `ZakaaDashCredentialsTest` (fake values) + `HerokuLoginTest` headless via core driver, `-Doutput.dir=target/zd-check` | 2/2 passed; screenshots/QA summary/Allure only under `target/zd-check` |
| `suites/shared-core.xml` via `-Dsurefire.suiteXmlFiles=` | 7/7 passed (-D precedence, legacy keys, actionable errors, URL join, JSON overrides, output roots) |
| Generated whole suites, TestNG dry-run | `suites/zulfi.xml` 257 methods, `suites/zakaadash.xml` 17 methods — matches the catalog (ZakaaDash 18 minus excluded Heroku demo) |
| Runner, Zulfi `LoginRegressionTest` + `DefectReportingContractTest` against the real portal (unauthenticated) | 13/13 passed, run-scoped screenshots + Allure HTML |
| Runner, ZakaaDash `AuthenticationTests` + credentials + Heroku with fake form credentials, **concurrently** with the Zulfi run | 3 passed, 1 controlled failure: AUTH-001 rejected by the app ("email not registered"), failure screenshot linked; AUTH-002 (invalid credentials rejected) passed |
| Runner, Zulfi controlled failure: Portal URL overridden to `https://the-internet.herokuapp.com/` | `failed` with 10 setup failures, screenshots + 10 defect markdown files in that run only — proves the UI URL override reached only that process |
| Runner, unreachable URL (`nonexistent-host.invalid`) | `error` "Environment unreachable (preflight) - tests not started" |
| Same-system serialization / cross-system parallelism | Two ZakaaDash runs serialized; Zulfi and ZakaaDash ran in parallel |
| Stop + rebuild during active run | Run stopped (process tree killed); forced rebuild created a new snapshot while the stopped run kept `1a0caf2eb7ae` |
| Restart / history | Run killed with the runner → `interrupted` after restart; history retained; second instance refuses to start |
| Request validation | HTTP portal URL, unknown class, reserved `output.dir` property, empty E2E scope → 400 with messages; POST without `X-Runner` → 403 |
| Secrets / tracked files | Fake credential values not found anywhere under `runner/runs/`; SHA-1 of profiles, `environments.json`, generated suites unchanged after override runs |
| Delete | Selected finished run deleted; unknown id ignored |

Bug found and fixed during validation: the runner first reported a run whose tests were all skipped by setup failures as
"passed". It now reports `failed` when any setup/teardown fails and `skipped` when nothing passed.

## Not exercised / known limitations

* Authenticated flows of both systems (Zulfi features beyond login, ZakaaDash data versions/upload/preprocessing/E2E),
  Zulfi gated Create Analysis — need real credentials. Selenium Grid mode, visible-browser mode, Edge/Firefox, video
  recording were not run.
* `regression.py` `KNOWN` lists are empty and no test uses `@Issue` yet, so "E2E + regression" currently means E2E plus
  classes that failed in recent runner history. Zulfi has no registered E2E classes.
* Runner has no login; it binds to localhost. Do not expose it without HTTPS + access control.
* The same-system lock covers runner runs only; a Maven run started separately can still share backends/accounts.
* If the runner process is force-killed (not Ctrl+C), the Java child keeps running; Ctrl+C or Stop kills the tree.
* Zulfi's own `AllureCliResolver` still looks for `tools/allure-2.29.0` while `install-allure-local.ps1` installs 2.29.1
  (pre-existing; the runner accepts any `tools/allure-2.*`).
* The new repository is `git init`-ed with nothing committed.

## Addendum — US-FAP-MCH-ANALYSIS (33009) refactor (2026-10-21)

* `MatchScheduleAnalysisAcceptanceTest` is now the single class for the story (MCH-ANL-001…017); 11 overlapping classes and
  10 suite XMLs were removed. Registry: `docs/matchschedule/MCH-ANL_TEST_CASE_REGISTRY.md`.
* Found and removed a source file with **double-encoded Arabic** (`MatchScheduleAnalysisFlowAcceptanceTest.java`) that made its
  report titles unreadable.
* Reporting: single specification per test, step journal, explicit failure classification, bug report format v2 and real
  evidence links; browser video via Chrome DevTools (`BrowserVideoRecorder`, `@VideoEvidence`). The old
  `Test case metadata`/`Preconditions`/`Test objective` attachments were merged into *Test case specification*.
* `DefectReportingContractTest` was updated to the new report headings (deliberate format change).
* Verified here without touching the live portal: compile, 30 contract/unit/real-browser (local page) tests, runner tests,
  dry runs of all suites, and an end-to-end failing sample that produced the bug report, 3 screenshots and an MP4.
  **The MCH-ANL scenarios themselves were not run against the live portal** (that would enter the user's password).
