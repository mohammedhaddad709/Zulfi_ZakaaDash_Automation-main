package com.taf.core.pacing;

/** Per-test pacing coverage counters for Allure diagnostics (no credential data). */
public final class PacingCoverageTracker {

    private static final ThreadLocal<Coverage> COVERAGE = ThreadLocal.withInitial(Coverage::new);

    private PacingCoverageTracker() {}

    public static void reset() {
        COVERAGE.set(new Coverage());
    }

    public static void recordPacedClick() {
        COVERAGE.get().pacedClicks++;
    }

    public static void recordProgressiveCharacters(int count) {
        COVERAGE.get().progressiveCharacters += Math.max(0, count);
    }

    public static void recordBulkTyping(int characterCount) {
        COVERAGE.get().bulkCharacters += Math.max(0, characterCount);
    }

    public static void recordTransitionPause() {
        COVERAGE.get().transitionPauses++;
    }

    public static void recordActionPause() {
        COVERAGE.get().actionPauses++;
    }

    public static Coverage snapshot() {
        return COVERAGE.get();
    }

    public static final class Coverage {
        private int pacedClicks;
        private int progressiveCharacters;
        private int bulkCharacters;
        private int transitionPauses;
        private int actionPauses;

        public int pacedClicks() {
            return pacedClicks;
        }

        public int progressiveCharacters() {
            return progressiveCharacters;
        }

        public int bulkCharacters() {
            return bulkCharacters;
        }

        public int transitionPauses() {
            return transitionPauses;
        }

        public int actionPauses() {
            return actionPauses;
        }

        public boolean usedProgressiveTyping() {
            return progressiveCharacters > 0;
        }
    }
}
