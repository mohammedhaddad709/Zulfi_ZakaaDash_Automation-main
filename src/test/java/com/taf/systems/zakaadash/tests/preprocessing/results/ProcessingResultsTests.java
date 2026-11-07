package com.taf.systems.zakaadash.tests.preprocessing.results;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.SaveProcessedDataModal;
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

import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_PREPROCESSING;
import static com.taf.systems.zakaadash.tests.TestGroups.PP_RESULTS;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

@Epic("ZakaaDash")
@Feature("Preprocessing")
public class ProcessingResultsTests extends ZakaaDashBaseTest {

    @Test(groups = {FEATURE_PREPROCESSING, PP_RESULTS, REGRESSION},
            description = "PP-SAVE-001: Cancelling the save creates no processed version")
    @TmsLink("PP-SAVE-001")
    @Story("Cancelling the save creates no processed version")
    @Severity(SeverityLevel.NORMAL)
    public void cancellingSaveCreatesNoProcessedVersion() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baseline);
        String sourceName = flow.preparedSourceName();
        int countBefore = flow.processedCountBefore();
        SaveProcessedDataModal saveModal = flow.openSaveProcessedDataModal(flow.goToResults(wizard));

        DataVersionsPage dataVersionsPage = flow.cancelSave(saveModal);

        BusinessStep.run("Data Versions", "Verify no processed version was added", () -> {
            Assert.assertFalse(saveModal.isLoaded(), "Save modal should be closed");
            Assert.assertFalse(wizard.isLoaded(), "Wizard should be closed");
            dataVersionsPage.waitForSource(sourceName);
            Assert.assertEquals(dataVersionsPage.getProcessedVersionsCount(sourceName), countBefore,
                    "Processed count of " + sourceName + " should not change after cancel");
        });
    }
}
