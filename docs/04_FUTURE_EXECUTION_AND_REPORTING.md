# Execution model and reporting acceptance

## Login-feature tests (future)

- Fresh WebDriver for each test.
- No automatic login in setup.
- Test may call LoginPage methods; must assert actual observed state.
- Driver quit after case; no leaked localStorage/cookies/session across tests.

## Protected-feature tests (future)

- Fresh WebDriver for each test.
- Read `ZULFI_USER` and `ZULFI_PASSWORD` from environment for that test.
- Visit Login, authenticate, verify Home readiness.
- Execute exactly one test method; quit driver and invalidate session.
- Next test repeats fresh browser + login — never relies on prior case.

## Reports after actual test execution

- New results folder per local Maven invocation; no old-result contamination.
- Latest: `test-execution-reports/latest/allure-html/index.html` (single-file if locally supported).
- History: `test-execution-reports/history/<runId>/`.
- Raw: `target/allure-results` and copied to history.
- Screenshots: captured after SoftAssert final result; current run in `target/screenshots` and archived.
- Video OFF by default; optional `-Dreport.video=ON_FAILURE` or `ALWAYS`, only with permission and careful privacy checks.
- CLI/browser failures produce warnings without changing the test outcome.
- If no `@Test` case exists (current stage), HTML report is not expected.

## Important limits

- The supplied TAF source includes Allure 2.29.1, NOT Allure 3. Preserve that baseline unless a separate upgrade is approved.
- HTML generation requires compatible Allure CLI (`allure.bat`) with `--single-file`; do not claim it was tested without running.
- Core reporting in this scaffold is Allure + history. A custom "Full Execution Report" can be added as a separate approved module, without faking execution content.
