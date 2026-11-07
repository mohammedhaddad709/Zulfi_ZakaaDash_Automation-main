package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Step 1 "إعدادات المعالجة". Owns the settings categories. Extract a category into its own panel class
 * (preprocessing/settings/) only when it gets substantial automated coverage.
 */
public class ProcessingSettingsStep extends WizardStepPage {

    public enum SettingsCategory {
        DATA_UNDERSTANDING("فهم البيانات"),
        DATA_QUALITY("جودة البيانات"),
        FEATURE_ELIGIBILITY("صلاحية الأعمدة كميزات"),
        VALUE_TRANSFORMATION("تحويل وتجهيز القيم"),
        GENERAL_FEATURE_PREPARATION("تجهيز الخصائص العامة");

        private final String label;

        SettingsCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public ProcessingSettingsStep(WebDriver driver) {
        super(driver, WizardStep.SETTINGS, categoryTab(SettingsCategory.DATA_UNDERSTANDING));
    }

    public boolean isCategoryAvailable(SettingsCategory category) {
        return isDisplayed(categoryTab(category));
    }

    /** Category tab labels of Step 1 as displayed, in order. */
    public List<String> getVisibleCategoryLabels() {
        return driver.findElements(AdvancedPreprocessingWizard.inWizard("//button[@role='tab']")).stream()
                .filter(WebElement::isDisplayed)
                .map(tab -> tab.getText().trim())
                .toList();
    }

    public void openCategory(SettingsCategory category) {
        wait.waitForAnyClickable(categoryTab(category)).click();
        HumanPaceUtil.pausePreprocessingAction();
    }

    // Live DOM 2026-11-07: categories are PrimeVue role="tab" buttons. Step 2 reuses some labels in its own
    // (hidden) tab list, so callers must accept any visible match.
    private static By categoryTab(SettingsCategory category) {
        return AdvancedPreprocessingWizard.inWizard(
                "//button[@role='tab'][normalize-space(.)='" + category.label() + "']");
    }
}
