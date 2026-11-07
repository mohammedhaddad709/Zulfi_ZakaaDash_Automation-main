package com.taf.core.pacing;

import io.qameta.allure.Allure;

/** Safe Allure attachment summarizing human-paced execution for the current test. */
public final class PacingDiagnostics {

    private PacingCoverageTracker.Coverage coverage;
    private long testDurationMs;
    private long testStartedAtMs;

    private PacingDiagnostics() {}

    public static PacingDiagnostics start() {
        PacingCoverageTracker.reset();
        PacingDiagnostics diagnostics = new PacingDiagnostics();
        diagnostics.testStartedAtMs = System.currentTimeMillis();
        return diagnostics;
    }

    public void finishAndAttach() {
        testDurationMs = System.currentTimeMillis() - testStartedAtMs;
        coverage = PacingCoverageTracker.snapshot();
        ExecutionPace pace = ExecutionPace.current();
        String summary =
                """
                Pacing profile: %s
                Typing delay (ms): %d–%d
                Action delay (ms): %d–%d
                Transition delay (ms): %d–%d
                Final observation delay (ms): %d–%d
                Paced clicks: %d
                Progressive characters typed: %d
                Bulk/fast characters typed: %d
                Action pauses applied: %d
                Transition pauses applied: %d
                Progressive typing used: %s
                Observed test duration (ms): %d
                Interaction mode: shared ElementActions / PacedElementInteraction
                """
                        .formatted(
                                pace.name(),
                                pace.minCharDelayMs(),
                                pace.maxCharDelayMs(),
                                pace.minActionDelayMs(),
                                pace.maxActionDelayMs(),
                                pace.minTransitionDelayMs(),
                                pace.maxTransitionDelayMs(),
                                pace.minFinalObservationDelayMs(),
                                pace.maxFinalObservationDelayMs(),
                                coverage.pacedClicks(),
                                coverage.progressiveCharacters(),
                                coverage.bulkCharacters(),
                                coverage.actionPauses(),
                                coverage.transitionPauses(),
                                coverage.usedProgressiveTyping(),
                                testDurationMs);
        Allure.addAttachment("Pacing diagnostics", "text/plain", summary);
    }
}
