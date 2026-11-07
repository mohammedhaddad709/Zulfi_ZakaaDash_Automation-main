package com.taf.systems.zulfi.flows;

import com.taf.core.utils.ElapsedWait;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.pages.*;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Reusable business-level authentication fixture for future protected features. */
public final class LoginFlow {

    /** Reduces auth/session races when TestNG runs several UI classes in parallel. */
    private static final Object PARALLEL_LOGIN_GATE = new Object();

    private final WebDriver driver;

    public LoginFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Authenticate user before protected test case")
    public HomePage loginAsConfiguredUser() {
        synchronized (PARALLEL_LOGIN_GATE) {
            var credentials = CredentialProvider.requireCredentials();
            new LoginPage(driver).open().enterEmail(credentials.username())
                    .enterPassword(credentials.password()).submit();
            HomePage home = new HomePage(driver).awaitReady();
            ElapsedWait.atLeast(Duration.ofMillis(150));
            return home;
        }
    }
}
