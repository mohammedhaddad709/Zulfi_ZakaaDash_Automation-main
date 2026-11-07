package com.taf.core.pacing;

import com.taf.core.utils.dataReader.PropertyReader;

/** Human-paced execution modes for UI interactions. */
public enum ExecutionPace {
    FAST(0, 25, 0, 25, 0, 25, 0, 0),
    NORMAL(80, 140, 400, 700, 600, 1000, 400, 700),
    DEMO(120, 200, 800, 1200, 1000, 1500, 600, 1000);

    private final int minCharDelayMs;
    private final int maxCharDelayMs;
    private final int minActionDelayMs;
    private final int maxActionDelayMs;
    private final int minTransitionDelayMs;
    private final int maxTransitionDelayMs;
    private final int minFinalObservationDelayMs;
    private final int maxFinalObservationDelayMs;

    ExecutionPace(
            int minCharDelayMs,
            int maxCharDelayMs,
            int minActionDelayMs,
            int maxActionDelayMs,
            int minTransitionDelayMs,
            int maxTransitionDelayMs,
            int minFinalObservationDelayMs,
            int maxFinalObservationDelayMs) {
        this.minCharDelayMs = minCharDelayMs;
        this.maxCharDelayMs = maxCharDelayMs;
        this.minActionDelayMs = minActionDelayMs;
        this.maxActionDelayMs = maxActionDelayMs;
        this.minTransitionDelayMs = minTransitionDelayMs;
        this.maxTransitionDelayMs = maxTransitionDelayMs;
        this.minFinalObservationDelayMs = minFinalObservationDelayMs;
        this.maxFinalObservationDelayMs = maxFinalObservationDelayMs;
    }

    public static ExecutionPace current() {
        String raw = System.getProperty("execution.pace");
        if (raw == null || raw.isBlank()) {
            raw = PropertyReader.getProperty("execution.pace");
        }
        if (raw == null || raw.isBlank()) {
            return DEMO;
        }
        return switch (raw.trim().toLowerCase()) {
            case "fast" -> FAST;
            case "normal" -> NORMAL;
            default -> DEMO;
        };
    }

    public int minCharDelayMs() {
        return minCharDelayMs;
    }

    public int maxCharDelayMs() {
        return maxCharDelayMs;
    }

    public int minActionDelayMs() {
        return minActionDelayMs;
    }

    public int maxActionDelayMs() {
        return maxActionDelayMs;
    }

    public int minTransitionDelayMs() {
        return minTransitionDelayMs;
    }

    public int maxTransitionDelayMs() {
        return maxTransitionDelayMs;
    }

    public int minFinalObservationDelayMs() {
        return minFinalObservationDelayMs;
    }

    public int maxFinalObservationDelayMs() {
        return maxFinalObservationDelayMs;
    }
}
