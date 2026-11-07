package com.taf.systems.zulfi.config;


import java.util.Optional;
/** Phased gate for US-FAP-MCH (Match Schedule view / filter). */
public final class MchScheduleGate {

    public static final String PHASE_PROPERTY = "mch.schedule.phase";

    private MchScheduleGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "MCH-SCHED", "docs/matchschedule/US-FAP-MCH_SCHEDULE_TEST_STRATEGY.md");
    }

    /** Optional club fragment that yields zero rows for MCH-SCHED-015 (phase c). */
    public static Optional<String> configuredEmptyFilterClubFragment() {
        String raw = System.getProperty("mch.schedule.emptyFilterClub");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("MCH_SCHEDULE_EMPTY_FILTER_CLUB");
        }
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim());
    }
}
