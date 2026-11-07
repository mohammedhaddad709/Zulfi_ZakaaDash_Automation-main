package com.taf.systems.zakaadash.utils;

import com.taf.systems.zakaadash.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

public class WaitUtil {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public WaitUtil(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.config().explicitWaitSeconds()));
    }

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void waitForBlockingOverlayToDisappear(String businessAction) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver, businessAction);
    }

    public WebElement waitForClickable(By locator) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(By locator, String businessAction) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver, businessAction);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver);
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public void clickWhenReady(By locator, String businessAction) {
        waitForClickable(locator, businessAction).click();
    }

    public void waitForInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits until any element matching the locator is displayed. Use it where hidden copies share the locator
     * (the preprocessing wizard keeps every step panel in the DOM and only toggles display).
     */
    public WebElement waitForAnyVisible(By locator) {
        return freshWait().withMessage("No visible element for " + locator)
                .until(webDriver -> webDriver.findElements(locator).stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null));
    }

    public WebElement waitForAnyClickable(By locator) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver);
        return freshWait().withMessage("No visible enabled element for " + locator)
                .until(webDriver -> webDriver.findElements(locator).stream()
                        .filter(element -> element.isDisplayed() && element.isEnabled())
                        .findFirst()
                        .orElse(null));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public void waitForUrlToContain(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public void waitUntilTrue(Function<WebDriver, Boolean> condition) {
        wait.until(condition);
    }

    public void waitUntilTrue(Function<WebDriver, Boolean> condition, String message) {
        freshWait().withMessage(message).until(condition);
    }

    private FluentWait<WebDriver> freshWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.config().explicitWaitSeconds()))
                .ignoring(StaleElementReferenceException.class);
    }
}
