package com.taf.systems.zulfi.pages.matchschedule;

/** Schedule row identity captured before opening analysis pop-up (US-FAP-MCH-ANALYSIS). */
public record MatchScheduleAnalysisRowContext(
        String rowText,
        String analysisStatus,
        String matchIdNumeric,
        String externalEventId,
        int homeScore,
        int awayScore,
        String scoreDisplay,
        String dateToken,
        boolean played) {}
