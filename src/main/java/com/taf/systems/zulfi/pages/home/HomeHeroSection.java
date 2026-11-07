package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Home Hero / analysis entry (US-FAP-HOME-001). */
public final class HomeHeroSection {

    private static final By START_ANALYSIS =
            By.cssSelector("button[aria-label='" + HomeOverviewMessages.HERO_PRIMARY_ACTION + "']");
    private static final By BROWSE_ANALYSES =
            By.xpath(
                    "//a[contains(normalize-space(.),'"
                            + HomeOverviewMessages.HERO_SECONDARY_ACTION
                            + "')"
                            + " or @aria-label='"
                            + HomeOverviewMessages.HERO_SECONDARY_ACTION
                            + "']"
                            + "|//button[contains(normalize-space(.),'"
                            + HomeOverviewMessages.HERO_SECONDARY_ACTION
                            + "')"
                            + " or @aria-label='"
                            + HomeOverviewMessages.HERO_SECONDARY_ACTION
                            + "']");

    private final WebDriver driver;
    private final ElementActions actions;

    public HomeHeroSection(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }

    public HomeHeroSection waitUntilVisible() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(ExpectedConditions.visibilityOfElementLocated(START_ANALYSIS));
        return this;
    }

    public boolean isPrimaryActionVisible() {
        return !driver.findElements(START_ANALYSIS).isEmpty()
                && driver.findElement(START_ANALYSIS).isDisplayed();
    }

    public boolean isPrimaryActionEnabled() {
        WebElement button = driver.findElement(START_ANALYSIS);
        return button.isEnabled() && !isDisabledInDom(button);
    }

    public boolean isSecondaryActionVisible() {
        return !driver.findElements(BROWSE_ANALYSES).isEmpty()
                && driver.findElement(BROWSE_ANALYSES).isDisplayed();
    }

    public boolean isTitleVisible() {
        return HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.HERO_TITLE);
    }

    public boolean isDescriptionVisible() {
        return HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.HERO_DESCRIPTION);
    }

    public HomeHeroSection clickStartAnalysis() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement button =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(START_ANALYSIS));
        actions.click(START_ANALYSIS);
        return this;
    }

    public HomeHeroSection clickBrowseAnalyses() {
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(BROWSE_ANALYSES));
        link.click();
        return this;
    }

    private static boolean isDisabledInDom(WebElement element) {
        String disabled = element.getAttribute("disabled");
        if (disabled != null && !disabled.isBlank() && !"false".equalsIgnoreCase(disabled)) {
            return true;
        }
        String aria = element.getAttribute("aria-disabled");
        return "true".equalsIgnoreCase(aria);
    }
}
