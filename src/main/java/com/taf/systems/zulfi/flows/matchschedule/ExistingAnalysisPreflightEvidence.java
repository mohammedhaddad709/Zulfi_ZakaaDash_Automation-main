package com.taf.systems.zulfi.flows.matchschedule;

import java.util.List;

/** Read-only evidence for whether an analysis already exists for a schedule fixture. */
public record ExistingAnalysisPreflightEvidence(
        String externalEventIdSearched,
        String internalMatchIdSearched,
        List<String> searchMechanismsUsed,
        List<MatchingAnalysisRecord> matchingRecords,
        String scheduleRowAnalysisStatus,
        String scheduleRowSnapshot,
        boolean listingPageContainsExternalEventId,
        boolean listingVisibleCardMatchedEventId,
        boolean listingVisibleCardMatchedInternalMatchId,
        EligibilityVerdict verdict,
        String evidenceSummary) {

    public record MatchingAnalysisRecord(String analysisIdHint, String status, String cardTitle, String rawExcerpt) {}

    public enum EligibilityVerdict {
        /** Authoritative UI evidence that an analysis record exists for this fixture/event id. */
        EXISTING_ANALYSIS_CONFIRMED,
        /** Searches completed; no analysis record evidence found (listing + schedule status). */
        NO_EXISTING_ANALYSIS_EVIDENCE,
        /** Cannot prove presence or absence (e.g. listing search inconclusive, no API). */
        NOT_VERIFIED
    }
}
