# Football Analysis Automation — TAF (Recovery Baseline)

Java 21 · Selenium 4 · TestNG · Maven · Allure 2 · Vue/PrimeVue Zulfi Portal.

**Root:** `E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation`

Functional reconstruction from a transferred foundation (see `docs/recovery/RECOVERY_COMPLETION_REPORT.md`). This is not a byte-for-byte copy of the former `D:\Football_Analysis_Auto` tree.

## What is implemented

- TAF core: drivers, `ElementActions` (human-paced **DEMO** by default), waits, listeners, screenshots.
- **Login:** LOGIN-HP-001 + 10 regression cases (`LoginRegressionTest`), suite `src/test/resources/suites/zulfi/login-regression-suite.xml`.
- **Create Analysis:** ANL-OFF-HP-001 with exactly-once submit and placeholder-based team verification (creation **off** by default).
- Reporting scaffolding: `TestStepReporter`, defect markdown, `AllureCliResolver`, run archive under `test-execution-reports/`.

## Prerequisites

- Windows, JDK 21, Maven 3.9+, Chrome (or configured browser).
- Portal test account via environment variables.
- Allure 2 CLI for HTML: run `.\scripts\install-allure-local.ps1` (installs 2.29.0 under `tools/`), or set `ALLURE_CLI` / `-Dreport.allureCli`.

## Configure credentials

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
$env:ZULFI_USER = Read-Host 'Login email'
$secure = Read-Host 'Login password' -AsSecureString
$env:ZULFI_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
Remove-Variable secure
mvn clean test-compile
```

## Run login regression (safe default demo)

```powershell
.\scripts\run-demo.ps1
```

Uses visible local Chrome, `-Dexecution.pace=demo`, login suite only — **no analysis creation**.

Manual equivalent:

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/login-regression-suite.xml" `
  "-DexecutionType=Local" "-Dexecution.pace=demo" "-Dreport.open=true"
```

## Official analysis happy path (requires explicit authorization)

Creation is blocked unless:

- `-Danalysis.create.enabled=true`
- Environment variable `ZULFI_OFFICIAL_FIXTURE` set to the **exact** official match label shown in the UI
- Optional video override: `ANALYSIS_VIDEO_URL` (default YouTube URL in test data)

```powershell
# Do not run until fixture eligibility is confirmed
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/demo-suite.xml" `
  "-Danalysis.create.enabled=true"
```

## Reports

- Raw Allure: `target/allure-results`
- Archived runs: `test-execution-reports/runs/<runId>/`
- Latest pointer: `test-execution-reports/latest-run.properties`
- Bug investigations: `target/bug-reports/` (copied into run folder when publisher runs)

## Backup

Pre-recovery snapshot (read-only reference):  
`E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation_PRE_RECOVERY_20261016`

## Documentation

- `AGENTS.md` — agent and contributor contract
- `docs/recovery/` — recovery and preflight plans
