package com.taf.systems.zulfi.pages.matchschedule;

/** Parsed Match Schedule row for US-FAP-MCH browse assertions. */
public record MatchScheduleRowSnapshot(
        int index,
        String rawText,
        boolean played,
        boolean unplayed,
        int homeScore,
        int awayScore,
        String dateToken,
        String kickoffTimeToken,
        String analysisStatus,
        boolean matchIdPresent) {}
