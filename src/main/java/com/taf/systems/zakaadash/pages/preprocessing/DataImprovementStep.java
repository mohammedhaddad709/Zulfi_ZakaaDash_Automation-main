package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

/**
 * Step 4 "تحسين البيانات". Live DOM 2026-11-07: heading {@code h3#cleaning-suggestions-heading}, followed by a
 * PrimeVue tab strip whose {@code button[role='tab']} labels match {@link ImprovementTab}; the active tab has
 * {@code aria-selected='true'} and its panel is referenced by {@code aria-controls}.
 */
public class DataImprovementStep extends WizardStepPage {

    private static final String TAB_STRIP_XPATH =
            "//h3[@id='cleaning-suggestions-heading']/following-sibling::div[@data-pc-name='tabs']";
    private static final By STEP_HEADING = AdvancedPreprocessingWizard.inWizard(
            "//h3[@id='cleaning-suggestions-heading']");
    private static final By TABS = AdvancedPreprocessingWizard.inWizard(TAB_STRIP_XPATH + "//button[@role='tab']");

    public DataImprovementStep(WebDriver driver) {
        super(driver, WizardStep.IMPROVEMENT, STEP_HEADING);
    }

    @Override
    public void waitUntilLoaded() {
        super.waitUntilLoaded();
        wait.waitForAnyVisible(TABS);
    }

    @Override
    public boolean isContentLoaded() {
        return isDisplayed(STEP_HEADING) && isDisplayed(TABS);
    }

    public List<ImprovementTab> getAvailableTabs() {
        return Arrays.stream(ImprovementTab.values()).filter(tab -> isDisplayed(tabLocator(tab))).toList();
    }

    public boolean isTabActive(ImprovementTab tab) {
        return driver.findElements(tabLocator(tab)).stream()
                .anyMatch(control -> "true".equals(control.getAttribute("aria-selected")));
    }

    /** Opens a tab and waits until it is selected and its panel is visible. */
    public void openTab(ImprovementTab tab) {
        WebElement control = wait.waitForAnyClickable(tabLocator(tab));
        if (!isTabActive(tab)) {
            control.click();
        }
        wait.waitUntilTrue(driver -> isTabActive(tab), "Step 4 tab should become selected: " + tab.label());
        String panelId = control.getAttribute("aria-controls");
        wait.waitForAnyVisible(By.id(panelId));
        wizard().waitForWizardIdle();
        HumanPaceUtil.pauseStep4TabSwitch();
    }

    /** Text of the visible panel of a tab, e.g. a suggestion list or "لا توجد اقتراحات في هذا القسم.". */
    public String getTabPanelText(ImprovementTab tab) {
        WebElement control = wait.waitForAnyVisible(tabLocator(tab));
        return wait.waitForAnyVisible(By.id(control.getAttribute("aria-controls"))).getText().trim();
    }

    /** Baseline happy path: opens every improvement tab once without changing any option. */
    public void visitAllTabsInOrder() {
        StringBuilder log = new StringBuilder("Step 4 tabs visited (no option changed):\n");
        for (ImprovementTab tab : ImprovementTab.values()) {
            openTab(tab);
            String summary = getTabPanelText(tab).replaceAll("\\s+", " ");
            log.append("- ").append(tab.label()).append(": ")
                    .append(summary, 0, Math.min(120, summary.length())).append('\n');
        }
        wizard().waitForNextEnabled();
        Allure.addAttachment("Step 4 tab traversal", "text/plain", log.toString(), ".txt");
    }

    private static By tabLocator(ImprovementTab tab) {
        return AdvancedPreprocessingWizard.inWizard(
                TAB_STRIP_XPATH + "//button[@role='tab'][normalize-space(.)='" + tab.label() + "']");
    }
}
