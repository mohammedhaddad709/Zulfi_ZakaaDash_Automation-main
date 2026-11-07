package com.taf.systems.zulfi.config;

/** Phased gate for US-FAP-HOME-001 acceptance tests. */
public final class HomeOverviewGate {

    public static final String PHASE_PROPERTY = "home.overview.phase";

    private HomeOverviewGate() {}

    public enum Phase {
        NONE,
        A,
        B,
        C,
        D
    }

    public static Phase currentPhase() {
        return PhaseGates.parse(PHASE_PROPERTY, Phase.class);
    }

    public static void requirePhaseAtLeast(Phase minimum) {
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "HOME-001", "docs/home/US-FAP-HOME-001_TEST_STRATEGY.md");
    }

    /** When true, phase-d empty/error tests may run against live empty tenant data. */
    public static boolean allowLiveEmptyStateChecks() {
        return Boolean.parseBoolean(System.getProperty("home.overview.allowEmptyStateChecks", "false"));
    }

    /** When true, phase-d error/retry tests may run (simulated failure hooks). */
    public static boolean allowErrorStateHooks() {
        return Boolean.parseBoolean(System.getProperty("home.overview.allowErrorStateHooks", "false"));
    }
}
