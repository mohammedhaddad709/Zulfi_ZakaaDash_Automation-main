package com.taf.systems.zulfi.flows.matchschedule;

/** Observed schedule ↔ official match identity (UI/manifest; not inferred from URL alone). */
public record ScheduleOfficialMatchIdentityEvidence(
        String configuredTargetExternalEventId,
        String scheduleRowExternalEventId,
        String scheduleRowMatchIdNumeric,
        String createUrlExternalEventId,
        String uiOfficialMatchNumericId,
        String manifestOfficialMatchId,
        String manifestExternalEventId,
        LinkageStatus linkageStatus,
        String diagnostic) {

    public enum LinkageStatus {
        /** UI combobox/form shows #ID consistent with schedule/manifest. */
        CONFIRMED_UI_OFFICIAL_MATCH_ID,
        /** latest-manifest.json maps externalEventId → matchId. */
        CONFIRMED_MANIFEST_MAPPING,
        /** Row href externalEventId equals configured target and form URL. */
        CONFIRMED_URL_BINDING,
        /** externalEventId present but internal Official Match ID not observable. */
        MAPPING_UNVERIFIED,
        /** Conflicting IDs across UI, URL, schedule row, or manifest. */
        DISCREPANCY
    }

    public boolean isDiscrepancyBlocker() {
        return linkageStatus == LinkageStatus.DISCREPANCY;
    }

    public boolean isMappingUnverified() {
        return linkageStatus == LinkageStatus.MAPPING_UNVERIFIED;
    }

    public String bestKnownOfficialMatchId() {
        if (uiOfficialMatchNumericId != null && !uiOfficialMatchNumericId.isBlank()) {
            return uiOfficialMatchNumericId;
        }
        if (manifestOfficialMatchId != null && !manifestOfficialMatchId.isBlank()) {
            return manifestOfficialMatchId;
        }
        if (scheduleRowMatchIdNumeric != null && !scheduleRowMatchIdNumeric.isBlank()) {
            return scheduleRowMatchIdNumeric;
        }
        return "";
    }
}
