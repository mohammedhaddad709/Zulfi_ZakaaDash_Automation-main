package com.taf.systems.zulfi.pages.home;

/** Parsed Home «آخر التحليلات» card for assertions. */
public record RecentAnalysisCardSnapshot(
        int index,
        String rawText,
        String matchTitle,
        String status,
        boolean hasVideoSourceIndicator,
        boolean viewDetailsPresent,
        boolean viewDetailsEnabled,
        boolean dashboardPresent,
        boolean dashboardEnabled) {}
