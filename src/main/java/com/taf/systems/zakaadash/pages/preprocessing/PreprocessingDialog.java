package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.pages.BasePage;
import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared behaviour of the preprocessing dialogs. A dialog is identified by its exact title, because several
 * {@code div[role='dialog']} can be open at once (for example the wizard and the final save modal).
 */
public abstract class PreprocessingDialog extends BasePage {

    protected final WaitUtil wait;
    private final String dialogXpath;

    protected PreprocessingDialog(WebDriver driver, String title) {
        super(driver);
        this.wait = new WaitUtil(driver);
        this.dialogXpath = dialogXpath(title);
    }

    /** The title lives in the dialog header (a span for most dialogs, an h2 for the save modal). */
    static String dialogXpath(String title) {
        return "//div[@role='dialog'][./div[@data-pc-section='header']//*[normalize-space(text())='" + title + "']]";
    }

    protected String getDialogXpath() {
        return dialogXpath;
    }

    public boolean isLoaded() {
        return isDisplayed(By.xpath(getDialogXpath()));
    }

    public void waitUntilClosed() {
        wait.waitForInvisible(By.xpath(getDialogXpath()));
    }

    protected void waitForDialog() {
        wait.waitForVisible(By.xpath(getDialogXpath()));
        wait.waitForBlockingOverlayToDisappear("Dialog ready");
    }

    protected By inDialog(String relativeXpath) {
        return By.xpath(getDialogXpath() + relativeXpath);
    }

    /** Footer button by its aria-label (PrimeVue sets it to the visible label). */
    protected By footerButton(String label) {
        return inDialog("//div[@data-pc-section='footer']//button[@aria-label='" + label + "']");
    }

    protected By closeButton() {
        return inDialog("//button[@data-pc-name='pcclosebutton']");
    }

    /**
     * Locates a selectable option card by its title: the outermost ancestor of the title that contains
     * only this option, so the click lands on the card and not on the inner label.
     */
    protected String optionCardXpath(String optionTitle, String... otherOptionTitles) {
        String anyOptionTitle = Stream.concat(Stream.of(optionTitle), Stream.of(otherOptionTitles))
                .map(title -> "starts-with(normalize-space(text()),'" + title + "')")
                .collect(Collectors.joining(" or "));
        return dialogXpath + "//*[starts-with(normalize-space(text()),'" + optionTitle + "')]"
                + "/ancestor::*[count(.//*[" + anyOptionTitle + "])=1][last()]";
    }
}
