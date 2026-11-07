package com.taf.systems.zulfi.validations;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DuplicateFixtureToastRulesTest {

    @Test
    public void detectsEnglishTechnicalDuplicateToast() {
        String toast =
                "An analysis already exists for this fixture (externalEventId=16651975, matchId=6)";
        Assert.assertTrue(DuplicateFixtureToastRules.isDuplicateFixtureMessage(toast));
        Assert.assertTrue(DuplicateFixtureToastRules.isUnlocalizedTechnicalDuplicateToast(toast));
    }
}
