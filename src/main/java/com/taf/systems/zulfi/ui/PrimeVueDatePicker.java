package com.taf.systems.zulfi.ui;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * PrimeVue date picker driven through its calendar: the portal's pickers ignore typed input. Values render as
 * {@code dd/MM/yyyy}. Read-only helper apart from the selection itself.
 */
public final class PrimeVueDatePicker {

    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final By PANEL = By.cssSelector(".p-datepicker-panel");
    private static final By PREV = By.cssSelector(".p-datepicker-panel .p-datepicker-prev-button");
    private static final By NEXT = By.cssSelector(".p-datepicker-panel .p-datepicker-next-button");
    private static final By DAY_CELL =
            By.cssSelector(".p-datepicker-panel td:not(.p-datepicker-other-month) span[data-pc-section='day']");

    private PrimeVueDatePicker() {}

    /** Picks {@code date}; returns false (and closes the panel) when that day is disabled or not rendered. */
    public static boolean pick(WebDriver driver, WebElement input, LocalDate date) {
        WebElement cell = openAt(driver, input, date);
        if (cell == null || isDisabled(cell)) {
            input.sendKeys(Keys.ESCAPE);
            return false;
        }
        cell.click();
        return true;
    }

    public static boolean isSelectable(WebDriver driver, WebElement input, LocalDate date) {
        WebElement cell = openAt(driver, input, date);
        boolean selectable = cell != null && !isDisabled(cell);
        input.sendKeys(Keys.ESCAPE);
        return selectable;
    }

    public static String value(WebElement input) {
        String value = input.getAttribute("value");
        return value == null ? "" : PortalDom.normalize(value);
    }

    private static WebElement openAt(WebDriver driver, WebElement input, LocalDate date) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        input.click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(PANEL).stream().anyMatch(WebElement::isDisplayed));
        String current = value(input);
        LocalDate shown = LocalDate.now();
        if (!current.isBlank()) {
            try {
                shown = LocalDate.parse(current.split("\\s+")[0], DISPLAY);
            } catch (RuntimeException unparsable) {
                // keep today: the panel opens on the current month
            }
        }
        long months = ChronoUnit.MONTHS.between(shown.withDayOfMonth(1), date.withDayOfMonth(1));
        for (long i = 0; i < Math.abs(months); i++) {
            driver.findElement(months < 0 ? PREV : NEXT).click();
        }
        String day = String.valueOf(date.getDayOfMonth());
        return driver.findElements(DAY_CELL).stream()
                .filter(WebElement::isDisplayed)
                .filter(c -> day.equals(PortalDom.normalize(c.getText())))
                .findFirst()
                .orElse(null);
    }

    private static boolean isDisabled(WebElement cell) {
        String cls = cell.getAttribute("class") == null ? "" : cell.getAttribute("class");
        return "true".equals(cell.getAttribute("data-p-disabled")) || cls.contains("p-disabled")
                || "true".equals(cell.getAttribute("aria-disabled"));
    }
}
