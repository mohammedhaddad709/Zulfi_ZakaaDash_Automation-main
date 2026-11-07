package com.taf.systems.zulfi.pages.analysis;

import com.taf.core.utils.actions.ElementActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.regex.Pattern;

/** Post-submit confirmation inside the create-analysis dialog. */
public final class AnalysisSuccessDialog {

    public static final String SUCCESS_MESSAGE = "تم إرسال المباراة للتحليل";
    public static final String VIEW_STATUS_LABEL = "عرض حالة التحليل";
    public static final String CREATE_ANOTHER_LABEL = "تحليل مباراة أخرى";

    private static final By SUCCESS_MESSAGE_LOCATOR =
            By.xpath("//*[contains(normalize-space(.),'" + SUCCESS_MESSAGE + "')]");
    private static final By VIEW_STATUS_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + VIEW_STATUS_LABEL
                            + "') or contains(normalize-space(.),'"
                            + VIEW_STATUS_LABEL
                            + "')]"
                            + " | //a[contains(@aria-label,'"
                            + VIEW_STATUS_LABEL
                            + "') or contains(normalize-space(.),'"
                            + VIEW_STATUS_LABEL
                            + "')]");
    private static final Pattern ANALYSIS_DETAIL_URL = Pattern.compile("(?i)/analysis/\\d+");

    private static final By CREATE_ANOTHER_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + CREATE_ANOTHER_LABEL
                            + "') or contains(normalize-space(.),'"
                            + CREATE_ANOTHER_LABEL
                            + "')]"
                            + " | //a[contains(@aria-label,'"
                            + CREATE_ANOTHER_LABEL
                            + "') or contains(normalize-space(.),'"
                            + CREATE_ANOTHER_LABEL
                            + "')]");

    private final WebDriver driver;
    private final ElementActions actions;

    public AnalysisSuccessDialog(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }

    public AnalysisSuccessDialog waitForSuccessMessage() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOfElementLocated(SUCCESS_MESSAGE_LOCATOR));
        return this;
    }

    public boolean isSuccessMessageVisible() {
        return !driver.findElements(SUCCESS_MESSAGE_LOCATOR).isEmpty()
                && driver.findElement(SUCCESS_MESSAGE_LOCATOR).isDisplayed();
    }

    public boolean isViewStatusActionVisible() {
        return !driver.findElements(VIEW_STATUS_BUTTON).isEmpty()
                && driver.findElement(VIEW_STATUS_BUTTON).isDisplayed();
    }

    public boolean isCreateAnotherActionVisible() {
        return !driver.findElements(CREATE_ANOTHER_BUTTON).isEmpty()
                && driver.findElement(CREATE_ANOTHER_BUTTON).isDisplayed();
    }

    /** Opens analysis status/detail from the success dialog (does not use «تحليل مباراة أخرى»). */
    public void clickViewAnalysisStatus() {
        String before = driver.getCurrentUrl();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        wait.until(ExpectedConditions.elementToBeClickable(VIEW_STATUS_BUTTON));
        actions.click(VIEW_STATUS_BUTTON);
        wait.until(
                d -> {
                    String url = d.getCurrentUrl();
                    return ANALYSIS_DETAIL_URL.matcher(url).find() && !url.equals(before);
                });
    }

    /** Returns to create form for another analysis (does not use «عرض حالة التحليل»). */
    public void clickCreateAnotherAnalysis() {
        actions.click(CREATE_ANOTHER_BUTTON);
    }
}
