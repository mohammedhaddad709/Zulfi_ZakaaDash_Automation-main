package com.taf.systems.zulfi.features.login;

import org.openqa.selenium.By;
import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.login.LoginTestIds;
import com.taf.systems.zulfi.pages.LoginPage;
import com.taf.systems.zulfi.reporting.TestCaseReport;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.function.Consumer;

@Epic("Zulfi Portal")
@Feature("Login")
public class LoginRegressionTest extends LoginFeatureFixture {

    private static final String FEATURE = LoginTestIds.FEATURE;
    /** Synthetic values only: never a real account. The reserved .invalid TLD cannot be a registered user. */
    private static final String INVALID_EMAIL = "taf.invalid.user@example.invalid";
    private static final String INVALID_PASSWORD = "Invalid-Passw0rd!";
    private static final String PRECONDITIONS =
            "Fresh browser; user logged out; login page opened by test fixture.";

    @Test(description = "LOGIN-UI-001 — Verify Login Form Fields")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_UI_001,
            title = "Verify Login Form Fields",
            feature = LoginTestIds.FEATURE,
            description = "Login page exposes email and submit controls.",
            testObjective = "Confirm primary login controls are visible.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "Email input and submit button visible.")
    public void loginUi001_verifyLoginFormFields() {
        publish(LoginTestIds.LOGIN_UI_001, "Verify Login Form Fields", "Confirm primary login controls are visible.");
        LoginPage login = new LoginPage(browser());
        TestStepReporter.runStep(
                "Verify login form fields",
                "Inspect email field and submit button",
                "Email input and submit button are visible on the login page.",
                () ->
                        login.isDisplayed() && login.isSubmitVisible()
                                ? StepResult.pass(login.loginPageObservation())
                                : StepResult.fail(login.loginPageObservation()),
                browser(),
                "LOGIN-UI-001");
        ScreenshotsManager.takeFullPageScreenshot(browser(), "LOGIN-UI-001_PASS");
    }

    @Test(description = "LOGIN-UI-002 — Password Masked by Default")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_UI_002,
            title = "Password Masked by Default",
            feature = LoginTestIds.FEATURE,
            description = "Password field uses masked input type.",
            testObjective = "Confirm password field uses password masking.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "Password input type is password.")
    public void loginUi002_passwordMaskedByDefault() {
        publish(LoginTestIds.LOGIN_UI_002, "Password Masked by Default", "Confirm password field uses password masking.");
        LoginPage login = new LoginPage(browser());
        TestStepReporter.runStep(
                "Verify password masking",
                "Read password input type attribute",
                "Password field type is password (masked).",
                () ->
                        login.isPasswordMasked()
                                ? StepResult.pass("password type=password")
                                : StepResult.fail("password type="
                                        + browser().findElement(By.id("password"))
                                                .getAttribute("type")),
                browser(),
                "LOGIN-UI-002");
        ScreenshotsManager.takeFullPageScreenshot(browser(), "LOGIN-UI-002_PASS");
    }

