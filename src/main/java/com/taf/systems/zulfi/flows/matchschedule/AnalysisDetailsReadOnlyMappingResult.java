package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.analysis.identity.AnalysisPageSourceIdentityScanner.AnalysisPageSourceIdentityScan;

/** Read-only identity probe for an existing analysis record. */
public record AnalysisDetailsReadOnlyMappingResult(
        long analysisId,
        String targetExternalEventId,
        String descriptionVisible,
        String matchNameVisible,
        String lifecycleStatus,
        AnalysisPageSourceIdentityScan pageSourceScan,
        String scheduleRowExternalEventId,
        String scheduleRowMatchIdNumeric,
        String inferredOfficialMatchId,
        String inferredExternalEventIdFromDetails,
        String createRouteUrlExternalEventId,
        String createRouteUiOfficialMatchId,
        MappingVerdict mappingVerdict,
        String apiProbeSummary,
        String narrative) {

    public enum MappingVerdict {
        MAPPING_VERIFIED,
        MAPPING_UNVERIFIED,
        MAPPING_DISCREPANCY
    }
}
