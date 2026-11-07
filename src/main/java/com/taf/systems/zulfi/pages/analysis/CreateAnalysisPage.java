package com.taf.systems.zulfi.pages.analysis;

import com.taf.core.pacing.HumanPacingManager;
import com.taf.systems.zulfi.ui.PrimeVueDatePicker;
import com.taf.systems.zulfi.validations.DuplicateFixtureToastRules;
import java.util.Map;
import com.taf.core.utils.WaitManager;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.analysis.fixture.FixtureDiscoveryResult;
import com.taf.systems.zulfi.analysis.fixture.IndexedFixtureEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureIndexCatalog;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.ui.PrimeVueIndexedSelect;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.OfficialFixtureLabelRules;
import com.taf.systems.zulfi.validations.TeamFieldDisplayRules;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** New match analysis form — full page or Home modal (`#analysis-create-form`). */
public final class CreateAnalysisPage {

    public static final String MATCH_TYPE_OFFICIAL = "رسمية";
    public static final String MATCH_TYPE_FRIENDLY = "ودية";
    public static final String OFFICIAL_MATCH_PLACEHOLDER = OfficialFixtureLabelRules.OFFICIAL_MATCH_PLACEHOLDER;
    public static final String DIALOG_TITLE_FRAGMENT = "تحليل مباراة جديدة";

    private static final Set<String> OFFICIAL_MATCH_UNSELECTED_LABELS =
            Set.of(OFFICIAL_MATCH_PLACEHOLDER, "اختر المباراة", "اختر مباراة");

    private static final By CREATE_FORM = By.id("analysis-create-form");
    private static final By CREATE_DIALOG_TITLE =
            By.xpath("//*[@role='dialog']//*[contains(normalize-space(.),'" + DIALOG_TITLE_FRAGMENT + "')]");
    /** Full-page {@code /analysis/create} route — title is outside {@code role='dialog'}. */
    // The page heading is typeset with tatweel («تحليـــــل مبـــــــاراة جديدة»), so text matching misses it: use its id.
    private static final By CREATE_PAGE_TITLE =
            By.xpath(
                    "//*[@id='analysis-create-page-title']"
                            + " | //main//*[self::h1 or self::h2][contains(normalize-space(.),'"
                            + DIALOG_TITLE_FRAGMENT
                            + "')]"
                            + " | //*[@id='analysis-create-form']//ancestor::main[1]//*[contains(normalize-space(.),'"
                            + DIALOG_TITLE_FRAGMENT
                            + "')]");
    private static final By MATCH_TYPE_FIELD = By.id("analysis-match-type");
    private static final By OFFICIAL_MATCH_FIELD = By.id("analysis-official-match");
    private static final By DESCRIPTION_FIELD = By.id("analysis-description");
    private static final By VIDEO_URL_FIELD = By.id("analysis-link");
    private static final By SUBMIT_BUTTON = By.cssSelector("button[aria-label='بدء التحليل']");
    private static final By MATCH_TYPE_COMBO = By.cssSelector("#analysis-match-type [role='combobox']");
    private static final By MATCH_TYPE_DROPDOWN_ICON = By.cssSelector("#analysis-match-type .p-select-dropdown");
    private static final By OFFICIAL_MATCH_COMBO = By.cssSelector("#analysis-official-match [role='combobox']");
    private static final By OFFICIAL_MATCH_DROPDOWN_ICON = By.cssSelector("#analysis-official-match .p-select-dropdown");
    private static final By MATCH_DATE_INPUT = By.cssSelector("#analysis-match-date input.p-datepicker-input");
    private static final By MATCH_DATE_INPUT_IN_FORM =
            By.cssSelector("#analysis-create-form #analysis-match-date input.p-datepicker-input");
    private static final By ANY_FORM_DATEPICKER_INPUT =
            By.cssSelector("#analysis-create-form input.p-datepicker-input");
    // The create page now renders the friendly name as a bare <input id="analysis-name"> (was #analysis-match-name).
    private static final By FRIENDLY_MATCH_NAME_ROOT = By.cssSelector("#analysis-match-name, #analysis-name");
    private static final By FRIENDLY_MATCH_NAME_INPUT =
            By.cssSelector("#analysis-match-name input, #analysis-match-name textarea, input#analysis-match-name,"
                    + " input#analysis-name");
    private static final By FRIENDLY_MATCH_DATE_ROOT = By.id("analysis-match-date");
    private static final DateTimeFormatter FRIENDLY_DATE_ENTRY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final By HOME_TEAM_FIELD = By.id("analysis-home-team");
    private static final By AWAY_TEAM_FIELD = By.id("analysis-away-team");
    private static final By LISTBOX = By.cssSelector("[role='listbox']");
    private static final By OVERLAY_OPTIONS =
            By.cssSelector(
                    ".p-select-overlay [role='option'], .p-select-list [role='option'],"
                            + " [data-pc-section='option'][role='option']");
    private static final By TOAST_SUCCESS =
            By.cssSelector(".p-toast-message-success, .p-toast-message.p-toast-message-success");
    private static final By TOAST_ERROR =
            By.cssSelector(".p-toast-message-error, .p-toast-message.p-toast-message-error");
    private static final By TOAST_MESSAGE_TEXT = By.cssSelector(".p-toast-message-text");
    private static final Pattern ANALYSIS_ID_IN_URL = Pattern.compile("(?i)/analysis/(\\d+)(?:[/?#]|$)");

    private static final int WAIT_SECONDS = 25;
    public static final int DEFAULT_FIXTURE_SCROLL_PASSES = 8;

    private final WebDriver driver;
    private final ElementActions actions;
    private final WaitManager waits;

