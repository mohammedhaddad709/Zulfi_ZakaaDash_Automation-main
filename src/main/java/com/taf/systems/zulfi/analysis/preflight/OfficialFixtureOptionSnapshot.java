package com.taf.systems.zulfi.analysis.preflight;

/** One row observed in the official-match PrimeVue dropdown. */
public record OfficialFixtureOptionSnapshot(
        String displayLabel,
        String rowKind,
        String homeTeam,
        String awayTeam,
        String roundLabel,
        boolean enabled,
        boolean selectable,
        String matchId,
        String externalEventId) {

    public static OfficialFixtureOptionSnapshot roundHeader(String label) {
        return new OfficialFixtureOptionSnapshot(
                label, "ROUND_HEADER", "Not available", "Not available", label, false, false, "Not available", "Not available");
    }

    public static OfficialFixtureOptionSnapshot fixtureRow(
            String label, boolean enabled, String matchId, String externalEventId) {
        String[] teams = FixtureLabelParser.splitTeams(label);
        return new OfficialFixtureOptionSnapshot(
                label,
                "FIXTURE",
                teams[0],
                teams[1],
                "Not available",
                enabled,
                enabled,
                nullToNa(matchId),
                nullToNa(externalEventId));
    }

    private static String nullToNa(String value) {
        return value == null || value.isBlank() ? "Not available" : value.trim();
    }
}
