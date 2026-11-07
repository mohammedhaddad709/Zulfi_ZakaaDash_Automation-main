package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Step 5 "عرض النتائج": final review before saving. Live DOM 2026-11-07: a PrimeVue datatable with the processed
 * preview, and the wizard footer shows "حفظ" instead of "التالي".
 */
public class ProcessingResultsStep extends WizardStepPage {

    private static final By RESULTS_ROWS = AdvancedPreprocessingWizard.inWizard(
            "//div[@data-pc-name='datatable']//tbody//tr[@role='row']");
    private static final By RESULTS_COLUMN_TITLES = AdvancedPreprocessingWizard.inWizard(
            "//div[@data-pc-name='datatable']//thead//th[@role='columnheader']//span[contains(@class,'truncate')]");

    public ProcessingResultsStep(WebDriver driver) {
        super(driver, WizardStep.RESULTS, RESULTS_ROWS);
    }

    public boolean isSaveAvailable() {
        return driver.findElements(wizard().saveButton()).stream().anyMatch(s -> s.isDisplayed() && s.isEnabled());
    }

    @Override
    public boolean isContentLoaded() {
        return isDisplayed(RESULTS_ROWS) && isSaveAvailable();
    }

    @Override
    public void waitUntilLoaded() {
        try {
            super.waitUntilLoaded();
            wizard().waitForWizardIdle();
            wait.waitForClickable(wizard().saveButton());
        } catch (TimeoutException ex) {
            attachDiagnostics();
            throw ex;
        }
    }

    @Override
    public void waitUntilReadyToContinue() {
        waitUntilLoaded();
    }

    /** Column titles of the visible results grid (processed preview). */
    public List<String> getVisibleColumnTitles() {
        return driver.findElements(RESULTS_COLUMN_TITLES).stream()
                .filter(WebElement::isDisplayed)
                .map(title -> title.getText().trim())
                .toList();
    }

    public int getVisibleRowCount() {
        return (int) driver.findElements(RESULTS_ROWS).stream().filter(WebElement::isDisplayed).count();
    }

    public SaveProcessedDataModal clickSave() {
        waitUntilReadyToContinue();
        wait.waitForClickable(wizard().saveButton()).click();
        SaveProcessedDataModal saveModal = new SaveProcessedDataModal(driver).waitUntilLoaded();
        HumanPaceUtil.pausePreprocessingAction();
        return saveModal;
    }

    private void attachDiagnostics() {
        String report = "Active step: " + wizard().getCurrentStep().map(WizardStep::label).orElse("none")
                + "\nVisible result rows: " + getVisibleRowCount()
                + "\nVisible columns: " + getVisibleColumnTitles()
                + "\nSave enabled: " + isSaveAvailable()
                + "\nURL: " + driver.getCurrentUrl();
        Allure.addAttachment("Step 5 diagnostics", "text/plain", report, ".txt");
    }
}
