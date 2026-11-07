package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/** Home analysis summary KPI cards. */
public final class HomeAnalysisSummarySection {

    private static final By ANY_KPI_LABEL =
            By.xpath(
                    "//*[contains(normalize-space(.),'"
                            + HomeOverviewMessages.KPI_TOTAL_ANALYSES
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeOverviewMessages.KPI_IN_PROGRESS
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeOverviewMessages.KPI_COMPLETED
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeOverviewMessages.KPI_PLAYER_COUNT
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + HomeOverviewMessages.KPI_TOP_SCORER
                            + "')]");

    private final WebDriver driver;

    public HomeAnalysisSummarySection(WebDriver driver) {
        this.driver = driver;
    }

    public HomeAnalysisSummarySection waitUntilAnyKpiVisible() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(ExpectedConditions.visibilityOfElementLocated(ANY_KPI_LABEL));
        return this;
    }

    public boolean isLoadErrorVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.SUMMARY_LOAD_ERROR);
    }

    public boolean isRetryVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.RETRY_ACTION);
    }

    public boolean hasExpectedKpiLabels() {
        return HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.KPI_TOTAL_ANALYSES)
                && HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.KPI_IN_PROGRESS)
                && HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.KPI_COMPLETED)
                && HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.KPI_PLAYER_COUNT)
                && HomeSectionSupport.visibleBodyContains(driver, HomeOverviewMessages.KPI_TOP_SCORER);
    }

    /** Player name or placeholder shown under «هداف الفريق» (not necessarily numeric). */
    public Optional<String> readTopScorerDisplay() {
        return HomeSectionSupport.parseDisplayNearLabel(driver, HomeOverviewMessages.KPI_TOP_SCORER);
    }

    public OptionalInt readTotalAnalyses() {
        return kpi(HomeOverviewMessages.KPI_TOTAL_ANALYSES);
    }

    public OptionalInt readInProgress() {
        return kpi(HomeOverviewMessages.KPI_IN_PROGRESS);
    }

    public OptionalInt readCompleted() {
        return kpi(HomeOverviewMessages.KPI_COMPLETED);
    }

    public OptionalInt readPlayerCount() {
        return kpi(HomeOverviewMessages.KPI_PLAYER_COUNT);
    }

    /** KPI cards are {@code <article><p>label</p>…<p>value</p></article>}; older markup falls back to proximity parsing. */
    private OptionalInt kpi(String label) {
        try {
            int value = new HomeClubWidgets(driver).kpiNumber(label);
            if (value >= 0) {
                return OptionalInt.of(value);
            }
        } catch (RuntimeException cardsNotRendered) {
            // fall through to the legacy parser
        }
        return HomeSectionSupport.parseKpiNearLabel(driver, label);
    }

    public boolean totalsConsistentWhenParseable() {
        OptionalInt total = readTotalAnalyses();
        OptionalInt processing = readInProgress();
        OptionalInt completed = readCompleted();
        if (total.isEmpty() || processing.isEmpty() || completed.isEmpty()) {
            return false;
        }
        return total.getAsInt() == processing.getAsInt() + completed.getAsInt();
    }
}
