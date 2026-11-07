package com.taf.systems.zakaadash.flows.preprocessing;

import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DataSourcePreparationFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.DataImprovementStep;
import com.taf.systems.zakaadash.pages.preprocessing.DataReviewStep;
import com.taf.systems.zakaadash.pages.preprocessing.PreprocessingEntryModal;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingResultsStep;
import com.taf.systems.zakaadash.pages.preprocessing.ProcessingSettingsStep;
import com.taf.systems.zakaadash.pages.preprocessing.SaveProcessedDataModal;
import com.taf.systems.zakaadash.pages.preprocessing.SystemSuggestionsStep;
import com.taf.systems.zakaadash.pages.preprocessing.WizardStep;
import com.taf.systems.zakaadash.pages.preprocessing.WizardStepPage;
import com.taf.systems.zakaadash.testdata.PreprocessingCase;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import org.openqa.selenium.WebDriver;

import java.util.function.Function;

/** Business navigation through Advanced Preprocessing. Owns no locators and makes no assertions. */
public class AdvancedPreprocessingFlow {

    private static final String DATA_VERSIONS_PAGE = "Data Versions";
    private static final String ENTRY_MODAL = "Processing mode modal";
    private static final String WIZARD = "Advanced Preprocessing Wizard";
    private static final String SAVE_MODAL = "Save Processed Data modal";

    private final WebDriver driver;
    private String preparedSourceName;
    private int processedCountBefore;

    public AdvancedPreprocessingFlow(WebDriver driver) {
        this.driver = driver;
    }

    public String preparedSourceName() {
        return preparedSourceName;
    }

    public int processedCountBefore() {
        return processedCountBefore;
    }

    public PreprocessingEntryModal startPreprocessing(PreprocessingCase testCase) {
        recordCase(testCase);
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver).loginSuccessfully();
        String sourceName = new DataSourcePreparationFlow().prepareSource(
                dataVersionsPage, testCase.testCaseId(), testCase.datasetFile(), testCase.sourceNamePrefix());
        preparedSourceName = sourceName;
        dataVersionsPage.waitForSource(sourceName);
        processedCountBefore = dataVersionsPage.getProcessedVersionsCount(sourceName);
        BusinessStep.record(BusinessStep.PROCESSED_COUNT_BEFORE, processedCountBefore);
        ZakaaDashLog.info("Original source — located: " + sourceName);
        ZakaaDashLog.info("Processed versions before — " + processedCountBefore);
        BusinessStep.recordDataset(testCase.datasetLabel());
        HumanPaceUtil.pausePreprocessingAction();

