package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-ANL-FRIENDLY acceptance tests. */
public final class AnlFriendlyAnalysisGate {

    public static final String PHASE_PROPERTY = "anl.friendly.phase";

    private AnlFriendlyAnalysisGate() {}

    public enum Phase {
        NONE,
        A,
        B,
        C,
        D,
        E
    }

    public static Phase currentPhase() {
        return PhaseGates.parse(PHASE_PROPERTY, Phase.class);
    }

    public static void requirePhaseAtLeast(Phase minimum) {
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "ANL-FRIENDLY", "docs/analysis/US-FAP-ANL-FRIENDLY_TEST_STRATEGY.md");
    }

    public static void requireFriendlySubmitPhaseE() {
        requirePhaseAtLeast(Phase.E);
        AnalysisCreateGate.requireFriendlyCreationEnabled();
    }
}
