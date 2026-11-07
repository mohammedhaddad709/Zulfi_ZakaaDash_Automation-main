package com.taf.systems.zulfi.features.login;

import org.openqa.selenium.By;
import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.LoginPage;
import com.taf.systems.zulfi.login.LoginTestIds;
import com.taf.systems.zulfi.reporting.TestCaseReport;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

@Epic("Zulfi Portal")
@Feature("Login")
@Story("Happy Path")
public class LoginHappyPathTest extends LoginFeatureFixture {

    private static final String TEST_CASE_ID = LoginTestIds.LOGIN_HP_001;
    private static final String TITLE = "Verify Successful Login with Valid Credentials";
    private static final String FEATURE = "Login";
    private static final String OBJECTIVE =
            "Confirm an authorized user can authenticate and reach the authenticated Home page.";
    private static final String PRECONDITIONS =
            "Fresh browser; user logged out; ZULFI_USER and ZULFI_PASSWORD configured; portal reachable.";
    private static final String TEST_DATA = "Environment credentials (values redacted in reports).";

    @Test(description = TEST_CASE_ID + " — " + TITLE)
    @TestCaseReport(
            id = LoginTestIds.LOGIN_HP_001,
            title = TITLE,
            feature = LoginTestIds.FEATURE,
            description = "Authenticate with valid portal credentials.",
            testObjective = OBJECTIVE,
            preconditions = {PRECONDITIONS},
            testDataReference = TEST_DATA,
            expectedFinalResult = "Authenticated Home page with «ابدأ التحليل» visible.")
    @Description("Fresh browser on Login → valid credentials → Home shows «ابدأ التحليل».")
    public void logHp001_verifySuccessfulLoginReachesHome() {
        TestStepReporter.publishTestMetadata(
                TEST_CASE_ID, TITLE, FEATURE, OBJECTIVE, PRECONDITIONS, TEST_DATA);

        if (!CredentialProvider.isConfigured()) {
            throw new SkipException(
                    "CONFIGURATION: ZULFI_USER and ZULFI_PASSWORD must be set before LOGIN-HP-001 execution.");
        }

        LoginPage login = new LoginPage(browser());

        TestStepReporter.runStep(
                "Verify Login page",
                "Observe login form after navigation",
                "Login page displayed with email and password inputs visible.",
                () -> StepResult.pass(login.loginPageObservation()),
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Verify email input",
                "Inspect #email visibility and type",
                "Email input is visible and accepts email entry.",
                () ->
                        login.isDisplayed() && "email".equalsIgnoreCase(
                                        browser().findElement(By.id("email"))
                                                .getAttribute("type"))
                                ? StepResult.pass("Email field visible, type=email")
                                : StepResult.fail("Email field not ready"),
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Verify password input",
                "Inspect #password visibility and masking",
                "Password input is visible and masked (type=password).",
                () ->
                        login.isPasswordFieldVisible() && login.isPasswordMasked()
                                ? StepResult.pass("Password field visible, type=password")
                                : StepResult.fail("Password field not masked or not visible"),
                browser(),
                TEST_CASE_ID);

        CredentialProvider.Credentials credentials = CredentialProvider.requireCredentials();

        TestStepReporter.runStep(
                "Enter credentials",
                "Type email and password (values redacted)",
                "Credentials accepted in form fields without logging secrets.",
                () -> {
                    login.enterEmail(credentials.username()).enterPassword(credentials.password());
                    return StepResult.pass("Credentials entered; email length=" + credentials.username().length());
                },
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Submit login",
                "Click تسجيل الدخول once",
                "Login request submitted.",
                () -> {
                    login.submit();
                    return StepResult.pass("Submit clicked once");
                },
                browser(),
                TEST_CASE_ID);

        HomePage home = new HomePage(browser());
        TestStepReporter.runStep(
                "Verify authenticated Home",
                "Wait for Home readiness marker",
                "Home page displayed; button «ابدأ التحليل» visible.",
                () -> {
                    home.awaitReady();
                    boolean ready = home.isReady();
                    return ready
                            ? StepResult.pass("Home ready; startAnalysisVisible=true, URL=" + browser().getCurrentUrl())
                            : StepResult.fail("Home CTA not visible after login");
                },
                browser(),
                TEST_CASE_ID);

        ScreenshotsManager.takeFullPageScreenshot(browser(), TEST_CASE_ID + "_PASS");
        Assert.assertTrue(home.isReady(), "Home must show «ابدأ التحليل» after valid login");
    }
}
