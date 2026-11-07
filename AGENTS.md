# AGENTS — Mandatory implementation contract

**Framework:** TAF-master lineage, Java 21, Selenium 4.49, TestNG 7.12, Maven, Allure 2.29, POM/Flows/Listeners.  
**Project root:** `D:\Zulfi_ZakaaDash_Automation` — one framework for two systems: **Zulfi Portal** (`zulfi`) and **ZakaaDash** (`zakaadash`).  
**Origin:** consolidated from `Football_Auto_TAF_Foundation` (Zulfi, working tree incl. uncommitted work) and `Auto_Test_ZakaaDash_DV/Selenium_Framework`; see `docs/MIGRATION_REPORT.md`. Zulfi recovery note: its baseline came from a USB transfer; original commit history was not recovered.

## Multi-system rules (both systems)

- Shared code lives in `com.taf.core` and must stay free of either application's URLs, login forms, and business entities. System behaviour lives in `com.taf.systems.<system>`.
- A run targets one system (`-Dactive.system`). Each system reads its own profile `systems/<system>.properties` via `SystemProfile` (`ZulfiConfig`, `ZakaaDashConfig`); JVM `-D` wins. Do not add generic URL keys shared by both systems.
- Every artifact writer uses `com.taf.core.utils.OutputPaths` (`-Doutput.dir`, `-Dallure.results.directory`, `-Dreports.dir`); never hardcode `target/...`.
- System reporting plugs into the shared lifecycle through `SystemReportingHooks` (ServiceLoader); system listeners must ignore other systems' tests.
- Browser holders are per system (Zulfi `BaseUiTest`, ZakaaDash `DriverManager`), both created by the core `GUIDriver`; never share a holder or session between systems.
- After adding/removing test classes run `python tools/gen_suites.py` and `python tools/gen_test_catalog.py`; never hand-edit `suites/<system>.xml` or `docs/test-catalog/`.
- The runner (`runner/server.py`) passes credentials only as child environment variables and never persists them (not in `run.json`, logs, schedules, the Systems page or the command line). Guide: `docs/runner/RUNNER_GUIDE.md`; decisions vs the NAWAH spec: `docs/runner/RUNNER_REBUILD_PLAN.md`.
- Runner rules: ZakaaDash runs serially (`serialOnly`); its opt-in timing retry (`com.taf.core.listeners.TimingRetry`) never retries assertions and is off whenever a create/submit/lifecycle gate is open; Claude Code requests work in a copy of the project and reach it only through a reviewed merge (undo available); runner state lives in `RUNNER_DATA_DIR` (default `runner/`, git-ignored).

## ZakaaDash system rules

- Follow `docs/zakaadash/AGENTS_ZAKAADASH.md` (smallest clean solution, no over-engineering, evidence before claiming success).
- Credentials: `ZAKAADASH_EMAIL` / `ZAKAADASH_PASSWORD` (JVM property, env, or the git-ignored `config/credentials.local.properties`) only. Generated DEV data uses `AUTO_PP_` / unique names; flows share one account, so ZakaaDash suites stay serial.
- Page loads use `PageLoadStrategy.NORMAL` (as originally written); waits come from `zakaadash.explicit.wait`.

## Zulfi system rules — Non-negotiables

- Preserve TAF architecture: one WebDriver manager, shared `ElementActions` / waits / validation / media / reporting.
- **One fresh browser per test method.** Login tests: unauthenticated start (`LoginFeatureFixture`). Protected features: independent login per test (`AuthenticatedUiTest`).
- Credentials: `ZULFI_USER` and `ZULFI_PASSWORD` only, resolved by `CredentialProvider` from `-D`, the environment, or the git-ignored `config/credentials.local.properties` — never commit secrets, never read passwords from JSON test-data.
- **Create Analysis:** default **disabled**. Require `-Danalysis.create.enabled=true` and `ZULFI_OFFICIAL_FIXTURE` (exact dropdown label). **At most one creating** submit click per authorized run (`AnalysisSubmissionAttempt`); the only exception is the MCH-ANL duplicate probe (`-Dmatch.schedule.analysis.duplicateCheck.enabled=true`): one extra click on a stale form that must be rejected and create nothing. No post-submit fixture rotation or automatic retries.
- **Execution pace:** default **DEMO** (`execution.pace=demo`); override with `-Dexecution.pace=fast|normal|demo`. Login interactions route through `ElementActions` / `PacedElementInteraction` (progressive typing in DEMO/NORMAL). Pacing is not a substitute for explicit waits.
- Do not weaken assertions, disable failing tests for pass rate, or fabricate Allure actual results.
- Screenshots on PASS/FAIL when browser is available; defect markdown under `<output.dir>/bug-reports/` (default `target/bug-reports/`) archived per run.
- Historical reports under `test-execution-reports/` are evidence — do not delete or rewrite outcomes.
- No destructive git operations; no push unless explicitly requested.

