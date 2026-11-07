# Zulfi + ZakaaDash Automation (one TAF framework, one runner)

Java 21 · Maven · Selenium 4.49 · TestNG 7.12 · Allure 2.29 · Python 3.10+ runner (stdlib only).

One automation framework for two independently configured systems:

| System key | Application | Code | Credentials (env vars) |
|---|---|---|---|
| `zulfi` | Zulfi Portal (Vue/PrimeVue) | `com.taf.systems.zulfi` | `ZULFI_USER`, `ZULFI_PASSWORD` |
| `zakaadash` | ZakaaDash (data versions, upload, preprocessing) | `com.taf.systems.zakaadash` | `ZAKAADASH_EMAIL`, `ZAKAADASH_PASSWORD` |

A run always targets **one** system (`-Dactive.system=zulfi|zakaadash`). The embedded runner can queue runs of
both systems from one page. Migration details and validation evidence: [docs/MIGRATION_REPORT.md](docs/MIGRATION_REPORT.md).

## Layout

```text
pom.xml                              merged dependency set; ${build.dir} overridable for runner snapshots
src/main/java/com/taf/core/          shared core: drivers (Local/Headless/Grid), ElementActions, waits, pacing,
                                     validations, media, Allure, logs, OutputPaths, PropertyReader/SystemProfile/JsonReader,
                                     TestNGListeners + SystemReportingHooks SPI
src/main/java/com/taf/systems/zulfi/      Zulfi config (ZulfiConfig/PortalConfig), pages, flows, reporting hooks
src/main/java/com/taf/systems/zakaadash/  ZakaaDash config (ZakaaDashConfig), driver holder, pages, flows, reporting
src/test/java/com/taf/systems/zulfi/      BaseUiTest / AuthenticatedUiTest + feature tests
src/test/java/com/taf/systems/zakaadash/  ZakaaDashBaseTest + tests (e2e under tests/e2e)
src/test/java/com/taf/core/               shared-core contract tests
src/main/resources/systems/<system>.properties   namespaced URL/profile defaults
src/test/resources/test-data/<system>/           fixtures (no secrets)
src/test/resources/suites/<system>/              original per-feature suites (kept)
suites/zulfi.xml, suites/zakaadash.xml           GENERATED whole suites (tools/gen_suites.py)
suites/shared-core.xml                           shared-core contract tests
tools/  systems.py testsource.py regression.py gen_suites.py gen_test_catalog.py
runner/ server.py store.py config.py schedules.py reliability.py reports.py vault.py bugreport.py agent.py
        orchestrator.py archive.py static/index.html environments.json modules.json presets.json Dockerfile tests/
docs/test-catalog/<system>.md                    GENERATED test catalog
```

## Prerequisites

JDK 21, Maven 3.9+ (or set `MVN`), Python 3.10+, Chrome/Edge/Firefox. Allure 2 CLI for HTML reports:
`.\scripts\install-allure-local.ps1` (installs under `tools/`) or set `ALLURE_CLI`.

Credentials are never committed. Easiest: copy `config/credentials.example.properties` to
`config/credentials.local.properties` (git-ignored) and fill it in; both systems and the runner read it automatically.
Precedence: `-D<NAME>` > environment variable > that file. The file is read from disk (never packaged into test
resources/snapshots) and values are never logged. `login-data.json` files hold variable *names* only; do not type
passwords into them (CORE-CRED-002 fails if you do). Alternatively use environment variables:

```powershell
$env:ZULFI_USER = Read-Host 'Zulfi login email'
$s = Read-Host 'Zulfi password' -AsSecureString; $env:ZULFI_PASSWORD = [Net.NetworkCredential]::new('', $s).Password
$env:ZAKAADASH_EMAIL = Read-Host 'ZakaaDash email'
$s = Read-Host 'ZakaaDash password' -AsSecureString; $env:ZAKAADASH_PASSWORD = [Net.NetworkCredential]::new('', $s).Password
```

