package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** "رفع البيانات" modal on Data Versions. Locators verified against the DEV DOM on 2026-11-07. */
public class UploadDataModal extends BasePage {

    private static final String DIALOG_XPATH =
            "//div[@role='dialog'][.//span[@data-pc-section='title'][normalize-space(.)='رفع البيانات']]";

    private static final By DIALOG = By.xpath(DIALOG_XPATH);
    private static final By CLOSE_BUTTON = By.xpath(DIALOG_XPATH + "//button[@data-pc-name='pcclosebutton']");
    private static final By SOURCE_NAME_INPUT = By.cssSelector("div[role='dialog'] input#data-source");
    private static final By FILE_INPUT = By.cssSelector("div[role='dialog'] input[type='file']");
    // Source cards are <div role="button"> that stay aria-disabled until a source name is entered.
    // Selecting a card changes its aria-label from "ملف" to "ملف - محدد".
    private static final By ENABLED_FILE_SOURCE_CARD =
            By.cssSelector("div[role='dialog'] div[role='button'][aria-label^='ملف'][aria-disabled='false']");
    private static final By SELECTED_FILE_SOURCE_CARD =
            By.cssSelector("div[role='dialog'] div[role='button'][aria-label='ملف - محدد']");
    private static final By SOURCE_CARDS = By.cssSelector("div[role='dialog'] div[role='button'][aria-disabled]");
    private static final By DROP_ZONE = By.xpath(DIALOG_XPATH + "//p[contains(normalize-space(.),'اسحب وافلت')]");
    // The attached-file card's remove button has no aria-label; the PrimeIcons trash glyph is its only hook.
    private static final By REMOVE_FILE_BUTTON = By.xpath(DIALOG_XPATH + "//button[.//span[contains(@class,'pi-trash')]]");
    // Outcome of attaching a file: the in-modal success/error message or an error toast.
    private static final By UPLOAD_OUTCOME = By.cssSelector("div[role='dialog'] [data-pc-name='message'].p-message-success,"
            + " div[role='dialog'] [data-pc-name='message'].p-message-error, .p-toast-message-error");
    private static final By UPLOAD_SUCCESS_MESSAGE =
            By.cssSelector("div[role='dialog'] [data-pc-name='message'].p-message-success");

    public enum Control {
        TITLE(By.xpath(DIALOG_XPATH + "//span[@data-pc-section='title']")),
        SOURCE_NAME(SOURCE_NAME_INPUT),
        DESCRIPTION(By.cssSelector("div[role='dialog'] input#description")),
        SOURCE_CHOOSER(By.xpath(DIALOG_XPATH + "//label[contains(normalize-space(.),'اختر مصدر البيانات')]")),
        FILE_SOURCE(By.cssSelector("div[role='dialog'] div[role='button'][aria-label^='ملف']")),
        GOOGLE_DRIVE_SOURCE(By.cssSelector("div[role='dialog'] div[role='button'][aria-label='Google Drive']")),
        DATABASE_SOURCE(By.cssSelector("div[role='dialog'] div[role='button'][aria-label='قاعدة بيانات']")),
        SAVE(By.cssSelector("div[role='dialog'] button#mainSave"));

        private final By locator;

        Control(By locator) {
            this.locator = locator;
        }
    }

    /** File analysis cards shown after a file is attached. */
    public enum SummaryField {
        FILE_NAME("اسم الملف"),
        ROW_COUNT("عدد الصفوف"),
        COLUMN_COUNT("عدد الأعمدة"),
        FILE_SIZE("حجم الملف"),
        MISSING_VALUES("القيم المفقودة");

        private final String label;

