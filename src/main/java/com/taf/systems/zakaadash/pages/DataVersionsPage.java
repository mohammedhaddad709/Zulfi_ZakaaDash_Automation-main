package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.pages.preprocessing.PreprocessingEntryModal;
import com.taf.systems.zakaadash.utils.ApplicationReadiness;
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

public class DataVersionsPage extends BasePage {

    private static final By DATA_SOURCES_LABEL = By.xpath("//section//h2[normalize-space(.)='مصادر البيانات']");
    private static final By UPLOAD_DATA_BUTTON = By.xpath("//header//button[normalize-space(.)='ارفع بياناتك الآن']");
    // The source list is paginated (10 per page), so sources are located through the search box.
    private static final By SOURCE_SEARCH_INPUT = By.cssSelector("section input[placeholder='بحث...']");
    private static final By SOURCE_NAMES = By.cssSelector("[data-pc-name='accordionpanel'] h3[title]");
    private static final String NO_SEARCH_RESULTS = "لم يتم العثور على نتائج للبحث عن";

    private final WaitUtil wait;

    public DataVersionsPage(WebDriver driver) {
        super(driver);
        this.wait = new WaitUtil(driver);
    }

    /** Opens Data Versions by URL (an authenticated session is required, otherwise the app redirects to login). */
    public void openByUrl() {
        open(ConfigManager.config().zakaadashBaseUrl() + ConfigManager.config().zakaadashDataVersionsPath());
    }

    /**
     * The source list is fetched after navigation behind the app's full-screen loading overlay; on DEV this has
     * taken longer than the explicit wait, so the overlay is bounded by the page-load timeout here.
     */
    public DataVersionsPage waitUntilLoaded() {
        wait.waitForUrlToContain(ConfigManager.config().zakaadashDataVersionsPath());
        wait.waitForVisible(DATA_SOURCES_LABEL);
        ApplicationReadiness.waitForBlockingOverlayToDisappear(driver, "Confirm Data Versions loaded",
                Duration.ofSeconds(ConfigManager.config().pageLoadTimeoutSeconds()));
        return this;
    }

    public boolean isLoaded() {
        return driver.getCurrentUrl().contains(ConfigManager.config().zakaadashDataVersionsPath())
                && isDisplayed(DATA_SOURCES_LABEL);
    }

    public boolean isUploadDataVisible() {
        return isDisplayed(UPLOAD_DATA_BUTTON);
    }

    public boolean isUploadDataEnabled() {
        return wait.waitForVisible(UPLOAD_DATA_BUTTON).isEnabled();
    }

    public UploadDataModal openUploadDataModal() {
        wait.waitForClickable(UPLOAD_DATA_BUTTON, "Open Upload Data modal").click();
        UploadDataModal uploadModal = new UploadDataModal(driver).waitUntilLoaded();
        HumanPaceUtil.pauseNormalAction();
        return uploadModal;
    }

    /** Filters the list with the search box and waits until the source is listed. */
    public DataVersionsPage waitForSource(String sourceName) {
        searchSources(sourceName);
        if (isNoSearchResultsShown(sourceName)) {
            throw new IllegalStateException("Data Versions search shows no results for expected source " + sourceName);
        }
        findSourceGroup(sourceName).locateNameElement();
        return this;
    }

    /** Types into the source search box and waits until the list shows matches or the no-results message. */
    public DataVersionsPage searchSources(String text) {
        WebElement search = wait.waitForVisible(SOURCE_SEARCH_INPUT);
        if (!text.equals(search.getAttribute("value"))) {
            search.clear();
            search.sendKeys(text);
        }
        wait.waitUntilTrue(driver -> listMatchesSearch(text), "Data Versions search did not settle for: " + text);
        wait.waitForBlockingOverlayToDisappear("Search data sources");
        return this;
    }

    public List<String> getListedSourceNames() {
        return driver.findElements(SOURCE_NAMES).stream()
                .filter(WebElement::isDisplayed)
                .map(name -> name.getAttribute("title").trim())
                .toList();
    }

    public boolean isNoSearchResultsShown(String text) {
        return isDisplayed(By.xpath("//section//*[contains(normalize-space(text()),'" + NO_SEARCH_RESULTS + "')"
                + " and contains(normalize-space(.),'" + text + "')]"));
    }

    private boolean listMatchesSearch(String text) {
        List<String> names = getListedSourceNames();
        boolean filtered = !names.isEmpty() && names.stream().allMatch(name -> name.contains(text));
        return filtered || isNoSearchResultsShown(text);
    }

    public SourceGroup findSourceGroup(String sourceName) {
        return SourceGroup.forSource(driver, sourceName);
    }

    /** Read-only badge parse on the collapsed source row; does not expand processed versions. */
    public int getProcessedVersionsCount(String sourceName) {
        return findSourceGroup(sourceName).getProcessedVersionsCount();
    }

    public void waitForProcessedVersionsCount(String sourceName, int expectedCount) {
        findSourceGroup(sourceName).waitForProcessedVersionsCount(expectedCount);
    }

    public DataVersionsPage expandProcessedVersions(String sourceName) {
        findSourceGroup(sourceName).expandIfCollapsed();
        HumanPaceUtil.pauseWizardTransition();
        return this;
    }

    public DataVersionsPage waitForProcessedVersionVisible(String originalSourceName, String processedVersionName) {
        findSourceGroup(originalSourceName).waitForProcessedVersionVisible(processedVersionName);
        return this;
    }

    public boolean isProcessedVersionVisible(String originalSourceName, String processedVersionName) {
        return findSourceGroup(originalSourceName).isProcessedVersionInGroup(processedVersionName);
    }

    public PreprocessingEntryModal startPreprocessingForSource(String sourceName) {
        findSourceGroup(sourceName).clickStartPreprocessing();
        HumanPaceUtil.pauseBetweenActions();
        return new PreprocessingEntryModal(driver).waitUntilLoaded();
    }
}
