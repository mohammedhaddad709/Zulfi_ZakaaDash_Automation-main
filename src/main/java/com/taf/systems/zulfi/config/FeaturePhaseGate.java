package com.taf.systems.zulfi.config;

/**
 * Phase gate shared by the club-data stories (same semantics as {@code HomeCurrentResultsGate}):
 * property unset/{@code none} → Skip; {@code a} smoke, {@code b} live-data regression, {@code c} design/deep checks.
 */
public final class FeaturePhaseGate {

    public enum Phase { NONE, A, B, C }

    public static final FeaturePhaseGate CLUB_DETAILS =
            new FeaturePhaseGate("club.details.phase", "docs/clubdetails/US-FAP-CLUB-DETAILS_TEST_STRATEGY.md");
    public static final FeaturePhaseGate STANDINGS =
            new FeaturePhaseGate("standings.phase", "docs/standings/US-FAP-STANDINGS_TEST_STRATEGY.md");
    public static final FeaturePhaseGate CLUB_DATA =
            new FeaturePhaseGate("club.data.phase", "docs/clubdata/US-FAP-ZULFI-DATA_TEST_STRATEGY.md");

    private final String property;
    private final String strategyDoc;

    private FeaturePhaseGate(String property, String strategyDoc) {
        this.property = property;
        this.strategyDoc = strategyDoc;
    }

    public String property() {
        return property;
    }

    public Phase current() {
        return PhaseGates.parse(property, Phase.class);
    }

    public void requireAtLeast(Phase minimum) {
        PhaseGates.require(property, current(), minimum, property.replace(".phase", "").toUpperCase(), strategyDoc);
    }
}
