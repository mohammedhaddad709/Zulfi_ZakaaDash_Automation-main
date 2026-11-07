package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-CLUBS (Yellow League clubs listing) acceptance tests. */
public final class ClubsYellowLeagueGate {

    public static final String PHASE_PROPERTY = "clubs.yellow.phase";

    private ClubsYellowLeagueGate() {}

    public enum Phase {
        NONE,
        A,
        B,
        C
    }

    public static Phase currentPhase() {
        return PhaseGates.parse(PHASE_PROPERTY, Phase.class);
    }

    public static void requirePhaseAtLeast(Phase minimum) {
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "CLUBS", "docs/clubs/US-FAP-CLUBS_TEST_STRATEGY.md");
    }
}