    @Test(description = "LOGIN-UI-003 — Show / Hide Password")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_UI_003,
            title = "Show / Hide Password",
            feature = LoginTestIds.FEATURE,
            description = "PrimeVue password visibility toggle.",
            testObjective = "Verify PrimeVue password eye toggles masking without changing entered value.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "Password toggles text/password; value length preserved.")
    public void loginUi003_showHidePassword() {
        publish(
                LoginTestIds.LOGIN_UI_003,
                "Show / Hide Password",
                "Verify PrimeVue password eye toggles masking without changing entered value.");
        LoginPage login = new LoginPage(browser());
        final String syntheticPassword = "SynTh3tic-Pw-UI003";
        final int expectedLength = syntheticPassword.length();

        if (!login.isPasswordVisibilityTogglePresent()) {
            throw new SkipException(
                    "Password visibility toggle (PrimeVue svg unmask/mask icon) not found in DOM");
        }

        TestStepReporter.runStep(
                "Verify Password Is Masked by Default",
                "Read #password input type on load",
                "Password input type is password.",
                () ->
                        login.isPasswordMasked()
                                ? StepResult.pass("passwordType=" + login.getPasswordInputType())
                                : StepResult.fail("passwordType=" + login.getPasswordInputType()),
                browser(),
                "LOGIN-UI-003");

        TestStepReporter.runStep(
                "Enter Synthetic Password",
                "Type non-secret synthetic password (value redacted in report)",
                "Password field retains entered length without submit.",
                () -> {
                    login.enterPassword(syntheticPassword);
                    return login.passwordValueLength() == expectedLength
                            ? StepResult.pass("valueLength=" + login.passwordValueLength())
                            : StepResult.fail("valueLength=" + login.passwordValueLength());
                },
                browser(),
                "LOGIN-UI-003");

        TestStepReporter.runStep(
                "Toggle Password Visibility ON",
                "Click PrimeVue eye icon (data-pc-section unmaskicon/maskicon)",
                "Password input type becomes text.",
                () -> {
                    login.clickPasswordVisibilityToggle();
                    return login.isPasswordVisible()
                            ? StepResult.pass(login.passwordToggleObservation())
                            : StepResult.fail(login.passwordToggleObservation());
                },
                null,
                "LOGIN-UI-003");

        TestStepReporter.runStep(
                "Verify Password Is Visible and Value Preserved",
                "Observe input type and value length after first toggle",
                "Type is text; value length unchanged.",
                () ->
                        login.isPasswordVisible() && login.passwordValueLength() == expectedLength
                                ? StepResult.pass(
                                        "passwordType=text; valueLength="
                                                + login.passwordValueLength())
                                : StepResult.fail(login.passwordToggleObservation()),
                null,
                "LOGIN-UI-003");

        TestStepReporter.runStep(
                "Toggle Password Visibility OFF",
                "Click eye icon again",
                "Password input type returns to password.",
                () -> {
                    login.clickPasswordVisibilityToggle();
                    return login.isPasswordMasked()
                            ? StepResult.pass(login.passwordToggleObservation())
                            : StepResult.fail(login.passwordToggleObservation());
                },
                browser(),
                "LOGIN-UI-003");

        TestStepReporter.runStep(
                "Verify Password Is Masked Again",
                "Confirm masking restored and length preserved",
                "Type is password; value length unchanged; still on login page.",
                () ->
                        login.isPasswordMasked()
                                        && login.passwordValueLength() == expectedLength
                                        && login.remainsOnLoginPage()
                                ? StepResult.pass(login.passwordToggleObservation())
                                : StepResult.fail(login.passwordToggleObservation()),
                browser(),
                "LOGIN-UI-003");

        Assert.assertTrue(login.isPasswordMasked(), "Password must be masked after second toggle");
        Assert.assertEquals(login.passwordValueLength(), expectedLength, "Password length must be preserved");
        ScreenshotsManager.takeFullPageScreenshot(browser(), "LOGIN-UI-003_PASS");
    }

    @Test(description = "LOGIN-UI-004 — Email Input Interaction")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_UI_004,
            title = "Email Input Interaction",
            feature = LoginTestIds.FEATURE,
            description = "Email field retains user input.",
            testObjective = "Confirm email field retains typed value.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "Typed email remains in #email.")
    public void loginUi004_emailInputInteraction() {
        publish(LoginTestIds.LOGIN_UI_004, "Email Input Interaction", "Confirm email field retains typed value.");
        LoginPage login = new LoginPage(browser());
        TestStepReporter.runStep(
                "Type sample email",
                "Enter qa.interaction@example.com into #email",
                "Email field displays the entered value.",
                () -> {
                    login.enterEmail("qa.interaction@example.com");
                    String actual = login.emailValue();
                    return "qa.interaction@example.com".equals(actual)
                            ? StepResult.pass("email value=" + actual)
                            : StepResult.fail("email value=" + actual);
                },
                browser(),
                "LOGIN-UI-004");
        ScreenshotsManager.takeFullPageScreenshot(browser(), "LOGIN-UI-004_PASS");
    }

