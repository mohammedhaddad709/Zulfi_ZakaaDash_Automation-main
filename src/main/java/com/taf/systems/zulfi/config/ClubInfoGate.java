package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-CLUB-001 (Club Information — عن النادي) acceptance tests. */
public final class ClubInfoGate {

    public static final String PHASE_PROPERTY = "club.info.phase";

    private ClubInfoGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "CLUB-INFO", "docs/clubinfo/US-FAP-CLUB-001_TEST_STRATEGY.md");
    }
}
