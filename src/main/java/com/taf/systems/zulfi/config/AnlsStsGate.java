package com.taf.systems.zulfi.config;

import org.testng.SkipException;

import java.util.OptionalLong;

/** Phased gate for US-FAP-ANL-002 (Track Match Analysis Processing Status). */
public final class AnlsStsGate {

    public static final String PHASE_PROPERTY = "anl.sts.phase";
    public static final String PROCESSING_ID_PROPERTY = "anl.sts.processingAnalysisId";
    public static final String COMPLETED_ID_PROPERTY = "anl.sts.completedAnalysisId";
    public static final String OBSERVE_SUBMIT_PROPERTY = "anl.sts.observeSubmit";

    public static final String PROCESSING_ID_ENV = "ZULFI_PROCESSING_ANALYSIS_ID";
    public static final String COMPLETED_ID_ENV = "ZULFI_COMPLETED_ANALYSIS_ID";

    private AnlsStsGate() {}

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
        PhaseGates.require(PHASE_PROPERTY, currentPhase(), minimum, "ANL-STS", "docs/analysis/US-FAP-ANL-002_PROCESSING_STATUS_TEST_STRATEGY.md");
    }

    public static boolean isObserveSubmitEnabled() {
        return Boolean.parseBoolean(System.getProperty(OBSERVE_SUBMIT_PROPERTY, "false"));
    }

    /** Phase D post-submit smoke — requires explicit observe flag plus creation authorization. */
    public static void requirePostSubmitObservationAuthorized() {
        requirePhaseAtLeast(Phase.D);
        if (!isObserveSubmitEnabled()) {
            throw new SkipException(
                    "Phase D post-submit observation skipped by default. Run with -D"
                            + OBSERVE_SUBMIT_PROPERTY
                            + "=true and -Danalysis.create.enabled=true for a single submit + details poll.");
        }
        AnalysisCreateGate.requireCreationEnabled();
    }

    public static OptionalLong configuredProcessingAnalysisId() {
        return parseId(System.getProperty(PROCESSING_ID_PROPERTY), System.getenv(PROCESSING_ID_ENV));
    }

    public static OptionalLong configuredCompletedAnalysisId() {
        return parseId(System.getProperty(COMPLETED_ID_PROPERTY), System.getenv(COMPLETED_ID_ENV));
    }

    public static long requireProcessingAnalysisId() {
        return configuredProcessingAnalysisId()
                .orElseThrow(
                        () ->
                                new SkipException(
                                        "Processing-state tests require -D"
                                                + PROCESSING_ID_PROPERTY
                                                + "=<id> or env "
                                                + PROCESSING_ID_ENV));
    }

    public static long requireCompletedAnalysisId() {
        return configuredCompletedAnalysisId()
                .orElseThrow(
                        () ->
                                new SkipException(
                                        "Completed-state tests require -D"
                                                + COMPLETED_ID_PROPERTY
                                                + "=<id> or env "
                                                + COMPLETED_ID_ENV));
    }

    private static OptionalLong parseId(String propertyValue, String envValue) {
        String raw = propertyValue;
        if (raw == null || raw.isBlank()) {
            raw = envValue;
        }
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            long id = Long.parseLong(raw.trim());
            if (id <= 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(id);
        } catch (NumberFormatException ex) {
            return OptionalLong.empty();
        }
    }
}
