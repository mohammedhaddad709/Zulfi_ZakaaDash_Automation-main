package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.clubdata.ClubWidgets;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.validations.HomeCurrentResultsMessages;
import com.taf.systems.zulfi.validations.HomeResultCardRules;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Home section: نتائج الفريق الحالية (US-FAP-HOME-CURRENT-RESULTS). */
public final class CurrentTeamResultsSection {

    private static final Pattern DATE_TOKEN =
            Pattern.compile("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2}");

    private static final By SECTION_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span or self::p]"
                            + "[contains(normalize-space(.),'"
                            + HomeCurrentResultsMessages.SECTION_TITLE
                            + "')]");
    private static final By VIEW_MORE =
            By.xpath(
                    "//a[contains(normalize-space(.),'"
                            + HomeCurrentResultsMessages.VIEW_MORE
                            + "')]"
                            + "|//button[contains(normalize-space(.),'"
                            + HomeCurrentResultsMessages.VIEW_MORE
                            + "')]");
    private static final By RETRY_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + HomeCurrentResultsMessages.RETRY_ACTION
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeCurrentResultsMessages.RETRY_ACTION
                            + "')]");

    private final WebDriver driver;
    private final ElementActions actions;

    public CurrentTeamResultsSection(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }

    public CurrentTeamResultsSection waitUntilSectionVisible() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(
                        d -> {
                            if (!d.findElements(SECTION_TITLE).isEmpty()
                                    && d.findElement(SECTION_TITLE).isDisplayed()) {
                                return true;
                            }
                            String body = d.findElement(By.tagName("body")).getText();
                            return body.contains(HomeCurrentResultsMessages.EMPTY_TITLE)
                                    || body.contains(HomeCurrentResultsMessages.LOAD_ERROR)
                                    || body.contains(HomeCurrentResultsMessages.RETRY_ACTION);
                        });
        return this;
    }

    public boolean isSectionTitleVisible() {
        return !driver.findElements(SECTION_TITLE).isEmpty()
                && driver.findElement(SECTION_TITLE).isDisplayed();
    }

    public boolean isViewMoreVisible() {
        return !driver.findElements(VIEW_MORE).isEmpty() && driver.findElement(VIEW_MORE).isDisplayed();
    }

    public boolean isEmptyStateVisible() {
        return pageContains(HomeCurrentResultsMessages.EMPTY_TITLE)
                && pageContains(HomeCurrentResultsMessages.EMPTY_DESCRIPTION);
    }

    public boolean isLoadErrorVisible() {
        return pageContains(HomeCurrentResultsMessages.LOAD_ERROR);
    }

    public boolean isRetryActionVisible() {
        return !driver.findElements(RETRY_BUTTON).isEmpty()
                && driver.findElement(RETRY_BUTTON).isDisplayed();
    }

    public CurrentTeamResultsSection clickViewMore() {
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(VIEW_MORE));
        link.click();
        return this;
    }

    public List<CurrentTeamResultCardSnapshot> readVisibleResultCards() {
        WebElement section = resolveSectionRoot();
        // Current markup: one div.group per result = opponent logo (alt = club name) + score tag, no date/status.
        List<ClubWidgets.ResultCard> logoCards =
                ClubWidgetReader.resultCards(driver, section);
        if (!logoCards.isEmpty()) {
            List<CurrentTeamResultCardSnapshot> snapshots = new ArrayList<>();
            for (int i = 0; i < logoCards.size(); i++) {
                var card = logoCards.get(i);
                boolean scored = card.clubGoals() >= 0;
                snapshots.add(new CurrentTeamResultCardSnapshot(
                        i, card.describe(), card.clubGoals(), card.opponentGoals(), scored, "", ""));
            }
            return snapshots;
        }
        List<WebElement> candidates =
                section.findElements(
                        By.cssSelector(
                                "[data-testid='current-team-result-card'],"
                                        + " [data-testid='team-result-card'],"
                                        + " article,"
                                        + " li"));
        List<CurrentTeamResultCardSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        for (WebElement candidate : candidates) {
            if (!candidate.isDisplayed()) {
                continue;
            }
            String text = candidate.getText() == null ? "" : candidate.getText().trim();
            if (text.isBlank() || !HomeResultCardRules.hasRecordedScore(text)) {
                continue;
            }
            int[] score = HomeResultCardRules.parseFirstScore(text);
            snapshots.add(
                    new CurrentTeamResultCardSnapshot(
                            index++,
                            text,
                            score.length == 2 ? score[0] : -1,
                            score.length == 2 ? score[1] : -1,
                            score.length == 2,
                            extractDateToken(text),
                            HomeResultCardRules.findKnownAnalysisStatus(text)));
        }
        return snapshots;
    }

    public int countVisibleResultCards() {
        return readVisibleResultCards().size();
    }

    private WebElement resolveSectionRoot() {
        WebElement title = driver.findElement(SECTION_TITLE);
        List<WebElement> sectionAncestors = title.findElements(By.xpath("./ancestor::section[1]"));
        if (!sectionAncestors.isEmpty()) {
            return sectionAncestors.getFirst();
        }
        List<WebElement> regionAncestors =
                title.findElements(By.xpath("./ancestor::*[@role='region' or contains(@class,'card')][1]"));
        if (!regionAncestors.isEmpty()) {
            return regionAncestors.getFirst();
        }
        return title.findElement(By.xpath("./ancestor::div[1]"));
    }

    private static String extractDateToken(String text) {
        Matcher matcher = DATE_TOKEN.matcher(text);
        if (matcher.find()) {
            String token = matcher.group();
            return MatchDateDisplayRules.looksLikeCalendarDateDisplay(token) ? token : "";
        }
        return "";
    }

    private boolean pageContains(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return false;
        }
        return driver.getPageSource().contains(fragment);
    }
}