## Configuration contract

* `src/main/resources/environment.properties` sets the default `active.system` (zulfi).
* Each system reads **its own** profile through `SystemProfile`, independent of `active.system`:
  `zulfi.baseUrl` (HTTPS enforced) and `zakaadash.baseUrl`, plus each system's own keys.
* Precedence: JVM `-D` property → profile default. Legacy aliases still work: `-DbaseUrlWeb` / env `ZULFI_BASE_URL`
  for Zulfi, `-Dzakaadash.base.url` for ZakaaDash. Missing/invalid settings fail with the system and key named.
* Shared-core keys (`browserType`, `executionType` = `Local|LocalHeadless|Remote`, `remoteHost`, `remotePort`,
  `execution.pace`, `report.video`) apply to both systems. ZakaaDash's former `-Dbrowser`/`-Dheadless` keys map to
  `-DbrowserType` / `-DexecutionType=LocalHeadless`.
* Fixtures: `new JsonReader("zakaadash", "login-data")`; per-run override `-Ddata.<system>.<file>.<json.path>=value`.
* Output isolation: `-Doutput.dir` (default `target`) roots screenshots, logs, downloads, recordings, bug reports and
  QA summaries; `-Dallure.results.directory` and `-Dreports.dir` (Zulfi run archive, default `test-execution-reports`).

## Run from Maven

```powershell
python tools/gen_suites.py          # regenerate suites/<system>.xml after adding/removing test classes
python tools/gen_test_catalog.py    # regenerate docs/test-catalog/<system>.md

mvn test "-Dsurefire.suiteXmlFiles=suites/zulfi.xml" "-Dactive.system=zulfi"
mvn test "-Dsurefire.suiteXmlFiles=suites/zakaadash.xml" "-Dactive.system=zakaadash"
mvn test "-Dsurefire.suiteXmlFiles=suites/shared-core.xml"

# original per-feature suites still work (Zulfi scripts use these):
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/login-regression-suite.xml"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zakaadash/testng-smoke.xml" "-Dactive.system=zakaadash"
```

Zulfi safety gates are unchanged: Create Analysis stays disabled unless `-Danalysis.create.enabled=true` plus the
fixture property are given explicitly; phase properties (`-Dhome.overview.phase=...` etc.) still select read-only phases.
The generated whole suites run with these safe defaults, so gated tests skip instead of submitting.

## Test reports, evidence and bug reports

* Every Zulfi test publishes one **Test case specification** (objective, acceptance criteria, business rules, preconditions,
  test data, expected final result) and, per step, an **expected-vs-actual table** with its screenshot.
* A failing step carries an explicit classification (`PRODUCT_DEFECT`, `INCONCLUSIVE`, `TEST_DATA_ERROR`, `CONFIGURATION_ERROR`,
  `ENVIRONMENT_ERROR`, `AUTOMATION_DEFECT`) and the bug report (`<output.dir>/bug-reports/LOCAL-*.md`) lists the executed steps,
  the root-cause analysis, the test data used, links to the real screenshots/video, and the exact re-run command.
* **Video** records the browser page itself (Chrome DevTools screencast, so it also works headless) and is encoded to MP4 with
  the bundled ffmpeg. `-Dreport.video=OFF|ON_FAILURE|ALWAYS` (a test class can set a default with `@VideoEvidence`). Chromium
  local/headless only; Firefox/Safari/Grid log a warning and record nothing. `-Dreport.video.engine=screen` keeps the legacy
  desktop recorder (visible local runs).
* US-FAP-MCH-ANALYSIS (33009): one class, one registry with AC/BR traceability: `docs/matchschedule/MCH-ANL_TEST_CASE_REGISTRY.md`.

## Embedded runner (Automation Control Center)

```powershell
python -m pip install -r runner/requirements.txt   # optional extras: Word/Excel, MySQL, Spaces, vault
python runner/server.py --port 8080                 # then open http://localhost:8080
```

