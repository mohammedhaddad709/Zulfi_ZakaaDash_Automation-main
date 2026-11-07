package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/** US-FAP-MCH-ANALYSIS — navigation and pop-up orchestration. */
public final class MatchScheduleAnalysisFlow {

    private final WebDriver driver;
    private final MatchScheduleBrowseFlow browseFlow;

    public MatchScheduleAnalysisFlow(WebDriver driver) {
        this.driver = driver;
        this.browseFlow = new MatchScheduleBrowseFlow(driver);
    }

    @Step("Open Match Schedule")
    public MatchSchedulePage openMatchSchedule() {
        return browseFlow.openFromSidebar();
    }

    @Step("Start analysis from eligible schedule row")
    public ScheduleOfficialAnalysisPopupPage startAnalysisFromFirstEligibleRow() {
        ScheduleAnalysisOpenResult opened =
                tryOpenOfficialAnalysisPopupFromFirstEligibleRow()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No eligible schedule row for analysis entry."));
        return opened.popup();
    }

    @Step("Open official analysis pop-up from first eligible schedule row")
    public Optional<ScheduleAnalysisOpenResult> tryOpenOfficialAnalysisPopupFromFirstEligibleRow() {
        MatchSchedulePage schedule = openMatchSchedule();
        Optional<MatchScheduleAnalysisRowContext> context =
                schedule.findFirstEligibleAnalysisRowContext();
        if (context.isEmpty()) {
            return Optional.empty();
        }
        MatchScheduleAnalysisRowContext rowContext = context.get();
        schedule.clickStartAnalysisForRowContext(rowContext);
        ScheduleOfficialAnalysisPopupPage popup =
                new ScheduleOfficialAnalysisPopupPage(driver).waitUntilReady();
        return Optional.of(new ScheduleAnalysisOpenResult(rowContext, popup));
    }
}
