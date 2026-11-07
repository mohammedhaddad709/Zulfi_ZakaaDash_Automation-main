package com.taf.systems.zulfi.analysis.fixture;

/** One fixture row in the discovery manifest (canonical index). */
public record OfficialFixtureManifestEntry(
        int fixtureIndex,
        String displayLabel,
        boolean enabled,
        String homeTeam,
        String awayTeam,
        String matchId,
        String externalEventId) {}
