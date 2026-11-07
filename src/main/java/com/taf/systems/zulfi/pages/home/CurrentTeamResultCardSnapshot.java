package com.taf.systems.zulfi.pages.home;

/** Parsed Home «نتائج الفريق الحالية» card for assertions. */
public record CurrentTeamResultCardSnapshot(
        int index,
        String rawText,
        int homeScore,
        int awayScore,
        boolean scorePresent,
        String dateToken,
        String analysisStatus) {}
