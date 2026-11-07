package com.taf.systems.zakaadash.tests.preprocessing.settings;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingSettingsStep;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingSettingsStep.SettingsCategory;
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

import java.util.Arrays;
import java.util.List;

import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_PREPROCESSING;
import static com.taf.systems.zakaadash.tests.TestGroups.PP_SETTINGS;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

/** Step 1 structure only; per-category business rules need approved cases first. */
@Epic("ZakaaDash")
@Feature("Preprocessing")
public class ProcessingSettingsTests extends ZakaaDashBaseTest {

    @Test(groups = {FEATURE_PREPROCESSING, PP_SETTINGS, REGRESSION},
            description = "PP-SET-001: Step 1 lists the five settings categories")
    @TmsLink("PP-SET-001")
    @Story("Step 1 lists the five settings categories")
    @Severity(SeverityLevel.NORMAL)
    public void settingsStepListsFiveCategories() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        ProcessingSettingsStep settings = flow.verifySettingsStep(flow.openAdvancedPreprocessing(baseline));
        // Same five categories as the step1-settings test-data folders.
        List<String> expected = Arrays.stream(SettingsCategory.values()).map(SettingsCategory::label).toList();

        BusinessStep.run("Advanced Preprocessing Wizard", "Verify settings categories", () ->
                Assert.assertEquals(settings.getVisibleCategoryLabels(), expected, "Step 1 settings categories, in order"));
    }
}
