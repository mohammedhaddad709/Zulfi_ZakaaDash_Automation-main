package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * "معالجة البيانات المتقدمة" wizard: shared navigation and step state only.
 *
 * <p>Live DOM 2026-11-07: the stepper is plain markup without ARIA state. Each step shows a title span followed by a
 * subtitle span; the current step's title carries {@code text-primary}, completed steps {@code text-green-600}.
 * Every step panel stays in the DOM and is hidden with {@code display:none}, so step locators must accept
 * "any visible match" rather than the first match.
 */
public class AdvancedPreprocessingWizard extends PreprocessingDialog {

    private static final String TITLE = "معالجة البيانات المتقدمة";
    private static final String NEXT_LABEL = "التالي";
    private static final String PREVIOUS_LABEL = "السابق";
    private static final String ACTIVE_STEP_CLASS = "text-primary";
    private static final String COMPLETED_STEP_CLASS = "text-green-600";

    private static final By STEPPER_TITLES = By.xpath(dialogXpath(TITLE)
            + "//span[@title][following-sibling::span[@title]]");
    private static final By WIZARD_BUSY = By.xpath(dialogXpath(TITLE)
            + "//*[@role='progressbar' or contains(@class,'pi-spin') or contains(@class,'p-progress-spinner')]");

    public AdvancedPreprocessingWizard(WebDriver driver) {
        super(driver, TITLE);
    }

    /** Scopes a step's locator to the wizard dialog. */
    public static By inWizard(String relativeXpath) {
        return By.xpath(dialogXpath(TITLE) + relativeXpath);
    }

    public AdvancedPreprocessingWizard waitUntilLoaded() {
        waitForDialog();
        return this;
    }

    /** Step titles as the product renders them in the stepper, in order. */
    public List<String> getStepperTitles() {
        return driver.findElements(STEPPER_TITLES).stream().map(title -> title.getAttribute("title")).toList();
    }

    public boolean isStepActive(WizardStep step) {
        return stepTitleHasClass(step, ACTIVE_STEP_CLASS);
    }

    public boolean isStepCompleted(WizardStep step) {
        return stepTitleHasClass(step, COMPLETED_STEP_CLASS);
    }

    public Optional<WizardStep> getCurrentStep() {
        return Arrays.stream(WizardStep.values()).filter(this::isStepActive).findFirst();
    }

    public AdvancedPreprocessingWizard waitForStep(WizardStep step) {
        waitForDialog();
        wait.waitUntilTrue(driver -> isStepActive(step), step.label() + " should become the active wizard step");
        return this;
    }

    public void waitForWizardIdle() {
        wait.waitForInvisible(WIZARD_BUSY);
    }

    public void waitForNextEnabled() {
        wait.waitForClickable(footerButton(NEXT_LABEL));
    }

    public void clickNextWhenReady() {
        waitForWizardIdle();
        wait.waitForBlockingOverlayToDisappear("Wizard Next — " + NEXT_LABEL);
        clickFooterButton(NEXT_LABEL);
    }

    public void clickNext() {
        clickNextWhenReady();
    }

    public boolean isPreviousEnabled() {
        return driver.findElements(footerButton(PREVIOUS_LABEL)).stream()
                .anyMatch(button -> button.isDisplayed() && button.isEnabled());
    }

    public void clickPrevious() {
        clickFooterButton(PREVIOUS_LABEL);
    }

    /** Step 5 replaces Next with Save ("حفظ"); scoped to the footer so no in-step button can match. */
    public By saveButton() {
        return footerButton("حفظ");
    }

    private void clickFooterButton(String label) {
        wait.waitForClickable(footerButton(label)).click();
        HumanPaceUtil.pausePreprocessingAction();
    }

    private boolean stepTitleHasClass(WizardStep step, String cssClass) {
        List<WebElement> titles = driver.findElements(STEPPER_TITLES);
        if (titles.size() <= step.ordinal()) {
            return false;
        }
        String classes = titles.get(step.ordinal()).getAttribute("class");
        return classes != null && Arrays.asList(classes.trim().split("\\s+")).contains(cssClass);
    }
}
