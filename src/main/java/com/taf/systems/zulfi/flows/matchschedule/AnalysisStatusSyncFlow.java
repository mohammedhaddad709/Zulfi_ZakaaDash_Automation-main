package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage.MatchResultScores;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/** US-FAP-ANL-SYNC — schedule grid status and navigation orchestration (read-only default). */
public final class AnalysisStatusSyncFlow {

    private final WebDriver driver;

    public AnalysisStatusSyncFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open Match Schedule")
    public MatchSchedulePage openMatchSchedule() {
        return new MatchSchedulePage(driver).openFromSidebar();
    }

    @Step("Refresh Match Schedule grid")
    public MatchSchedulePage refreshMatchSchedule() {
        return new MatchSchedulePage(driver).openScheduleAndRefresh();
    }

    public Optional<WebElement> findRowWithStatus(String statusFragment) {
        return schedule().findFirstRowWithAnalysisStatus(statusFragment);
    }

    public Optional<WebElement> findRowByMatchIdFragment(String matchIdFragment) {
        return schedule().getRowByMatchIdFragment(matchIdFragment);
    }

    @Step("Open Analysis Details from schedule row (read-only navigation)")
    public String openViewDetailsFromRow(WebElement row) {
        schedule().clickViewDetailsOnRow(row);
        return driver.getCurrentUrl();
    }

    @Step("Open Analysis Dashboard from schedule row (read-only navigation)")
    public String openViewDashboardFromRow(WebElement row) {
        schedule().clickViewDashboardOnRow(row);
        return driver.getCurrentUrl();
    }

    public Optional<MatchResultScores> readScores(WebElement row) {
        return schedule().parseMatchResultScores(row);
    }

    public boolean rowMatchesStatusActionRules(WebElement row, String expectedStatus) {
        String status = schedule().readAnalysisStatusForRow(row);
        if (!status.contains(expectedStatus)) {
            return false;
        }
        return switch (expectedStatus) {
            case MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED ->
                    schedule().isStartAnalysisVisible(row)
                            && !schedule().isViewDetailsVisible(row)
                            && !schedule().isViewDashboardVisible(row);
            case MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS ->
                    !schedule().isStartAnalysisVisible(row) && schedule().isViewDetailsVisible(row);
            case MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED ->
                    !schedule().isStartAnalysisVisible(row)
                            && schedule().isViewDetailsVisible(row)
                            && schedule().isViewDashboardVisible(row);
            default -> false;
        };
    }

    private MatchSchedulePage schedule() {
        return new MatchSchedulePage(driver);
    }
}
