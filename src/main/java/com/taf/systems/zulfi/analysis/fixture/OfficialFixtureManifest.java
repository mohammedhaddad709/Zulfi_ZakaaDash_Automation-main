package com.taf.systems.zulfi.analysis.fixture;

import java.util.Optional;
import java.util.List;

/** Run-specific discovery snapshot for index stability checks. */
public record OfficialFixtureManifest(
        String suiteRunId,
        String discoveredAtIso,
        boolean discoverySucceeded,
        boolean enumerationComplete,
        int scrollPassesUsed,
        int fixtureCount,
        int enabledCount,
        int disabledCount,
        List<OfficialFixtureManifestEntry> fixtures) {

    public OfficialFixtureManifestEntry entryAt(int fixtureIndex) {
        return fixtures.stream()
                .filter(entry -> entry.fixtureIndex() == fixtureIndex)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IndexOutOfBoundsException(
                                        "No manifest entry for fixture index " + fixtureIndex));
    }

    public Optional<OfficialFixtureManifestEntry> findByExternalEventId(String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            return Optional.empty();
        }
        String needle = externalEventId.trim();
        return fixtures.stream()
                .filter(entry -> needle.equals(entry.externalEventId()))
                .findFirst();
    }

    public Optional<OfficialFixtureManifestEntry> findByMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty();
        }
        String needle = matchId.trim();
        return fixtures.stream().filter(entry -> needle.equals(entry.matchId())).findFirst();
    }
}
