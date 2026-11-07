package com.taf.systems.zulfi.reporting;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/** Collects real browser observations for Actual Result fields (no copied expectations). */
public final class BrowserObservations {

    private BrowserObservations() {}

    public static String currentUrl(WebDriver driver) {
        if (driver == null) {
            return "Browser URL unavailable — WebDriver was not initialized.";
        }
        try {
            return driver.getCurrentUrl();
        } catch (Exception ex) {
            return "Browser URL unavailable — " + ex.getClass().getSimpleName() + ": " + safeMessage(ex);
        }
    }

    public static String elementVisibility(WebDriver driver, By locator, String friendlyName) {
        if (driver == null) {
            return friendlyName + " observation unavailable — WebDriver was not initialized.";
        }
        try {
            WebElement element = driver.findElement(locator);
            boolean displayed = element.isDisplayed();
            boolean enabled = element.isEnabled();
            return friendlyName
                    + " — visible="
                    + displayed
                    + ", enabled="
                    + enabled
                    + ", currentUrl="
                    + driver.getCurrentUrl();
        } catch (Exception ex) {
            return friendlyName
                    + " not observed — "
                    + ex.getClass().getSimpleName()
                    + ": "
                    + safeMessage(ex)
                    + ", currentUrl="
                    + currentUrl(driver);
        }
    }

    public static String observationFromThrowable(WebDriver driver, Throwable throwable) {
        String url = currentUrl(driver);
        if (throwable == null) {
            return "Failure detected; currentUrl=" + url;
        }
        return "Failure: "
                + throwable.getClass().getSimpleName()
                + " — "
                + safeMessage(throwable)
                + "; currentUrl="
                + url;
    }

    public static Optional<WebDriver> contextDriver(WebDriver explicit) {
        if (explicit != null) {
            return Optional.of(explicit);
        }
        return TestExecutionContext.driver();
    }

    static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "(no message)";
        }
        return SensitiveRedaction.redact(message);
    }
}
