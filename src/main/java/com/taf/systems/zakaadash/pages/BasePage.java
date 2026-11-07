package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.utils.ApplicationReadiness;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    protected void open(String url) {
        driver.get(url);
    }

    protected void type(By locator, String text) {
        WebElement element = driver.findElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void click(By locator) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver);
        driver.findElement(locator).click();
    }

    protected void waitForBlockingOverlayToDisappear() {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver);
    }

    protected void waitForBlockingOverlayToDisappear(String businessAction) {
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver, businessAction);
    }

    protected boolean isDisplayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    protected String getText(By locator) {
        Duration timeout = Duration.ofSeconds(ConfigManager.config().explicitWaitSeconds());
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator))
                .getText();
    }
}
