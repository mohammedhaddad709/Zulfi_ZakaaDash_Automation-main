# ZakaaDash Advanced Preprocessing Test Architecture

## Business flow

Data Versions → source card → `بدء المعالجة المسبقة` → modal `ابدأ معالجة البيانات` → `معالجة متقدمة` → wizard `معالجة البيانات المتقدمة`.

| # | Wizard step | Title |
|---|-------------|-------|
| 1 | Settings | `إعدادات المعالجة` |
| 2 | Suggestions | `اقتراحات النظام` |
| 3 | Review | `مراجعة البيانات` (DEV renders `مراجعه البيانات`, see verification note) |
| 4 | Improvement | `تحسين البيانات` |
| 5 | Results | `عرض النتائج` → `حفظ` → modal `معالجة البيانات` |

## Layers

```
Page Objects   pages/zakaadash/preprocessing   PreprocessingEntryModal, AdvancedPreprocessingWizard (common),
                                               ProcessingSettingsStep, SystemSuggestionsStep, DataReviewStep,
                                               DataImprovementStep, ProcessingResultsStep, SaveProcessedDataModal
Flows          flows/zakaadash/preprocessing   AdvancedPreprocessingFlow (no locators, no assertions)
               flows/zakaadash                 DataSourcePreparationFlow (unique AUTO_PP_* source via UploadFlow)
Tests          tests/preprocessing/<step>      one package per wizard step
E2E            tests/e2e                       AdvancedPreprocessingJourneyE2ETest
Test data      testdata/PreprocessingCase      typed record read from case.json
```

Rules: tests assert, flows orchestrate, page objects own locators. A step class only knows its own controls.
Each "Next" waits for the destination step: the step is marked active **and** one control unique to that step is visible.
The wizard keeps all five step panels in the DOM and hides the inactive ones (`display:none`), so step locators use
"any visible match" (`WaitUtil.waitForAnyVisible`) instead of the first match.

## Deterministic data

Every case owns a folder `src/test/resources/testdata/preprocessing/<area>/<case>/` with `input.csv` and `case.json`
(`testCaseId`, `title`, `dataset`, `sourceNamePrefix`, `settings`, `expected`).
`DataSourcePreparationFlow` uploads `input.csv` once per case per JVM as `AUTO_PP_<...>_<yyMMddHHmmss>` and reuses that source for the other tests of the same case.
Sources are located through the Data Versions search box (the list is paginated, 10 per page).
The generated name is written to the test result (`Source Name` column in `qa-summary.md` and in each defect candidate).
Existing customer data is never reused. There is no cleanup of generated `AUTO_PP_*` sources (TODO: agree a cleanup approach with the product team).

```
testdata/preprocessing/
  entry/baseline/{input.csv,case.json}
  step1-settings/{data-understanding,data-quality,feature-eligibility,value-transformation,general-feature-preparation}/
  step2-suggestions/   step3-review/   step4-improvement/   step5-results/
  e2e/PP-E2E-001/{input.csv,case.json}
```

JSON is read with Selenium's built-in `org.openqa.selenium.json.Json`. No new JSON or Excel library was added.

## Coverage matrix

| ID | Behaviour | Groups |
|----|-----------|--------|
| PP-ENTRY-001 | Start Preprocessing opens the processing mode modal | feature-preprocessing, pp-entry, smoke, sanity, regression, critical |
| PP-ENTRY-002 | Advanced Processing opens the wizard | feature-preprocessing, pp-entry, smoke, sanity, regression, critical |
| PP-NAV-001 | Step 1 Next → Step 2 | feature-preprocessing, pp-navigation, sanity, regression, critical |
| PP-NAV-002 | Step 2 Next → Step 3 | same |
| PP-NAV-003 | Step 3 Next → Step 4 | same |
| PP-NAV-004 | Step 4 Next → Step 5 | same |
| PP-NAV-005 | Step 5 Save opens the save modal (both save options visible) | same |
| PP-ENTRY-003 | Closing the processing mode modal returns to Data Versions (no wizard) | feature-preprocessing, pp-entry, regression |
| PP-NAV-006 | Previous is disabled on Step 1 | feature-preprocessing, pp-navigation, regression |
| PP-NAV-007 | Previous on Step 2 returns to Step 1 | feature-preprocessing, pp-navigation, regression |
| PP-SET-001 | Step 1 lists the five settings categories in order | feature-preprocessing, pp-settings, regression |
| PP-REV-001 | Step 3 lists every source column in file order | feature-preprocessing, pp-review, regression |
| PP-IMP-001 | Step 4 shows the seven improvement tabs; each opens its panel | feature-preprocessing, pp-improvement, regression |
| PP-SAVE-001 | "إلغاء" on the save modal creates no processed version | feature-preprocessing, pp-results, regression |
| E2E-PP-001 | Steps 1–5, then Save as new copy `AUTO_PP_E2E_<suffix>`, new source listed | e2e, regression, critical |

The step cases added 2026-11-07 (PP-SET/REV/IMP/SAVE) check structure and behaviour verified on DEV only.
Still not covered: per-category Step 1 settings, Step 2–5 content rules — do not invent them until the business
cases are provided. `pp-navigation` is an extra group beyond the requested list.

