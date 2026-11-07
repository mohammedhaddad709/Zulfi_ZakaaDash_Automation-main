# Phase 11 — Runtime Verification & Reporting Stabilization

**Date:** 2026-10-16  
**Project root:** `E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation`  
**Git branch:** `master` (uncommitted Phase 11 changes at report time)

---

## 1. Modified / new files

| File | Change |
|------|--------|
| `TestStepReporter.java` | Step runner with duration, metadata attachment, observed Actual results |
| `TestCaseMetadata.java` | Parse `LOGIN-XX-NNN — Title` from TestNG descriptions |
| `TestNGListeners.java` | Start `TestExecutionContext` from parsed test case ID/title |
| `LoginPage.java` | Removed nested Allure `@Step` (avoid duplicate steps); added observations |
| `CredentialProvider.java` | `isConfigured()` for safe HP skip |
| `LoginHappyPathTest.java` | Full step-level reporting; skip when credentials missing |
| `LoginRegressionTest.java` | Full step-level reporting for all regression cases |
| `AllureCliResolver.java` | Auto-discover `tools/allure-2.29.0/bin/allure.bat` |
| `pom.xml` | Maven profile `suite` when `-DsuiteXmlFile=...` is set |
| `login-hp-only-suite.xml` | Isolated LOGIN-HP-001 execution |
| `scripts/install-allure-local.ps1` | Project-local Allure 2.29.0 install (no admin) |
| `.gitignore` | Exclude `tools/allure-*/` |
| `DefectReportingContractTest.java` | Non-UI classification contract checks |

**Not executed:** `OfficialMatchAnalysisHappyPathTest` (Create Analysis protected).

---

## 2. Environment versions

| Component | Version / status |
|-----------|------------------|
| JDK | 21.0.12 |
| Maven | 3.9.16 |
| Git | 2.55.0.windows.3 |
| Chrome | 155.0.8059.80 |
| Selenium | 4.35.0 (Selenium Manager; CDP warning for Chrome 155 — non-blocking) |
| Allure adapters (Maven) | 2.29.1 BOM |
| Allure CLI | **2.29.0** at `tools/allure-2.29.0/bin/allure.bat` |
| Surefire | 3.5.3 + TestNG |
| Portal base URL | `https://zulfi.zakaadash.com/zakaadashZulfi-portal-vue/` (UI loads; HTTP HEAD returned 404) |
| `ZULFI_USER` | **Not set** (User/Machine/process scope checked; values not printed) |
| `ZULFI_PASSWORD` | **Not set** |

---

## 3. Allure CLI status

- **Before Phase 11:** CLI missing; historical runs had `REPORT_NOT_GENERATED.txt`.
- **Action:** `scripts/install-allure-local.ps1` downloaded Allure **2.29.0** zip into `tools/allure-2.29.0/` (compatible with adapter 2.29.x).
- **Discovery verified:** `allure.bat --version` → `2.29.0`; `AllureCliResolver` finds project-local bat without `ALLURE_CLI` when cwd is project root.
- **Override supported:** `ALLURE_CLI`, `-Dreport.allureCli=...`.

---

## 4. Browser lifecycle verification

- `BaseUiTest`: new `GUIDriver` per `@BeforeMethod`, `quitDriver()` in `@AfterMethod`.
- `UnauthenticatedUiTest`: opens Login before each test (no shared authenticated session).
- Login suite: `parallel="false"` (TestNG maps to `none`).
- Regression run: **10 fresh Chrome sessions** (one per `@Test` in `LoginRegressionTest`).
- Full suite run: **11 sessions** (HP + 10 regression methods).
- Listener captures final **PASS/SKIP** screenshots per test when WebDriver exists.

---

## 5. DEMO pacing verification

- Default: `execution.pace=demo` in `webapp.properties`.
- Executions used `-Dexecution.pace=demo`.
- CLI overrides `fast|normal|demo` remain supported via `ExecutionPace` (not re-run for all modes in this phase).
- Pacing integrated in `ElementActions`; business assertions independent of pace.

---

## 6. LOGIN-HP-001 result

| Item | Result |
|------|--------|
| Command | `mvn test -DsuiteXmlFile=src/test/resources/login-hp-only-suite.xml -Dexecution.pace=demo` |
| Maven exit code | **0** |
| TestNG | **SKIPPED** (1 test) |
| Reason | `CONFIGURATION: ZULFI_USER and ZULFI_PASSWORD must be set before LOGIN-HP-001 execution.` |
| Authentication | **Not attempted** (by design when credentials absent) |
| PASS screenshot for HP | **Not captured** (test skipped before login steps) |

**Blocker for HP PASS evidence:** set credentials in the shell, re-run `login-hp-only-suite.xml` only (single login attempt).

---

## 7. Login regression inventory (11 cases)

