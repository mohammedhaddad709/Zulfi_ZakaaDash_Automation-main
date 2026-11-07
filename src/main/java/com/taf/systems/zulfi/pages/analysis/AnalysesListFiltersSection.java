package com.taf.systems.zulfi.pages.analysis;

import com.taf.systems.zulfi.ui.PrimeVueDatePicker;
import com.taf.systems.zulfi.ui.PrimeVueSelect;
import com.taf.systems.zulfi.validations.AnalysesListingMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Filter toolbar of «التحليلات» (US-FAP-ANL-003): search field, «حالة التحليل» / «نوع المباراة» / «نوع الفيديو»
 * selects and the single «التاريخ» date filter (re-baselined 2026-10-22, docs/testing/REBASELINE_2026-10-22.md).
 */
public final class AnalysesListFiltersSection {

    /** Toolbar selects in display order (position is the fallback once a pick replaces the placeholder aria-label). */
    private static final List<String> TOOLBAR_SELECTS = List.of(
            AnalysesListingMessages.FILTER_STATUS_LABEL,
            AnalysesListingMessages.FILTER_MATCH_TYPE_LABEL,
            AnalysesListingMessages.FILTER_VIDEO_SOURCE_LABEL);
    private static final String TOOLBAR = "//*[contains(@class,'p-iconfield')]/parent::*";
    private static final By DATE_FILTER = By.xpath(TOOLBAR + "//input[contains(@class,'p-datepicker-input')]");
    private static final By RESET_BUTTON = By.xpath(
            "//button[contains(normalize-space(.),'" + AnalysesListingMessages.RESET_FILTERS + "')"
                    + " or contains(@aria-label,'" + AnalysesListingMessages.RESET_FILTERS + "')]");

    private final WebDriver driver;

    public AnalysesListFiltersSection(WebDriver driver) {
        this.driver = driver;
    }

    // --- selects ---

    public AnalysesListFiltersSection selectStatus(String optionLabel) {
        select(AnalysesListingMessages.FILTER_STATUS_LABEL).pick(optionLabel);
        return this;
    }

    public AnalysesListFiltersSection selectVideoSource(String optionLabel) {
        select(AnalysesListingMessages.FILTER_VIDEO_SOURCE_LABEL).pick(optionLabel);
        return this;
    }

    /** «الكل»: no such option since 2026-10-22 — a cleared select (placeholder shown) means all. */
    public AnalysesListFiltersSection selectStatusAll() {
        select(AnalysesListingMessages.FILTER_STATUS_LABEL).clear();
        return this;
    }

    public AnalysesListFiltersSection selectVideoSourceAll() {
        select(AnalysesListingMessages.FILTER_VIDEO_SOURCE_LABEL).clear();
        return this;
    }

    /** True when the select shows no choice: its placeholder (e.g. «حالة التحليل»), blank, or a legacy «الكل». */
    public boolean isCleared(String filterLabel) {
        String selection = select(filterLabel).label();
        return selection.isBlank() || selection.equals(filterLabel) || selection.contains(AnalysesListingMessages.FILTER_ALL);
    }

    public String readStatusFilterSelection() {
        return select(AnalysesListingMessages.FILTER_STATUS_LABEL).label();
    }

    public String readVideoSourceFilterSelection() {
        return select(AnalysesListingMessages.FILTER_VIDEO_SOURCE_LABEL).label();
    }

    private PrimeVueSelect select(String label) {
        int position = TOOLBAR_SELECTS.indexOf(label) + 1;
        String byAria = "//*[@role='combobox'][@aria-label='" + label + "']";
        By combo = position == 0
                ? By.xpath(byAria)
                : By.xpath(byAria + "|(" + TOOLBAR + "/div[contains(@class,'p-select')]//*[@role='combobox'])[" + position + "]");
        return new PrimeVueSelect(driver, combo, label);
    }

    // --- «التاريخ» single-date filter ---

    public boolean isDateFilterVisible() {
        return driver.findElements(DATE_FILTER).stream().anyMatch(WebElement::isDisplayed);
    }

    public String dateFilterPlaceholder() {
        return driver.findElement(DATE_FILTER).getAttribute("placeholder");
    }

    public boolean pickDate(LocalDate date) {
        return PrimeVueDatePicker.pick(driver, driver.findElement(DATE_FILTER), date);
    }

    public String readDateValue() {
        return PrimeVueDatePicker.value(driver.findElement(DATE_FILTER));
    }

    public AnalysesListFiltersSection clearDate() {
        WebElement picker = driver.findElement(DATE_FILTER).findElement(By.xpath("./ancestor::*[@data-pc-name='datepicker'][1]"));
        picker.findElements(By.cssSelector("[data-pc-section='clearicon'], .p-datepicker-clear-icon")).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .ifPresent(WebElement::click);
        return this;
    }

    // --- legacy «من تاريخ» / «إلى تاريخ» (story wording; kept for environments that still render them) ---

    public AnalysesListFiltersSection setFromDate(String displayDate) {
        return typeIntoLegacyDate(AnalysesListingMessages.FILTER_FROM_DATE, displayDate);
    }

    public AnalysesListFiltersSection setToDate(String displayDate) {
        return typeIntoLegacyDate(AnalysesListingMessages.FILTER_TO_DATE, displayDate);
    }

    /** Clears the legacy from/to fields, or the single «التاريخ» filter on the current UI. */
    public AnalysesListFiltersSection clearDateRange() {
        if (driver.findElements(legacyDateInput(AnalysesListingMessages.FILTER_FROM_DATE)).isEmpty()) {
            return isDateFilterVisible() ? clearDate() : this;
        }
        typeIntoLegacyDate(AnalysesListingMessages.FILTER_FROM_DATE, "");
        return typeIntoLegacyDate(AnalysesListingMessages.FILTER_TO_DATE, "");
    }

    public String readFromDateValue() {
        return legacyDateValue(AnalysesListingMessages.FILTER_FROM_DATE);
    }

    public String readToDateValue() {
        return legacyDateValue(AnalysesListingMessages.FILTER_TO_DATE);
    }

    public boolean isDateRangeValidationVisible() {
        return driver.getPageSource().contains(AnalysesListingMessages.DATE_RANGE_VALIDATION);
    }

    private AnalysesListFiltersSection typeIntoLegacyDate(String label, String value) {
        WebElement input = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(legacyDateInput(label)));
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        if (value != null && !value.isBlank()) {
            input.sendKeys(value.trim());
            input.sendKeys(Keys.TAB);
        }
        return this;
    }

    private String legacyDateValue(String label) {
        List<WebElement> inputs = driver.findElements(legacyDateInput(label));
        return inputs.isEmpty() ? "" : PrimeVueDatePicker.value(inputs.getFirst());
    }

    private static By legacyDateInput(String label) {
        return By.xpath("//*[contains(normalize-space(.),'" + label + "')]/following::input[contains(@class,'p-datepicker-input')][1]"
                + "|//label[contains(normalize-space(.),'" + label + "')]/following::input[contains(@class,'p-datepicker-input')][1]");
    }

    // --- reset ---

    public AnalysesListFiltersSection resetFilters() {
        if (!isResetFiltersVisible()) {
            return this;
        }
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(RESET_BUTTON)).click();
        return this;
    }

    public boolean isResetFiltersVisible() {
        return driver.findElements(RESET_BUTTON).stream().anyMatch(WebElement::isDisplayed);
    }
}
