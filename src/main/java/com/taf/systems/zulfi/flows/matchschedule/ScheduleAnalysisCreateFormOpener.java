package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;

import java.util.Optional;

/** Opens schedule-bound Create Analysis form for a pinned externalEventId (no submit). */
public final class ScheduleAnalysisCreateFormOpener {

    private final WebDriver driver;
    private final MatchScheduleAnalysisFlow scheduleFlow;

    public ScheduleAnalysisCreateFormOpener(WebDriver driver) {
        this.driver = driver;
        this.scheduleFlow = new MatchScheduleAnalysisFlow(driver);
    }

    public record OpenedScheduleCreateForm(
            MatchScheduleAnalysisRowContext scheduleRow, ScheduleOfficialAnalysisPopupPage createView) {
        public CreateAnalysisPage form() {
            return createView.form();
        }
    }

    @Step("Open Create Analysis from schedule for externalEventId")
    public OpenedScheduleCreateForm openForExternalEventId(String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            throw new IllegalArgumentException("externalEventId required");
        }
        MatchSchedulePage schedule = scheduleFlow.openMatchSchedule();
        MatchScheduleAnalysisRowContext row =
                schedule
                        .findEligibleAnalysisRowContext(Optional.of(externalEventId.trim()))
                        .orElseThrow(
                                () ->
                                        new SkipException(
                                                "No eligible schedule row for externalEventId="
                                                        + externalEventId));
        if (!externalEventId.trim().equals(row.externalEventId())
                && !externalEventId.trim().equals(row.matchIdNumeric())) {
            throw new SkipException(
                    "Refusing fixture switch: row externalEventId="
                            + row.externalEventId()
                            + " matchId="
                            + row.matchIdNumeric()
                            + " != target="
                            + externalEventId);
        }
        schedule.waitForStartAnalysisActionVisibleForContext(row);
        schedule.clickStartAnalysisForRowContext(row);
        ScheduleOfficialAnalysisPopupPage createView =
                new ScheduleOfficialAnalysisPopupPage(driver).waitUntilReady();
        String urlId = createView.form().readExternalEventIdFromCurrentUrl();
        if (!externalEventId.trim().equals(urlId)) {
            throw new IllegalStateException(
                    "Create URL externalEventId=" + urlId + " != expected=" + externalEventId);
        }
        return new OpenedScheduleCreateForm(row, createView);
    }

    public static String configuredTargetOrProperty() {
        return MatchScheduleAnalysisGate.configuredTargetExternalEventId()
                .orElseThrow(
                        () ->
                                new SkipException(
                                        "Configure -D"
                                                + MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY
                                                + "=<id>"));
    }
}
