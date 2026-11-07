package com.taf.systems.zakaadash.pages.preprocessing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Step 2 "اقتراحات النظام". Suggestion rule accessors are added with the first suggestion-logic test. */
public class SystemSuggestionsStep extends WizardStepPage {

    // Live DOM 2026-11-07: this rule category tab exists only in Step 2 (Step 1 ends with "تجهيز الخصائص العامة").
    private static final By SUGGESTION_CATEGORY = AdvancedPreprocessingWizard.inWizard(
            "//button[@role='tab'][normalize-space(.)='تجهيز الميزات للنمذجة']");

    public SystemSuggestionsStep(WebDriver driver) {
        super(driver, WizardStep.SUGGESTIONS, SUGGESTION_CATEGORY);
    }
}
