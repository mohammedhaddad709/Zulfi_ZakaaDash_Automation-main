package com.taf.systems.zakaadash.flows;

import com.taf.systems.zakaadash.config.ZakaaDashCredentials;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.ForgotPasswordPage;
import com.taf.systems.zakaadash.pages.ZakaaDashAppShellPage;
import com.taf.systems.zakaadash.pages.ZakaaDashLoginPage;
import com.taf.systems.zakaadash.utils.BusinessStep;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class AuthenticationFlow {

    private static final String LOGIN_PAGE = "Login";
    private static final String DATA_VERSIONS_PAGE = "Data Versions";
    private static final String APP_SHELL = "App Shell";

    private final WebDriver driver;

    public AuthenticationFlow(WebDriver driver) {
        this.driver = driver;
    }

    public DataVersionsPage loginSuccessfully() {
        return submitValidCredentials(openLogin());
    }

    public ZakaaDashLoginPage openLogin() {
        return BusinessStep.get(LOGIN_PAGE, "Open Login", () -> new ZakaaDashLoginPage(driver).open());
    }

    public DataVersionsPage submitValidCredentials(ZakaaDashLoginPage loginPage) {
        ZakaaDashCredentials credentials = ZakaaDashCredentials.load();
        BusinessStep.run(LOGIN_PAGE, "Enter Email", () -> loginPage.enterEmail(credentials.email()));
        BusinessStep.run(LOGIN_PAGE, "Enter Password", () -> loginPage.enterPassword(credentials.password()));
        BusinessStep.run(LOGIN_PAGE, "Submit Login", loginPage::submitLogin);
        return BusinessStep.get(DATA_VERSIONS_PAGE, "Reach Data Versions", () -> waitForDataVersions(loginPage));
    }

    /** Requests Data Versions by URL; without a session the app should land on the login page. */
    public ZakaaDashLoginPage requestDataVersionsWithoutSession() {
        return BusinessStep.get(DATA_VERSIONS_PAGE, "Open Data Versions URL without a session", () -> {
            new DataVersionsPage(driver).openByUrl();
            return new ZakaaDashLoginPage(driver).waitUntilLoginScreenVisible();
        });
    }

    public ForgotPasswordPage openForgotPassword(ZakaaDashLoginPage loginPage) {
        return BusinessStep.get(LOGIN_PAGE, "Open forgot password", loginPage::openForgotPassword);
    }

    public ZakaaDashLoginPage logout() {
        return BusinessStep.get(APP_SHELL, "Logout from profile menu", () -> new ZakaaDashAppShellPage(driver).logout());
    }

    private DataVersionsPage waitForDataVersions(ZakaaDashLoginPage loginPage) {
        try {
            return new DataVersionsPage(driver).waitUntilLoaded();
        } catch (TimeoutException ex) {
            if (loginPage.isOnLoginRoute()) {
                String error = loginPage.getAuthenticationError()
                        .map(text -> "Visible authentication error: " + text)
                        .orElse("No visible authentication error");
                throw new IllegalStateException("Authentication failed: still on /auth/login. " + error, ex);
            }
            throw new IllegalStateException(
                    "Data Versions did not load after login. Current URL: " + driver.getCurrentUrl(), ex);
        }
    }
}