## Feature test index

- **Catalog (canonical):** `docs/testing/FEATURE_TEST_CATALOG.md` — per-feature registries, suite XML, phase gates, overlap, gap register.
- **Naming / regression:** `docs/testing/TEST_ID_NAMING_STANDARD.md`, `docs/testing/REGRESSION_TIERS.md`.
- **Story map:** `docs/PORTAL_USER_STORY_AUTOMATION_MAP.md`.
- **Tier 1 read-only smoke:** `scripts/run-portal-safe-smoke.ps1` (requires `ZULFI_USER` / `ZULFI_PASSWORD`).

## Current automated scope

| Feature | Tests |
|---------|--------|
| Login | LOGIN-HP-001 + regression batch incl. LOGIN-AUTH-001 invalid credentials rejected (see `src/test/resources/suites/zulfi/login-regression-suite.xml`; all Zulfi feature suites live in that folder) |
| **Yellow League clubs browse** | **CLUB-001…010** (`clubs-yellow-league-suite.xml`) — **US-FAP-CLUBS** (sidebar **الأندية**); `-Dclubs.yellow.phase=`; read-only |
| ~~Club information (about club)~~ | **Retired 2026-10-22** — CLUB-INF-001…030 (US-FAP-CLUB-001) superseded by CLUB-DET; skips unless `-Dclub.info.legacy=true`; see `docs/testing/REBASELINE_2026-10-22.md` |
| **Club details page** | **CLUB-DET-001…030** (`club-details-suite.xml`) — US-FAP-CLUB-DETAILS (`/clubs/{id}`: header, tabs, stats, leaderboards, fixtures + competition filter, overview ↔ fixtures ↔ squad logic, youth squads, squad photos/positions, about, layout); `-Dclub.details.phase=a|b|c`; read-only |
| **League standings** | **STD-001…010** (`standings-suite.xml`) — US-FAP-STANDINGS (Home snippet + `/standings`); `-Dstandings.phase=a|b|c`; read-only |
| **Default Club data consistency (E2E)** | **ZDC-001…016** (`zulfi-data-consistency-suite.xml`) — US-FAP-ZULFI-DATA: Home ↔ جدول المباريات ↔ ترتيب الدوري ↔ الأندية ↔ club page (results, totals, goals, top scorer, rank, upcoming, «عرض المزيد»); `-Dclub.data.phase=b`; read-only; registered as Zulfi E2E |
| **Home overview & recent analyses** | **HOME-001-001…045** (`home-overview-suite.xml`) — US-FAP-HOME-001; `-Dhome.overview.phase=`; read-only (no create submit) |
| **Home current team results** | **HOME-RES-001…014** (`home-current-results-suite.xml`) — US-FAP-HOME-CURRENT-RESULTS; `-Dhome.current.results.phase=`; read-only (same page, separate story) |
| **Match Schedule browse** | **MCH-SCHED-001…016** (`mch-schedule-suite.xml`) — US-FAP-MCH; `-Dmch.schedule.phase=`; read-only |
| Create Analysis | ANL-OFF-HP-001 (gated; `-Danalysis.create.enabled=true` + fixture label or index) |
| Official fixture selection | ANL-OFF-DIS-001 + ANL-OFF-SEL-NNN read-only (`analysis-fixture-selection-suite.xml`) — never submits |
| Official preflight | PREFLIGHT-OFF-001 read-only (`official-fixture-preflight-suite.xml`) — never submits |
| **Match Schedule → Analysis (33009)** | **MCH-ANL-001…019** in one class, driven by the user story and its business rules only (`match-schedule-analysis-suite.xml` read-only; `match-schedule-analysis-submit-suite.xml` authorized submit) — US-FAP-MCH-ANALYSIS; phased `-Dmatch.schedule.analysis.phase=`; **no submit** until phase **c** + `analysis.create.enabled=true` + `match.schedule.analysis.submit.enabled=true`; registry `docs/matchschedule/MCH-ANL_TEST_CASE_REGISTRY.md` |
| **Match Schedule analysis status sync** | **ANL-SYNC-001…013** (`anl-sync-suite.xml`) — US-FAP-ANL-SYNC; `-Danl.sync.phase=`; read-only default; lifecycle create only with `-Danl.sync.lifecycle.enabled=true` **and** `-Danl.sync.creation.enabled=true` **and** `-Danalysis.create.enabled=true` |
| **Match Type + Official link (Home)** | **ANL-TYPE-001…013** (`match-type-official-suite.xml`; phase **b+** use `match-type-official-b-suite.xml` for discovery) — US-FAP-ANL-MATCH-TYPE; `-Danl.match.type.phase=`; submit only **e** + `analysis.create.enabled=true` (013 → **ANL-OFF-HP-001**) |
| **Friendly match metadata (Home)** | **ANL-FRD-001…014** (`friendly-match-analysis-suite.xml`; phase **e** manifest guards: `friendly-match-analysis-e-suite.xml`) — US-FAP-ANL-FRIENDLY; `-Danl.friendly.phase=`; submit only **e** + `-Danalysis.create.enabled=true` **and** `-Danalysis.friendly.create.enabled=true` |
| **Analyses browse / search / filter** | **ANL-LST-001…059** (`anl-lst-suite.xml`) — US-FAP-ANL-003; `-Danl.lst.phase=`; read-only (create dialog only) |
| **Analysis processing status** | **ANL-STS-001…035** (`anl-sts-suite.xml`) — US-FAP-ANL-002; `-Danl.sts.phase=`; read-only default; post-submit observe only with `-Danl.sts.observeSubmit=true` **and** `-Danalysis.create.enabled=true` (single submit in ANL-STS-001) |

