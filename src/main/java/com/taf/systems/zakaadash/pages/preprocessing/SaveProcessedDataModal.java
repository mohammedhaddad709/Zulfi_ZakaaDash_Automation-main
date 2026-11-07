package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Final "معالجة البيانات" modal shown after Save in Step 5 (it replaces the wizard dialog).
 * Live DOM 2026-11-07: two option cards titled by an {@code h3}, a source-name input and footer buttons
 * "إلغاء" / "حفظ البيانات".
 */
public class SaveProcessedDataModal extends PreprocessingDialog {

    private static final String SAVE_AS_NEW_COPY_OPTION = "حفظ كنسخة جديدة";
    private static final String REPLACE_ORIGINAL_OPTION = "استبدال الملف الأصلي";
    private static final String SAVE_LABEL = "حفظ البيانات";
    private static final String CANCEL_LABEL = "إلغاء";

    public SaveProcessedDataModal(WebDriver driver) {
        super(driver, "معالجة البيانات");
    }

    public SaveProcessedDataModal waitUntilLoaded() {
        waitForDialog();
        return this;
    }

    public boolean isSaveAsNewCopyAvailable() {
        return isDisplayed(optionCard(SAVE_AS_NEW_COPY_OPTION));
    }

    public boolean isReplaceOriginalAvailable() {
        return isDisplayed(optionCard(REPLACE_ORIGINAL_OPTION));
    }

    public void selectSaveAsNewCopy() {
        wait.waitForClickable(optionCard(SAVE_AS_NEW_COPY_OPTION)).click();
        HumanPaceUtil.pausePreprocessingAction();
    }

    public void enterSourceName(String sourceName) {
        By sourceNameInput = inDialog("//input[@placeholder='ادخل اسما لمصدر البيانات']");
        wait.waitForVisible(sourceNameInput);
        type(sourceNameInput, sourceName);
        HumanPaceUtil.pausePreprocessingAction();
    }

    public void save() {
        wait.waitForClickable(footerButton(SAVE_LABEL)).click();
    }

    public void cancel() {
        wait.waitForClickable(footerButton(CANCEL_LABEL)).click();
        waitUntilClosed();
    }

    /** The clickable card is the closest ancestor of the option title that also holds its description. */
    private By optionCard(String optionTitle) {
        return inDialog("//h3[normalize-space(.)='" + optionTitle + "']/ancestor::div[contains(@class,'cursor-pointer')][1]");
    }
}
