package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;

import java.time.Instant;
import java.util.Objects;

/** Tracks exactly-once Create Analysis submission for a single test method. */
public final class AnalysisSubmissionAttempt {

    private final boolean submissionAuthorized;
    private String selectedFixture;
    private boolean submissionAttempted;
    private Instant submissionTimestamp;
    private int submitClickCount;
    private AnalysisPostSubmitOutcome observedOutcome;

    private AnalysisSubmissionAttempt(boolean submissionAuthorized) {
        this.submissionAuthorized = submissionAuthorized;
    }

    public static AnalysisSubmissionAttempt authorized(String fixtureLabel) {
        AnalysisSubmissionAttempt attempt = new AnalysisSubmissionAttempt(true);
        attempt.selectedFixture = fixtureLabel == null ? "" : fixtureLabel.trim();
        return attempt;
    }

    public static AnalysisSubmissionAttempt notAuthorized() {
        return new AnalysisSubmissionAttempt(false);
    }

    public boolean isSubmissionAuthorized() {
        return submissionAuthorized;
    }

    public String selectedFixture() {
        return selectedFixture;
    }

    public void selectFixtureBeforeSubmit(String fixtureLabel) {
        ensureNotYetSubmitted();
        this.selectedFixture = Objects.requireNonNullElse(fixtureLabel, "").trim();
    }

    public void recordSubmitClick() {
        if (!submissionAuthorized) {
            throw new IllegalStateException("Submission not authorized for this test run");
        }
        if (submissionAttempted) {
            throw new IllegalStateException(
                    "Exactly-once violation: submit already attempted for fixture=" + selectedFixture);
        }
        submissionAttempted = true;
        submitClickCount++;
        submissionTimestamp = Instant.now();
    }

    public void recordOutcome(AnalysisPostSubmitOutcome outcome) {
        this.observedOutcome = outcome;
    }

    public boolean submissionAttempted() {
        return submissionAttempted;
    }

    public int submitClickCount() {
        return submitClickCount;
    }

    public Instant submissionTimestamp() {
        return submissionTimestamp;
    }

    public AnalysisPostSubmitOutcome observedOutcome() {
        return observedOutcome;
    }

    private void ensureNotYetSubmitted() {
        if (submissionAttempted) {
            throw new IllegalStateException(
                    "Cannot change fixture after submit was attempted for: " + selectedFixture);
        }
    }
}
