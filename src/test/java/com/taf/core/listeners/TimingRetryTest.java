package com.taf.core.listeners;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/** Contract of the runner's opt-in timing retry: timing failures only, never assertions, never with a submit gate. */
public class TimingRetryTest {

    @AfterMethod(alwaysRun = true)
    public void clear() {
        System.clearProperty("analysis.create.enabled");
        System.clearProperty("match.schedule.analysis.submit.enabled");
    }

    @Test(description = "CORE-RTY-001 — Timeouts and stale elements (also wrapped) are timing failures")
    public void timingFailuresDetected() {
        Assert.assertTrue(TimingRetry.isTimingFailure(new TimeoutException("waited 15s")));
        Assert.assertTrue(TimingRetry.isTimingFailure(new RuntimeException(new StaleElementReferenceException("stale"))));
        Assert.assertTrue(TimingRetry.isTimingFailure(new AssertionError("Step failed", new TimeoutException("t"))));
    }

    @Test(description = "CORE-RTY-002 — Assertions and missing elements are not retried")
    public void assertionsNotRetried() {
        Assert.assertFalse(TimingRetry.isTimingFailure(new AssertionError("expected [true] but found [false]")));
        Assert.assertFalse(TimingRetry.isTimingFailure(new NoSuchElementException("no such element")));
        Assert.assertFalse(TimingRetry.isTimingFailure(null));
    }

    @Test(description = "CORE-RTY-003 — Any open create/submit gate disables retries for the whole run")
    public void submitGateDisablesRetry() {
        Assert.assertFalse(TimingRetry.submitGateOpen());
        System.setProperty("match.schedule.analysis.submit.enabled", "false");
        Assert.assertFalse(TimingRetry.submitGateOpen());
        System.setProperty("analysis.create.enabled", "true");
        Assert.assertTrue(TimingRetry.submitGateOpen());
    }
}
