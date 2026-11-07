# Login Feature — readiness contract (zero test cases)

## DOM selectors grounded in user-provided login HTML

| UI piece | Stable locator candidate | Purpose |
|---|---|---|
| Email | `By.id("email")` | Email entry |
| Password | `By.id("password")` | Masked password entry |
| Form | `form[data-pc-name='form']` | Login readiness |
| Submit button | `form button[type='submit'][aria-label='تسجيل الدخول']` | Perform login |
| Home readiness | `button[aria-label='ابدأ التحليل']` | Confirm authenticated Home |

Dynamic Vue/PrimeVue attributes like `pc40`, `pc41` should never be used for locators.

## Login infrastructure only

- `LoginPage.open()` / `.awaitReady()` / `.enterEmail()` / `.enterPassword()` / `.submit()`.
- `LoginFlow.loginAsConfiguredUser()` uses environment credentials, and waits for Home readiness.
- `LoginFeatureFixture` is abstract and intentionally contains no test methods.
- New future Login tests inherit `UnauthenticatedUiTest` and start from an unauthenticated state.

## Important contradiction resolved

"Login before every Test Case" is correct for tests of features that **require authenticated access**. It is not appropriate before a Login test, where an unauthenticated starting state is often the core precondition. For special login tests such as authenticated-user revisit, the test explicitly creates its own authenticated state after fresh browser setup.

## Not implemented yet

No valid/invalid login tests, no negative tests, no UI assertions, no data providers, no Excel parser integration, no hardcoded expected backend behavior. Await user Excel and feature sign-off.
