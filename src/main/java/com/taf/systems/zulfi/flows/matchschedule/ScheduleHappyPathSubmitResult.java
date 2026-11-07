package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;

/** Post-submit MCH-ANL-010 evidence bundle. */
public record ScheduleHappyPathSubmitResult(
        SchedulePreparedCreateContext preparedContext,
        AnalysisPostSubmitOutcome submitOutcome,
        Long analysisId,
        String successMessageObserved,
        String listingStatusObserved,
        boolean officialTypeOnListing,
        boolean persistenceAfterRefresh,
        ScheduleOfficialMatchIdentityEvidence.LinkageStatus linkageStatus,
        boolean mappingUnverifiedAcceptanceBlocker) {

    public String toReportingLine() {
        return "analysisId="
                + analysisId
                + "; outcome="
                + submitOutcome
                + "; externalEventId="
                + preparedContext.identityEvidence().createUrlExternalEventId()
                + "; officialMatchId="
                + preparedContext.identityEvidence().bestKnownOfficialMatchId()
                + "; linkage="
                + linkageStatus
                + "; listingStatus="
                + listingStatusObserved
                + "; persistent="
                + persistenceAfterRefresh;
    }
}
