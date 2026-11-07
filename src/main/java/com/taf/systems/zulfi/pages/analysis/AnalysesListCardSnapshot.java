package com.taf.systems.zulfi.pages.analysis;

/** Parsed analysis card on the التحليلات listing page. */
public record AnalysesListCardSnapshot(
        int index,
        String rawText,
        String matchTitle,
        String submissionDateToken,
        String videoSource,
        String status,
        boolean viewDetailsPresent,
        boolean viewDetailsEnabled,
        boolean dashboardPresent,
        boolean dashboardEnabled) {}
