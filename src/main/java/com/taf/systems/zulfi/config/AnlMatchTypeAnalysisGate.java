package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-ANL-MATCH-TYPE acceptance tests. */
public final class AnlMatchTypeAnalysisGate {

    public static final String PHASE_PROPERTY = "anl.match.type.phase";

    private AnlMatchTypeAnalysisGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "ANL-MATCH-TYPE", "docs/analysis/US-FAP-ANL-MATCH-TYPE_TEST_STRATEGY.md");
    }

    public static void requireSubmitPhaseE() {
        requirePhaseAtLeast(Phase.E);
        AnalysisCreateGate.requireCreationEnabled();
    }
}
