package com.taf.systems.zulfi.config;

import org.testng.SkipException;

import java.util.OptionalInt;

/** Phased gate for US-FAP-ANL-SYNC (schedule ↔ analysis status synchronization). */
public final class AnlsSyncGate {

    public static final String PHASE_PROPERTY = "anl.sync.phase";
    public static final String FIXTURE_INDEX_PROPERTY = "anl.sync.fixture.index";
    public static final String FIXTURE_TAGS_PROPERTY = "anl.sync.fixture.tags";
    public static final String LIFECYCLE_ENABLED_PROPERTY = "anl.sync.lifecycle.enabled";
    public static final String CREATION_ENABLED_PROPERTY = "anl.sync.creation.enabled";

    private AnlsSyncGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "ANL-SYNC", "docs/matchschedule/US-FAP-ANL-SYNC_TEST_STRATEGY.md");
    }

    public static boolean isSyncCreationExplicitlyEnabled() {
        return Boolean.parseBoolean(System.getProperty(CREATION_ENABLED_PROPERTY, "false"));
    }

    public static boolean isLifecycleCreationEnabled() {
        return Boolean.parseBoolean(System.getProperty(LIFECYCLE_ENABLED_PROPERTY, "false"));
    }

    /** Phase D lifecycle (AC-002 / AC-004) — double gate; never implied by phase alone. */
    public static void requireLifecycleSubmitAuthorized() {
        requirePhaseAtLeast(Phase.D);
        if (!isLifecycleCreationEnabled()) {
            throw new SkipException(
                    "ANL-SYNC lifecycle mutation disabled. Run with -D"
                            + LIFECYCLE_ENABLED_PROPERTY
                            + "=true to authorize creation/sync observation tests.");
        }
        if (!isSyncCreationExplicitlyEnabled()) {
            throw new SkipException(
                    "ANL-SYNC creation gate requires -D"
                            + CREATION_ENABLED_PROPERTY
                            + "=true in addition to "
                            + LIFECYCLE_ENABLED_PROPERTY
                            + "=true.");
        }
        AnalysisCreateGate.requireCreationEnabled();
    }

    public static void requirePhaseDFixtureBinding() {
        requirePhaseAtLeast(Phase.D);
        if (!hasConfiguredFixtureBinding()) {
            throw new SkipException(
                    "Phase D ANL-SYNC requires -D"
                            + FIXTURE_INDEX_PROPERTY
                            + "=<n> and/or -D"
                            + FIXTURE_TAGS_PROPERTY
                            + "=tag1,tag2 for known schedule rows.");
        }
    }

    public static boolean hasConfiguredFixtureBinding() {
        String index = System.getProperty(FIXTURE_INDEX_PROPERTY);
        if (index != null && !index.isBlank()) {
            try {
                Integer.parseInt(index.trim());
                return true;
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        String tags = System.getProperty(FIXTURE_TAGS_PROPERTY);
        return tags != null && !tags.isBlank();
    }

    public static OptionalInt configuredFixtureIndex() {
        String raw = System.getProperty(FIXTURE_INDEX_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }
}
