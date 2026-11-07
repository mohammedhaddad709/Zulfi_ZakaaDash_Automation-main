package com.taf.systems.zulfi.config;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/** Fail-safe contract shared by every phased feature gate (no browser). */
public class PhaseGatesContractTest {

    private static final String PROPERTY = "phase.gates.contract.phase";

    private enum Phase { NONE, A, B, C }

    @AfterMethod(alwaysRun = true)
    public void clear() {
        System.clearProperty(PROPERTY);
    }

    @Test
    public void unsetBlankOrUnknownMeansNone() {
        Assert.assertEquals(PhaseGates.parse(PROPERTY, Phase.class), Phase.NONE);
        System.setProperty(PROPERTY, " ");
        Assert.assertEquals(PhaseGates.parse(PROPERTY, Phase.class), Phase.NONE);
        System.setProperty(PROPERTY, "z");
        Assert.assertEquals(PhaseGates.parse(PROPERTY, Phase.class), Phase.NONE);
        System.setProperty(PROPERTY, "none");
        Assert.assertEquals(PhaseGates.parse(PROPERTY, Phase.class), Phase.NONE);
    }

    @Test
    public void lettersAreCaseInsensitive() {
        System.setProperty(PROPERTY, " B ");
        Assert.assertEquals(PhaseGates.parse(PROPERTY, Phase.class), Phase.B);
    }

    @Test
    public void requireSkipsBelowMinimumWithActionableMessage() {
        SkipException skip = Assert.expectThrows(SkipException.class,
                () -> PhaseGates.require(PROPERTY, Phase.A, Phase.B, "DEMO", "docs/demo.md"));
        Assert.assertEquals(skip.getMessage(), "DEMO tests require -D" + PROPERTY + "=b (current=a). See docs/demo.md");
        Assert.expectThrows(SkipException.class, () -> PhaseGates.require(PROPERTY, Phase.NONE, Phase.A, "DEMO", "d"));
        PhaseGates.require(PROPERTY, Phase.C, Phase.B, "DEMO", "d");
    }

    @Test
    public void featureGatesFollowTheirProperty() {
        System.setProperty(MchScheduleGate.PHASE_PROPERTY, "b");
        try {
            Assert.assertEquals(MchScheduleGate.currentPhase(), MchScheduleGate.Phase.B);
            MchScheduleGate.requirePhaseAtLeast(MchScheduleGate.Phase.A);
            Assert.expectThrows(SkipException.class, () -> MchScheduleGate.requirePhaseAtLeast(MchScheduleGate.Phase.C));
        } finally {
            System.clearProperty(MchScheduleGate.PHASE_PROPERTY);
        }
    }
}
