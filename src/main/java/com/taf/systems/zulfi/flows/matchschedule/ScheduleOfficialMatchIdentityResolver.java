package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;

/** Resolves externalEventId ↔ Official Match ID using UI + optional discovery manifest only. */
public final class ScheduleOfficialMatchIdentityResolver {

    private ScheduleOfficialMatchIdentityResolver() {}

    public static ScheduleOfficialMatchIdentityEvidence resolve(
            String configuredTargetExternalEventId,
            MatchScheduleAnalysisRowContext row,
            CreateAnalysisPage form) {
        String urlEventId = form.readExternalEventIdFromCurrentUrl();
        String uiId = MatchScheduleAnalysisHappyPathFlow.readUiOfficialMatchNumericId(form);
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        OfficialFixtureManifestEntry manifestEntry = null;
        if (manifest != null && !urlEventId.isBlank()) {
            manifestEntry = manifest.findByExternalEventId(urlEventId).orElse(null);
        }
        String manifestMatchId = manifestEntry == null ? "" : nullToEmpty(manifestEntry.matchId());
        String manifestExtId = manifestEntry == null ? "" : nullToEmpty(manifestEntry.externalEventId());

        if (!configuredTargetExternalEventId.equals(urlEventId)) {
            return evidence(
                    configuredTargetExternalEventId,
                    row,
                    urlEventId,
                    uiId,
                    manifestMatchId,
                    manifestExtId,
                    ScheduleOfficialMatchIdentityEvidence.LinkageStatus.DISCREPANCY,
                    "Configured target externalEventId does not match create URL");
        }
        if (!row.externalEventId().isBlank() && !row.externalEventId().equals(urlEventId)) {
            return evidence(
                    configuredTargetExternalEventId,
                    row,
                    urlEventId,
                    uiId,
                    manifestMatchId,
                    manifestExtId,
                    ScheduleOfficialMatchIdentityEvidence.LinkageStatus.DISCREPANCY,
                    "Schedule row externalEventId does not match create URL");
        }

        if (!uiId.isBlank()) {
            if (!manifestMatchId.isBlank() && !uiId.equals(manifestMatchId)) {
                return evidence(
                        configuredTargetExternalEventId,
                        row,
                        urlEventId,
                        uiId,
                        manifestMatchId,
                        manifestExtId,
                        ScheduleOfficialMatchIdentityEvidence.LinkageStatus.DISCREPANCY,
                        "UI #ID does not match manifest matchId");
            }
            if (!row.matchIdNumeric().isBlank()
                    && !uiId.equals(row.matchIdNumeric())
                    && !uiId.equals(urlEventId)) {
                return evidence(
                        configuredTargetExternalEventId,
                        row,
                        urlEventId,
                        uiId,
                        manifestMatchId,
                        manifestExtId,
                        ScheduleOfficialMatchIdentityEvidence.LinkageStatus.DISCREPANCY,
                        "UI #ID does not match schedule row matchId");
            }
            return evidence(
                    configuredTargetExternalEventId,
                    row,
                    urlEventId,
                    uiId,
                    manifestMatchId,
                    manifestExtId,
                    ScheduleOfficialMatchIdentityEvidence.LinkageStatus.CONFIRMED_UI_OFFICIAL_MATCH_ID,
                    "Official Match #ID visible on create form");
        }

        if (!manifestMatchId.isBlank() && manifestExtId.equals(urlEventId)) {
            return evidence(
                    configuredTargetExternalEventId,
                    row,
                    urlEventId,
                    uiId,
                    manifestMatchId,
                    manifestExtId,
                    ScheduleOfficialMatchIdentityEvidence.LinkageStatus.CONFIRMED_MANIFEST_MAPPING,
                    "Manifest maps externalEventId to matchId="
                            + manifestMatchId);
        }

        if (!row.matchIdNumeric().isBlank() && row.matchIdNumeric().equals(urlEventId)) {
            return evidence(
                    configuredTargetExternalEventId,
                    row,
                    urlEventId,
                    uiId,
                    manifestMatchId,
                    manifestExtId,
                    ScheduleOfficialMatchIdentityEvidence.LinkageStatus.CONFIRMED_URL_BINDING,
                    "Schedule row matchId equals externalEventId (same numeric identity)");
        }

        return evidence(
                configuredTargetExternalEventId,
                row,
                urlEventId,
                uiId,
                manifestMatchId,
                manifestExtId,
                ScheduleOfficialMatchIdentityEvidence.LinkageStatus.MAPPING_UNVERIFIED,
                "Only URL externalEventId observed; internal Official Match ID linkage not proven");
    }

    private static ScheduleOfficialMatchIdentityEvidence evidence(
            String configuredTarget,
            MatchScheduleAnalysisRowContext row,
            String urlEventId,
            String uiId,
            String manifestMatchId,
            String manifestExtId,
            ScheduleOfficialMatchIdentityEvidence.LinkageStatus status,
            String diagnostic) {
        return new ScheduleOfficialMatchIdentityEvidence(
                configuredTarget,
                row.externalEventId(),
                row.matchIdNumeric(),
                urlEventId,
                uiId,
                manifestMatchId,
                manifestExtId,
                status,
                diagnostic);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
