package com.taf.systems.zakaadash.tests.preprocessing.navigation;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.DataImprovementStep;
import com.taf.systems.zakaadash.pages.preprocessing.DataReviewStep;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingResultsStep;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingSettingsStep;
import com.taf.systems.zakaadash.pages.preprocessing.SaveProcessedDataModal;
import com.taf.systems.zakaadash.pages.preprocessing.SystemSuggestionsStep;
import com.taf.systems.zakaadash.pages.preprocessing.WizardStep;
import com.taf.systems.zakaadash.pages.preprocessing.WizardStepPage;
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
import static com.taf.systems.zakaadash.tests.TestGroups.PP_NAVIGATION;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;

/** Baseline wizard transitions only. Business rules of each step belong to the step-specific test classes. */
@Epic("ZakaaDash")
@Feature("Preprocessing")
public class WizardNavigationTests extends ZakaaDashBaseTest {

    private static final String WIZARD = "Advanced Preprocessing Wizard";

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, SANITY, REGRESSION, CRITICAL},
            description = "PP-NAV-001: Step 1 Next opens Step 2 System Suggestions")
    @TmsLink("PP-NAV-001")
    @Story("Step 1 Next opens Step 2 System Suggestions")
    @Severity(SeverityLevel.CRITICAL)
    public void settingsNextOpensSystemSuggestions() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());

        SystemSuggestionsStep suggestions = flow.goFromSettingsToSuggestions(wizard);

        assertStepReady(wizard, WizardStep.SUGGESTIONS, suggestions);
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, SANITY, REGRESSION, CRITICAL},
            description = "PP-NAV-002: Step 2 Next opens Step 3 Data Review")
    @TmsLink("PP-NAV-002")
    @Story("Step 2 Next opens Step 3 Data Review")
    @Severity(SeverityLevel.CRITICAL)
    public void suggestionsNextOpensDataReview() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());
        flow.goFromSettingsToSuggestions(wizard);

        DataReviewStep review = flow.goFromSuggestionsToReview(wizard);

        assertStepReady(wizard, WizardStep.REVIEW, review);
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, SANITY, REGRESSION, CRITICAL},
            description = "PP-NAV-003: Step 3 Next opens Step 4 Data Improvement")
    @TmsLink("PP-NAV-003")
    @Story("Step 3 Next opens Step 4 Data Improvement")
    @Severity(SeverityLevel.CRITICAL)
    public void reviewNextOpensDataImprovement() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());
        flow.goFromSettingsToSuggestions(wizard);
        flow.goFromSuggestionsToReview(wizard);

        DataImprovementStep improvement = flow.goFromReviewToImprovement(wizard);

        assertStepReady(wizard, WizardStep.IMPROVEMENT, improvement);
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, SANITY, REGRESSION, CRITICAL},
            description = "PP-NAV-004: Step 4 Next opens Step 5 Processing Results")
    @TmsLink("PP-NAV-004")
    @Story("Step 4 Next opens Step 5 Processing Results")
    @Severity(SeverityLevel.CRITICAL)
    public void improvementNextOpensProcessingResults() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());
        flow.goFromSettingsToSuggestions(wizard);
        flow.goFromSuggestionsToReview(wizard);
        DataImprovementStep improvement = flow.goFromReviewToImprovement(wizard);
        assertStepReady(wizard, WizardStep.IMPROVEMENT, improvement);

        flow.reviewAllImprovementTabs(improvement);
        ProcessingResultsStep results = flow.continueToResults(wizard, improvement);

        assertStepReady(wizard, WizardStep.RESULTS, results);
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, SANITY, REGRESSION, CRITICAL},
            description = "PP-NAV-005: Step 5 Save opens Save Processed Data modal")
    @TmsLink("PP-NAV-005")
    @Story("Step 5 Save opens Save Processed Data modal")
    @Severity(SeverityLevel.CRITICAL)
    public void resultsSaveOpensSaveProcessedDataModal() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());
        ProcessingResultsStep results = flow.goToResults(wizard);

        SaveProcessedDataModal saveModal = flow.openSaveProcessedDataModal(results);

        BusinessStep.run("Save Processed Data modal", "Verify save modal loaded", () -> {
            Assert.assertTrue(saveModal.isLoaded(), "Modal 'معالجة البيانات' should be visible");
            Assert.assertTrue(saveModal.isSaveAsNewCopyAvailable(), "Option 'حفظ كنسخة جديدة' should be visible");
            Assert.assertTrue(saveModal.isReplaceOriginalAvailable(), "Option 'استبدال الملف الأصلي' should be visible");
        });
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, REGRESSION},
            description = "PP-NAV-006: Previous is disabled on Step 1")
    @TmsLink("PP-NAV-006")
    @Story("Previous is disabled on Step 1")
    @Severity(SeverityLevel.MINOR)
    public void previousIsDisabledOnFirstStep() {
        AdvancedPreprocessingWizard wizard = new AdvancedPreprocessingFlow(driver()).openAdvancedPreprocessing(baselineCase());

        BusinessStep.run(WIZARD, "Verify Previous on Step 1", () -> {
            Assert.assertTrue(wizard.isStepActive(WizardStep.SETTINGS), "Step 1 should be active");
            Assert.assertFalse(wizard.isPreviousEnabled(), "'السابق' should be disabled on the first step");
        });
    }

    @Test(groups = {FEATURE_PREPROCESSING, PP_NAVIGATION, REGRESSION},
            description = "PP-NAV-007: Previous on Step 2 returns to Step 1")
    @TmsLink("PP-NAV-007")
    @Story("Previous on Step 2 returns to Step 1")
    @Severity(SeverityLevel.NORMAL)
    public void previousOnSuggestionsReturnsToSettings() {
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baselineCase());
        flow.goFromSettingsToSuggestions(wizard);

        ProcessingSettingsStep settings = flow.goBackFromSuggestionsToSettings(wizard);

        assertStepReady(wizard, WizardStep.SETTINGS, settings);
        BusinessStep.run(WIZARD, "Verify Step 2 is no longer active", () ->
                Assert.assertFalse(wizard.isStepActive(WizardStep.SUGGESTIONS), "Step 2 should not stay active"));
    }

    private static PreprocessingCase baselineCase() {
        return PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
    }

    private static void assertStepReady(AdvancedPreprocessingWizard wizard, WizardStep step, WizardStepPage stepPage) {
        BusinessStep.run(WIZARD, "Verify " + step.label() + " ready", () -> {
            Assert.assertTrue(wizard.isLoaded(), "Advanced Preprocessing Wizard should be open");
            Assert.assertTrue(wizard.isStepActive(step), step.label() + " should be the active step in the stepper");
            Assert.assertTrue(stepPage.isContentLoaded(), step.label() + " content should be visible");
        });
    }
}
