package com.taf.systems.zakaadash.tests.e2e;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static com.taf.systems.zakaadash.tests.TestGroups.E2E;

/**
 * Oracle-heavy football preprocessing (PP-FOOTBALL-001) is deferred.
 * Use {@link AdvancedPreprocessingJourneyE2ETest} with the football upload fixture instead.
 */
@Epic("ZakaaDash")
@Feature("Football ROI Preprocessing Journey")
public class FootballPreprocessingJourneyE2ETest extends ZakaaDashBaseTest {

    @Test(groups = E2E, enabled = false,
            description = "Deferred: run AdvancedPreprocessingJourneyE2ETest with zd_football_roi_raw.csv")
    void deferredFootballOracleScenario() {
        // Intentionally disabled — not part of the baseline happy-path scope.
    }
}
