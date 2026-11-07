package com.taf.systems.zulfi.config;


import java.util.Optional;
/** Phased gate for US-FAP-ANL-003 acceptance tests. */
public final class AnlsLstGate {

    public static final String PHASE_PROPERTY = "anl.lst.phase";

    private AnlsLstGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "ANL-LST", "docs/analysis/US-FAP-ANL-003_LISTING_TEST_STRATEGY.md");
    }

    /** When true, phase-d global empty-state tests may run against live empty tenant. */
    public static boolean allowLiveEmptyStateChecks() {
        return Boolean.parseBoolean(System.getProperty("anl.lst.allowEmptyStateChecks", "false"));
    }

    /** When true, phase-d loading/error/retry tests may run (simulated failure hooks). */
    public static boolean allowErrorStateHooks() {
        return Boolean.parseBoolean(System.getProperty("anl.lst.allowErrorStateHooks", "false"));
    }

    /** Optional search needle guaranteed to yield zero matches (phase d). */
    public static Optional<String> configuredNoMatchSearchTerm() {
        String raw = System.getProperty("anl.lst.noMatchSearch");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("ANL_LST_NO_MATCH_SEARCH");
        }
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim());
    }
}
