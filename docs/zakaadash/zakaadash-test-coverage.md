# ZakaaDash UI Test Coverage

## Scope boundary

Login → Data Versions → Open Upload Data modal → Select File source → Attach `zd_baseline_valid.csv` → UI recognizes the CSV → **STOP**.

Out of scope: clicking Save, creating a data source, dashboards, AutoML, RAG.

Preprocessing is a separate feature; see [preprocessing-test-architecture.md](preprocessing-test-architecture.md). Its setup step saves a unique `AUTO_PP_*` source through `DataSourcePreparationFlow`.

## Layers

```
Page Objects   pages/zakaadash      ZakaaDashLoginPage, DataVersionsPage, UploadDataModal
Flows          flows/zakaadash      AuthenticationFlow, DataVersionsFlow, UploadFlow
Feature tests  tests/auth, tests/dataversions, tests/upload
E2E tests      tests/e2e            DataUploadJourneyE2ETest (reuses the three flows)
```

Every test starts its own browser and builds its own prerequisites through flows. No test depends on another test or on execution order.

## Feature hierarchy and classification

| Test ID | Scenario | Feature | Class | Priority | Smoke | Sanity | Regression | E2E | Dataset | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| AUTH-001 | Valid login reaches Data Versions | Authentication | AuthenticationTests | Critical | YES | YES | YES | Covered | - | Implemented |
| AUTH-002 | Invalid credentials rejected | Authentication | AuthenticationTests | Critical | NO | NO | YES | NO | - | Implemented (error locator verified on DEV 2026-11-07) |
| AUTH-003 | Required fields block empty submit | Authentication | AuthenticationTests | High | NO | NO | YES | NO | - | Implemented 2026-11-07 (field-level messages verified on DEV) |
| DV-001 | Access Data Versions | Data Versions | DataVersionsTests | Critical | YES | YES | YES | Covered | - | Implemented |
| DV-002 | Upload CTA opens Upload modal | Data Versions | DataVersionsTests | Critical | YES | YES | YES | Covered | - | Implemented |
| UPLOAD-001 | Modal shows required controls | Upload | UploadModalTests | Critical | NO | YES | YES | Covered | - | Implemented |
| UPLOAD-002 | Close modal without saving | Upload | UploadModalTests | High | NO | NO | YES | NO | zd_baseline_valid.csv | Implemented 2026-11-07 (header close, no prompt; search proves no source created) |
| FILE-001 | Select File source | Upload | FileSelectionTests | Critical | NO | YES | YES | Covered | - | Implemented |
| FILE-002 | Attach and recognize valid CSV | Upload | FileSelectionTests | Critical | NO | YES | YES | Covered | zd_baseline_valid.csv | Implemented (also checks success message and 20 rows / 9 columns from the fixture contract) |
| AUTH-004 | Password eye icon shows/hides the password | Authentication | AuthenticationTests | Low | NO | NO | YES | NO | - | Added 2026-11-07 |
| AUTH-005 | Forgot-password link opens the reset page (not submitted) and returns to login | Authentication | AuthenticationTests | High | NO | NO | YES | NO | - | Added 2026-11-07 |
| AUTH-006 | Data Versions URL without a session redirects to login (`redirect=/data-versions`) | Authentication | AuthenticationTests | Critical | NO | NO | YES | NO | - | Added 2026-11-07 |
| AUTH-007 | Logout ends the session (Data Versions redirects to login afterwards) | Authentication | AuthenticationTests | Critical | NO | NO | YES | NO | - | Added 2026-11-07 |
| DV-003 | Search by source name lists only that source | Data Versions | DataVersionsTests | High | NO | NO | YES | NO | PP-BASELINE input.csv | Added 2026-11-07 |
| DV-004 | Search without matches shows the no-results message | Data Versions | DataVersionsTests | High | NO | NO | YES | NO | - | Added 2026-11-07 |
| UPLOAD-003 | Source cards stay disabled until a source name is entered | Upload | UploadModalTests | High | NO | NO | YES | NO | - | Added 2026-11-07 |
| UPLOAD-004 | Source name is limited to 50 characters (`maxlength`) | Upload | UploadModalTests | Low | NO | NO | YES | NO | - | Added 2026-11-07 |
| UPLOAD-005 | Save stays disabled until data source and file are provided (both marked `*`) | Upload | UploadModalTests | High | NO | NO | YES | NO | zd_baseline_valid.csv | Added 2026-11-07 — **fails: PRODUCT_DEFECT ZD-DEF-01** |
| UPLOAD-006 | Removing the attached file restores the drop zone and disables Save | Upload | UploadModalTests | High | NO | NO | YES | NO | zd_baseline_valid.csv | Added 2026-11-07 |
| FILE-003 | Saving a valid CSV creates an original source ("الملف الأصلي", 0 processed) | Upload | FileSelectionTests | Critical | NO | YES | YES | NO | zd_baseline_valid.csv | Added 2026-11-07 (creates `AUTO_UPLOAD_SAVE_*`) |
| FILE-004 | Unsupported file type (.txt) is rejected | Upload | FileSelectionTests | High | NO | NO | YES | NO | invalid-files/zd_unsupported_type.txt | Added 2026-11-07 — **fails: PRODUCT_DEFECT ZD-DEF-02** |
| E2E-DV-001 | Login → CSV recognized | Cross-feature | DataUploadJourneyE2ETest | Critical | NO | NO | YES | YES | zd_baseline_valid.csv | Implemented |

