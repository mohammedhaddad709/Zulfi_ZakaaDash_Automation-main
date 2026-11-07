package com.taf.systems.zakaadash.tests.auth;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.ForgotPasswordPage;
import com.taf.systems.zakaadash.pages.ZakaaDashLoginPage;
import com.taf.systems.zakaadash.utils.BusinessStep;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_AUTH;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;
import static com.taf.systems.zakaadash.tests.TestGroups.SMOKE;

@Epic("ZakaaDash")
@Feature("Authentication")
public class AuthenticationTests extends ZakaaDashBaseTest {

    private static final String LOGIN_PAGE = "Login";
    // Reserved .invalid domain: can never match a real account, so lockout of real users is impossible.
    private static final String INVALID_EMAIL = "automation.invalid-user@example.invalid";
    private static final String INVALID_PASSWORD = "Invalid-Password-For-Negative-Test-1";
    private static final String DATA_VERSIONS_ROUTE = "/data-versions";

    @Test(groups = {FEATURE_AUTH, SMOKE, SANITY, REGRESSION, CRITICAL},
            description = "AUTH-001: Valid user can login and reach Data Versions")
    @TmsLink("AUTH-001")
    @Story("Valid user can login and reach Data Versions")
    @Severity(SeverityLevel.CRITICAL)
    public void validUserCanLoginAndReachDataVersions() {
        AuthenticationFlow authenticationFlow = new AuthenticationFlow(driver());

        ZakaaDashLoginPage loginPage = authenticationFlow.openLogin();
        BusinessStep.run(LOGIN_PAGE, "Verify login page available", () ->
                Assert.assertTrue(loginPage.isLoaded(), "Login page should be available before authentication"));

        DataVersionsPage dataVersionsPage = authenticationFlow.submitValidCredentials(loginPage);
        BusinessStep.run("Data Versions", "Verify authenticated destination", () -> {
            Assert.assertTrue(driver().getCurrentUrl().contains("/data-versions"),
                    "URL should contain /data-versions but was: " + driver().getCurrentUrl());
            Assert.assertTrue(dataVersionsPage.isLoaded(), "Data Versions page should be loaded");
        });
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION, CRITICAL},
            description = "AUTH-002: Invalid credentials are rejected")
    @TmsLink("AUTH-002")
    @Story("Invalid credentials are rejected")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidCredentialsAreRejected() {
        ZakaaDashLoginPage loginPage = new AuthenticationFlow(driver()).openLogin();

        BusinessStep.run(LOGIN_PAGE, "Submit controlled invalid credentials", () ->
                loginPage.login(INVALID_EMAIL, INVALID_PASSWORD));

        BusinessStep.run(LOGIN_PAGE, "Verify authentication rejected", () -> {
            Optional<String> authenticationError = loginPage.getAuthenticationError();
            Assert.assertTrue(authenticationError.isPresent(), "A visible authentication error should be shown");
            Assert.assertTrue(loginPage.isOnLoginRoute(),
                    "User should remain on /auth/login but URL was: " + driver().getCurrentUrl());
            Assert.assertFalse(new DataVersionsPage(driver()).isLoaded(), "Data Versions must not be reached");
        });
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION},
            description = "AUTH-003: Required fields block empty submit")
    @TmsLink("AUTH-003")
    @Story("Required fields block empty submit")
    @Severity(SeverityLevel.NORMAL)
    public void requiredFieldsBlockEmptySubmit() {
        ZakaaDashLoginPage loginPage = new AuthenticationFlow(driver()).openLogin();

        BusinessStep.run(LOGIN_PAGE, "Submit with empty email and password", () -> {
            loginPage.submitLogin();
            loginPage.waitForFieldValidation();
        });

        BusinessStep.run(LOGIN_PAGE, "Verify required-field validation blocks login", () -> {
            Optional<String> emailMessage = loginPage.getEmailValidationMessage();
            Optional<String> passwordMessage = loginPage.getPasswordValidationMessage();
            Assert.assertTrue(emailMessage.isPresent(), "Email field should show a required-field message");
            Assert.assertTrue(passwordMessage.isPresent(), "Password field should show a required-field message");
            Assert.assertTrue(loginPage.isOnLoginRoute(),
                    "User should remain on /auth/login but URL was: " + driver().getCurrentUrl());
            Assert.assertFalse(new DataVersionsPage(driver()).isLoaded(), "Data Versions must not be reached");
        });
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION},
            description = "AUTH-004: Password visibility toggle shows and hides the password")
    @TmsLink("AUTH-004")
    @Story("Password visibility toggle shows and hides the password")
    @Severity(SeverityLevel.MINOR)
    public void passwordVisibilityToggleShowsAndHidesPassword() {
        ZakaaDashLoginPage loginPage = new AuthenticationFlow(driver()).openLogin();
        BusinessStep.run(LOGIN_PAGE, "Type a dummy password", () -> loginPage.enterPassword(INVALID_PASSWORD));

        BusinessStep.run(LOGIN_PAGE, "Reveal password", () -> {
            Assert.assertEquals(loginPage.getPasswordInputType(), "password", "Password should be masked by default");
            loginPage.revealPassword();
            Assert.assertEquals(loginPage.getPasswordInputType(), "text", "Eye icon should reveal the password");
        });
        BusinessStep.run(LOGIN_PAGE, "Hide password again", () -> {
            loginPage.hidePassword();
            Assert.assertEquals(loginPage.getPasswordInputType(), "password", "Eye icon should mask the password again");
        });
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION},
            description = "AUTH-005: Forgot password link opens the reset page and returns to login")
    @TmsLink("AUTH-005")
    @Story("Forgot password link opens the reset page and returns to login")
    @Severity(SeverityLevel.NORMAL)
    public void forgotPasswordLinkOpensResetPage() {
        AuthenticationFlow authenticationFlow = new AuthenticationFlow(driver());
        ForgotPasswordPage forgotPasswordPage = authenticationFlow.openForgotPassword(authenticationFlow.openLogin());

        BusinessStep.run("Forgot password", "Verify reset page (not submitted)", () ->
                Assert.assertTrue(forgotPasswordPage.isLoaded(),
                        "Forgot password page should show the e-mail field and the send-code button; URL: "
                                + driver().getCurrentUrl()));

        ZakaaDashLoginPage loginPage = BusinessStep.get("Forgot password", "Return to login", forgotPasswordPage::backToLogin);
        BusinessStep.run(LOGIN_PAGE, "Verify login page", () ->
                Assert.assertTrue(loginPage.isLoaded(), "Login page should be shown again"));
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION, CRITICAL},
            description = "AUTH-006: Data Versions without a session redirects to login")
    @TmsLink("AUTH-006")
    @Story("Data Versions without a session redirects to login")
    @Severity(SeverityLevel.CRITICAL)
    public void dataVersionsWithoutSessionRedirectsToLogin() {
        ZakaaDashLoginPage loginPage = new AuthenticationFlow(driver()).requestDataVersionsWithoutSession();

        BusinessStep.run(LOGIN_PAGE, "Verify redirect to login", () -> {
            Assert.assertTrue(loginPage.isLoaded(), "Login page should be shown; URL: " + driver().getCurrentUrl());
            Assert.assertEquals(loginPage.getRedirectTarget().orElse(""), DATA_VERSIONS_ROUTE,
                    "Login should keep the requested page as redirect target");
            Assert.assertFalse(new DataVersionsPage(driver()).isLoaded(), "Data Versions must not be shown");
        });
    }

    @Test(groups = {FEATURE_AUTH, REGRESSION, CRITICAL},
            description = "AUTH-007: Logout ends the session")
    @TmsLink("AUTH-007")
    @Story("Logout ends the session")
    @Severity(SeverityLevel.CRITICAL)
    public void logoutEndsTheSession() {
        AuthenticationFlow authenticationFlow = new AuthenticationFlow(driver());
        authenticationFlow.loginSuccessfully();

        ZakaaDashLoginPage loginAfterLogout = authenticationFlow.logout();
        BusinessStep.run(LOGIN_PAGE, "Verify logout lands on login", () ->
                Assert.assertTrue(loginAfterLogout.isLoaded(), "Login page should be shown after logout"));

        ZakaaDashLoginPage loginAfterRevisit = authenticationFlow.requestDataVersionsWithoutSession();
        BusinessStep.run(LOGIN_PAGE, "Verify Data Versions is protected after logout", () -> {
            Assert.assertTrue(loginAfterRevisit.isLoaded(),
                    "Data Versions should redirect to login after logout; URL: " + driver().getCurrentUrl());
            Assert.assertFalse(new DataVersionsPage(driver()).isLoaded(), "Data Versions must not be shown after logout");
        });
    }
}
