package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Processing Mode modal ("ابدأ معالجة البيانات"). Automatic and Advanced processing are footer buttons
 * identified by their aria-label (live DOM 2026-11-07).
 */
public class PreprocessingEntryModal extends PreprocessingDialog {

    private static final String MODAL_TITLE = "ابدأ معالجة البيانات";
    private static final String AUTOMATIC_PROCESSING = "معالجة تلقائية";
    private static final String ADVANCED_PROCESSING = "معالجة متقدمة";

    public PreprocessingEntryModal(WebDriver driver) {
        super(driver, MODAL_TITLE);
    }

    public PreprocessingEntryModal waitUntilLoaded() {
        waitForDialog();
        wait.waitForVisible(processingModeButton(AUTOMATIC_PROCESSING));
        wait.waitForVisible(processingModeButton(ADVANCED_PROCESSING));
        return this;
    }

    public boolean isTitleVisible() {
        return isDisplayed(inDialog("//span[@data-pc-section='title'][normalize-space(.)='" + MODAL_TITLE + "']"));
    }

    public boolean isAutomaticProcessingAvailable() {
        return isDisplayed(processingModeButton(AUTOMATIC_PROCESSING));
    }

    public boolean isAdvancedProcessingAvailable() {
        return isDisplayed(processingModeButton(ADVANCED_PROCESSING));
    }

    public AdvancedPreprocessingWizard selectAdvancedProcessing() {
        wait.waitForClickable(processingModeButton(ADVANCED_PROCESSING)).click();
        HumanPaceUtil.pausePreprocessingAction();
        return new AdvancedPreprocessingWizard(driver).waitUntilLoaded();
    }

    public void close() {
        wait.waitForClickable(closeButton()).click();
        HumanPaceUtil.pausePreprocessingAction();
        waitUntilClosed();
    }

    private By processingModeButton(String label) {
        return footerButton(label);
    }
}
