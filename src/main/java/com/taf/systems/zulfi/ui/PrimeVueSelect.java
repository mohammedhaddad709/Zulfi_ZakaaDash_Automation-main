package com.taf.systems.zulfi.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * One PrimeVue {@code p-select} (filters, team selects): open, read options, pick by text, clear, read the label.
 * Encapsulates the portal quirks: options re-render while the overlay opens, a previous overlay may still be closing,
 * the clear icon only shows on hover, and the label's {@code aria-label} is the placeholder until something is picked.
 */
public final class PrimeVueSelect {

    public static final By OVERLAY_OPTIONS = By.cssSelector(
            ".p-select-overlay [role='option'], .p-select-list [role='option'], [data-pc-section='option'][role='option']");
    private static final By CLEAR_ICON = By.cssSelector("[data-pc-section='clearicon'], .p-select-clear-icon");
    private static final Duration WAIT = Duration.ofSeconds(15);

    private final WebDriver driver;
    private final By combobox;
    private final String name;

    /** @param combobox locator of the select's {@code [role=combobox]} label; @param name used in error messages */
    public PrimeVueSelect(WebDriver driver, By combobox, String name) {
        this.driver = driver;
        this.combobox = combobox;
        this.name = name;
    }

    public boolean isPresent() {
        return driver.findElements(combobox).stream().anyMatch(WebElement::isDisplayed);
    }

    /** Selected option text, or the placeholder when nothing is selected; empty when the select is absent. */
    public String label() {
        List<WebElement> found = driver.findElements(combobox);
        if (found.isEmpty()) {
            return "";
        }
        String aria = found.getFirst().getAttribute("aria-label");
        if (aria != null && !aria.isBlank()) {
            return PortalDom.normalize(aria);
        }
        return PortalDom.normalize(found.getFirst().getText());
    }

    public PrimeVueSelect open() {
        waitClosed();
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement combo = new WebDriverWait(driver, WAIT).until(ExpectedConditions.elementToBeClickable(combobox));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", combo);
        try {
            combo.click();
        } catch (ElementClickInterceptedException covered) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", combo);
        }
        try {
            new WebDriverWait(driver, WAIT).until(d -> !visibleOptions(d).isEmpty());
        } catch (TimeoutException none) {
            throw new IllegalStateException("Options did not appear for «" + name + "»; url=" + driver.getCurrentUrl(), none);
        }
        return this;
    }

    public List<String> options() {
        return UiRetry.onStale(() -> visibleOptions(driver).stream().map(PrimeVueSelect::optionText).toList());
    }

    /** Opens the select and clicks the first option containing {@code fragment}; fails listing the visible options. */
    public PrimeVueSelect pick(String fragment) {
        open();
        boolean clicked = clickOption(fragment);
        if (!clicked && visibleOptions(driver).isEmpty()) {
            open(); // the open click toggled an overlay that was still closing
            clicked = clickOption(fragment);
        }
        if (!clicked) {
            List<String> visible = options();
            close();
            throw new IllegalStateException(
                    "No option «" + fragment + "» in «" + name + "»; visible=" + visible + "; label=" + label());
        }
        waitClosed();
        return this;
    }

    /** Clears the selection (the select then shows its placeholder, i.e. «all»). No-op when nothing is selected. */
    public PrimeVueSelect clear() {
        WebElement root = driver.findElement(combobox)
                .findElement(By.xpath("./ancestor::*[@data-pc-name='select' or contains(@class,'p-select ')][1]"));
        root.findElements(CLEAR_ICON).stream().findFirst().ifPresent(icon -> ((JavascriptExecutor) driver)
                .executeScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true}));", icon));
        waitClosed();
        return this;
    }

    public void close() {
        if (!visibleOptions(driver).isEmpty()) {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        }
        waitClosed();
    }

    private boolean clickOption(String fragment) {
        return UiRetry.onStale(() -> {
            for (WebElement option : visibleOptions(driver)) {
                if (optionText(option).contains(fragment)) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'nearest'});", option);
                    option.click();
                    return true;
                }
            }
            return false;
        });
    }

    private void waitClosed() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .ignoring(RuntimeException.class)
                    .until(d -> visibleOptions(d).isEmpty());
        } catch (TimeoutException stillOpen) {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        }
    }

    private static List<WebElement> visibleOptions(WebDriver driver) {
        return driver.findElements(OVERLAY_OPTIONS).stream().filter(WebElement::isDisplayed).toList();
    }

    private static String optionText(WebElement option) {
        String aria = option.getAttribute("aria-label");
        return PortalDom.normalize(aria != null && !aria.isBlank() ? aria : option.getText());
    }
}
