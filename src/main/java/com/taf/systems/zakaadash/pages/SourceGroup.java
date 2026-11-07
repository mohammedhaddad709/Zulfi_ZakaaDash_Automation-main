package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One original data source on Data Versions. Live DOM 2026-11-07 (PrimeVue Accordion):
 * <ul>
 *   <li>header {@code button[data-pc-name='accordionheader']} holds {@code h3[title=<source>]}, a status tag
 *       ("الملف الأصلي" or "تمت المعالجة (N) مرات") and the row actions;</li>
 *   <li>{@code button[aria-label='accordion-toggle']} expands it; the header's {@code aria-controls} names the
 *       content region that lists processed versions as {@code h4[title=<version>]} rows.</li>
 * </ul>
 * Call {@link DataVersionsPage#waitForSource(String)} first: the list is paginated and filtered by search.
 */
public final class SourceGroup {

    private static final Pattern PROCESSED_COUNT = Pattern.compile("\\((\\d+)\\)");
    private static final String PROCESSED_BADGE_TEXT = "تمت المعالجة";
    private static final String START_PREPROCESSING = "بدء المعالجة المسبقة";
    private static final String CREATE_DASHBOARD = "إنشاء لوحة بيانات";

    private final WebDriver driver;
    private final WaitUtil wait;
    private final String sourceName;

    private SourceGroup(WebDriver driver, String sourceName) {
        this.driver = driver;
        this.wait = new WaitUtil(driver);
        this.sourceName = sourceName;
    }

    public static SourceGroup forSource(WebDriver driver, String sourceName) {
        if (sourceName.contains("'")) {
            throw new IllegalArgumentException("Source name must not contain a single quote: " + sourceName);
        }
        return new SourceGroup(driver, sourceName);
    }

    public WebElement locateNameElement() {
        return wait.waitForVisible(nameLocator());
    }

    /** Read-only: parses "تمت المعالجة (N) مرات" on the header; an untouched source ("الملف الأصلي") counts 0. */
    public int getProcessedVersionsCount() {
        List<WebElement> badges = driver.findElements(By.xpath(headerXpath()
                + "//span[@data-pc-name='tag'][contains(normalize-space(.),'" + PROCESSED_BADGE_TEXT + "')]"));
        if (badges.isEmpty()) {
            return 0;
        }
        Matcher matcher = PROCESSED_COUNT.matcher(badges.get(0).getText());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    /** Status tag on the source header: "الملف الأصلي" for an untouched upload, "تمت المعالجة (N) مرات" after processing. */
    public String getStatusTag() {
        return wait.waitForVisible(By.xpath(headerXpath() + "//span[@data-pc-name='tag']")).getText().trim();
    }

    public void waitForProcessedVersionsCount(int expectedCount) {
        wait.waitUntilTrue(driver -> getProcessedVersionsCount() == expectedCount,
                "Processed count of " + sourceName + " should become " + expectedCount);
    }

    public boolean isExpanded() {
        return "true".equals(header().getAttribute("aria-expanded"));
    }

    public void expandIfCollapsed() {
        wait.waitForBlockingOverlayToDisappear("Expand processed versions");
        if (isExpanded()) {
            return;
        }
        wait.waitForClickable(By.xpath(headerXpath() + "//button[@aria-label='accordion-toggle']")).click();
        wait.waitUntilTrue(driver -> isExpanded(), "Source " + sourceName + " should expand");
        HumanPaceUtil.pauseWizardTransition();
    }

    public void waitForProcessedVersionVisible(String processedVersionName) {
        wait.waitUntilTrue(driver -> isProcessedVersionInGroup(processedVersionName),
                "Processed version " + processedVersionName + " should be listed under " + sourceName);
    }

    /** True when the expanded content region of this source lists the processed version. */
    public boolean isProcessedVersionInGroup(String processedVersionName) {
        if (!isExpanded()) {
            return false;
        }
        return driver.findElements(processedVersionLocator(processedVersionName)).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    /** Names of the processed versions currently listed under this (expanded) source. */
    public List<String> getListedProcessedVersions() {
        return driver.findElements(By.xpath(contentXpath() + "//h4[@title]")).stream()
                .filter(WebElement::isDisplayed)
                .map(title -> title.getAttribute("title").trim())
                .toList();
    }

    /** Clicks create-dashboard on the row of {@code processedVersionName}, not on the original source row. */
    public void clickCreateDashboardForProcessedVersion(String processedVersionName) {
        expandIfCollapsed();
        By createButton = By.xpath(processedVersionXpath(processedVersionName)
                + "/ancestor::div[.//button[normalize-space(.)='" + CREATE_DASHBOARD + "']][1]"
                + "//button[normalize-space(.)='" + CREATE_DASHBOARD + "']");
        WebElement button = wait.waitForClickable(createButton, "Create dashboard from processed version");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", button);
        HumanPaceUtil.pauseNormalAction();
        button.click();
        HumanPaceUtil.pauseWizardTransition();
    }

    public void clickStartPreprocessing() {
        By startButton = By.xpath(headerXpath() + "//button[normalize-space(.)='" + START_PREPROCESSING + "']");
        WebElement button = wait.waitForClickable(startButton, "Start preprocessing");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", button);
        button.click();
    }

    /** Human-readable state for failure messages and Allure. */
    public String describeState(String processedVersionName, int countBefore) {
        String state = "Source: " + sourceName
                + "\nListed: " + !driver.findElements(nameLocator()).isEmpty()
                + "\nProcessed count before: " + countBefore
                + "\nProcessed count now: " + safeCount()
                + "\nExpanded: " + safeExpanded()
                + "\nExpected processed version: " + processedVersionName
                + "\nListed processed versions: " + safeVersions();
        Allure.addAttachment("Source group state — " + sourceName, "text/plain", state, ".txt");
        return state;
    }

    private WebElement header() {
        return wait.waitForVisible(By.xpath(headerXpath()));
    }

    private By nameLocator() {
        return By.xpath("//h3[@title='" + sourceName + "']");
    }

    private String headerXpath() {
        return "//h3[@title='" + sourceName + "']/ancestor::button[@data-pc-name='accordionheader'][1]";
    }

    private String contentXpath() {
        return "//div[@data-pc-name='accordioncontent'][@id=" + headerXpath() + "/@aria-controls]";
    }

    private String processedVersionXpath(String processedVersionName) {
        return contentXpath() + "//h4[@title='" + processedVersionName + "']";
    }

    private By processedVersionLocator(String processedVersionName) {
        return By.xpath(processedVersionXpath(processedVersionName));
    }

    private String safeCount() {
        try {
            return String.valueOf(getProcessedVersionsCount());
        } catch (RuntimeException ex) {
            return "unreadable (" + ex.getClass().getSimpleName() + ")";
        }
    }

    private String safeExpanded() {
        try {
            return String.valueOf(isExpanded());
        } catch (RuntimeException ex) {
            return "unreadable (" + ex.getClass().getSimpleName() + ")";
        }
    }

    private String safeVersions() {
        try {
            return getListedProcessedVersions().toString();
        } catch (RuntimeException ex) {
            return "unreadable (" + ex.getClass().getSimpleName() + ")";
        }
    }
}
