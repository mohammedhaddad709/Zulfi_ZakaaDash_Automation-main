package com.taf.systems.zakaadash.tests.preprocessing.entry;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.PreprocessingEntryModal;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingSettingsStep;
import com.taf.systems.zakaadash.pages.preprocessing.WizardStep;
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

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_PREPROCESSING;
import static com.taf.systems.zakaadash.tests.TestGroups.PP_ENTRY;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;
import static com.taf.systems.zakaadash.tests.TestGroups.SMOKE;

@Epic("ZakaaDash")
@Feature("Preprocessing")
public class PreprocessingEntryTests extends ZakaaDashBaseTest {

    private static final String ENTRY_MODAL = "Processing mode modal";
    private static final String WIZARD = "Advanced Preprocessing Wizard";

    @Test(groups = {FEATURE_PREPROCESSING, PP_ENTRY, SMOKE, SANITY, REGRESSION, CRITICAL},
            description = "PP-ENTRY-001: Start Preprocessing opens processing mode modal")
    @TmsLink("PP-ENTRY-001")
    @Story("Start Preprocessing opens processing mode modal")
    @Severity(SeverityLevel.CRITICAL)
    public void startPreprocessingOpensProcessingModeModal() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);

        PreprocessingEntryModal entryModal = new AdvancedPreprocessingFlow(driver()).startPreprocessing(baseline);

        BusinessStep.run(ENTRY_MODAL, "Verify processing mode modal loaded", () -> {
            Assert.assertTrue(entryModal.isLoaded(), "Modal 'ابدأ معالجة البيانات' should be visible");
            Assert.assertTrue(entryModal.isTitleVisible(), "Modal title 'ابدأ معالجة البيانات' should be visible");
            Assert.assertTrue(entryModal.isAutomaticProcessingAvailable(),
                    "Option 'معالجة تلقائية' should be visible");
            Assert.assertTrue(entryModal.isAdvancedProcessingAvailable(),
                    "Option 'معالجة متقدمة' should be visible");
        });
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_ENTRY, SMOKE, SANITY, REGRESSION, CRITICAL},
            description = "PP-ENTRY-002: Advanced Processing opens Advanced Preprocessing Wizard")
    @TmsLink("PP-ENTRY-002")
    @Story("Advanced Processing opens Advanced Preprocessing Wizard")
    @Severity(SeverityLevel.CRITICAL)
    public void advancedProcessingOpensAdvancedPreprocessingWizard() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());

        AdvancedPreprocessingWizard wizard = flow.chooseAdvanced(flow.startPreprocessing(baseline));

        ProcessingSettingsStep settings = new ProcessingSettingsStep(driver());
        BusinessStep.run(WIZARD, "Verify wizard opened on Step 1", () -> {
            Assert.assertTrue(wizard.isLoaded(), "Wizard 'معالجة البيانات المتقدمة' should be visible");
            Assert.assertTrue(wizard.isStepActive(WizardStep.SETTINGS),
                    "Step 'إعدادات المعالجة' should be the active step in the stepper");
            Assert.assertTrue(settings.isContentLoaded(), "Step 'إعدادات المعالجة' content should be visible");
        });
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_ENTRY, REGRESSION},
            description = "PP-ENTRY-003: Closing the processing mode modal returns to Data Versions")
    @TmsLink("PP-ENTRY-003")
    @Story("Closing the processing mode modal returns to Data Versions")
    @Severity(SeverityLevel.NORMAL)
    public void closingProcessingModeModalReturnsToDataVersions() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        PreprocessingEntryModal entryModal = flow.startPreprocessing(baseline);

        DataVersionsPage dataVersionsPage = flow.closeProcessingModeModal(entryModal);

        BusinessStep.run("Data Versions", "Verify no preprocessing dialog remains", () -> {
            Assert.assertFalse(entryModal.isLoaded(), "Modal 'ابدأ معالجة البيانات' should be closed");
            Assert.assertFalse(new AdvancedPreprocessingWizard(driver()).isLoaded(), "Wizard must not open");
            Assert.assertTrue(dataVersionsPage.isLoaded(), "Data Versions should remain visible");
        });
    }
}