        PreprocessingEntryModal entryModal = BusinessStep.get(DATA_VERSIONS_PAGE,
                "Start preprocessing for " + sourceName, () -> {
                    PreprocessingEntryModal modal = dataVersionsPage.startPreprocessingForSource(sourceName);
                    ZakaaDashLog.info("Start Preprocessing — clicked");
                    return modal;
                });
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, "Entry — processing mode");
        return entryModal;
    }

    public DataVersionsPage closeProcessingModeModal(PreprocessingEntryModal entryModal) {
        BusinessStep.run(ENTRY_MODAL, "Close processing mode modal", entryModal::close);
        return new DataVersionsPage(driver);
    }

    public AdvancedPreprocessingWizard chooseAdvanced(PreprocessingEntryModal entryModal) {
        AdvancedPreprocessingWizard wizard = BusinessStep.get(ENTRY_MODAL, "Choose Advanced processing",
                entryModal::selectAdvancedProcessing);
        ZakaaDashLog.info("Advanced Processing — opened");
        verifySettingsStep(wizard);
        return wizard;
    }

    public AdvancedPreprocessingWizard openAdvancedPreprocessing(PreprocessingCase testCase) {
        return chooseAdvanced(startPreprocessing(testCase));
    }

    public ProcessingSettingsStep verifySettingsStep(AdvancedPreprocessingWizard wizard) {
        ProcessingSettingsStep settings = BusinessStep.get(WIZARD, "Verify " + WizardStep.SETTINGS.label(), () ->
                loaded(new ProcessingSettingsStep(driver)));
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, WizardStep.SETTINGS.label());
        return settings;
    }

    public SystemSuggestionsStep continueFromSettings(AdvancedPreprocessingWizard wizard) {
        return continueFrom(wizard, new ProcessingSettingsStep(driver), SystemSuggestionsStep::new, WizardStep.SUGGESTIONS);
    }

    public DataReviewStep continueFromSuggestions(AdvancedPreprocessingWizard wizard) {
        return continueFrom(wizard, new SystemSuggestionsStep(driver), DataReviewStep::new, WizardStep.REVIEW);
    }

    public DataImprovementStep continueFromReview(AdvancedPreprocessingWizard wizard) {
        return continueFrom(wizard, new DataReviewStep(driver), DataImprovementStep::new, WizardStep.IMPROVEMENT);
    }

    public DataImprovementStep reviewAllImprovementTabs(DataImprovementStep improvement) {
        BusinessStep.run(WIZARD, "Open Step 4 — Data Improvement", improvement::waitUntilLoaded);
        BusinessStep.run(WIZARD, "Review all Step 4 improvement tabs", improvement::visitAllTabsInOrder);
        return improvement;
    }

    public ProcessingResultsStep continueToResults(AdvancedPreprocessingWizard wizard, DataImprovementStep improvement) {
        return continueFrom(wizard, improvement, ProcessingResultsStep::new, WizardStep.RESULTS);
    }

    public ProcessingResultsStep continueFromImprovement(AdvancedPreprocessingWizard wizard) {
        return continueToResults(wizard, new DataImprovementStep(driver));
    }

    public SystemSuggestionsStep goFromSettingsToSuggestions(AdvancedPreprocessingWizard wizard) {
        return continueFromSettings(wizard);
    }

    public DataReviewStep goFromSuggestionsToReview(AdvancedPreprocessingWizard wizard) {
        return continueFromSuggestions(wizard);
    }

    public DataImprovementStep goFromReviewToImprovement(AdvancedPreprocessingWizard wizard) {
        return continueFromReview(wizard);
    }

    public ProcessingResultsStep goFromImprovementToResults(AdvancedPreprocessingWizard wizard) {
        return continueToResults(wizard, loaded(new DataImprovementStep(driver)));
    }

    public ProcessingResultsStep goToResults(AdvancedPreprocessingWizard wizard) {
        continueFromSettings(wizard);
        continueFromSuggestions(wizard);
        DataImprovementStep improvement = continueFromReview(wizard);
        reviewAllImprovementTabs(improvement);
        return continueToResults(wizard, improvement);
    }

    public ProcessingSettingsStep goBackFromSuggestionsToSettings(AdvancedPreprocessingWizard wizard) {
        ProcessingSettingsStep settings = BusinessStep.get(WIZARD, "Go back to " + WizardStep.SETTINGS.label(), () -> {
            wizard.clickPrevious();
            return loaded(new ProcessingSettingsStep(driver));
        });
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, WizardStep.SETTINGS.label());
        return settings;
    }

    /** "إلغاء" on the save modal: the product closes the modal and the wizard and returns to Data Versions. */
    public DataVersionsPage cancelSave(SaveProcessedDataModal saveModal) {
        BusinessStep.run(SAVE_MODAL, "Cancel saving processed data", saveModal::cancel);
        return BusinessStep.get(DATA_VERSIONS_PAGE, "Return to Data Versions after cancel",
                () -> new DataVersionsPage(driver).waitUntilLoaded());
    }

    public SaveProcessedDataModal openResultsSave(ProcessingResultsStep resultsStep) {
        return openSaveProcessedDataModal(resultsStep);
    }

    public SaveProcessedDataModal openSaveProcessedDataModal(ProcessingResultsStep resultsStep) {
        SaveProcessedDataModal saveModal = BusinessStep.get(WIZARD, "Save processed data", resultsStep::clickSave);
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, "Save Processed Data modal");
        return saveModal;
    }

    public DataVersionsPage saveAsNewCopy(SaveProcessedDataModal saveModal, String processedVersionName) {
        BusinessStep.record(BusinessStep.PROCESSED_VERSION_NAME, processedVersionName);
        BusinessStep.run(SAVE_MODAL, "Select Save as new copy", () -> {
            saveModal.selectSaveAsNewCopy();
            ZakaaDashLog.info("Save As New Copy — selected");
        });
        BusinessStep.run(SAVE_MODAL, "Enter processed version name " + processedVersionName, () -> {
            saveModal.enterSourceName(processedVersionName);
            ZakaaDashLog.info("Processed version name: " + processedVersionName);
        });
        BusinessStep.run(SAVE_MODAL, "Save processed data", saveModal::save);
        BusinessStep.run(SAVE_MODAL, "Wait for Save Processed Data modal to close", saveModal::waitUntilClosed);
        return BusinessStep.get(DATA_VERSIONS_PAGE, "Return to Data Versions after save", () -> {
            DataVersionsPage dataVersionsPage = new DataVersionsPage(driver).waitUntilLoaded();
            HumanPaceUtil.pausePreprocessingAction();
            ZakaaDashLog.info("Save Data — completed");
            return dataVersionsPage;
        });
    }

    private <T extends WizardStepPage> T continueFrom(
            AdvancedPreprocessingWizard wizard,
            WizardStepPage currentStep,
            Function<WebDriver, T> nextStepFactory,
            WizardStep destination) {
        T nextStep = BusinessStep.get(WIZARD, "Navigate to " + destination.label(), () -> {
            currentStep.waitUntilReadyToContinue();
            wizard.clickNextWhenReady();
            T step = loaded(nextStepFactory.apply(driver));
            if (destination == WizardStep.RESULTS) {
                HumanPaceUtil.pausePreprocessingAction();
            }
            return step;
        });
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, destination.label());
        return nextStep;
    }

    private static <T extends WizardStepPage> T loaded(T step) {
        step.waitUntilLoaded();
        return step;
    }

    private static void recordCase(PreprocessingCase testCase) {
        BusinessStep.recordDataset(testCase.datasetLabel());
        BusinessStep.record(BusinessStep.PREPROCESSING_STEP, "Not reached: login / source setup");
        if (!testCase.category().isBlank()) {
            BusinessStep.recordCategory(testCase.category());
        }
        BusinessStep.recordConfiguration(testCase.settings().toString());
        BusinessStep.record(BusinessStep.EXPECTED_RESULT, testCase.expected().toString());
    }
}
