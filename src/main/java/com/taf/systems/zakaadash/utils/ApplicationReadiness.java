package com.taf.systems.zakaadash.utils;

import com.taf.systems.zakaadash.config.ConfigManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Waits for full-screen blocking overlays before user interactions. */
public final class ApplicationReadiness {

    private static final By BLOCKING_OVERLAY = By.xpath(
            "//div[contains(@class,'fixed') and contains(@class,'inset-0')"
                    + " and contains(@class,'bg-black') and contains(@class,'flex-center')]");

    private ApplicationReadiness() {
    }

    public static void waitForBlockingOverlayToDisappear(WebDriver driver) {
        waitForBlockingOverlayToDisappear(driver, null);
    }

    public static void waitForBlockingOverlayToDisappear(WebDriver driver, String businessAction) {
        waitForBlockingOverlayToDisappear(driver, businessAction,
                Duration.ofSeconds(ConfigManager.config().explicitWaitSeconds()));
    }

    /** Same wait with an explicit bound, e.g. the page-load timeout while a page fetches its data after navigation. */
    public static void waitForBlockingOverlayToDisappear(WebDriver driver, String businessAction, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        try {
            wait.until(webDriver -> !hasVisibleBlockingOverlay(webDriver));
        } catch (TimeoutException ex) {
            attachOverlayTimeoutEvidence(driver, businessAction);
            String action = businessAction == null || businessAction.isBlank() ? "interaction" : businessAction;
            throw new TimeoutException("Application blocking overlay did not disappear before: " + action, ex);
        }
    }

    public static boolean hasVisibleBlockingOverlay(WebDriver driver) {
        return driver.findElements(BLOCKING_OVERLAY).stream().anyMatch(WebElement::isDisplayed);
    }

    private static void attachOverlayTimeoutEvidence(WebDriver driver, String businessAction) {
        StringBuilder report = new StringBuilder();
        report.append("Business action: ").append(businessAction).append('\n');
        report.append("URL: ").append(driver.getCurrentUrl()).append('\n');
        report.append("Overlay locator: ").append(BLOCKING_OVERLAY).append('\n');
        report.append("Visible blocking overlays:\n");
        for (WebElement overlay : driver.findElements(BLOCKING_OVERLAY)) {
            if (overlay.isDisplayed()) {
                report.append(overlay.getAttribute("outerHTML")).append('\n');
            }
        }
        if (!hasVisibleBlockingOverlay(driver)) {
            report.append("(none displayed at timeout capture)\n");
        }
        Allure.addAttachment("Blocking overlay timeout",
                "text/plain",
                report.toString(), ".txt");
    }

    public static String blockingOverlayLocatorDescription() {
        return BLOCKING_OVERLAY.toString();
    }
}
