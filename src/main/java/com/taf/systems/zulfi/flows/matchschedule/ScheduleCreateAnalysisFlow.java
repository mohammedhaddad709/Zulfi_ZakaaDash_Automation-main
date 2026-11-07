package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Exactly-once submit for schedule-sourced official analysis (no Home fixture dropdown). */
public final class ScheduleCreateAnalysisFlow {

    private final WebDriver driver;

    public ScheduleCreateAnalysisFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Submit schedule official analysis exactly once")
    public AnalysisPostSubmitOutcome submitScheduleOfficialExactlyOnce(
            SchedulePreparedCreateContext prepared, AnalysisSubmissionAttempt attempt) {
        MatchScheduleAnalysisGate.requireExplicitScheduleSubmitAuthorized();
        AnalysisCreateGate.requireCreationEnabled();
        CreateAnalysisPage form = prepared.form();
        if (attempt.submissionAttempted()) {
            throw new IllegalStateException("Exactly-once violation before schedule submit");
        }
        String fixtureKey =
                prepared.configuredTargetExternalEventId()
                        + "|"
                        + prepared.homeTeamDisplay()
                        + "×"
                        + prepared.awayTeamDisplay();
        attempt.selectFixtureBeforeSubmit(fixtureKey);
        assertFinalIdentityBeforeSubmit(prepared);
        if (!form.isSubmitEnabled()) {
            throw new IllegalStateException("Submit disabled immediately before authorized click");
        }
        attempt.recordSubmitClick();
        form.submitOnce();
        AnalysisPostSubmitOutcome outcome = form.waitForPostSubmitOutcome();
        attempt.recordOutcome(outcome);
        return outcome;
    }

    private void assertFinalIdentityBeforeSubmit(SchedulePreparedCreateContext prepared) {
        String urlId = prepared.form().readExternalEventIdFromCurrentUrl();
        if (!prepared.configuredTargetExternalEventId().equals(urlId)) {
            throw new IllegalStateException(
                    "Refusing submit: URL externalEventId="
                            + urlId
                            + " != configured target="
                            + prepared.configuredTargetExternalEventId());
        }
        if (prepared.identityEvidence().isDiscrepancyBlocker()) {
            throw new IllegalStateException(
                    "Refusing submit due to identity DISCREPANCY: "
                            + prepared.identityEvidence().diagnostic());
        }
        if (!prepared.scheduleRow().played()) {
            throw new IllegalStateException("Refusing submit: schedule row is not played");
        }
    }

    public Long readAnalysisIdAfterSubmit(CreateAnalysisPage form) {
        return CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl());
    }
}
