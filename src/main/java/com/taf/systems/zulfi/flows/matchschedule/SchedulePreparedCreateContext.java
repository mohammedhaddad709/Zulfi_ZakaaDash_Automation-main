package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;

/** Create form ready for submit after schedule pre-checks (form left open). */
public record SchedulePreparedCreateContext(
        String configuredTargetExternalEventId,
        MatchScheduleAnalysisRowContext scheduleRow,
        ScheduleOfficialAnalysisPopupPage createView,
        CreateAnalysisPage form,
        String videoUrl,
        String homeTeamDisplay,
        String awayTeamDisplay,
        String matchDateValue,
        ScheduleOfficialMatchIdentityEvidence identityEvidence) {

    public String listingSearchTitle() {
        return homeTeamDisplay + " × " + awayTeamDisplay;
    }

    public String fixtureIdentityLabel() {
        return form.getOfficialMatchSelectedLabel();
    }
}
