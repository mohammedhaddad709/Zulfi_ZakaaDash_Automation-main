package com.taf.systems.zulfi.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Dismiss PrimeVue overlays that block clicks (read-only safe). */
public final class PortalOverlaySupport {

    private static final By TOAST_CLOSE =
            By.cssSelector(
                    ".p-toast-icon-close, .p-toast-close-button, button.p-toast-close-button,"
                            + " [data-pc-section='closebutton']");

    /** Full-screen portal loading mask shown during route/data loads (not the create-analysis dialog mask). */
    private static final By BLOCKING_LOADER =
            By.cssSelector("div.fixed.inset-0[class*='z-999999']");

    private static final Duration LOADER_TIMEOUT = Duration.ofSeconds(30);

    private PortalOverlaySupport() {}

    /** Waits until the portal loading mask is gone so the next click reaches its target. */
    public static void waitForBlockingLoaderToClear(WebDriver driver) {
        try {
            new WebDriverWait(driver, LOADER_TIMEOUT)
                    .ignoring(RuntimeException.class)
                    .until(d -> d.findElements(BLOCKING_LOADER).stream().noneMatch(WebElement::isDisplayed));
        } catch (TimeoutException stillLoading) {
            throw new IllegalStateException(
                    "Portal loading mask still blocks the page after " + LOADER_TIMEOUT.toSeconds()
                            + "s; url=" + driver.getCurrentUrl(),
                    stillLoading);
        }
    }

    public static void dismissVisibleToasts(WebDriver driver) {
        waitForBlockingLoaderToClear(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            boolean clicked = false;
            for (WebElement close : driver.findElements(TOAST_CLOSE)) {
                if (!close.isDisplayed()) {
                    continue;
                }
                try {
                    close.click();
                    clicked = true;
                } catch (RuntimeException ignored) {
                    // toast may detach during click
                }
            }
            if (!clicked) {
                break;
            }
        }
    }
}
