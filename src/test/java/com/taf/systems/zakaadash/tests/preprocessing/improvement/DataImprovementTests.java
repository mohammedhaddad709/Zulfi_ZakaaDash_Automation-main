package com.taf.systems.zakaadash.tests.preprocessing.improvement;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.DataImprovementStep;
import com.taf.systems.zakaadash.pages.preprocessing.ImprovementTab;
import com.taf.systems.zakaadash.testdata.PreprocessingCase;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.TestDataUtil;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_PREPROCESSING;
import static com.taf.systems.zakaadash.tests.TestGroups.PP_IMPROVEMENT;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

/** Step 4 structure only; suggestion content rules need approved cases first. */
@Epic("ZakaaDash")
@Feature("Preprocessing")
public class DataImprovementTests extends ZakaaDashBaseTest {

    private static final String WIZARD = "Advanced Preprocessing Wizard";

    @Test(groups = {FEATURE_PREPROCESSING, PP_IMPROVEMENT, REGRESSION},
            description = "PP-IMP-001: Step 4 shows the seven improvement tabs and each opens its panel")
    @TmsLink("PP-IMP-001")
    @Story("Step 4 shows the seven improvement tabs and each opens its panel")
    @Severity(SeverityLevel.NORMAL)
    public void improvementStepShowsSevenWorkingTabs() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baseline);
        flow.continueFromSettings(wizard);
        flow.continueFromSuggestions(wizard);
        DataImprovementStep improvement = flow.continueFromReview(wizard);

        BusinessStep.run(WIZARD, "Verify the improvement tabs", () ->
                Assert.assertEquals(improvement.getAvailableTabs(), List.of(ImprovementTab.values()),
                        "Step 4 should show all improvement tabs"));

        for (ImprovementTab tab : ImprovementTab.values()) {
            BusinessStep.run(WIZARD, "Open tab " + tab.label(), () -> {
                improvement.openTab(tab);
                Assert.assertTrue(improvement.isTabActive(tab), "Tab should be selected: " + tab.label());
                Assert.assertFalse(improvement.getTabPanelText(tab).isBlank(),
                        "Tab panel should show suggestions or the empty-section message: " + tab.label());
            });
        }
    }
}
