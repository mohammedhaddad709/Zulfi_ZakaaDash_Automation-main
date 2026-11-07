package com.taf.systems.zulfi.config;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/** Safe gate contract checks — no browser, no submit. */
public final class MatchScheduleAnalysisGateContractTest {

    private static final String[] PROPS = {
        MatchScheduleAnalysisGate.PHASE_PROPERTY,
        MatchScheduleAnalysisGate.SUBMIT_ENABLED_PROPERTY,
        MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY,
        "analysis.create.enabled"
    };

    private final Map<String, String> saved = new HashMap<>();

    @AfterMethod
    public void restoreProperties() {
        for (String key : PROPS) {
            if (saved.containsKey(key)) {
                String value = saved.get(key);
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            }
        }
        saved.clear();
    }

    @Test
    public void submitRequiresCreationAndScheduleSubmitGates() {
        rememberAndClear();
        System.setProperty(MatchScheduleAnalysisGate.PHASE_PROPERTY, "c");
        try {
            MatchScheduleAnalysisGate.requireExplicitScheduleSubmitAuthorized();
            Assert.fail("Expected SkipException when analysis.create.enabled=false");
        } catch (SkipException expected) {
            Assert.assertTrue(expected.getMessage().contains("analysis.create.enabled"));
        }
    }

    @Test
    public void submitRequiresExplicitTargetExternalEventId() {
        rememberAndClear();
        System.setProperty(MatchScheduleAnalysisGate.PHASE_PROPERTY, "c");
        System.setProperty("analysis.create.enabled", "true");
        System.setProperty(MatchScheduleAnalysisGate.SUBMIT_ENABLED_PROPERTY, "true");
        try {
            MatchScheduleAnalysisGate.requireExactTargetExternalEventIdForSubmit();
            Assert.fail("Expected SkipException when targetExternalEventId missing");
        } catch (SkipException expected) {
            Assert.assertTrue(
                    expected.getMessage().contains(MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY));
        }
    }

    @Test
    public void preSubmitRejectsCreationEnabled() {
        rememberAndClear();
        System.setProperty(MatchScheduleAnalysisGate.PHASE_PROPERTY, "c");
        System.setProperty("analysis.create.enabled", "true");
        try {
            MatchScheduleAnalysisGate.requirePreSubmitVerificationMode();
            Assert.fail("Expected SkipException for pre-submit with creation enabled");
        } catch (SkipException expected) {
            Assert.assertTrue(expected.getMessage().contains("analysis.create.enabled=false"));
        }
    }

    private void rememberAndClear() {
        for (String key : PROPS) {
            saved.putIfAbsent(key, System.getProperty(key));
            System.clearProperty(key);
        }
    }
}
