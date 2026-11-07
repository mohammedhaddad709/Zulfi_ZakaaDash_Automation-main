package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.pages.BasePage;
import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** A wizard step is loaded when the stepper marks it active and a control unique to the step is visible. */
public abstract class WizardStepPage extends BasePage {

    protected final WaitUtil wait;
    private final AdvancedPreprocessingWizard wizard;
    private final WizardStep step;
    private final By stepContent;

    protected WizardStepPage(WebDriver driver, WizardStep step, By stepContent) {
        super(driver);
        this.wait = new WaitUtil(driver);
        this.wizard = new AdvancedPreprocessingWizard(driver);
        this.step = step;
        this.stepContent = stepContent;
    }

    protected AdvancedPreprocessingWizard wizard() {
        return wizard;
    }

    public WizardStep step() {
        return step;
    }

    public boolean isLoaded() {
        return wizard.isStepActive(step) && isContentLoaded();
    }

    /** A control unique to this step is visible (hidden panels of other steps are ignored). */
    public boolean isContentLoaded() {
        return isDisplayed(stepContent);
    }

    public void waitUntilLoaded() {
        wait.waitForBlockingOverlayToDisappear("Load " + step.label());
        wizard.waitForStep(step);
        wait.waitForAnyVisible(stepContent);
        wait.waitForBlockingOverlayToDisappear("Render " + step.label());
    }

    /** Baseline happy path: step content is ready and Next is enabled when the product allows continuing. */
    public void waitUntilReadyToContinue() {
        waitUntilLoaded();
        wizard.waitForWizardIdle();
        wizard.waitForNextEnabled();
    }
}
