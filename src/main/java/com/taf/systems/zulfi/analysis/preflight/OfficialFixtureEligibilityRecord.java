package com.taf.systems.zulfi.analysis.preflight;

/** Evaluated fixture row for markdown report. */
public record OfficialFixtureEligibilityRecord(
        String fixture,
        String matchDate,
        String homeTeam,
        String awayTeam,
        String enabled,
        String existingAnalysisEvidence,
        String matchId,
        EligibilityStatus eligibilityStatus,
        String eligibilityReason,
        String evidenceSource,
        String recommendedAction) {}