The E2E never uses `استبدال الملف الأصلي`. It verifies that the copy is listed in Data Versions; it does not verify the processed file content.

## Maven commands (PowerShell — quote every `-D` value)

Run from the project root and add `"-Dactive.system=zakaadash"` to every command; the suite files live in
`src/test/resources/suites/zakaadash/` and are selected with `"-DsuiteXmlFile=src/test/resources/suites/zakaadash/<file>"`
(the `-Dsuite=<file>` form below is the pre-merge shorthand).

| Scope | Command |
|-------|---------|
| Preprocessing feature | `mvn clean test "-Dsuite=testng-preprocessing.xml"` |
| Step 1 | `mvn clean test "-Dsuite=testng-preprocessing.xml" "-Dgroups=pp-settings"` |
| Step 2 | `mvn clean test "-Dsuite=testng-preprocessing.xml" "-Dgroups=pp-suggestions"` |
| Step 3 | `mvn clean test "-Dsuite=testng-preprocessing.xml" "-Dgroups=pp-review"` |
| Step 4 | `mvn clean test "-Dsuite=testng-preprocessing.xml" "-Dgroups=pp-improvement"` |
| Step 5 | `mvn clean test "-Dsuite=testng-preprocessing.xml" "-Dgroups=pp-results"` |
| Entry / navigation | `... "-Dgroups=pp-entry"` / `... "-Dgroups=pp-navigation"` |
| E2E | `mvn clean test "-Dsuite=testng-e2e.xml"` |
| Smoke | `mvn clean test "-Dsuite=testng-smoke.xml"` |
| Sanity | `mvn clean test "-Dsuite=testng-sanity.xml"` |
| Regression | `mvn clean test "-Dsuite=testng-regression.xml"` |

Credentials come from `ZAKAADASH_EMAIL` / `ZAKAADASH_PASSWORD` (environment or JVM property). Add `"-Dheadless=true"` for headless runs.
Step 1–5 group commands fail with "no tests" (`failIfNoTests=true`) until tests tagged with that group exist.

## Adding a step test

1. Create `src/test/resources/testdata/preprocessing/<stepN-...>/<case>/{input.csv,case.json}`.
2. Add a test class in `tests/preprocessing/<step>/` extending the same base as `PreprocessingEntryTests`, groups `{FEATURE_PREPROCESSING, PP_<STEP>, ...}`.
3. Call `AdvancedPreprocessingFlow` to reach the step; put new controls in that step's page object only.
4. Add no suite XML: the package is already included in `testng-preprocessing.xml`.

## Failure reporting

Each defect candidate (`target/qa-summary/defect-candidates/`) carries: last completed step / failed step, preprocessing step, category, dataset, source name, applied configuration, expected result, URL, screenshot, duration.

| Classification | Meaning |
|----------------|---------|
| Potential Product Defect | Stable page, assertion on expected state failed |
| Automation or Product UI Investigation | Locator or wait failed on a loaded page |
| Automation Infrastructure Failure | Missing credentials, login failure, driver failure, 502/503/504 gateway pages |

A 5xx page is never classified as a product defect.

## Testability gaps and verified locators

All preprocessing locators were verified against the DEV DOM on 2026-11-07 (details and evidence:
`ZAKAADASH_DEV_VERIFICATION_2026-11-07.md`). There are still no `data-testid` hooks; current anchors:

| Area | Anchor |
|------|--------|
| Source row | `h3[@title=<source>]` → `button[data-pc-name='accordionheader']` (`aria-expanded`, `aria-controls`), toggle `button[aria-label='accordion-toggle']`, processed versions `h4[@title=<version>]` |
| Processing mode modal | dialog header title `ابدأ معالجة البيانات`; footer `button[aria-label='معالجة متقدمة' / 'معالجة تلقائية']`; close `data-pc-name='pcclosebutton'` |
| Wizard navigation | footer `button[aria-label='التالي' / 'السابق' / 'حفظ']` (a tab scroller also carries `aria-label='التالي'`, so always scope to the footer) |
| Active step | stepper title span (title followed by a subtitle span): current step has class `text-primary`, completed `text-green-600` — **no ARIA state** (recommend `aria-current="step"`) |
| Step 1 / Step 2 | `button[role='tab']` "فهم البيانات" / "تجهيز الميزات للنمذجة" |
| Step 3 / Step 5 | visible `div[data-pc-name='datatable']` rows |
| Step 4 | `h3#cleaning-suggestions-heading` + `button[role='tab']` (`aria-selected`, `aria-controls`) |
| Save modal | header `h2` "معالجة البيانات", option cards by `h3`, input placeholder `ادخل اسما لمصدر البيانات`, footer `حفظ البيانات` / `إلغاء` |

Recommended `data-testid` values remain: `start-preprocessing`, `processing-mode-modal`, `advanced-processing-option`,
`preprocessing-wizard`, `preprocessing-next`, `preprocessing-previous`, `pp-step-<name>`, `processed-data-save`,
`save-as-new-copy`, `processed-source-name`, `save-processed-data`.