LOGIN-UI-003 verifies PrimeVue password visibility toggle (`.p-password svg[data-pc-section='unmaskicon'|'maskicon']`).

## Feature expansion process

1. User story + approved Excel + DOM review.  
2. Page Object + Flow before tests.  
3. One happy path, verify with Allure steps and screenshots.  
4. Map Excel IDs; implement only approved cases.  
5. Sequential suite (`parallel="false"`).  
6. Publish Allure + defect artifacts.

## Reporting contract

Each test: ID, title, feature, preconditions, numbered steps with **distinct** expected vs actual, status, evidence.  
Failures: classification (`PRODUCT_DEFECT`, `AUTOMATION_DEFECT`, `TEST_DATA_ERROR`, `CONFIGURATION_ERROR`, `ENVIRONMENT_ERROR`, `INCONCLUSIVE`) separate from TestNG status.

## Execution examples

```powershell
cd D:\Zulfi_ZakaaDash_Automation
# Tier 1 portal smoke (phase-a, no submit)
# .\scripts\run-portal-safe-smoke.ps1

# Login only (recommended demo)
.\scripts\run-demo.ps1

# Analysis (explicit authorization only)
# mvn test -DsuiteXmlFile=src/test/resources/suites/zulfi/demo-suite.xml `
#   -Danalysis.create.enabled=true `
#   -DZULFI_OFFICIAL_FIXTURE="exact fixture label from UI"

# Match Schedule analysis status sync (read-only phases)
# mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sync-suite.xml" `
#   "-Danl.sync.phase=a" "-Dexecution.pace=fast"

# Analysis processing status (read-only phase a)
# mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" `
#   "-Danl.sts.phase=a" "-Dexecution.pace=fast"

# Analyses listing browse/search/filter (read-only)
# mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" `
#   "-Danl.lst.phase=a" "-Dexecution.pace=fast"
```

Allure HTML requires Allure 2 CLI (`ALLURE_CLI` or `report.allureCli` Maven property).