| ID | Title | Credentials | Expected (summary) | Status in suite run |
|----|-------|-------------|--------------------|---------------------|
| LOGIN-HP-001 | Successful Login | Required | Home + «ابدأ التحليل» | **SKIPPED** (no env creds) |
| LOGIN-UI-001 | Login Form Fields | No | Email + submit visible | **PASSED** |
| LOGIN-UI-002 | Password Masked | No | type=password | **PASSED** |
| LOGIN-UI-003 | Show/Hide Password | No | REQUIRES_BUSINESS_CONFIRMATION | **SKIPPED** |
| LOGIN-UI-004 | Email Interaction | No | Retains typed email | **PASSED** |
| LOGIN-VAL-001 | Empty submit | No | Stays on login | **PASSED** |
| LOGIN-VAL-002 | No email | No | Stays on login | **PASSED** |
| LOGIN-VAL-003 | No password | No | Stays on login | **PASSED** |
| LOGIN-VAL-004 | Invalid email | No | Stays on login | **PASSED** |
| LOGIN-VAL-005 | Missing domain | No | Stays on login | **PASSED** |
| LOGIN-VAL-006 | Missing local part | No | Stays on login | **PASSED** |

Validation cases use synthetic invalid data only (no valid login attempts in regression batch).

---

## 8. Login regression execution results

| Run | Scope | Tests | Failures | Skipped | Maven exit |
|-----|-------|-------|----------|---------|------------|
| A | `LoginRegressionTest` only | 10 | 0 | 1 (UI-003) | **0** |
| B | `login-regression-suite.xml` (HP first) | 11 | 0 | 2 (HP-001, UI-003) | **0** |
| C | `DefectReportingContractTest` | 2 | 0 | 0 | **0** |

Full combined suite **not claimed as “11 passed”** — **9 passed, 2 skipped**.

---

## 9. Expected / Actual reporting

- Each login test calls `TestStepReporter.publishTestMetadata(...)` then numbered `runStep(...)` with **distinct Actual** observations (e.g. URL, field visibility, email value).
- Allure attachments include `Step NN detail` with ACTION / EXPECTED / ACTUAL / STATUS / DURATION.
- Example Actual (LOGIN-VAL-001): `URL=.../auth/login, emailVisible=true, passwordVisible=true, submitVisible=true`.
- Nested duplicate steps from `LoginPage` Allure annotations **removed**.

---

## 10. PASS / FAIL screenshots

| Type | Evidence |
|------|----------|
| PASS (regression) | 27+ PNGs under `test-execution-reports/latest/screenshots/` (step + test-level PASS) |
| FAIL | **None** in this phase (no failing tests) |
| SKIP | e.g. `loginUi003_showHidePassword_SKIP-*.png`, HP skip screenshot when suite includes HP |

---

## 11. Defect-reporting verification

- `DefectReportingContractTest`: duplicate-analysis message → `TEST_DATA_ERROR`; missing credential message → `TEST_DATA_ERROR`.
- Live UI failures: **none** in this phase → **no** `target/bug-reports/*.md` produced (listener writes on throwable only).
- Classification logic reviewed: product defects not inferred without evidence.

---

## 12. Allure HTML

| Item | Value |
|------|--------|
| Generated | **Yes** |
| Latest run ID | `20261016-125834-182` |
| HTML path | `test-execution-reports/runs/20261016-125834-182/allure-html/index.html` |
| Mirror | `test-execution-reports/latest/allure-html/index.html` |
| Pointer | `test-execution-reports/latest-run.properties` → `allureHtmlGenerated=true` |
| Historical runs | Preserved under `test-execution-reports/history/` |

---

## 13. Maven exit codes (summary)

| Command | Exit code |
|---------|-----------|
| `mvn clean test-compile` | **0** |
| `mvn test` (HP-only suite) | **0** (1 skipped) |
| `mvn test -Dtest=LoginRegressionTest` | **0** |
| `mvn test -DsuiteXmlFile=login-regression-suite.xml` | **0** |
| `mvn test -Dtest=DefectReportingContractTest` | **0** |

---

## 14. Create Analysis protection

- `-Danalysis.create.enabled` default **false**.
- **No** `OfficialMatchAnalysisHappyPathTest` execution in Phase 11.
- **No** submit / fixture rotation / backend changes.

---

## 15. Remaining blockers

1. **Credentials** required for LOGIN-HP-001 PASS verification and authenticated lifecycle proof.
2. **LOGIN-UI-003** pending business confirmation.
3. **Exact validation error messages** not asserted (by design until Excel/BA approval).
4. **CDP version warning** for Chrome 155 (informational).
5. Optional: set `ALLURE_CLI` globally or document `install-allure-local.ps1` in README for new machines.

---

## 16. Final readiness

**READY_WITH_BLOCKERS**

- Login regression (non-auth + invalid submit) is **runtime-verified** with Allure HTML and step-level Expected/Actual.
- LOGIN-HP-001 and full **11/11 pass** claim blocked until credentials are configured and HP is re-run once.
- Create Analysis remains **disabled**; await explicit authorization before ANL-OFF-HP-001 or preflight.

**Next step for Mohamed:** In PowerShell, set `ZULFI_USER` / `ZULFI_PASSWORD`, then:

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
.\scripts\install-allure-local.ps1   # if tools/ not present
mvn test "-DsuiteXmlFile=src/test/resources/login-hp-only-suite.xml" "-Dexecution.pace=demo" "-Dreport.open=true"
```

Do **not** run Create Analysis until separately approved.
