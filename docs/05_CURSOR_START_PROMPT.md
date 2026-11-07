# CURSOR MASTER HANDOFF — Foundation Phase only

You are a Principal QA Automation Architect working on a brand-new Selenium/TestNG project.

Project root: `D:\Football_Analysis_Auto`.
Reference: `TAF-master(3).zip` is the required target design. The current foundation was adapted from the actual source in that archive.

## User instructions, strictly interpreted

1. Rebuild cleanly from scratch and retain the reusable TAF capabilities, structure and standards.
2. Only prepare **Login** as a feature; **DO NOT write any Test Cases** yet.
3. Once a new feature and Excel file are supplied, implement its **Happy Path FIRST**, then the user-approved Excel cases.
4. For every FUTURE authenticated-feature test method, open a fresh browser, login, execute one case, finish, quit and repeat for the next test. Login tests themselves start unauthenticated.
5. Keep Selenium + TestNG + Java 21 + Maven + TAF actions/driver factories + Page Objects + Flows + data readers + listeners + screenshot/video + Allure.
6. Do not hardcode credentials, disable security assertions, or invent product rules.

## Mandatory audit before edits

- Read `AGENTS.md`, `README.md`, and `docs/*`.
- Compare code with supplied original TAF source and identify any retained functionality gaps.
- Validate Maven POM and Java test compilation on Windows JDK 21.
- Check `mvn test-compile` output and Maven exit code.
- Check that there are **zero methods annotated `@Test`** and no old project-domain classes in the active source tree.
- Verify inherited `@BeforeMethod` ordering (fresh browser then Login setup, only for protected features) using framework inspection. Do NOT add a sample test without approval.
- Verify config priority: `-DbaseUrlWeb` > `ZULFI_BASE_URL` > default, and credentials from env only.
- Ensure `src/main/resources/META-INF/services/org.testng.ITestNGListener` still registers the lifecycle listener.
- Review screenshots, single-file Allure CLI compatibility and attachment routing; mark runtime-unverified capabilities clearly.

## Implementation restrictions

- No new Login test cases, no example assertions, no smoke suites, no analysis feature logic or other Page Objects.
- Do not import arbitrary earlier feature tests from previous projects.
- Do not assert future app behavior not evidenced by DOM or approved stories.
- Preserve TAF architecture and use minimal maintainable changes.
- Run only non-destructive checks; do not launch production transactions.

## Deliverable

1. Compiled framework foundation, actual Maven command/exit code.
2. Login Page/Flow/Test fixture ready, still zero tests.
3. Clean project tree and strict future feature lifecycle.
4. Allure and screenshot evidence architecture validated or explicitly pending runtime verification.
5. Exact unresolved blockers, with file/line and reproducible steps.

STOP. Await user Excel test-case file before implementing ANY test.
