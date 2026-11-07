package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Home upcoming match section (singular/plural titles). */
public final class HomeUpcomingMatchSection {

    private static final By SECTION_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span or self::p]"
                            + "[contains(normalize-space(.),'"
                            + HomeOverviewMessages.UPCOMING_SECTION_TITLE_SINGULAR
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeOverviewMessages.UPCOMING_SECTION_TITLE_PLURAL
                            + "')]");

    private final WebDriver driver;

    public HomeUpcomingMatchSection(WebDriver driver) {
        this.driver = driver;
    }

    public HomeUpcomingMatchSection waitUntilSectionVisible() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(ExpectedConditions.visibilityOfElementLocated(SECTION_TITLE));
        return this;
    }

    public boolean isSectionTitleVisible() {
        return !driver.findElements(SECTION_TITLE).isEmpty()
                && driver.findElement(SECTION_TITLE).isDisplayed();
    }

    public boolean isEmptyStateVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.UPCOMING_EMPTY);
    }

    public boolean isLoadErrorVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.UPCOMING_LOAD_ERROR);
    }

    public boolean hasMatchContentWhenNotEmpty() {
        if (isEmptyStateVisible() || isLoadErrorVisible()) {
            return false;
        }
        String sectionText = HomeSectionSupport.resolveSectionRoot(driver, SECTION_TITLE).getText();
        return sectionText != null && sectionText.trim().length() > 20;
    }

    public String sectionText() {
        if (!isSectionTitleVisible()) {
            return "";
        }
        return HomeSectionSupport.resolveSectionRoot(driver, SECTION_TITLE).getText();
    }
}
