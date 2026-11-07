package com.taf.systems.zulfi.flows.matchschedule;

/** Captured state at pre-submit gate for MCH-ANL-010 reporting (no mutation). */
public record ScheduleHappyPathPreSubmitSnapshot(
        String scheduleRowText,
        String scheduleExternalEventId,
        String scheduleMatchIdNumeric,
        String createUrlExternalEventId,
        String authoritativeOfficialMatchId,
        String homeTeamDisplay,
        String awayTeamDisplay,
        String matchDateValue,
        String scoreOnScheduleRow,
        String videoUrlEntered,
        boolean descriptionOptionalLeftEmpty,
        boolean submitControlEnabled,
        boolean creationGateEnabled,
        boolean submitGateEnabled,
        boolean submitAttempted) {

    public String toReportingLine() {
        return "externalEventId="
                + scheduleExternalEventId
                + "; urlEventId="
                + createUrlExternalEventId
                + "; officialMatchId="
                + authoritativeOfficialMatchId
                + "; home="
                + homeTeamDisplay
                + "; away="
                + awayTeamDisplay
                + "; date="
                + matchDateValue
                + "; score="
                + scoreOnScheduleRow
                + "; video="
                + videoUrlEntered
                + "; submitEnabled="
                + submitControlEnabled
                + "; createGate="
                + creationGateEnabled
                + "; submitAttempted="
                + submitAttempted;
    }
}
