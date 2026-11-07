package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-HOME-CURRENT-RESULTS acceptance tests. */
public final class HomeCurrentResultsGate {

    public static final String PHASE_PROPERTY = "home.current.results.phase";

    private HomeCurrentResultsGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "HOME-CURRENT-RESULTS", "docs/home/US-FAP-HOME-CURRENT-RESULTS_TEST_STRATEGY.md");
    }

    /** Optional upper bound for HOME-RES-008 (not a business rule — layout hint). */
    public static int configuredMaxHomeResultCards() {
        String raw = System.getProperty("home.results.maxDisplayedCards", "12");
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return 12;
        }
    }
}