    public CreateAnalysisPage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.waits = new WaitManager(driver);
    }

    public CreateAnalysisPage waitForCreatePopupReady() {
        new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                .until(ExpectedConditions.visibilityOfElementLocated(CREATE_FORM));
        new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                .until(ExpectedConditions.visibilityOfElementLocated(SUBMIT_BUTTON));
        // Home CTA opens a dialog; the «تحليل مباراة جديدة» CTA on «التحليلات» opens the /analysis/create page.
        new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS)).until(d -> isCreateViewTitleVisible());
        return this;
    }

    /** Match Schedule entry navigates to {@code /analysis/create?externalEventId=} (full page, not modal). */
    public CreateAnalysisPage waitForScheduleEntryFromMatchSchedule() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));
        wait.until(
                d ->
                        d.getCurrentUrl().contains("/analysis/create")
                                || isScheduleCreateViewOpen()
                                || (isCreatePopupDisplayed() && isCreateViewTitleVisible()));
        wait.until(ExpectedConditions.visibilityOfElementLocated(CREATE_FORM));
        wait.until(ExpectedConditions.visibilityOfElementLocated(SUBMIT_BUTTON));
        waits.fluentWait().until(d -> isMatchType(MATCH_TYPE_OFFICIAL));
        if (!readExternalEventIdFromCurrentUrl().isBlank()) {
            try {
                waits.fluentWait()
                        .until(
                                d ->
                                        isOfficialMatchSelected()
                                                && hasPopulatedTeamPlaceholders());
            } catch (TimeoutException autofillSlow) {
                // Schedule binding is already encoded in externalEventId; teams may hydrate async.
            }
        } else {
            waits.fluentWait()
                    .until(
                            d ->
                                    isOfficialMatchSelected()
                                            && hasPopulatedTeamPlaceholders());
        }
        return this;
    }

    public boolean isScheduleCreateViewOpen() {
        if (!driver.getCurrentUrl().contains("/analysis/create")) {
            return isDialogTitleVisible();
        }
        return !driver.findElements(CREATE_FORM).isEmpty()
                && driver.findElement(CREATE_FORM).isDisplayed();
    }

    public void closeScheduleCreateViewIfOpen() {
        if (driver.getCurrentUrl().contains("/analysis/create")) {
            driver.navigate().back();
            new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                    .until(
                            d ->
                                    d.getCurrentUrl().contains("matches")
                                            || d.getCurrentUrl().contains("match"));
            return;
        }
        closeCreateAnalysisDialogIfOpen();
    }

    public boolean isDialogTitleVisible() {
        return isCreateViewTitleVisible();
    }

    /** Home modal title or full-page create route heading («تحليل مباراة جديدة»). */
    public boolean isCreateViewTitleVisible() {
        return isAnyDisplayed(CREATE_DIALOG_TITLE) || isAnyDisplayed(CREATE_PAGE_TITLE);
    }

    private boolean isSubmitButtonVisible() {
        List<WebElement> buttons = driver.findElements(SUBMIT_BUTTON);
        return !buttons.isEmpty() && buttons.getFirst().isDisplayed();
    }

    private boolean isAnyDisplayed(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    public CreateAnalysisPage ensureMatchTypeOfficial() {
        if (isMatchType(MATCH_TYPE_OFFICIAL)) {
            return this;
        }
        return selectMatchTypeOfficialByUser();
    }

    public CreateAnalysisPage ensureMatchTypeFriendly() {
        if (isMatchType(MATCH_TYPE_FRIENDLY)) {
            return this;
        }
        return selectMatchTypeFriendlyByUser();
    }

    /** Explicit user selection (AC: no implicit type change). */
    public CreateAnalysisPage selectMatchTypeOfficialByUser() {
        openMatchTypeSelector();
        selectOptionFromOpenSelectContaining(MATCH_TYPE_OFFICIAL, "match-type");
        waits.fluentWait().until(d -> isMatchType(MATCH_TYPE_OFFICIAL));
        closeMatchTypeOverlayIfOpen();
        return this;
    }

    /**
     * Re-picking the option that is already selected («رسمية» is now the default) leaves the PrimeVue overlay open;
     * its two options would then be read as the official-match list.
     */
    private void closeMatchTypeOverlayIfOpen() {
        By list = By.cssSelector("[id^='analysis-match-type'][role='listbox'], #analysis-match-type_list");
        if (driver.findElements(list).stream().anyMatch(WebElement::isDisplayed)) {
            driver.findElement(MATCH_TYPE_COMBO).sendKeys(Keys.ESCAPE);
            waits.fluentWait().until(d -> d.findElements(list).stream().noneMatch(WebElement::isDisplayed));
        }
    }

    public CreateAnalysisPage selectMatchTypeFriendlyByUser() {
        openMatchTypeSelector();
        selectOptionFromOpenSelectContaining(MATCH_TYPE_FRIENDLY, "match-type");
        waits.fluentWait().until(d -> isMatchType(MATCH_TYPE_FRIENDLY));
        closeMatchTypeOverlayIfOpen();
        try {
            waits.fluentWait()
                    .until(
                            d ->
                                    isFriendlyMatchNameFieldVisible()
                                            || isFriendlyMatchDateFieldVisible());
        } catch (TimeoutException slowFields) {
            // Friendly metadata may hydrate async; callers assert visibility.
        }
        return this;
    }

    public boolean isMatchType(String labelFragment) {
        String value = getMatchTypeValue();
        return value != null && value.contains(labelFragment);
    }

    /** Schedule flow: match type pre-set to official and not user-changeable. */
    public boolean isMatchTypeLockedToOfficial() {
        return isMatchType(MATCH_TYPE_OFFICIAL) && isMatchTypeSelectorDisabled();
    }

    public boolean isMatchTypeSelectorDisabled() {
        if (driver.findElements(MATCH_TYPE_FIELD).isEmpty()) {
            return false;
        }
        WebElement root = driver.findElement(MATCH_TYPE_FIELD);
        String clazz = root.getAttribute("class");
        if (clazz != null
                && (clazz.contains("p-disabled") || clazz.contains("p-inputwrapper-disabled"))) {
            return true;
        }
        if (driver.findElements(MATCH_TYPE_COMBO).isEmpty()) {
            return "true".equalsIgnoreCase(root.getAttribute("aria-disabled"));
        }
        WebElement combo = driver.findElement(MATCH_TYPE_COMBO);
        return !combo.isEnabled() || "true".equalsIgnoreCase(combo.getAttribute("aria-disabled"));
    }

    /** Schedule-locked official match: selected and not replaceable via dropdown. */
    public boolean isOfficialMatchSelectorLockedForScheduleFlow() {
        if (!isOfficialMatchSelected()) {
            return false;
        }
        if (!isOfficialMatchSelectorVisible()) {
            return true;
        }
        WebElement root = visible(OFFICIAL_MATCH_FIELD);
        String clazz = root.getAttribute("class");
        if (clazz != null
                && (clazz.contains("p-disabled") || clazz.contains("p-inputwrapper-disabled"))) {
            return true;
        }
        WebElement combo = visible(OFFICIAL_MATCH_COMBO);
        return !combo.isEnabled() || "true".equalsIgnoreCase(combo.getAttribute("aria-disabled"));
    }

    /** Reads bound official match id from schedule-sourced form state (not fixture index). */
    public String readBoundOfficialMatchIdFromForm() {
        String fromUrl = readExternalEventIdFromCurrentUrl();
        if (!fromUrl.isBlank()) {
            return fromUrl;
        }
        WebElement root = visible(OFFICIAL_MATCH_FIELD);
        for (String attribute :
                List.of("data-match-id", "data-selected-match-id", "data-value", "data-id")) {
            String value = root.getAttribute(attribute);
            if (value != null && value.matches("\\d+")) {
                return value.trim();
            }
        }
        String label = getOfficialMatchSelectedLabel();
        Matcher hashId = Pattern.compile("#\\s*(\\d+)").matcher(label);
        if (hashId.find()) {
            return hashId.group(1);
        }
        Matcher arabicId = Pattern.compile("مباراة\\s*#\\s*(\\d+)").matcher(label);
        if (arabicId.find()) {
            return arabicId.group(1);
        }
        String formText = visible(CREATE_FORM).getText();
        hashId = Pattern.compile("#\\s*(\\d+)").matcher(formText);
        if (hashId.find()) {
            return hashId.group(1);
        }
        arabicId = Pattern.compile("مباراة\\s*#\\s*(\\d+)").matcher(formText);
        if (arabicId.find()) {
            return arabicId.group(1);
        }
        return "";
    }

    public String readExternalEventIdFromCurrentUrl() {
        String url = driver.getCurrentUrl();
        if (url == null || url.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("[?&]externalEventId=(\\d+)").matcher(url);
        return matcher.find() ? matcher.group(1) : "";
    }

    public boolean isMatchTypeSelectorVisible() {
        return !driver.findElements(MATCH_TYPE_FIELD).isEmpty()
                && driver.findElement(MATCH_TYPE_FIELD).isDisplayed();
    }

    /** True when neither «رسمية» nor «ودية» is the current combobox label. */
    public boolean isMatchTypeUnselected() {
        if (!isMatchTypeSelectorVisible()) {
            return false;
        }
        return !isMatchType(MATCH_TYPE_OFFICIAL) && !isMatchType(MATCH_TYPE_FRIENDLY);
    }

    public String getMatchTypeValue() {
        for (WebElement label :
                driver.findElements(By.cssSelector("#analysis-match-type .p-select-label"))) {
            if (label.isDisplayed()) {
                String text = label.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        WebElement combo = visible(MATCH_TYPE_COMBO);
        String aria = combo.getAttribute("aria-label");
        return aria == null ? "" : aria.trim();
    }

    public boolean isOfficialMatchSelectorVisible() {
        return !driver.findElements(OFFICIAL_MATCH_FIELD).isEmpty()
                && driver.findElement(OFFICIAL_MATCH_FIELD).isDisplayed();
    }

    public boolean isCreateFormMessageVisible(String textFragment) {
        if (textFragment == null || textFragment.isBlank()) {
            return false;
        }
        By locator =
                By.xpath(
                        "//*[@id='analysis-create-form']//*[contains(normalize-space(.), '"
                                + textFragment
                                + "')]");
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    /** Clicks «بدء التحليل» only when enabled (validation probes). */
    public CreateAnalysisPage clickSubmitWhenEnabled() {
        if (isSubmitEnabled()) {
            actions.click(SUBMIT_BUTTON);
        }
        return this;
    }

    // --- US-FAP-ANL-FRIENDLY (ودية metadata) ---

    public boolean isFriendlyMatchNameFieldVisible() {
        return isRootFieldVisible(FRIENDLY_MATCH_NAME_ROOT);
    }

    public boolean isFriendlyMatchDateFieldVisible() {
        return isRootFieldVisible(FRIENDLY_MATCH_DATE_ROOT)
                && !driver.findElements(MATCH_DATE_INPUT).isEmpty()
                && driver.findElement(MATCH_DATE_INPUT).isDisplayed();
    }

    public boolean isDescriptionFieldVisible() {
        return isRootFieldVisible(DESCRIPTION_FIELD);
    }

    public boolean isVideoUrlFieldVisible() {
        return isRootFieldVisible(VIDEO_URL_FIELD);
    }

    public CreateAnalysisPage enterFriendlyMatchName(String matchName) {
        WebElement input = resolveFriendlyMatchNameInput();
        scrollIntoView(input);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        if (matchName != null && !matchName.isEmpty()) {
            input.sendKeys(matchName);
        }
        return this;
    }

    public CreateAnalysisPage clearFriendlyMatchName() {
        return enterFriendlyMatchName("");
    }

    public String getFriendlyMatchNameValue() {
        return inputElementValue(resolveFriendlyMatchNameInput());
    }

    public Integer getFriendlyMatchNameMaxLengthAttribute() {
        String max = resolveFriendlyMatchNameInput().getAttribute("maxlength");
        if (max == null || max.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(max.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public CreateAnalysisPage blurFriendlyMatchNameField() {
        WebElement input = resolveFriendlyMatchNameInput();
        input.sendKeys(Keys.TAB);
        return this;
    }

    public CreateAnalysisPage selectFriendlyMatchDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date required");
        }
        WebElement input = visible(MATCH_DATE_INPUT);
        scrollIntoView(input);
        input.click();
        String formatted = FRIENDLY_DATE_ENTRY.format(date);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(formatted);
        input.sendKeys(Keys.TAB);
        if (getMatchDateValue().isBlank() && !pickMatchDateFromCalendar(date)) {
            throw new IllegalStateException("Date " + formatted + " is disabled in the match date calendar");
        }
        waits.fluentWait().until(d -> !getMatchDateValue().isBlank());
        return this;
    }

    /** True when {@code date} can be chosen in the match-date calendar (the field does not accept typed dates). */
    public boolean isMatchDateSelectableInCalendar(LocalDate date) {
        return PrimeVueDatePicker.isSelectable(driver, visible(MATCH_DATE_INPUT), date);
    }

    /** The live date picker ignores typed input; returns false when the day is disabled (e.g. future dates). */
    private boolean pickMatchDateFromCalendar(LocalDate date) {
        return PrimeVueDatePicker.pick(driver, visible(MATCH_DATE_INPUT), date);
    }

    public CreateAnalysisPage clearFriendlyMatchDate() {
        WebElement input = visible(MATCH_DATE_INPUT);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(Keys.TAB);
        return this;
    }

    public CreateAnalysisPage probeFriendlyValidationBySubmitAttempt() {
        clickSubmitWhenEnabled();
        return this;
    }

    public CreateAnalysisPage selectFirstEligibleOfficialFixture(String preferredLabelFragment) {
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        boolean selected =
                preferredLabelFragment != null
                        && !preferredLabelFragment.isBlank()
                        && clickOfficialFixtureOptionContaining(preferredLabelFragment);
        if (!selected) {
            ensureOfficialMatchOverlayOpen();
            clickFirstEligibleOfficialFixtureOption();
        }
        if (!isOfficialMatchSelected()) {
            throw new IllegalStateException(
                    "Official fixture not selected after dropdown interaction. " + readinessDiagnostics());
        }
        waitForOfficialFixtureAutofill();
        return this;
    }

    public CreateAnalysisPage selectOfficialMatchPreferringValidDate(String preferredLabelFragment) {
        selectFirstEligibleOfficialFixture(preferredLabelFragment);
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (isOfficialMatchDateDisplayValid()) {
                return this;
            }
            if (attempt >= 5) {
                break;
            }
            String current = getOfficialMatchSelectedLabel();
            selectNextEligibleOfficialMatch(current);
        }
        throw new IllegalStateException(
                "No official fixture with valid calendar date; lastDate="
                        + getMatchDateValue()
                        + "; fixture="
                        + getOfficialMatchSelectedLabel());
    }

    /** All fixture rows currently offered in the official-match dropdown (opens overlay if needed). */
    public List<String> listEligibleOfficialFixtures() {
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        return Collections.unmodifiableList(new ArrayList<>(eligibleOfficialFixtureLabels()));
    }

    public CreateAnalysisPage selectOfficialFixtureByExactLabel(String exactLabel) {
        if (exactLabel == null || exactLabel.isBlank()) {
            throw new IllegalArgumentException("Fixture label must not be blank");
        }
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        if (!clickOfficialFixtureOptionByExactLabel(exactLabel.trim())) {
            throw new IllegalStateException(
                    "Could not select official fixture «" + exactLabel + "». " + readinessDiagnostics());
        }
        waitForOfficialFixtureAutofill();
        return this;
    }

    public CreateAnalysisPage selectNextEligibleOfficialMatch(String currentLabel) {
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        List<String> labels = eligibleOfficialFixtureLabels();
        if (labels.isEmpty()) {
            throw new IllegalStateException("No eligible official fixtures in dropdown");
        }
        int start = 0;
        if (currentLabel != null && !currentLabel.isBlank()) {
            int idx = labels.indexOf(currentLabel.trim());
            start = idx >= 0 ? (idx + 1) % labels.size() : 0;
        }
        for (int i = 0; i < labels.size(); i++) {
            String candidate = labels.get((start + i) % labels.size());
            if (currentLabel != null && candidate.equals(currentLabel.trim()) && labels.size() == 1) {
                break;
            }
            if (currentLabel != null && candidate.equals(currentLabel.trim())) {
                continue;
            }
            ensureOfficialMatchOverlayOpen();
            if (clickOfficialFixtureOptionByExactLabel(candidate)) {
                waitForOfficialFixtureAutofill();
                return this;
            }
        }
        throw new IllegalStateException("Could not select another official fixture after: " + currentLabel);
    }

    public String getOfficialMatchSelectedLabel() {
        WebElement combo = visible(OFFICIAL_MATCH_COMBO);
        String ariaLabel = combo.getAttribute("aria-label");
        if (ariaLabel != null && !ariaLabel.isBlank() && !isOfficialMatchPlaceholderLabel(ariaLabel.trim())) {
            return ariaLabel.trim();
        }
        for (WebElement label : driver.findElements(By.cssSelector("#analysis-official-match .p-select-label"))) {
            if (!label.isDisplayed()) {
                continue;
            }
            String text = label.getText().trim();
            if (!text.isEmpty() && !isOfficialMatchPlaceholderLabel(text)) {
                return text;
            }
        }
        String comboText = combo.getText().trim();
        return comboText.isEmpty() ? OFFICIAL_MATCH_PLACEHOLDER : comboText;
    }

    public boolean isOfficialMatchSelected() {
        return !isOfficialMatchPlaceholderLabel(getOfficialMatchSelectedLabel());
    }

    public static boolean isOfficialMatchPlaceholderLabel(String label) {
        if (label == null || label.isBlank()) {
            return true;
        }
        String trimmed = label.trim();
        if (OFFICIAL_MATCH_UNSELECTED_LABELS.contains(trimmed)) {
            return true;
        }
        return trimmed.startsWith("اختر") && !OfficialFixtureLabelRules.isEligibleFixtureRow(trimmed);
    }

    public String getMatchDateValue() {
        if (!isCreatePopupDisplayed() && !driver.getCurrentUrl().contains("/analysis/create")) {
            throw new IllegalStateException(
                    "Create analysis form is not open; cannot read match date. "
                            + readinessDiagnosticsWithoutDateRead());
        }
        return readMatchDateInputValue();
    }

    public String getHomeTeamValue() {
        return fieldText(HOME_TEAM_FIELD);
    }

    public String getAwayTeamValue() {
        return fieldText(AWAY_TEAM_FIELD);
    }

    public String getHomeTeamPlaceholder() {
        return observeHomeTeamField().resolvedDisplayText();
    }

    public String getAwayTeamPlaceholder() {
        return observeAwayTeamField().resolvedDisplayText();
    }

    public TeamFieldObservation observeHomeTeamField() {
        return observeTeamField(HOME_TEAM_FIELD, "analysis-home-team");
    }

    public TeamFieldObservation observeAwayTeamField() {
        return observeTeamField(AWAY_TEAM_FIELD, "analysis-away-team");
    }

    public boolean hasPopulatedTeamPlaceholders() {
        return !getHomeTeamPlaceholder().isBlank() && !getAwayTeamPlaceholder().isBlank();
    }

    public boolean hasPopulatedTeamFields() {
        return hasPopulatedTeamPlaceholders() || (!getHomeTeamValue().isBlank() && !getAwayTeamValue().isBlank());
    }

    public boolean isOfficialMatchDateDisplayValid() {
        return MatchDateDisplayRules.looksLikeCalendarDateDisplay(getMatchDateValue());
    }

    public boolean isOfficialFixtureFieldsPopulated() {
        return isMatchType(MATCH_TYPE_OFFICIAL)
                && isOfficialMatchSelected()
                && !getMatchDateValue().isBlank()
                && hasPopulatedTeamPlaceholders();
    }

    public boolean isMatchDateReadOnlyOrDisabled() {
        WebElement input = findMatchDateInputInOpenForm();
        return !input.isEnabled() || "true".equalsIgnoreCase(input.getAttribute("readonly"));
    }

    /** @deprecated Prefer {@link #verifyTeamFieldsNonEditable()} — readonly DOM flag alone is insufficient. */
    public boolean areTeamFieldsReadOnly() {
        return verifyTeamFieldsNonEditable().bothNonEditable();
    }

    public TeamFieldsEditabilityVerdict verifyTeamFieldsNonEditable() {
        TeamFieldEditabilityObservation home = observeTeamFieldEditability("analysis-home-team");
        TeamFieldEditabilityObservation away = observeTeamFieldEditability("analysis-away-team");
        boolean homeBlocked = isTeamFieldUserEditBlocked(home);
        boolean awayBlocked = isTeamFieldUserEditBlocked(away);
        return new TeamFieldsEditabilityVerdict(homeBlocked, awayBlocked, home, away);
    }

    public TeamFieldEditabilityObservation observeHomeTeamEditability() {
        return observeTeamFieldEditability("analysis-home-team");
    }

    public TeamFieldEditabilityObservation observeAwayTeamEditability() {
        return observeTeamFieldEditability("analysis-away-team");
    }

    public CreateAnalysisPage enterOptionalDescription(String description) {
        if (description != null && !description.trim().isEmpty()) {
            actions.type(DESCRIPTION_FIELD, description.trim());
        }
        return this;
    }

    public CreateAnalysisPage enterVideoUrl(String videoUrl) {
        actions.type(VIDEO_URL_FIELD, videoUrl);
        return this;
    }

    public boolean isSubmitEnabled() {
        WebElement button = visible(SUBMIT_BUTTON);
        String disabled = button.getAttribute("data-p-disabled");
        return button.isEnabled() && !"true".equals(disabled);
    }

    /** Avoid long visibility waits when rotating through many fixtures. */
    public boolean isSubmitEnabledQuick() {
        List<WebElement> buttons = driver.findElements(SUBMIT_BUTTON);
        if (buttons.isEmpty() || !buttons.getFirst().isDisplayed()) {
            return false;
        }
        WebElement button = buttons.getFirst();
        String disabled = button.getAttribute("data-p-disabled");
        return button.isEnabled() && !"true".equals(disabled);
    }

    public CreateAnalysisPage submitOnce() {
        if (!isSubmitEnabled()) {
            throw new IllegalStateException("Submit button not enabled before click");
        }
        actions.click(SUBMIT_BUTTON);
        return this;
    }

    public AnalysisPostSubmitOutcome waitForPostSubmitOutcome() {
        return waitForPostSubmitOutcome(AnalysisCreateGate.submitWaitSeconds());
    }

    public AnalysisPostSubmitOutcome waitForPostSubmitOutcome(int timeoutSeconds) {
        try {
            return new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(100))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(
                            d -> {
                                if (isCreationSuccessState()) {
                                    return AnalysisPostSubmitOutcome.SUCCESS;
                                }
                                if (isDuplicateFixtureAnalysisError()) {
                                    return AnalysisPostSubmitOutcome.DUPLICATE_FIXTURE;
                                }
                                return null;
                            });
        } catch (TimeoutException exception) {
            if (isCreationSuccessState()) {
                return AnalysisPostSubmitOutcome.SUCCESS;
            }
            if (isDuplicateFixtureAnalysisError()) {
                return AnalysisPostSubmitOutcome.DUPLICATE_FIXTURE;
            }
            if (isCreatePopupDisplayed()) {
                return AnalysisPostSubmitOutcome.STILL_ON_CREATE;
            }
            return AnalysisPostSubmitOutcome.TIMEOUT;
        }
    }

    public boolean isCreatePopupDisplayed() {
        List<WebElement> forms = driver.findElements(CREATE_FORM);
        return !forms.isEmpty() && forms.getFirst().isDisplayed();
    }

    public boolean isCreationSuccessState() {
        if (pageContainsSuccessDialogMessage()) {
            return true;
        }
        String url = driver.getCurrentUrl();
        if (url.matches("(?i).*/analysis/\\d+([/?].*)?")) {
            return true;
        }
        return isSuccessToastVisible();
    }

    public boolean pageContainsSuccessDialogMessage() {
        return driver.getPageSource().contains("تم إرسال المباراة للتحليل");
    }

    public boolean isDuplicateFixtureAnalysisError() {
        return DuplicateFixtureToastRules.isDuplicateFixtureMessage(
                getVisibleErrorToastBody());
    }

    public String getVisibleErrorToastBody() {
        for (WebElement toast : driver.findElements(TOAST_ERROR)) {
            if (!isDisplayedSafely(toast)) {
                continue;
            }
            String text = toast.getText().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        for (WebElement line : driver.findElements(TOAST_MESSAGE_TEXT)) {
            if (!isDisplayedSafely(line)) {
                continue;
            }
            String text = line.getText().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    public boolean isSuccessToastVisible() {
        return driver.findElements(TOAST_SUCCESS).stream().anyMatch(this::isDisplayedSafely);
    }

    public static Long extractAnalysisIdFromCurrentUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = ANALYSIS_ID_IN_URL.matcher(url);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    /** Visible text of the create form (labels, selected match, helper/validation texts) for read-only assertions. */
    public String readVisibleFormText() {
        List<WebElement> forms = driver.findElements(CREATE_FORM);
        if (!forms.isEmpty() && isDisplayedSafely(forms.getFirst())) {
            return forms.getFirst().getText();
        }
        return driver.findElement(By.tagName("body")).getText();
    }

    public String getDescriptionValue() {
        return readControlValue(resolveDescriptionControl());
    }

    public String getVideoUrlValue() {
        return readControlValue(resolveVideoUrlControl());
    }

    public CreateAnalysisPage clearDescription() {
        WebElement control = resolveDescriptionControl();
        scrollIntoView(control);
        control.click();
        control.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        control.sendKeys(Keys.DELETE);
        return this;
    }

    public CreateAnalysisPage clearVideoUrl() {
        WebElement control = resolveVideoUrlControl();
        scrollIntoView(control);
        control.click();
        control.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        control.sendKeys(Keys.DELETE);
        control.sendKeys(Keys.TAB);
        return this;
    }

    public CreateAnalysisFieldRequirementEvidence observeDescriptionFieldRequirement() {
        return observeFieldRequirement(DESCRIPTION_FIELD, "analysis-description");
    }

    public CreateAnalysisFieldRequirementEvidence observeVideoUrlFieldRequirement() {
        return observeFieldRequirement(VIDEO_URL_FIELD, "analysis-link");
    }

    public boolean hasVisibleClientValidationNearDescription() {
        return hasVisibleClientValidationNear(DESCRIPTION_FIELD);
    }

    public boolean hasVisibleClientValidationNearVideoUrl() {
        return hasVisibleClientValidationNear(VIDEO_URL_FIELD);
    }

    public String readVisibleFormValidationMessages() {
        StringBuilder messages = new StringBuilder();
        for (WebElement alert :
                driver.findElements(
                        By.cssSelector(
                                "#analysis-create-form .p-error, #analysis-create-form [role='alert']"))) {
            if (!isDisplayedSafely(alert)) {
                continue;
            }
            String text = alert.getText();
            if (text != null && !text.isBlank()) {
                if (!messages.isEmpty()) {
                    messages.append(" | ");
                }
                messages.append(text.trim());
            }
        }
        return messages.toString();
    }

    private CreateAnalysisFieldRequirementEvidence observeFieldRequirement(By rootBy, String rootId) {
        WebElement root = visible(rootBy);
        WebElement control = resolveControlWithinRoot(root);
        String labelText = readFieldLabelText(root);
        String labelSnippet = readFieldLabelDomSnippet(root);
        boolean marker =
                labelShowsRequiredMarker(labelText, labelSnippet, root)
                        || rootMarkedAsRequired(root);
        return new CreateAnalysisFieldRequirementEvidence(
                rootId,
                labelText,
                marker,
                rootMarkedAsRequired(root),
                isTruthy(control.getAttribute("aria-required")),
                isTruthy(control.getAttribute("required")),
                control.getTagName(),
                readValidationTextNearRoot(root),
                labelSnippet);
    }

    private WebElement resolveDescriptionControl() {
        return resolveControlWithinRoot(visible(DESCRIPTION_FIELD));
    }

    private WebElement resolveVideoUrlControl() {
        return resolveControlWithinRoot(visible(VIDEO_URL_FIELD));
    }

    private WebElement resolveControlWithinRoot(WebElement root) {
        List<WebElement> inputs = resolveInputs(root);
        if (!inputs.isEmpty()) {
            return inputs.getFirst();
        }
        return root;
    }

    private static String readControlValue(WebElement control) {
        String value = control.getAttribute("value");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return control.getText() == null ? "" : control.getText().trim();
    }

    private boolean hasVisibleClientValidationNear(By rootBy) {
        WebElement root = driver.findElement(rootBy);
        for (WebElement node :
                root.findElements(By.cssSelector(".p-error, .p-invalid, [role='alert']"))) {
            if (isDisplayedSafely(node)) {
                String text = node.getText();
                if (text != null && !text.isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String readValidationTextNearRoot(WebElement root) {
        for (WebElement node :
                root.findElements(By.cssSelector(".p-error, .p-invalid, [role='alert']"))) {
            if (isDisplayedSafely(node)) {
                String text = node.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return "";
    }

    private String readFieldLabelText(WebElement root) {
        for (WebElement label : root.findElements(By.cssSelector("label"))) {
            if (label.isDisplayed()) {
                return label.getText() == null ? "" : label.getText().trim();
            }
        }
        for (WebElement label :
                root.findElements(
                        By.xpath(
                                "./ancestor-or-self::*[contains(@class,'field')][1]//label"
                                        + "|./preceding::label[1]"))) {
            if (label.isDisplayed()) {
                String text = label.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return "";
    }

    private String readFieldLabelDomSnippet(WebElement root) {
        for (WebElement label : root.findElements(By.cssSelector("label"))) {
            if (label.isDisplayed()) {
                return label.getAttribute("outerHTML");
            }
        }
        WebElement host = root.findElement(By.xpath("./ancestor-or-self::*[contains(@class,'field')][1]"));
        List<WebElement> labels = host.findElements(By.cssSelector("label"));
        if (!labels.isEmpty()) {
            return labels.getFirst().getAttribute("outerHTML");
        }
        return root.getAttribute("outerHTML");
    }

    private static boolean labelShowsRequiredMarker(String labelText, String labelHtml, WebElement root) {
        if (labelText != null && labelText.contains("*")) {
            return true;
        }
        if (labelHtml != null
                && (labelHtml.contains("*")
                        || labelHtml.contains("p-required")
                        || labelHtml.contains("required-marker"))) {
            return true;
        }
        String rootClass = root.getAttribute("class");
        return rootClass != null && rootClass.contains("p-required");
    }

    private static boolean rootMarkedAsRequired(WebElement root) {
        if (isTruthy(root.getAttribute("aria-required"))) {
            return true;
        }
        return !root.findElements(By.cssSelector(".p-required, .required, [data-required='true']")).isEmpty();
    }

    private static boolean isTruthy(String attribute) {
        return attribute != null
                && ("true".equalsIgnoreCase(attribute.trim())
                        || "required".equalsIgnoreCase(attribute.trim()));
    }

    /** Read-only: opens official dropdown and collects visible rows (optional scroll passes). */
    public List<OfficialFixtureOptionSnapshot> collectOfficialFixtureOptionsReadOnly(int maxScrollPasses) {
        return discoverOfficialFixtureOptionsOrdered(maxScrollPasses);
    }

    public CreateAnalysisPage openOfficialFixtureDropdown() {
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        return this;
    }

    public List<OfficialFixtureOptionSnapshot> discoverOfficialFixtureOptionsOrdered(int maxScrollPasses) {
        return discoverOfficialFixtureOptionsWithProgress(maxScrollPasses).orderedSnapshots();
    }

    public FixtureDiscoveryResult discoverOfficialFixtureOptionsWithProgress(int maxScrollPasses) {
        ensureMatchTypeOfficial();
        ensureOfficialMatchOverlayOpen();
        List<OfficialFixtureOptionSnapshot> ordered = new ArrayList<>();
        int passesExecuted = 0;
        int idlePasses = 0;
        int limit = Math.max(0, maxScrollPasses);
        for (int pass = 0; pass <= limit; pass++) {
            passesExecuted = pass;
            int fixtureRowsBefore = countFixtureRows(ordered);
            ordered = PrimeVueIndexedSelect.mergeOrderedSnapshots(ordered, snapshotVisibleOptions());
            int fixtureRowsAfter = countFixtureRows(ordered);
            if (pass < limit) {
                long scrollTopBefore = readOfficialMatchListScrollTop();
                scrollOfficialMatchListDown();
                long scrollTopAfter = readOfficialMatchListScrollTop();
                if (fixtureRowsAfter == fixtureRowsBefore && scrollTopAfter <= scrollTopBefore) {
                    idlePasses++;
                } else if (fixtureRowsAfter == fixtureRowsBefore) {
                    idlePasses++;
                } else {
                    idlePasses = 0;
                }
                if (idlePasses >= 2) {
                    break;
                }
            }
        }
        boolean endLikely = idlePasses >= 2;
        boolean complete = endLikely && passesExecuted >= 1;
        return new FixtureDiscoveryResult(
                List.copyOf(ordered), passesExecuted, idlePasses, endLikely, complete);
    }

    private static int countFixtureRows(List<OfficialFixtureOptionSnapshot> ordered) {
        int count = 0;
        for (OfficialFixtureOptionSnapshot row : ordered) {
            if ("FIXTURE".equals(row.rowKind())) {
                count++;
            }
        }
        return count;
    }

    public List<IndexedFixtureEntry> getOfficialFixtureOptions() {
        return OfficialFixtureIndexCatalog.fromOrderedSnapshots(
                discoverOfficialFixtureOptionsOrdered(DEFAULT_FIXTURE_SCROLL_PASSES));
    }

    public int getOfficialFixtureOptionCount() {
        return getOfficialFixtureOptions().size();
    }

    public IndexedFixtureEntry getOfficialFixtureOptionByIndex(int fixtureIndex) {
        return OfficialFixtureIndexCatalog.requireEntry(getOfficialFixtureOptions(), fixtureIndex);
    }

    public boolean isOfficialFixtureOptionEnabled(int fixtureIndex) {
        return getOfficialFixtureOptionByIndex(fixtureIndex).enabled();
    }

    public OfficialFixtureOptionMetadata getOfficialFixtureOptionMetadata(int fixtureIndex) {
        IndexedFixtureEntry entry = getOfficialFixtureOptionByIndex(fixtureIndex);
        OfficialFixtureOptionSnapshot snap = entry.snapshot();
        return new OfficialFixtureOptionMetadata(
                entry.fixtureIndex(),
                snap.displayLabel(),
                snap.enabled(),
                snap.homeTeam(),
                snap.awayTeam(),
                snap.matchId(),
                snap.externalEventId());
    }

    public String getSelectedOfficialFixture() {
        return getOfficialMatchSelectedLabel();
    }

    public CreateAnalysisPage selectOfficialFixtureByIndex(int fixtureIndex) {
        IndexedFixtureEntry entry = getOfficialFixtureOptionByIndex(fixtureIndex);
        if (!entry.enabled()) {
            throw new IllegalStateException(
                    "Fixture index "
                            + fixtureIndex
                            + " is disabled and cannot be selected: "
                            + entry.displayLabel());
        }
        ensureMatchTypeOfficial();
        if (!scrollAndClickFixtureByLabel(entry.displayLabel())) {
            throw new IllegalStateException(
                    "Could not select official fixture at index "
                            + fixtureIndex
                            + " («"
                            + entry.displayLabel()
                            + "»). "
                            + readinessDiagnostics());
        }
        waitForOfficialFixtureAutofill();
        return this;
    }

    public record OfficialFixtureOptionMetadata(
            int fixtureIndex,
            String displayLabel,
            boolean enabled,
            String homeTeam,
            String awayTeam,
            String matchId,
            String externalEventId) {}

    /** Closes PrimeVue overlay without submitting the form. */
    public CreateAnalysisPage dismissOfficialMatchOverlay() {
        if (!isOfficialMatchOverlayOpen()) {
            return this;
        }
        try {
            visible(OFFICIAL_MATCH_COMBO).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
            actions.click(OFFICIAL_MATCH_COMBO);
        }
        waits.fluentWait().until(d -> !isOfficialMatchOverlayOpen());
        return this;
    }

    /** Closes create-analysis dialog using supported header close control. */
    public void closeCreateAnalysisDialogIfOpen() {
        if (!isCreatePopupDisplayed()) {
            return;
        }
        for (int attempt = 0; attempt < 3 && isCreatePopupDisplayed(); attempt++) {
            dismissOfficialMatchOverlay();
            PortalOverlaySupport.dismissVisibleToasts(driver);
            List<By> closeCandidates =
                    List.of(
                            By.cssSelector("[role='dialog'] button[aria-label='Close']"),
                            By.cssSelector(".p-dialog-header-close"),
                            By.cssSelector("[data-pc-section='closebutton']"),
                            By.xpath(
                                    "//div[@role='dialog']//button[contains(@class,'p-dialog-header-close')]"),
                            By.xpath(
                                    "//div[@role='dialog']//button[contains(@aria-label,'إغلاق')]"));
            for (By locator : closeCandidates) {
                for (WebElement button : driver.findElements(locator)) {
                    if (!button.isDisplayed() || !button.isEnabled()) {
                        continue;
                    }
                    try {
                        button.click();
                    } catch (ElementClickInterceptedException intercepted) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                    }
                    if (waitUntilCreatePopupClosed(Duration.ofSeconds(12))) {
                        return;
                    }
                }
            }
            sendEscapeToDismissCreateUi();
            if (waitUntilCreatePopupClosed(Duration.ofSeconds(8))) {
                return;
            }
        }
        if (isCreatePopupDisplayed() && driver.getCurrentUrl().contains("/analysis/create")) {
            driver.navigate().back();
            waitUntilCreatePopupClosed(Duration.ofSeconds(15));
            return;
        }
        if (isCreatePopupDisplayed()) {
            driver.navigate().to(PortalConfig.homeUrl());
            waitUntilCreatePopupClosed(Duration.ofSeconds(15));
        }
    }

    private void sendEscapeToDismissCreateUi() {
        try {
            visible(OFFICIAL_MATCH_COMBO).sendKeys(Keys.ESCAPE);
        } catch (RuntimeException ignored) {
            // combo may be hidden while dialog closes
        }
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (RuntimeException ignored) {
            // no focused element
        }
        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
    }

    private boolean waitUntilCreatePopupClosed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(d -> !isCreatePopupDisplayed());
            return true;
        } catch (TimeoutException stillOpen) {
            return false;
        }
    }

    /**
     * Selects an enabled fixture for read-only autofill inspection. Never clicks Submit.
     */
    public CreateAnalysisPage selectEnabledFixtureForReadOnlyInspection(String exactLabel) {
        OfficialFixtureOptionSnapshot row =
                collectOfficialFixtureOptionsReadOnly(2).stream()
                        .filter(o -> "FIXTURE".equals(o.rowKind()))
                        .filter(o -> exactLabel.equals(o.displayLabel()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Fixture not found in dropdown scan: " + exactLabel));
        if (!row.enabled()) {
            throw new IllegalStateException("Fixture is disabled; cannot inspect selection: " + exactLabel);
        }
        selectOfficialFixtureByExactLabel(exactLabel);
        return this;
    }

    // --- private interaction helpers ---

    private boolean scrollAndClickFixtureByLabel(String exactLabel) {
        ensureOfficialMatchOverlayOpen();
        for (int pass = 0; pass <= DEFAULT_FIXTURE_SCROLL_PASSES; pass++) {
            if (clickOfficialFixtureOptionByExactLabel(exactLabel)) {
                return true;
            }
            scrollOfficialMatchListDown();
        }
        ensureOfficialMatchOverlayOpen();
        return clickOfficialFixtureOptionByExactLabel(exactLabel);
    }

    private List<OfficialFixtureOptionSnapshot> snapshotVisibleOptions() {
        List<OfficialFixtureOptionSnapshot> rows = new ArrayList<>();
        for (WebElement option : freshVisibleOptions()) {
            String text = readOptionText(option);
            if (text.isBlank()) {
                continue;
            }
            if (text.startsWith("الجولة")) {
                rows.add(OfficialFixtureOptionSnapshot.roundHeader(text));
                continue;
            }
            if (!OfficialFixtureLabelRules.isEligibleFixtureRow(text)) {
                continue;
            }
            boolean enabled = isOptionEnabled(option);
            rows.add(
                    OfficialFixtureOptionSnapshot.fixtureRow(
                            text, enabled, readOptionData(option, "match"), readOptionData(option, "event")));
        }
        return rows;
    }

    private static String readOptionData(WebElement option, String kind) {
        String[] keys =
                "match".equals(kind)
                        ? new String[] {"data-match-id", "data-matchid", "data-id"}
                        : new String[] {"data-external-event-id", "data-event-id", "data-externaleventid"};
        for (String key : keys) {
            String value = option.getAttribute(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isOptionEnabled(WebElement option) {
        if (!option.isDisplayed()) {
            return false;
        }
        String ariaDisabled = option.getAttribute("aria-disabled");
        if ("true".equalsIgnoreCase(ariaDisabled)) {
            return false;
        }
        String dataDisabled = option.getAttribute("data-p-disabled");
        if ("true".equalsIgnoreCase(dataDisabled)) {
            return false;
        }
        String clazz = option.getAttribute("class");
        if (clazz != null && clazz.contains("p-disabled")) {
            return false;
        }
        return option.isEnabled();
    }

    private void scrollOfficialMatchListDown() {
        List<By> containers =
                List.of(
                        By.cssSelector(".p-select-list-container"),
                        By.cssSelector(".p-select-overlay .p-select-list"),
                        By.cssSelector(".p-select-overlay"));
        for (By locator : containers) {
            for (WebElement container : driver.findElements(locator)) {
                if (!container.isDisplayed()) {
                    continue;
                }
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollTop = arguments[0].scrollTop + 280;", container);
                return;
            }
        }
    }

    private boolean isOfficialMatchOverlayOpen() {
        return !freshVisibleOptions().isEmpty();
    }

    /** Opens the official-match list only when PrimeVue overlay is not already visible (toggle-safe). */
    private void ensureOfficialMatchOverlayOpen() {
        if (isOfficialMatchOverlayOpen()) {
            return;
        }
        openOfficialMatchSelector();
        waitForSelectableOfficialFixtureOptions();
    }

    private void openOfficialMatchSelector() {
        WebElement combo = visible(OFFICIAL_MATCH_COMBO);
        scrollIntoView(combo);
        actions.click(OFFICIAL_MATCH_COMBO);
        try {
            waitForOpenSelectOptions("official-match");
        } catch (IllegalStateException exception) {
            if (!driver.findElements(OFFICIAL_MATCH_DROPDOWN_ICON).isEmpty()) {
                actions.click(OFFICIAL_MATCH_DROPDOWN_ICON);
                waitForOpenSelectOptions("official-match");
            } else {
                throw exception;
            }
        }
    }

    private void openMatchTypeSelector() {
        WebElement combo = visible(MATCH_TYPE_COMBO);
        scrollIntoView(combo);
        actions.click(MATCH_TYPE_COMBO);
        waitForOpenSelectOptions("match-type");
    }

    private void waitForOfficialFixtureAutofill() {
        try {
            waits.fluentWait()
                    .until(
                            d ->
                                    isOfficialMatchSelected()
                                            && MatchDateDisplayRules.looksLikeCalendarDateDisplay(
                                                    readMatchDateInputValueOrEmpty()));
            waits.fluentWait().until(d -> hasPopulatedTeamPlaceholders());
        } catch (TimeoutException exception) {
            throw new IllegalStateException(readinessDiagnostics(), exception);
        }
    }

    private void waitForSelectableOfficialFixtureOptions() {
        try {
            waits.fluentWait().until(d -> countEligibleOfficialFixtureOptions() > 0);
        } catch (TimeoutException exception) {
            List<String> visible = freshVisibleOptions().stream().map(CreateAnalysisPage::readOptionText).toList();
            throw new IllegalStateException(
                    "No eligible official fixtures appeared within wait; visibleOptions=" + visible,
                    exception);
        }
    }

    private int countEligibleOfficialFixtureOptions() {
        int count = 0;
        for (WebElement option : freshVisibleOptions()) {
            if (OfficialFixtureLabelRules.isEligibleFixtureRow(readOptionText(option))) {
                count++;
            }
        }
        return count;
    }

    private List<String> eligibleOfficialFixtureLabels() {
        List<String> labels = new ArrayList<>();
        for (WebElement option : freshVisibleOptions()) {
            String text = readOptionText(option);
            if (OfficialFixtureLabelRules.isEligibleFixtureRow(text)) {
                labels.add(text);
            }
        }
        return labels;
    }

    private void clickFirstEligibleOfficialFixtureOption() {
        Boolean clicked =
                waits.fluentWait()
                        .until(
                                d -> {
                                    if (tryClickFirstEligibleOfficialFixtureOptionOnce()) {
                                        return true;
                                    }
                                    return null;
                                });
        if (clicked == null || !clicked) {
            throw new IllegalStateException("No eligible official fixture options in dropdown");
        }
    }

    private boolean tryClickFirstEligibleOfficialFixtureOptionOnce() {
        if (!isOfficialMatchOverlayOpen()) {
            openOfficialMatchSelector();
            if (freshVisibleOptions().isEmpty()) {
                return false;
            }
        }
        for (WebElement option : freshVisibleOptions()) {
            String text = readOptionText(option);
            if (!OfficialFixtureLabelRules.isEligibleFixtureRow(text)) {
                continue;
            }
            clickOfficialFixtureOption(option);
            return isOfficialMatchSelected();
        }
        return false;
    }

    private boolean clickOfficialFixtureOptionContaining(String labelFragment) {
        for (WebElement option : freshVisibleOptions()) {
            String text = readOptionText(option);
            if (text.contains(labelFragment)) {
                clickOfficialFixtureOption(option);
                return isOfficialMatchSelected();
            }
        }
        return false;
    }

    private boolean clickOfficialFixtureOptionByExactLabel(String label) {
        for (WebElement option : freshVisibleOptions()) {
            if (label.equals(readOptionText(option))) {
                clickOfficialFixtureOption(option);
                return isOfficialMatchSelected() && label.equals(getOfficialMatchSelectedLabel());
            }
        }
        return false;
    }

    private void clickOfficialFixtureOption(WebElement option) {
        scrollIntoView(option);
        try {
            if (!isOptionEnabled(option)) {
                throw new IllegalStateException(
                        "Refusing to click disabled fixture option: " + readOptionText(option));
            }
            option.click();
            HumanPacingManager.pauseForAction();
        } catch (StaleElementReferenceException stale) {
            retryClickOfficialFixtureByText(readOptionText(option));
        } catch (IllegalStateException disabled) {
            throw disabled;
        } catch (Exception exception) {
            if (!isOptionEnabled(option)) {
                throw new IllegalStateException(
                        "Refusing JS click on disabled fixture option: " + readOptionText(option));
            }
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
            HumanPacingManager.pauseForAction();
        }
    }

    private void retryClickOfficialFixtureByText(String label) {
        if (label == null || label.isBlank()) {
            return;
        }
        for (WebElement option : freshVisibleOptions()) {
            if (label.equals(readOptionText(option))) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
                return;
            }
        }
    }

    private void selectOptionFromOpenSelectContaining(String labelFragment, String controlLabel) {
        waitForOpenSelectOptions(controlLabel);
        if (tryClickOptionContaining(labelFragment)) {
            return;
        }
        if ("match-type".equals(controlLabel) && !driver.findElements(MATCH_TYPE_DROPDOWN_ICON).isEmpty()) {
            actions.click(MATCH_TYPE_DROPDOWN_ICON);
            waitForOpenSelectOptions(controlLabel);
            if (tryClickOptionContaining(labelFragment)) {
                return;
            }
        }
        List<String> visible = freshVisibleOptions().stream().map(CreateAnalysisPage::readOptionText).toList();
        throw new IllegalStateException(
                "No option containing '" + labelFragment + "' for " + controlLabel + "; visible=" + visible);
    }

    private boolean tryClickOptionContaining(String labelFragment) {
        for (WebElement option : freshVisibleOptions()) {
            if (readOptionText(option).contains(labelFragment)) {
                scrollIntoView(option);
                try {
                    option.click();
                } catch (StaleElementReferenceException exception) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
                }
                return true;
            }
        }
        return false;
    }

    private void waitForOpenSelectOptions(String controlLabel) {
        try {
            waits.fluentWait().until(d -> !freshVisibleOptions().isEmpty());
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "Select options did not appear for " + controlLabel + "; url=" + driver.getCurrentUrl(),
                    exception);
        }
    }

    private List<WebElement> freshVisibleOptions() {
        List<WebElement> visible = new ArrayList<>();
        for (WebElement option : driver.findElements(OVERLAY_OPTIONS)) {
            try {
                if (option.isDisplayed()) {
                    visible.add(option);
                }
            } catch (StaleElementReferenceException ignored) {
                // re-query on next poll
            }
        }
        if (!visible.isEmpty()) {
            return visible;
        }
        for (WebElement listbox : driver.findElements(LISTBOX)) {
            try {
                if (!listbox.isDisplayed()) {
                    continue;
                }
                for (WebElement option : listbox.findElements(By.cssSelector("[role='option']"))) {
                    if (option.isDisplayed()) {
                        visible.add(option);
                    }
                }
            } catch (StaleElementReferenceException ignored) {
                // continue
            }
        }
        return visible;
    }

    private static String readOptionText(WebElement option) {
        try {
            return option.getText().trim();
        } catch (StaleElementReferenceException exception) {
            return "";
        }
    }

    private String readinessDiagnostics() {
        return readinessDiagnosticsWithoutDateRead()
                + "; date="
                + readMatchDateInputValueOrEmpty();
    }

    private String readinessDiagnosticsWithoutDateRead() {
        return "matchType="
                + getMatchTypeValue()
                + "; official="
                + getOfficialMatchSelectedLabel()
                + "; home="
                + getHomeTeamPlaceholder()
                + "; away="
                + getAwayTeamPlaceholder();
    }

    private String readMatchDateInputValueOrEmpty() {
        try {
            return readMatchDateInputValue();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private String readMatchDateInputValue() {
        WebElement input = findMatchDateInputInOpenForm();
        return inputElementValue(input);
    }

    /** Official autofill may bind a readonly date input that is present but not Selenium-visible. */
    private WebElement findMatchDateInputInOpenForm() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                    .until(
                            d -> {
                                WebElement candidate = firstUsableMatchDateInput(d);
                                return candidate == null ? null : candidate;
                            });
        } catch (TimeoutException timeout) {
            throw new TimeoutException(
                    "Match date input not populated in create form (official or friendly). "
                            + readinessDiagnosticsWithoutDateRead(),
                    timeout);
        }
    }

    private static WebElement firstUsableMatchDateInput(WebDriver driver) {
        for (By locator :
                List.of(MATCH_DATE_INPUT_IN_FORM, MATCH_DATE_INPUT, ANY_FORM_DATEPICKER_INPUT)) {
            for (WebElement input : driver.findElements(locator)) {
                String value = input.getAttribute("value");
                if (value != null && !value.isBlank()) {
                    return input;
                }
                if (input.isDisplayed()) {
                    return input;
                }
            }
        }
        return null;
    }

    private WebElement visible(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private String inputValue(By locator) {
        WebElement element = visible(locator);
        String value = element.getAttribute("value");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return element.getText().trim();
    }

    private String fieldPlaceholder(By fieldId) {
        WebElement wrapper = visible(fieldId);
        for (WebElement input : wrapper.findElements(By.cssSelector("input, textarea"))) {
            if (!input.isDisplayed()) {
                continue;
            }
            String placeholder = readDomProperty(input, "placeholder");
            if (placeholder != null && !placeholder.isBlank()) {
                return placeholder.trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private TeamFieldObservation observeTeamField(By fieldLocator, String fieldId) {
        visible(fieldLocator);
        Map<String, Object> dom =
                (Map<String, Object>)
                        ((JavascriptExecutor) driver)
                                .executeScript(
                                        """
                                        const fieldId = arguments[0];
                                        const root = document.getElementById(fieldId);
                                        const pack = (v) => (v == null ? '' : String(v));
                                        if (!root) {
                                          return {
                                            rootTagName: '',
                                            inputTagName: '',
                                            domPropertyValue: '',
                                            domPropertyPlaceholder: '',
                                            attributeValue: '',
                                            attributePlaceholder: '',
                                            inputTextContent: '',
                                            rootTextContent: '',
                                            inputReadonly: false,
                                            inputDisabled: false,
                                            inputDisplayed: false
                                          };
                                        }
                                        let input = root.matches('input,textarea')
                                            ? root
                                            : root.querySelector('input:not([type=hidden]),textarea');
                                        if (!input) {
                                          input = document.querySelector('#' + fieldId + ' input:not([type=hidden])');
                                        }
                                        if (!input) {
                                          return {
                                            rootTagName: root.tagName,
                                            inputTagName: '',
                                            domPropertyValue: '',
                                            domPropertyPlaceholder: '',
                                            attributeValue: '',
                                            attributePlaceholder: '',
                                            inputTextContent: '',
                                            rootTextContent: '',
                                            inputReadonly: false,
                                            inputDisabled: false,
                                            inputDisplayed: false
                                          };
                                        }
                                        return {
                                          rootTagName: root.tagName,
                                          inputTagName: input.tagName,
                                          domPropertyValue: pack(input.value).trim(),
                                          domPropertyPlaceholder: pack(input.placeholder).trim(),
                                          attributeValue: pack(input.getAttribute('value')).trim(),
                                          attributePlaceholder: pack(input.getAttribute('placeholder')).trim(),
                                          inputTextContent: pack(input.textContent).trim(),
                                          rootTextContent: pack(input.value || input.placeholder || '').trim(),
                                          inputReadonly: !!input.readOnly,
                                          inputDisabled: !!input.disabled,
                                          inputDisplayed: !!(input.offsetParent || input.getClientRects().length)
                                        };
                                        """,
                                        fieldId);

        String domValue = stringFrom(dom.get("domPropertyValue"));
        String domPlaceholder = stringFrom(dom.get("domPropertyPlaceholder"));
        String attrValue = stringFrom(dom.get("attributeValue"));
        String attrPlaceholder = stringFrom(dom.get("attributePlaceholder"));
        String inputText = stringFrom(dom.get("inputTextContent"));
        String rootText = stringFrom(dom.get("rootTextContent"));

        TeamFieldDisplayRules.ResolvedDisplay resolved =
                TeamFieldDisplayRules.resolveFromDomParts(
                        domValue, domPlaceholder, attrValue, attrPlaceholder, inputText);

        return new TeamFieldObservation(
                fieldId,
                resolved.text(),
                resolved.source(),
                stringFrom(dom.get("rootTagName")),
                stringFrom(dom.get("inputTagName")),
                domValue,
                domPlaceholder,
                attrValue,
                attrPlaceholder,
                inputText,
                rootText,
                Boolean.TRUE.equals(dom.get("inputReadonly")),
                Boolean.TRUE.equals(dom.get("inputDisabled")),
                Boolean.TRUE.equals(dom.get("inputDisplayed")));
    }

    private static String stringFrom(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long readOfficialMatchListScrollTop() {
        Object result =
                ((JavascriptExecutor) driver)
                        .executeScript(
                                """
                                const selectors = [
                                  '.p-select-list-container',
                                  '.p-select-overlay .p-select-list',
                                  '.p-select-overlay'
                                ];
                                for (const sel of selectors) {
                                  for (const el of document.querySelectorAll(sel)) {
                                    if (el && el.offsetParent !== null && el.scrollHeight > el.clientHeight) {
                                      return Math.round(el.scrollTop || 0);
                                    }
                                  }
                                }
                                return 0;
                                """);
        if (result instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static List<WebElement> resolveInputs(WebElement root) {
        if ("input".equalsIgnoreCase(root.getTagName()) || "textarea".equalsIgnoreCase(root.getTagName())) {
            return List.of(root);
        }
        return root.findElements(By.cssSelector("input, textarea"));
    }

    private static String readDomProperty(WebElement input, String name) {
        try {
            String dom = input.getDomProperty(name);
            if (dom != null && !dom.isBlank()) {
                return dom;
            }
        } catch (Exception ignored) {
            // fallback to attribute
        }
        return input.getAttribute(name);
    }

    private String fieldText(By fieldId) {
        WebElement wrapper = visible(fieldId);
        for (WebElement input : wrapper.findElements(By.cssSelector("input, textarea"))) {
            if (!input.isDisplayed()) {
                continue;
            }
            String value = input.getAttribute("value");
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
            String text = input.getText().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        String aria = wrapper.getAttribute("aria-label");
        return aria == null ? "" : aria.trim();
    }

    private boolean isReadOnly(By fieldId) {
        WebElement wrapper = visible(fieldId);
        for (WebElement input : resolveInputs(wrapper)) {
            if (!input.isDisplayed()) {
                continue;
            }
            if (isInputDomNonEditable(input)) {
                return true;
            }
        }
        return "true".equalsIgnoreCase(wrapper.getAttribute("aria-disabled"));
    }

    private static boolean isInputDomNonEditable(WebElement input) {
        if (!input.isEnabled()) {
            return true;
        }
        if (Boolean.TRUE.equals(input.getDomProperty("readOnly"))) {
            return true;
        }
        return "true".equalsIgnoreCase(input.getAttribute("readonly"));
    }

    @SuppressWarnings("unchecked")
    private TeamFieldEditabilityObservation observeTeamFieldEditability(String fieldId) {
        visible(By.id(fieldId));
        Map<String, Object> dom =
                (Map<String, Object>)
                        ((JavascriptExecutor) driver)
                                .executeScript(
                                        """
                                        const fieldId = arguments[0];
                                        const root = document.getElementById(fieldId);
                                        const pack = (v) => (v == null ? '' : String(v));
                                        if (!root) {
                                          return { missing: true };
                                        }
                                        const input = root.matches('input,textarea')
                                            ? root
                                            : root.querySelector('input:not([type=hidden]),textarea');
                                        if (!input) {
                                          return { tagName: root.tagName, missingInput: true };
                                        }
                                        const style = window.getComputedStyle(input);
                                        const tabIndex = input.tabIndex;
                                        return {
                                          tagName: input.tagName,
                                          domReadOnly: !!input.readOnly,
                                          domDisabled: !!input.disabled,
                                          ariaReadonly: pack(input.getAttribute('aria-readonly')),
                                          ariaDisabled: pack(input.getAttribute('aria-disabled')),
                                          pointerEvents: pack(style.pointerEvents),
                                          dataPcDisabled: pack(input.getAttribute('data-p-disabled')),
                                          focusable: tabIndex >= 0 && !input.disabled,
                                          domPropertyValue: pack(input.value).trim(),
                                          domPropertyPlaceholder: pack(input.placeholder).trim()
                                        };
                                        """,
                                        fieldId);
        if (Boolean.TRUE.equals(dom.get("missing")) || Boolean.TRUE.equals(dom.get("missingInput"))) {
            return new TeamFieldEditabilityObservation(
                    fieldId, stringFrom(dom.get("tagName")), false, false, "", "", "", "", false, "", "",
                    false, null, "input not resolved");
        }
        return new TeamFieldEditabilityObservation(
                fieldId,
                stringFrom(dom.get("tagName")),
                Boolean.TRUE.equals(dom.get("domReadOnly")),
                Boolean.TRUE.equals(dom.get("domDisabled")),
                stringFrom(dom.get("ariaReadonly")),
                stringFrom(dom.get("ariaDisabled")),
                stringFrom(dom.get("pointerEvents")),
                stringFrom(dom.get("dataPcDisabled")),
                Boolean.TRUE.equals(dom.get("focusable")),
                stringFrom(dom.get("domPropertyValue")),
                stringFrom(dom.get("domPropertyPlaceholder")),
                null,
                null,
                "probe not executed yet");
    }

    private boolean isTeamFieldUserEditBlocked(TeamFieldEditabilityObservation observation) {
        if (observation.domSignalsNonEditable()) {
            return true;
        }
        return probeTeamFieldEditBlocked(observation);
    }

    private boolean probeTeamFieldEditBlocked(TeamFieldEditabilityObservation observation) {
        WebElement input = resolveTeamInputElement(observation.fieldId());
        if (input == null) {
            return false;
        }
        String original =
                observation.domPropertyValue().isBlank()
                        ? observeTeamField(
                                        observation.fieldId().equals("analysis-home-team")
                                                ? HOME_TEAM_FIELD
                                                : AWAY_TEAM_FIELD,
                                        observation.fieldId())
                                .resolvedDisplayText()
                        : observation.domPropertyValue();
        String probeToken = "§";
        try {
            scrollIntoView(input);
            input.click();
            input.sendKeys(probeToken);
            String after =
                    observeTeamFieldEditability(observation.fieldId()).domPropertyValue();
            boolean unchanged = original.equals(after);
            if (!unchanged) {
                input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                input.sendKeys(Keys.BACK_SPACE);
                if (!original.isBlank()) {
                    input.sendKeys(original);
                }
                String restored =
                        observeTeamFieldEditability(observation.fieldId()).domPropertyValue();
                if (!original.equals(restored)) {
                    return false;
                }
            }
            return unchanged;
        } catch (InvalidElementStateException refused) {
            return true; // the browser refused the keystrokes: the field is not user-editable
        }
    }

    private WebElement resolveTeamInputElement(String fieldId) {
        WebElement root = driver.findElement(By.id(fieldId));
        List<WebElement> inputs = resolveInputs(root);
        if (inputs.isEmpty()) {
            return null;
        }
        return inputs.getFirst();
    }

    private boolean isRootFieldVisible(By root) {
        return !driver.findElements(root).isEmpty() && driver.findElement(root).isDisplayed();
    }

    private WebElement resolveFriendlyMatchNameInput() {
        if (!driver.findElements(FRIENDLY_MATCH_NAME_INPUT).isEmpty()) {
            WebElement candidate = driver.findElement(FRIENDLY_MATCH_NAME_INPUT);
            if (candidate.isDisplayed()) {
                return candidate;
            }
        }
        if (!driver.findElements(FRIENDLY_MATCH_NAME_ROOT).isEmpty()) {
            WebElement root = driver.findElement(FRIENDLY_MATCH_NAME_ROOT);
            if (root.isDisplayed()) {
                List<WebElement> nested = root.findElements(By.cssSelector("input, textarea"));
                if (!nested.isEmpty()) {
                    return nested.getFirst();
                }
            }
        }
        throw new NoSuchElementException(
                "Friendly match name field not found (#analysis-match-name). US-FAP-ANL-FRIENDLY DOM pending?");
    }

    private static String inputElementValue(WebElement input) {
        String value = input.getAttribute("value");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return input.getText() == null ? "" : input.getText().trim();
    }

    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    private boolean isDisplayedSafely(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }
}
