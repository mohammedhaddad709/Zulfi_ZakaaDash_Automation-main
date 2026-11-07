# TAF source audit — TAF-master(3).zip

This assessment is based on the actual source archive, not assumptions.

## Source features retained (or deliberately adapted)

| Source component | Foundation handling |
|---|---|
| Browser factory (`Browser`, `ChromeFactory`, `EdgeFactory`, `FirefoxFactory`, `SafariFactory`) | Retained; default Chrome; sample CRX removed |
| `GUIDriver`, `ThreadGuard` | Retained; explicit per-method creation and `ThreadLocal.remove()` cleanup |
| Element, browser, frame and alert actions | Retained; removed raw typed-value logging |
| `WaitManager` | Retained; new LoginPage uses explicit condition waits |
| BaseAssertion / Verification / Validation | Retained; SoftAssert now isolated per thread and reset on result |
| `ExcelReader`, JSON/Property readers | Retained for future Excel cases; no Excel cases today |
| `TestNGListeners` | Retained and adapted for screenshots after soft assertion finalization |
| `ScreenshotsManager` | Retained; real viewport screenshot, unique name, Allure attachment |
| `ScreenRecordManager` | Retained as optional, opt-in recording; attach verified path when available |
| Allure TestNG | Retained at original pinned version 2.29.1; raw output and HTML archiving adapted |
| Allure report generation | Original dynamic GitHub auto-download removed; explicit installed CLI, graceful warnings |
| REST Assured dependency | Preserved for future API features; domain example API class removed |
| Original domain pages/tests | Removed (automationexercise.com cases are not relevant to Zulfi) |

## High-risk bugs corrected

1. Original `Validation` shared one static SoftAssert across methods/threads.
2. Original input action logged the actual text passed to `sendKeys` including passwords.
3. Original listener captured PASS screenshot before `Validation.assertAll()` converted result to FAIL.
4. Original `afterInvocation` checked `instanceof UITest` even though `UITest` is an annotation (not an interface).
5. Original video attachment guessed file path without actual `.mp4` resolution.
6. Original Allure CLI downloader fetched latest version dynamically and could block local runs.
7. Original report paths with absolute separator construction could resolve incorrectly on non-Windows systems.
8. Original Excel reader does not close its workbook; future Excel ingestion must use a safe reader implementation before imports.

## Source compatibility limitations / confirmation gates

- Apache Maven is not installed in the assistant's runtime; `mvn test-compile` was NOT executed here. Must run locally in Cursor.
- Browser login against the private target application is NOT executed; fixtures are unverified runtime scaffolds.
- Allure 2 `--single-file` CLI compatibility needs verification against the locally installed version.
- Screen recorder integration is optional and requires a local functional test when enabled.
- The supplied login DOM does not prove exact backend behavior, login error UX, rate limiting, or session policy; those require future stories/Excel.
- JDK 21 is available in assistant runtime but dependencies were not resolved.

## Attribution

User-provided original TAF is identified in its README as `Ashraaf7/TAF`, distributed under the MIT License according to that README. Preserve any applicable license notices when publishing redistributed derivatives.
