package com.taf.core.pacing;

import com.taf.core.utils.ElapsedWait;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class HumanPacingManager {

    private HumanPacingManager() {}

    public static void pauseForAction() {
        sleep(randomMs(ExecutionPace.current().minActionDelayMs(), ExecutionPace.current().maxActionDelayMs()));
        PacingCoverageTracker.recordActionPause();
    }

    public static void pauseForTransition() {
        sleep(
                randomMs(
                        ExecutionPace.current().minTransitionDelayMs(),
                        ExecutionPace.current().maxTransitionDelayMs()));
        PacingCoverageTracker.recordTransitionPause();
    }

    /** Presentation-only pause before browser teardown (DEMO/NORMAL); skipped in FAST. */
    public static void pauseForFinalObservation() {
        ExecutionPace pace = ExecutionPace.current();
        if (pace == ExecutionPace.FAST) {
            return;
        }
        sleep(randomMs(pace.minFinalObservationDelayMs(), pace.maxFinalObservationDelayMs()));
    }

    public static void pauseBetweenCharacters() {
        ExecutionPace pace = ExecutionPace.current();
        if (pace == ExecutionPace.FAST) {
            return;
        }
        sleep(randomMs(pace.minCharDelayMs(), pace.maxCharDelayMs()));
    }

    private static int randomMs(int min, int max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        ElapsedWait.atLeast(Duration.ofMillis(ms));
    }
}