Full guide (pages, settings, API, deployment): `docs/runner/RUNNER_GUIDE.md`; design decisions and gap analysis
against the NAWAH runner specification: `docs/runner/RUNNER_REBUILD_PLAN.md`.

* **Runs**: system → environment → URLs (yellow when changed; `-D` for that run only) → logins (environment variables of
  that run's JVM only, never saved) → scope (full, E2E + regression, E2E only, classes, modules, retest failures, all
  systems) → test type → parallel classes → Headless / Visible / Grid → switches (preflight, retry timing failures once,
  count in success criteria, count as full run). Presets and the phase-gate helper (`runner/presets.json`) stay.
* **Snapshots**: sources are compiled into `runner/.build/<id>`; a run keeps its snapshot even if the code changes.
* **Lifecycle**: `queued → building → running → passed | finished with failures | failed to start | environment down |
  stopped | cancelled | error | interrupted`. Same system / same server never overlap; `RUNNER_MAX_PARALLEL` (default 4).
  Environment-down runs are retried automatically (3× every 10 min).
* **Results ≠ reports**: results (runs, test results, defect registry, schedules) live in JSON files under
  `RUNNER_DATA_DIR` (default `runner/`) or MySQL (`RUNNER_DB_HOST`); reports (Allure HTML, screenshots, console) are
  files, optionally moved to DigitalOcean Spaces. Removing reports keeps results.
* **Dashboard**: release status per system (🔴 فيه Critical / 🟡 فيه مخاطر / 🟢 جاهز للإصدار), pass rate, open defects with
  severity + stakeholder sentence + retest, reopen / flaky rates, environment availability, last full run, module map,
  trends, slowest / most failing tests, reliability (flaky tests, quarantine, cut-off runs), CLI report history, storage.
* **Defects**: bug IDs `ZUL-01` / `ZKD-01`, New / Reopened, "not a bug", Word / Excel bug report in the project layout.
* **Schedules**: daily runs with Gmail notifications. **Claude**: requests in a private copy of the project, live log,
  diff, merge / undo, heal locator failures. **Orchestrator**: sprint, reported bug, regression and retest workflows
  with approvals. **Systems**: URL fields, environments, login fields, parallel rules, test types, module map.
* Security: binds `127.0.0.1` by default; every POST needs the `X-Runner: 1` header; foreign `Host` headers are refused;
  Basic auth when `RUNNER_PASSWORD` is set. Credentials are never written to `run.json`, logs or the command line.
* Single instance: a new `server.py` takes over the old one only when it has no active run (`--force` to override);
  `python runner/server.py --stop` stops it.
* Docker: `RUNNER_PASSWORD=change-me docker compose -f runner/docker-compose.yml up -d --build`.

Runner tests (87 cases: preflight PF-001..014, runs/suites/scheduler, summary, dashboard and defects, vault, stores,
configuration, reliability, schedules, Claude integration with a fake CLI, orchestrator, HTTP API; no external network):

```powershell
python -m unittest discover -s runner/tests -v
```

## Adding tests

Add the class under the system's test package (any `*Test`/`*Tests` name with TestNG `@Test` methods), then run
`python tools/gen_suites.py` and `python tools/gen_test_catalog.py`. Register E2E classes in `tools/systems.py`;
reviewed known-bug checks go into `KNOWN` in `tools/regression.py` (or annotate with Allure `@Issue`).

System-specific rules: Zulfi — [AGENTS.md](AGENTS.md), [docs/zulfi/README_ZULFI.md](docs/zulfi/README_ZULFI.md),
[docs/testing/FEATURE_TEST_CATALOG.md](docs/testing/FEATURE_TEST_CATALOG.md); ZakaaDash —
[docs/zakaadash/AGENTS_ZAKAADASH.md](docs/zakaadash/AGENTS_ZAKAADASH.md), [docs/zakaadash/](docs/zakaadash/).