        SummaryField(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final WaitUtil wait;

    public UploadDataModal(WebDriver driver) {
        super(driver);
        this.wait = new WaitUtil(driver);
    }

    public UploadDataModal waitUntilLoaded() {
        wait.waitForVisible(DIALOG);
        wait.waitForVisible(Control.TITLE.locator);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(DIALOG) && isDisplayed(Control.TITLE.locator);
    }

    public boolean isControlVisible(Control control) {
        return isDisplayed(control.locator);
    }

    public boolean isFileSourceAvailable() {
        return isControlVisible(Control.FILE_SOURCE);
    }

    public boolean isFileSourceSelected() {
        return isDisplayed(SELECTED_FILE_SOURCE_CARD);
    }

    /** True when every source card (ملف / Google Drive / قاعدة بيانات) is enabled; false when any is aria-disabled. */
    public boolean areSourceCardsEnabled() {
        List<WebElement> cards = driver.findElements(SOURCE_CARDS);
        return !cards.isEmpty() && cards.stream().noneMatch(card -> "true".equals(card.getAttribute("aria-disabled")));
    }

    public boolean areSourceCardsDisabled() {
        List<WebElement> cards = driver.findElements(SOURCE_CARDS);
        return !cards.isEmpty() && cards.stream().allMatch(card -> "true".equals(card.getAttribute("aria-disabled")));
    }

    public String getSourceNameValue() {
        return wait.waitForVisible(SOURCE_NAME_INPUT).getAttribute("value");
    }

    public boolean isDropZoneVisible() {
        return isDisplayed(DROP_ZONE);
    }

    public UploadDataModal removeAttachedFile() {
        wait.waitForClickable(REMOVE_FILE_BUTTON).click();
        HumanPaceUtil.pauseBetweenActions();
        wait.waitForVisible(DROP_ZONE);
        return this;
    }

    public boolean isSaveEnabled() {
        return driver.findElements(Control.SAVE.locator).stream()
                .anyMatch(save -> save.isDisplayed() && save.isEnabled());
    }

    /**
     * The source cards stay aria-disabled until a source name is entered, so call {@link #enterSourceName} first.
     * Selecting File renders a drop-zone whose input[type='file'] is hidden: wait for presence, not visibility.
     */
    public UploadDataModal selectFileSource() {
        wait.waitForClickable(ENABLED_FILE_SOURCE_CARD).click();
        HumanPaceUtil.pauseBetweenActions();
        wait.waitForVisible(SELECTED_FILE_SOURCE_CARD);
        wait.waitForPresence(FILE_INPUT);
        return this;
    }

    public UploadDataModal enterSourceName(String sourceName) {
        WebElement field = wait.waitForVisible(SOURCE_NAME_INPUT);
        field.clear();
        field.sendKeys(sourceName);
        HumanPaceUtil.pauseBetweenActions();
        return this;
    }

    public void save() {
        wait.waitForClickable(Control.SAVE.locator).click();
        HumanPaceUtil.pauseBetweenActions();
    }

    /** Header close (X): the product closes the modal without a confirmation prompt. */
    public void close() {
        wait.waitForClickable(CLOSE_BUTTON).click();
        HumanPaceUtil.pauseBetweenActions();
        waitUntilClosed();
    }

    public void waitUntilClosed() {
        wait.waitForInvisible(DIALOG);
    }

    public boolean isFileInputAvailable() {
        return !driver.findElements(FILE_INPUT).isEmpty();
    }

    public WebElement getFileInput() {
        return wait.waitForPresence(FILE_INPUT);
    }

    public UploadDataModal attachFile(Path file) {
        Path absoluteFile = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absoluteFile)) {
            throw new IllegalArgumentException("Upload fixture does not exist: " + absoluteFile);
        }
        getFileInput().sendKeys(absoluteFile.toString());
        HumanPaceUtil.pauseBetweenActions();
        return this;
    }

    /** The attached-file card shows the original file name while the product analyses the file. */
    public String getSelectedFilename(String expectedFileName) {
        return wait.waitForVisible(By.xpath(DIALOG_XPATH + "//div[normalize-space(text())='" + expectedFileName + "']"))
                .getText().trim();
    }

    /** No-wait check that the attached-file card for this name is shown. */
    public boolean isFileListed(String fileName) {
        return isDisplayed(By.xpath(DIALOG_XPATH + "//div[normalize-space(text())='" + fileName + "']"));
    }

    public boolean isFileRecognized(String expectedFileName) {
        try {
            return getSelectedFilename(expectedFileName).contains(expectedFileName);
        } catch (TimeoutException ex) {
            return false;
        }
    }

    /** Waits for the product's file analysis to finish ("تم الرفع بنجاح"). */
    public Optional<String> waitForUploadSuccessMessage() {
        try {
            return Optional.of(wait.waitForVisible(UPLOAD_SUCCESS_MESSAGE).getText().trim());
        } catch (TimeoutException ex) {
            return Optional.empty();
        }
    }

    /** Waits for the product's verdict on the attached file and returns its text (success or error). */
    public String waitForUploadOutcome() {
        return wait.waitForAnyVisible(UPLOAD_OUTCOME).getText().trim();
    }

    public boolean isUploadSuccessShown() {
        return isDisplayed(UPLOAD_SUCCESS_MESSAGE);
    }

    /** Value of one analysis card, e.g. "20" for {@link SummaryField#ROW_COUNT}. */
    public String getSummaryValue(SummaryField field) {
        return wait.waitForVisible(By.xpath(DIALOG_XPATH + "//span[normalize-space(.)='" + field.label() + "']"
                + "/parent::div/following-sibling::span")).getText().trim();
    }
}
