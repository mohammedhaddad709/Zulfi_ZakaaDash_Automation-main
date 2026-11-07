package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * Yellow League schedule table — grid load, row discovery, and analysis entry hover/click (no submit).
 */
public final class MatchScheduleTableFlow {

    private final WebDriver driver;
    private final MatchScheduleBrowseFlow browseFlow;

    public MatchScheduleTableFlow(WebDriver driver) {
        this.driver = driver;
        this.browseFlow = new MatchScheduleBrowseFlow(driver);
    }

    @Step("Open جدول المباريات (/matches) and wait for grid")
    public MatchSchedulePage openScheduleDirect() {
        return browseFlow.openDirectly();
    }

    @Step("Open جدول المباريات from sidebar")
    public MatchSchedulePage openScheduleFromSidebar() {
        return browseFlow.openFromSidebar();
    }

    @Step("Locate played «غير محللة» row eligible for analysis")
    public Optional<MatchScheduleAnalysisRowContext> findEligibleNotAnalyzedRow(
            Optional<String> targetExternalEventId) {
        MatchSchedulePage schedule = page();
        schedule.waitUntilLoaded();
        return schedule.findEligibleAnalysisRowContext(targetExternalEventId);
    }

    @Step("Hover schedule row to reveal start-analysis action")
    public MatchSchedulePage hoverToRevealStartAnalysis(MatchScheduleAnalysisRowContext row) {
        MatchSchedulePage schedule = page();
        schedule.hoverNotAnalyzedRowToRevealStartAnalysis(row);
        return schedule;
    }

    public boolean isStartAnalysisVisibleAfterHover(MatchScheduleAnalysisRowContext row) {
        return page().isStartAnalysisActionVisibleAfterHover(row);
    }

    @Step("Click «بدء تحليل المباراة» on schedule row")
    public ScheduleOfficialAnalysisPopupPage openCreateAnalysisFromRow(
            MatchScheduleAnalysisRowContext row) {
        page().clickStartAnalysisForRowContext(row);
        return new ScheduleOfficialAnalysisPopupPage(driver).waitUntilReady();
    }

    @Step("Find schedule row by externalEventId (any analysis status)")
    public Optional<MatchScheduleAnalysisRowContext> findRowByExternalEventIdReadOnly(
            String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            return Optional.empty();
        }
        return page()
                .findRowContextByFixtureIdentityReadOnly(externalEventId.trim(), "", "", "");
    }

    /**
     * US-FAP-MCH: after successful submit the fixture must not stay «غير محللة»; expect «قيد التحليل»
     * or «اكتمل التحليل» and no «بدء تحليل» entry.
     */
    @Step("Verify schedule row reflects submitted analysis")
    public PostSubmitScheduleRowVerdict verifyPostSubmitScheduleRow(
            String externalEventId,
            String matchIdNumeric,
            String homeTeam,
            String awayTeam,
            String scoreDisplay) {
        MatchSchedulePage schedule = openScheduleDirect();
        schedule.waitUntilLoaded();
        Optional<MatchScheduleAnalysisRowContext> row =
                schedule.findRowContextByFixtureIdentityReadOnly(
                        externalEventId, homeTeam, awayTeam, scoreDisplay);
        if (row.isEmpty() && matchIdNumeric != null && !matchIdNumeric.isBlank()) {
            row =
                    schedule
                            .getRowByMatchIdFragment(matchIdNumeric.trim())
                            .map(schedule::captureAnalysisRowContext);
        }
        if (row.isEmpty()) {
            schedule.openScheduleAndRefresh();
            row =
                    schedule.findRowContextByFixtureIdentityReadOnly(
                            externalEventId, homeTeam, awayTeam, scoreDisplay);
            if (row.isEmpty() && matchIdNumeric != null && !matchIdNumeric.isBlank()) {
                row =
                        schedule
                                .getRowByMatchIdFragment(matchIdNumeric.trim())
                                .map(schedule::captureAnalysisRowContext);
            }
        }
        if (row.isEmpty()) {
            boolean stillEligible =
                    findEligibleNotAnalyzedRow(Optional.of(externalEventId)).isPresent();
            if (!stillEligible) {
                return new PostSubmitScheduleRowVerdict(
                        true,
                        externalEventId,
                        "row-not-visible;eligible=false",
                        false,
                        false,
                        true,
                        true);
            }
            return PostSubmitScheduleRowVerdict.notFound(externalEventId);
        }
        MatchScheduleAnalysisRowContext context = row.get();
        String status = schedule.resolveAnalysisStatusForContext(context);
        boolean postSubmit = MatchScheduleAnalysisStatusMessages.isPostSubmitAnalysisStatus(status);
        boolean stillOpen =
                MatchScheduleAnalysisStatusMessages.stillNotAnalyzed(status)
                        || MatchScheduleAnalysisStatusMessages.stillNotAnalyzed(context.rowText());
        boolean startAbsent = schedule.lacksStartAnalysisAction(context);
        boolean stillEligible =
                findEligibleNotAnalyzedRow(Optional.of(externalEventId)).isPresent();
        boolean stillNeedsAnalysis = stillOpen || stillEligible;
        return new PostSubmitScheduleRowVerdict(
                true,
                externalEventId,
                status,
                postSubmit,
                stillNeedsAnalysis,
                startAbsent,
                !stillEligible);
    }

    public record PostSubmitScheduleRowVerdict(
            boolean rowFound,
            String externalEventId,
            String analysisStatus,
            boolean postSubmitStatus,
            boolean stillShowsNotAnalyzed,
            boolean startAnalysisAbsent,
            boolean excludedFromEligibleScan) {

        public boolean meetsExpectation() {
            if (!rowFound) {
                return false;
            }
            if (stillShowsNotAnalyzed) {
                return false;
            }
            boolean noLongerEligible = excludedFromEligibleScan && startAnalysisAbsent;
            boolean statusLabelOk = postSubmitStatus;
            return noLongerEligible && (statusLabelOk || excludedFromEligibleScan);
        }

        static PostSubmitScheduleRowVerdict notFound(String externalEventId) {
            return new PostSubmitScheduleRowVerdict(
                    false, externalEventId, "", false, false, false, false);
        }

        public String reportingLine() {
            return "externalEventId="
                    + externalEventId
                    + "; status="
                    + analysisStatus
                    + "; postSubmit="
                    + postSubmitStatus
                    + "; stillNotAnalyzed="
                    + stillShowsNotAnalyzed
                    + "; startAnalysisAbsent="
                    + startAnalysisAbsent
                    + "; excludedFromEligible="
                    + excludedFromEligibleScan;
        }
    }

    private MatchSchedulePage page() {
        return new MatchSchedulePage(driver);
    }
}
