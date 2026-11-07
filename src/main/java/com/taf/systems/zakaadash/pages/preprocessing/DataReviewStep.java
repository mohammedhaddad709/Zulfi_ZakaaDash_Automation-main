package com.taf.systems.zakaadash.pages.preprocessing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Step 3 "مراجعة البيانات". */
public class DataReviewStep extends WizardStepPage {

    // Live DOM 2026-11-07: a PrimeVue datatable whose column headers carry the type-review actions. Step 5 has its
    // own (hidden until reached) table, so the waits accept any visible match.
    private static final By REVIEW_TABLE = AdvancedPreprocessingWizard.inWizard(
            "//div[@data-pc-name='datatable']//table[@role='table']");
    private static final By REVIEW_ROWS = AdvancedPreprocessingWizard.inWizard(
            "//div[@data-pc-name='datatable']//tbody//tr[@role='row']");

    private static final By HEADER_TITLES =
            By.xpath(".//thead//th[@role='columnheader']//span[contains(@class,'truncate')]");

    public DataReviewStep(WebDriver driver) {
        super(driver, WizardStep.REVIEW, REVIEW_TABLE);
    }

    /**
     * Column names of the visible review table, in display order. Wide tables scroll horizontally, and Selenium
     * reports scrolled-out header cells as not displayed (empty getText), so the text is read from textContent.
     */
    public List<String> getColumnTitles() {
        WebElement table = wait.waitForAnyVisible(REVIEW_TABLE);
        return table.findElements(HEADER_TITLES).stream()
                .map(title -> title.getAttribute("textContent").trim())
                .toList();
    }

    @Override
    public void waitUntilLoaded() {
        super.waitUntilLoaded();
        wait.waitForAnyVisible(REVIEW_ROWS);
    }
}
