package com.taf.systems.zulfi.flows.matchschedule;

import java.util.List;

/** Read-only post-submit persistence evidence (no mutation). */
public record PostSubmitReadOnlyVerificationResult(
        String descriptionMarker,
        String targetExternalEventId,
        String homeTeam,
        String awayTeam,
        String matchDate,
        List<String> searchAttempts,
        boolean recordFound,
        boolean descriptionVisibleOnListing,
        boolean teamsMatchOnCard,
        boolean officialTypeObserved,
        String analysisIdCaptured,
        String analysisStatusObserved,
        String matchingCardExcerpt,
        boolean persistenceAfterRefresh,
        String identityMappingVerdict,
        String evidenceSummary) {

    public boolean creationConfirmed() {
        return recordFound && (descriptionVisibleOnListing || analysisIdCaptured != null && !analysisIdCaptured.isBlank());
    }

    public boolean persistenceVerified() {
        return persistenceAfterRefresh && recordFound;
    }
}