Priority is carried by Allure `@Severity`: Critical = `CRITICAL`, High = `NORMAL`, Low = `MINOR`.
Cases marked "Added 2026-11-07" were derived from behaviour verified on DEV (required markers, the modal's own
supported-types text, `maxlength`, button states, fixture contract). They are not from an approved Excel; confirm them
with the product owner.

## TestNG groups

- Feature groups: `feature-auth`, `feature-data-versions`, `feature-upload` (DV-002 carries both Data Versions and Upload).
- Execution groups: `smoke`, `sanity`, `regression`, `e2e`, `critical`.

`critical` is a priority marker, not a suite. AUTH-002 and FILE-002 are critical but not smoke.

## Running tests

Every command targets one system: always pass `-Dactive.system=zakaadash`. The per-feature suites live in
`src/test/resources/suites/zakaadash/`; the whole generated suite is `suites/zakaadash.xml`.
Pass `-Doutput.dir` **and** `-Dallure.results.directory` so evidence of one run stays together (Allure itself only
honours `allure.results.directory`).

| Goal | Command |
|---|---|
| Whole suite | `mvn test "-Dsurefire.suiteXmlFiles=suites/zakaadash.xml" "-Dactive.system=zakaadash"` |
| Authentication only | `mvn test "-DsuiteXmlFile=src/test/resources/suites/zakaadash/testng-auth.xml" "-Dactive.system=zakaadash"` |
| Data Versions only | `... testng-data-versions.xml ...` |
| Upload only | `... testng-upload.xml ...` |
| Preprocessing only | `... testng-preprocessing.xml ...` |
| E2E only | `... testng-e2e.xml ...` |
| Smoke / Sanity / Regression | `... testng-smoke.xml` / `testng-sanity.xml` / `testng-regression.xml ...` |
| Single class | `mvn test "-Dtest=FileSelectionTests" "-Dactive.system=zakaadash"` |
| Headless | add `"-DexecutionType=LocalHeadless"` (window forced to `zakaadash.headless.window.width/height`, default 1920x1080) |
| Credentials check (no browser) | `mvn test "-Dtest=ZakaaDashCredentialsTest" "-Dactive.system=zakaadash"` |

Tester workflow: new deployment → Smoke; small fix → affected feature suite; release candidate → Regression;
cross-feature validation → E2E. ZakaaDash suites run serially (one shared DEV account); `testng-parallel.xml` only
covers the non-ZakaaDash Heroku sample.

## Credentials

`ZAKAADASH_EMAIL` / `ZAKAADASH_PASSWORD`, resolved from JVM system property, then environment variable, then the
git-ignored `config/credentials.local.properties`; otherwise the test fails fast.
Prefer session-only environment variables:

```powershell
$env:ZAKAADASH_EMAIL = "<email>"
$env:ZAKAADASH_PASSWORD = "<password>"
```

## Reports

- Allure results: `<allure.results.directory>` (Epic `ZakaaDash`, Feature, Story, TMS link = Test Case ID, Severity, business steps, screenshot + failure context).
- QA summary: `<output.dir>/qa-summary/qa-summary.md` (Test Case ID, Feature, Priority, Execution Type, Test Type, Dataset, Status, Duration, Failure Step).
- Screenshots: `<output.dir>/screenshots` — `<ID>_pass_*.png` on PASS and `<ID>_failure_*.png` on FAIL (also attached to Allure).

## Test data

`src/test/resources/test-data/zakaadash/uploads/baseline/zd_baseline_valid.csv`: 20 rows, 9 columns
(`record_id, customer_name, city, order_date, category, quantity, unit_price, total_amount, is_active`), no missing values, no duplicates.
Reserved folders for future cases: `missing-values, duplicates, date-time, data-types, categorical, numeric, outliers, invalid-files`.

## Testability gaps

- No `data-testid` on these screens. Locators were re-verified on DEV 2026-11-07 and now use stable product
  attributes: login `#email` / `#password` / `form > [data-pc-name='message'][role='alert']`; Data Versions header CTA,
  `h2` "مصادر البيانات" and the search box (the list is paginated 10 per page, so sources are located by search);
  Upload modal `#data-source`, `#description`, `div[role='button'][aria-label='ملف' | 'Google Drive' | 'قاعدة بيانات']`
  (selected card becomes `aria-label='ملف - محدد'`), `#mainSave`, header close `data-pc-name='pcclosebutton'`.
- Upload modal: the required-name message is already shown when the modal opens, before any input (see
  `ZAKAADASH_DEV_VERIFICATION_2026-11-07.md`).
- The attached-file remove button has no `aria-label` (only the `pi-trash` icon is a hook).
- Not covered (no approved cases): Google Drive / Database sources, description, "إعدادات متقدمة", "معاينة البيانات",
  Data Versions filters (file type, date range), pagination and row menus.
