package com.taf.systems.zakaadash.tests.e2e;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DashboardCreationFlow;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.dashboard.DashboardCreationModal;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.SourceGroup;
import com.taf.systems.zakaadash.pages.ZakaaDashLoginPage;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.PreprocessingEntryModal;
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
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.TestDataUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.E2E;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

@Epic("ZakaaDash")
@Feature("Advanced Preprocessing Journey")
public class AdvancedPreprocessingJourneyE2ETest extends ZakaaDashBaseTest {

    private static final String WIZARD = "Advanced Preprocessing Wizard";
    private static final String SAVE_MODAL = "Save Processed Data modal";
    private static final String DATA_VERSIONS = "Data Versions";

    @Test(groups = {E2E, REGRESSION, CRITICAL},
            description = "E2E-PP-001: User can complete Advanced Preprocessing happy path")
    @TmsLink("E2E-PP-001")
    @Story("User can complete Advanced Preprocessing happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void userCanCompleteAdvancedPreprocessingHappyPath() {
        PreprocessingCase journey = PreprocessingCase.load(TestDataUtil.PREPROCESSING_E2E_CASE);
        String processedVersionName = TestDataUtil.uniqueName(journey.requiredExpectedText("processedCopyPrefix"));
        String dashboardName = "تحليل العائد المالي لأندية كرة القدم_"
                + processedVersionName.substring(processedVersionName.lastIndexOf('_') + 1);
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        DashboardCreationFlow dashboardFlow = new DashboardCreationFlow(driver());

        PreprocessingEntryModal entryModal = flow.startPreprocessing(journey);
        String originalSourceName = flow.preparedSourceName();
        int processedCountBefore = flow.processedCountBefore();
        AdvancedPreprocessingWizard wizard = flow.chooseAdvanced(entryModal);

        assertStepReady(wizard, WizardStep.SETTINGS, new ProcessingSettingsStep(driver()));

        SystemSuggestionsStep suggestions = flow.continueFromSettings(wizard);
        assertStepReady(wizard, WizardStep.SUGGESTIONS, suggestions);

        DataReviewStep review = flow.continueFromSuggestions(wizard);
        assertStepReady(wizard, WizardStep.REVIEW, review);

        DataImprovementStep improvement = flow.continueFromReview(wizard);
        assertStepReady(wizard, WizardStep.IMPROVEMENT, improvement);

        flow.reviewAllImprovementTabs(improvement);
        ProcessingResultsStep results = flow.continueToResults(wizard, improvement);
        assertStepReady(wizard, WizardStep.RESULTS, results);

        SaveProcessedDataModal saveModal = flow.openResultsSave(results);
        BusinessStep.run(SAVE_MODAL, "Verify save modal loaded", () -> {
            Assert.assertTrue(saveModal.isLoaded(), "Modal 'معالجة البيانات' should be visible");
            Assert.assertTrue(saveModal.isSaveAsNewCopyAvailable(), "Option 'حفظ كنسخة جديدة' should be visible");
        });

        DataVersionsPage dataVersionsPage = flow.saveAsNewCopy(saveModal, processedVersionName);
        int expectedProcessedCount = processedCountBefore + 1;

        SourceGroup sourceGroup = dataVersionsPage.findSourceGroup(originalSourceName);

        BusinessStep.run(DATA_VERSIONS, "Locate original source after save", () -> {
            Assert.assertTrue(dataVersionsPage.isLoaded(), "Data Versions should be visible after save");
            dataVersionsPage.waitForSource(originalSourceName);
            ZakaaDashLog.info("Original source — located after save");
        });
        HumanPaceUtil.pauseNormalAction();

        BusinessStep.run(DATA_VERSIONS, "Verify processed version count increased", () -> {
            sourceGroup.waitForProcessedVersionsCount(expectedProcessedCount);
            int processedCountAfter = sourceGroup.getProcessedVersionsCount();
            ZakaaDashLog.info("Processed versions after — " + processedCountAfter);
            Assert.assertEquals(processedCountAfter, expectedProcessedCount,
                    "Processed version count should increase by 1 for " + originalSourceName);
            ZakaaDashLog.info("Processed count increment — VERIFIED");
        });
        HumanPaceUtil.pauseNormalAction();

        BusinessStep.run(DATA_VERSIONS, "Expand processed versions for original source", () -> {
            sourceGroup.expandIfCollapsed();
            ZakaaDashLog.info("Processed versions collapse — EXPANDED");
        });
        HumanPaceUtil.pauseWizardTransition();

        BusinessStep.run(DATA_VERSIONS, "Verify generated processed version under original source", () -> {
            try {
                sourceGroup.waitForProcessedVersionVisible(processedVersionName);
                Assert.assertTrue(sourceGroup.isProcessedVersionInGroup(processedVersionName),
                        "Processed version should be visible within the original source group");
                ZakaaDashLog.info("Processed version — verified: " + processedVersionName);
            } catch (RuntimeException ex) {
                throw new AssertionError(sourceGroup.describeState(processedVersionName, processedCountBefore), ex);
            }
        });
        HumanPaceUtil.pauseWizardTransition();

        DashboardCreationModal dashboardModal = dashboardFlow.openFromProcessedVersion(sourceGroup, processedVersionName);
        dashboardFlow.completeDataEntryStep(dashboardModal, dashboardName);
        dashboardFlow.completeGraphicsStep(dashboardModal);
        dashboardFlow.submitDesignAndWaitForRecommendedCharts(dashboardModal);
        dashboardFlow.completeRecommendedChartSelection(dashboardModal, dashboardName);
        dashboardFlow.closeDashboardWizard(dashboardModal);
        HumanPaceUtil.pauseWizardTransition();

        ZakaaDashLoginPage loginPage = new AuthenticationFlow(driver()).logout();

        BusinessStep.run("Login", "Verify logout completed", () -> {
            Assert.assertTrue(loginPage.isLoaded(), "Login page should be visible after logout");
            Assert.assertFalse(new DataVersionsPage(driver()).isLoaded(), "Data Versions should not remain authenticated");
            ZakaaDashLog.info("Logout — verified");
        });
    }

    private static void assertStepReady(AdvancedPreprocessingWizard wizard, WizardStep step, WizardStepPage stepPage) {
        BusinessStep.run(WIZARD, "Verify " + step.label() + " ready", () -> {
            Assert.assertTrue(wizard.isLoaded(), "Wizard 'معالجة البيانات المتقدمة' should be open");
            Assert.assertTrue(wizard.isStepActive(step), step.label() + " should be the active step in the stepper");
            Assert.assertTrue(stepPage.isContentLoaded(), step.label() + " content should be visible");
        });
    }
}
