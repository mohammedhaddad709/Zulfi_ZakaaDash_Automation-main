package com.taf.systems.zulfi.config;

import org.testng.SkipException;

import java.util.Optional;

/** Phased automation gate for US-FAP-MCH-ANALYSIS (Match Schedule entry). */
public final class MatchScheduleAnalysisGate {

    public static final String PHASE_PROPERTY = "match.schedule.analysis.phase";
    public static final String TARGET_MATCH_ID_PROPERTY = "match.schedule.analysis.targetMatchId";
    public static final String TARGET_MATCH_ID_ENV = "MCH_ANALYSIS_MATCH_ID";
    /** Explicit authorization for MCH-ANL-010 submit (in addition to phase C + analysis.create.enabled). */
    public static final String SUBMIT_ENABLED_PROPERTY = "match.schedule.analysis.submit.enabled";
    public static final String TARGET_EXTERNAL_EVENT_ID_PROPERTY =
            "match.schedule.analysis.targetExternalEventId";
    public static final String TARGET_EXTERNAL_EVENT_ID_ENV = "MCH_ANALYSIS_EXTERNAL_EVENT_ID";
    public static final String HAPPY_PATH_DESCRIPTION_ENV = "MCH_ANALYSIS_DESCRIPTION";
    public static final String HAPPY_PATH_DESCRIPTION_PROPERTY = "match.schedule.analysis.description";

    /**
     * Extra authorization for the duplicate probe: after the single creating submit, a stale second form is submitted
     * once and MUST be rejected. Needs the other submit gates too; without it the probe steps are reported as skipped.
     */
    public static final String DUPLICATE_PROBE_ENABLED_PROPERTY = "match.schedule.analysis.duplicateCheck.enabled";

    private MatchScheduleAnalysisGate() {}

    public static boolean isDuplicateProbeEnabled() {
        return Boolean.parseBoolean(System.getProperty(DUPLICATE_PROBE_ENABLED_PROPERTY, "false"));
    }

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "Match Schedule analysis", "docs/matchschedule/US-FAP-MCH-ANALYSIS_TEST_STRATEGY.md");
    }

    public static Optional<String> configuredTargetMatchId() {
        String raw = System.getProperty(TARGET_MATCH_ID_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(TARGET_MATCH_ID_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim());
    }

    public static void requireSubmitAuthorizedForPhaseC() {
        requirePhaseAtLeast(Phase.C);
        AnalysisCreateGate.requireCreationEnabled();
    }

    /** Pre-submit HP verification: phase C, creation gate must stay OFF. */
    public static void requirePreSubmitVerificationMode() {
        requirePhaseAtLeast(Phase.C);
        if (AnalysisCreateGate.isCreationEnabled()) {
            throw new SkipException(
                    "Pre-submit verification requires -Danalysis.create.enabled=false (default). "
                            + "Do not run with creation enabled in the same session as pre-submit checks.");
        }
    }

    public static void requireExplicitScheduleSubmitAuthorized() {
        requireSubmitAuthorizedForPhaseC();
        if (!Boolean.parseBoolean(System.getProperty(SUBMIT_ENABLED_PROPERTY, "false"))) {
            throw new SkipException(
                    "Schedule happy-path submit requires -D"
                            + SUBMIT_ENABLED_PROPERTY
                            + "=true in addition to -Danalysis.create.enabled=true.");
        }
    }

    public static boolean isExplicitScheduleSubmitEnabled() {
        return Boolean.parseBoolean(System.getProperty(SUBMIT_ENABLED_PROPERTY, "false"));
    }

    public static Optional<String> configuredTargetExternalEventId() {
        Optional<String> threadPinned = MatchScheduleAnalysisTargetRegistry.externalEventId();
        if (threadPinned.isPresent()) {
            return threadPinned;
        }
        String raw = System.getProperty(TARGET_EXTERNAL_EVENT_ID_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(TARGET_EXTERNAL_EVENT_ID_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return configuredTargetMatchId();
        }
        return Optional.of(raw.trim());
    }

    /** Authorized submit runs must pin one verified schedule fixture (no silent fallback scan). */
    /** Non-empty Description for MCH-ANL-010 (avoids non-blocking MCH-ANL-007 label ambiguity). */
    public static String resolveHappyPathDescription() {
        String raw = System.getProperty(HAPPY_PATH_DESCRIPTION_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(HAPPY_PATH_DESCRIPTION_ENV);
        }
        if (raw == null || raw.isBlank()) {
            return "US-FAP-MCH-ANALYSIS — MCH-ANL-010 schedule verification";
        }
        return raw.trim();
    }

    public static String requireExactTargetExternalEventIdForSubmit() {
        return resolveSubmitTargetExternalEventId();
    }

    /**
     * Pinned -D / env id, else harness from read-only tests (003), else {@link SkipException}.
     */
    public static String resolveSubmitTargetExternalEventId() {
        requireExplicitScheduleSubmitAuthorized();
        return configuredTargetExternalEventId()
                .orElseThrow(
                        () ->
                                new SkipException(
                                        "Schedule submit requires -D"
                                                + TARGET_EXTERNAL_EVENT_ID_PROPERTY
                                                + "=<verified-id>, "
                                                + TARGET_EXTERNAL_EVENT_ID_ENV
                                                + ", or run 003 / chain discovery first."));
    }
}