    @Test(description = "LOGIN-VAL-001 — Submit Empty Email and Password")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_001,
            title = "Submit Empty Email and Password",
            feature = LoginTestIds.FEATURE,
            description = "Empty credentials must not authenticate.",
            testObjective = "Empty submit must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal001_submitEmptyCredentials() {
        publish(LoginTestIds.LOGIN_VAL_001, "Submit Empty Email and Password", "Empty submit must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_001,
                "Clear fields and submit",
                "User remains on login page.",
                login -> login.clearFields());
    }

    @Test(description = "LOGIN-VAL-002 — Submit Without Email")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_002,
            title = "Submit Without Email",
            feature = LoginTestIds.FEATURE,
            description = "Password-only submit must not authenticate.",
            testObjective = "Missing email must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal002_submitWithoutEmail() {
        publish(LoginTestIds.LOGIN_VAL_002, "Submit Without Email", "Missing email must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_002,
                "Enter password only and submit",
                "User remains on login page.",
                login -> login.clearFields().enterPassword("any-password"));
    }

    @Test(description = "LOGIN-VAL-003 — Submit Without Password")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_003,
            title = "Submit Without Password",
            feature = LoginTestIds.FEATURE,
            description = "Email-only submit must not authenticate.",
            testObjective = "Missing password must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal003_submitWithoutPassword() {
        publish(LoginTestIds.LOGIN_VAL_003, "Submit Without Password", "Missing password must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_003,
                "Enter email only and submit",
                "User remains on login page.",
                login -> login.clearFields().enterEmail("user@example.com"));
    }

    @Test(description = "LOGIN-VAL-004 — Invalid Email Without @")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_004,
            title = "Invalid Email Without @",
            feature = LoginTestIds.FEATURE,
            description = "Malformed email must not authenticate.",
            testObjective = "Malformed email must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal004_invalidEmailWithoutAt() {
        publish(LoginTestIds.LOGIN_VAL_004, "Invalid Email Without @", "Malformed email must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_004,
                "Enter invalid-email and submit",
                "User remains on login page.",
                login -> login.clearFields().enterEmail("invalid-email").enterPassword("any-password"));
    }

    @Test(description = "LOGIN-VAL-005 — Email Missing Domain")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_005,
            title = "Email Missing Domain",
            feature = LoginTestIds.FEATURE,
            description = "Incomplete email must not authenticate.",
            testObjective = "Incomplete email must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal005_emailMissingDomain() {
        publish(LoginTestIds.LOGIN_VAL_005, "Email Missing Domain", "Incomplete email must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_005,
                "Enter user@ and submit",
                "User remains on login page.",
                login -> login.clearFields().enterEmail("user@").enterPassword("any-password"));
    }

    @Test(description = "LOGIN-VAL-006 — Email Missing Local Part")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_VAL_006,
            title = "Email Missing Local Part",
            feature = LoginTestIds.FEATURE,
            description = "Incomplete email must not authenticate.",
            testObjective = "Incomplete email must not authenticate.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "User remains on login page.")
    public void loginVal006_emailMissingLocalPart() {
        publish(LoginTestIds.LOGIN_VAL_006, "Email Missing Local Part", "Incomplete email must not authenticate.");
        runValidationSubmit(
                LoginTestIds.LOGIN_VAL_006,
                "Enter @example.com and submit",
                "User remains on login page.",
                login -> login.clearFields().enterEmail("@example.com").enterPassword("any-password"));
    }

    @Test(description = "LOGIN-AUTH-001 — Invalid Credentials Are Rejected")
    @TestCaseReport(
            id = LoginTestIds.LOGIN_AUTH_001,
            title = "Invalid Credentials Are Rejected",
            feature = LoginTestIds.FEATURE,
            description = "A well-formed but unregistered email with a wrong password must not authenticate.",
            testObjective = "Confirm the portal rejects invalid credentials and keeps the user on the login page.",
            preconditions = {PRECONDITIONS},
            expectedFinalResult = "Login is rejected; user remains on the login page and the form is still usable.")
    public void loginAuth001_invalidCredentialsAreRejected() {
        publish(
                LoginTestIds.LOGIN_AUTH_001,
                "Invalid Credentials Are Rejected",
                "Confirm the portal rejects invalid credentials and keeps the user on the login page.");
        LoginPage login = new LoginPage(browser());
        login.clearFields().enterEmail(INVALID_EMAIL).enterPassword(INVALID_PASSWORD);
        TestStepReporter.runStep(
                "Submit invalid credentials and verify rejection",
                "Enter a well-formed unregistered email and a wrong password, then submit",
                "Login is rejected: the user is not redirected away from the login page.",
                () -> {
                    login.submit();
                    login.awaitSubmitOutcome();
                    String message = login.visibleErrorText();
                    String observation =
                            login.loginPageObservation()
                                    + ", rejectionMessage="
                                    + (message.isBlank() ? "(none visible)" : message);
                    return login.remainsOnLoginPage()
                            ? StepResult.pass(observation)
                            : StepResult.fail("Authenticated with invalid credentials: " + observation);
                },
                browser(),
                LoginTestIds.LOGIN_AUTH_001);
        TestStepReporter.runStep(
                "Verify login form is still usable",
                "Inspect email, password and submit controls after the rejection",
                "Email input, password input and submit button are still visible.",
                () ->
                        login.isDisplayed() && login.isPasswordFieldVisible() && login.isSubmitVisible()
                                ? StepResult.pass(login.loginPageObservation())
                                : StepResult.fail(login.loginPageObservation()),
                browser(),
                LoginTestIds.LOGIN_AUTH_001);
        Assert.assertTrue(login.remainsOnLoginPage(), "Invalid credentials must not authenticate");
        ScreenshotsManager.takeFullPageScreenshot(browser(), LoginTestIds.LOGIN_AUTH_001 + "_PASS");
    }

    private static void publish(String id, String title, String objective) {
        TestStepReporter.publishTestMetadata(
                id, title, FEATURE, objective, PRECONDITIONS, "Synthetic invalid data where applicable.");
    }

    private void runValidationSubmit(
            String testCaseId, String action, String expected, Consumer<LoginPage> prepare) {
        LoginPage login = new LoginPage(browser());
        prepare.accept(login);
        TestStepReporter.runStep(
                "Submit and verify no authentication",
                action,
                expected,
                () -> {
                    login.submit();
                    return login.remainsOnLoginPage()
                            ? StepResult.pass(login.loginPageObservation())
                            : StepResult.fail("Navigated away from login: " + browser().getCurrentUrl());
                },
                browser(),
                testCaseId);
        Assert.assertTrue(login.remainsOnLoginPage(), "Must remain on login page");
        ScreenshotsManager.takeFullPageScreenshot(browser(), testCaseId + "_PASS");
    }
}
