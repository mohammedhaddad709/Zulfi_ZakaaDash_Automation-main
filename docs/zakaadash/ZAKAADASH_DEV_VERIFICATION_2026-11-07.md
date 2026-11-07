# ZakaaDash — DEV verification and stabilisation (2026-11-07)

Environment: `https://app.dev.zakaadash.com/innova-vue` (DEV), Chrome headless 1920x1080, serial runs, shared account
from `ZAKAADASH_EMAIL` / `ZAKAADASH_PASSWORD` (never printed). Every locator below was read from the live DOM on this
date (login page in the browser pane; authenticated pages through the project's own `AuthenticationFlow`).

## Flows and layers

| Flow | Page Objects | Flow class | Tests |
|------|--------------|-----------|-------|
| Authentication | `ZakaaDashLoginPage` | `AuthenticationFlow` | AUTH-001, AUTH-002, AUTH-003 |
| Data Versions | `DataVersionsPage`, `SourceGroup` | `DataVersionsFlow` | DV-001, DV-002 |
| Upload Data | `UploadDataModal` | `UploadFlow` | UPLOAD-001, UPLOAD-002, FILE-001, FILE-002, E2E-DV-001 |
| Preprocessing entry | `PreprocessingEntryModal`, `AdvancedPreprocessingWizard`, `ProcessingSettingsStep` | `AdvancedPreprocessingFlow` (+ `DataSourcePreparationFlow`) | PP-ENTRY-001, PP-ENTRY-002 |
| Wizard navigation | step pages 1–5, `SaveProcessedDataModal` | `AdvancedPreprocessingFlow` | PP-NAV-001…005 |
| Preprocessing + dashboard journey | all of the above + `DashboardCreationModal`, `RecommendedChartSelectionStage`, `ZakaaDashAppShellPage` | `AdvancedPreprocessingFlow`, `DashboardCreationFlow` | E2E-PP-001 |

New test methods (both were listed as "Not implemented" in `zakaadash-test-coverage.md` only because the product
behaviour/control had not been verified): **AUTH-003** required fields block empty submit, **UPLOAD-002** close the
Upload modal without saving.

## Locator and automation fixes (all `AUTOMATION_DEFECT`)

| Area | Before | Now (verified) |
|------|--------|----------------|
| Login error | `[role='alert'], .p-message-error, …` — also matched field validation | `form > [data-pc-name='message'][role='alert']`; field messages read per `.p-formfield` |
| Data Versions | `//*[contains(text(),'مصادر البيانات')]`, any button/link with the CTA text; first page only | `section//h2`, `header//button`; sources found through the search box (list is paginated, 10 per page) |
| Upload modal | label-`following::input`, positional card XPath, close control unknown | `#data-source`, `#description`, `div[role='button'][aria-label=…]`, selected state `aria-label='ملف - محدد'`, `#mainSave`, header `pcclosebutton`; file-analysis cards + success message |
| Processing mode modal | close by `aria-label='Close'` (does not exist) | footer `button[aria-label='معالجة متقدمة'|'معالجة تلقائية']`, close `pcclosebutton` |
| Wizard active step | PrimeVue stepper attributes that the product never renders → `isStepActive` always false | stepper title span class `text-primary` (current) / `text-green-600` (done); tests now assert the active step again |
| Wizard Next/Save | text match anywhere in the dialog | `.p-dialog-footer button[aria-label=…]` (a tab scroller also has `aria-label='التالي'`) |
| Hidden step panels | first-match waits (`visibilityOfElementLocated`) could pick a hidden step's element | `WaitUtil.waitForAnyVisible / waitForAnyClickable` |
| Step 4 | 693-line DOM-walking heuristic | `h3#cleaning-suggestions-heading` + `button[role='tab']` (`aria-selected`, `aria-controls`) |
| Step 5 | heading check that was always true (stepper text), sample cells that default processing drops | visible datatable rows + enabled footer Save; diagnostics only on failure |
| Save modal | label-`following::input`, generic option-card XPath | `h3` option titles, input placeholder, footer `aria-label` |
| Source row | JS heuristics; toggle lookup skipped the real `aria-label='accordion-toggle'` button | header `aria-expanded`/`aria-controls`, `accordion-toggle`, versions `h4[@title]` |
| Profile / logout | 319-line heuristic | `nav button[aria-label='فتح القائمة']` → popover `button[aria-label='تسجيل الخروج']` |
| Dashboard stars | stars found only by `aria-label='إضافة للمفضلة'`; once starred the product renames the switch to `إزالة من المفضلة`, so starred charts became invisible to the code (run failed with "Could not activate any chart star", counter already 8/8) | both labels accepted; state still read from `aria-checked` |
| Dashboard save (never reached before) | save-error script redeclared the shared `clean` helper (`Identifier 'clean' has already been declared`); confirmation script called the helper `saveButton()` without loading the helpers | both run through the shared helpers |
| Logout after dashboard save | the success toast covers the avatar (RTL top-left) → click intercepted | wait for `.p-toast-message` to auto-dismiss before opening the profile menu; logout now goes through `AuthenticationFlow.logout()` |
| Waits | `waitUntilTrue(cond, message)` set the message on the shared wait permanently | fresh wait per message |
| Headless | `maximize()` is ignored headless → 784x497 (tablet layout) | `zakaadash.headless.window.width/height` (1920x1080) |
| Evidence | screenshot only on failure; Allure ignored `-Doutput.dir` | `<ID>_pass_*.png` on PASS as well; pass `-Dallure.results.directory` |

Page-object size: SourceGroup 528→185, AppShell 319→42, Step 4 693→85, Step 5 182→81 lines.

## Run results

Per-flow runs while stabilising (headless, serial, DEV):

| Run | Tests | Result | Notes |
|-----|-------|--------|-------|
| Authentication | AUTH-001…003 | 3/3 PASS | |
| Data Versions + Upload | DV-001/002, UPLOAD-001/002, FILE-001/002 | 6/6 PASS | |
| Preprocessing entry | PP-ENTRY-001/002 | 2/2 PASS | |
| Wizard navigation | PP-NAV-001…005 | 5/5 PASS | |
| E2E (1st) | E2E-DV-001, E2E-PP-001 | 1/2 | E2E-PP-001 failed at chart star — AUTOMATION_DEFECT (starred switch renamed), fixed |
| E2E-PP-001 rerun 1 | | FAIL | latent JS redeclaration at dashboard save — AUTOMATION_DEFECT, fixed |
| E2E-PP-001 rerun 2 | | FAIL | toast covered avatar at logout — AUTOMATION_DEFECT, fixed |

Final consolidated run — whole generated suite `suites/zakaadash.xml`, `-Dactive.system=zakaadash`, LocalHeadless:
**19 run, 19 passed, 0 failed, 0 skipped** (BUILD SUCCESS). `FootballPreprocessingJourneyE2ETest` is disabled by
design and `HerokuLoginTest` is not part of the ZakaaDash suite.

Evidence (git-ignored, local): `target/zd-out/full/allure-results`, Allure HTML `target/zd-out/full/allure-html`,
19 PASS screenshots in `target/zd-out/full/screenshots`, `target/zd-out/full/qa-summary/qa-summary.md`, log
`target/zd-out/full.log`. DEV data created by that run: `AUTO_PP_E2E_SRC_261107222530` → processed copy
`AUTO_PP_RESULT_261107222522` → dashboard "تحليل العائد المالي لأندية كرة القدم_261107222522";
`AUTO_PP_BASELINE_261107223015`. Earlier stabilisation runs created further `AUTO_PP_*` sources (no cleanup exists).

## Additional cases (added on request, 2026-11-07)

19 cases derived from behaviour verified on DEV (not from an approved Excel — confirm with the product owner):
AUTH-004…007, DV-003/004, UPLOAD-003…006, FILE-003/004, PP-ENTRY-003, PP-NAV-006/007, PP-SET-001, PP-REV-001,
PP-IMP-001, PP-SAVE-001. New page object `ForgotPasswordPage`; new classes `ProcessingSettingsTests`,
`DataReviewTests`, `DataImprovementTests`, `ProcessingResultsTests`; fixture `uploads/invalid-files/zd_unsupported_type.txt`.
One automation fix while adding them: Step 3 header cells scrolled out of the wide table are "not displayed" to
Selenium, so column names are read with `textContent` from the visible table.

### Run after adding the 19 cases

Whole suite `suites/zakaadash.xml` (15 classes, 38 executable tests), LocalHeadless, serial:
**38 run — 35 passed, 3 failed.**

| Failed test | Classification | Detail |
|-------------|----------------|--------|
| UPLOAD-005 | PRODUCT_DEFECT (ZD-DEF-01) | Save enabled with only a name |
| FILE-004 | PRODUCT_DEFECT (ZD-DEF-02) | .txt accepted as a successful upload |
| FILE-002 | ENVIRONMENT_ERROR | Right after FILE-003 saved a file, the Data Versions list stayed behind the app's loading overlay for more than the 20 s explicit wait (screenshot shows the overlay and an empty list). Previously always well under 20 s. Fix: the Data Versions load overlay is now bounded by `zakaadash.page.load.timeout` (60 s). Upload module rerun: FILE-002 PASS; only UPLOAD-005 and FILE-004 fail. |

Evidence: `target/zd-out/full2/` (allure-results, allure-html, 38 screenshots, qa-summary with 3 defect candidates)
and `target/zd-out/upload-rerun/`. DEV data from that run: `AUTO_PP_E2E_SRC_261107225438` (+ processed copy and
dashboard `…_261107225429`), `AUTO_PP_BASELINE_261107225850`, `AUTO_UPLOAD_SAVE_*` from FILE-003.

## Product defects (failing tests)

| ID | Test | Severity | Expected | Actual (DEV 2026-11-07) | Classification |
|----|------|----------|----------|-------------------------|----------------|
| ZD-DEF-01 | UPLOAD-005 | Medium | Save stays disabled while the required "اختر مصدر البيانات *" / "رفع البيانات *" are empty (it is disabled on an empty form and again after a file is removed) | With only a source name, Save is enabled; clicking it (checked manually, not by the test) shows a raw server error that exposes an internal path: `ملف البيانات التحليلية غير موجود: /home/ubuntu/Knowage-Server-CE-dev/resources/DEFAULT_TENANT/dataset/files/temp/_metadata.json`. No source is created. | PRODUCT_DEFECT (validation + information disclosure) |
| ZD-DEF-02 | FILE-004 | Medium | A .txt file is rejected (the drop zone says "يمكنك رفع ملفات CSV أو Excel أو JSON"; the input accepts `.csv, .xls, .xlsx, .json, .xml`) | The .txt file is reported "تم رفع الملف بنجاح" / "تم الرفع بنجاح" and Save is enabled (the OS picker filter is the only guard; drag-and-drop uses the same path) | PRODUCT_DEFECT (server-side type validation missing) |

## Product observations (not failing any test)

None of the originally approved cases fails because of the product (the two product failures are in cases added on
request, see "Product defects"). These were seen on DEV and need a product decision; none is asserted because no case
covers it (do not turn them into tests without a business case):

| ID | Severity | Expected | Actual (DEV 2026-11-07) | Classification |
|----|----------|----------|-------------------------|----------------|
| ZD-OBS-01 | Low (copy) | Wizard step 3 title `مراجعة البيانات` (preprocessing-test-architecture.md) | Stepper shows `مراجعه البيانات`; step heading `اقتراحات مراجعه اعمدة البيانات` | PRODUCT_DEFECT candidate (spelling) |
| ZD-OBS-02 | Low (UX) | Required-field message after the user interacts or submits | Upload modal opens with `الحقل اسم مصدر البيانات مطلوب` already shown (`aria-invalid="true"`) | needs product confirmation |
| ZD-OBS-03 | Low (data) | One entry per department | Dashboard "اختر القسم" lists both `الشئون القانونية` and `الشؤون القانونية` | PRODUCT_DEFECT candidate (duplicate option) |
| ZD-OBS-04 | Low (copy) | `حدد أهدافك` | Dashboard step 2 label `حدد اهدافك` | PRODUCT_DEFECT candidate (spelling) |
| ZD-OBS-05 | Unknown | Business rules for Step 2 suggestions | Baseline CSV (no missing values, no duplicates) gets every rule enabled, e.g. "حذف عمود أغلبه فارغ" and "حذف الصفوف المكررة بالكامل" list all 9 columns as affected | INCONCLUSIVE — Step 2 business cases needed |
| ZD-OBS-06 | — | Search by full source name finds the source | Once (while another session used the same account) the full-name search returned "لم يتم العثور على نتائج"; not reproducible in 4 later tries | INCONCLUSIVE; `waitForSource` now fails with an explicit message if it recurs |

## Gaps (not automated — no approved cases)

- Step 1–5 business rules per category (packages `tests/preprocessing/{settings,suggestions,review,improvement,results}` stay empty by design).
- "معالجة تلقائية" (automatic processing) path.
- Upload: Google Drive and Database sources, description, "إعدادات متقدمة", "معاينة البيانات", invalid/oversized files.
- Data Versions: file-type and date filters, pagination, row option menus, "عرض لوحة البيانات".
- Dashboard creation outside E2E-PP-001; the dashboard pages still carry assertions inside page objects and heavy JS (left as is — they pass; candidate for a later clean-up).
- No cleanup of generated `AUTO_PP_*` sources, processed copies and dashboards (DEV already holds dozens).
- No `data-testid`; the wizard stepper exposes no ARIA state (recommend `aria-current="step"`).
- Deferred football oracle (`FootballPreprocessingJourneyE2ETest`, disabled); its helpers (`FootballDuplicateRowRemoval`, `FootballDatasetVerifier`, `CsvDataTable`, `FootballExpectedResults`, `*BlockedException`) are currently unused.
