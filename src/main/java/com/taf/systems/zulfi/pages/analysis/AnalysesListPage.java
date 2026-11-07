package com.taf.systems.zulfi.pages.analysis;

import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.ui.UiRetry;
import java.util.regex.Matcher;
import org.openqa.selenium.TimeoutException;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.validations.AnalysesListCardRules;
import com.taf.systems.zulfi.validations.AnalysesListingMessages;
import com.taf.systems.zulfi.validations.AnalysisProcessingStatusMessages;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AnalysesListPage {

    private static final Pattern ANALYSIS_DETAIL_ROUTE =
            Pattern.compile("(?i)/analysis/\\d+(?:[/?#]|$)");
    private static final Pattern ANALYSIS_CREATE_ROUTE = Pattern.compile("(?i)/analysis/create");

    private static final By SEARCH_FIELD_PRIMARY =
            By.cssSelector("input[placeholder='" + AnalysesListingMessages.SEARCH_PLACEHOLDER + "']");
    private static final By[] SEARCH_FIELD_FALLBACKS =
            new By[] {
                By.cssSelector("input#analysis-search"),
                By.cssSelector("[data-testid*='analysis-search'] input"),
                By.cssSelector("input[placeholder*='ابحث']"),
                By.cssSelector("input[aria-label*='ابحث']")
            };
    private static final By ANALYSES_NAV =
            By.cssSelector("a[href*='/analysis'][aria-label='التحليلات'], a[aria-label='التحليلات']");
    private static final By ANALYSIS_CARD_HEADING = By.cssSelector("article h3.font-bold, article h3");
    private static final By ANALYSIS_CARD_ROOT = By.cssSelector("article");
    private static final By PAGE_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2 or self::h3][contains(normalize-space(.),'"
                            + AnalysesListingMessages.PAGE_TITLE
                            + "')]");
    private static final By PRIMARY_CTA =
            By.xpath(
                    "//a[contains(normalize-space(.),'"
                            + AnalysesListingMessages.PRIMARY_CTA
                            + "')]"
                            + "|//button[contains(normalize-space(.),'"
                            + AnalysesListingMessages.PRIMARY_CTA
                            + "')"
                            + " or contains(@aria-label,'"
                            + AnalysesListingMessages.PRIMARY_CTA
                            + "')]");
    private static final By RETRY_BUTTON =
            By.xpath(
                    "//button[contains(normalize-space(.),'"
                            + AnalysesListingMessages.RETRY_ACTION
                            + "')"
                            + " or contains(@aria-label,'"
                            + AnalysesListingMessages.RETRY_ACTION
                            + "')]");
    private static final By LOADING_HINT =
            By.cssSelector(
                    ".p-skeleton, [data-testid='analyses-loading'], [aria-busy='true'], .p-progress-spinner");

    private final WebDriver driver;

    public AnalysesListPage(WebDriver driver) {
        this.driver = driver;
    }

    public AnalysesListFiltersSection filters() {
        return new AnalysesListFiltersSection(driver);
    }

    public AnalysesListPage openViaSidebarNavigation() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(25))
                        .until(ExpectedConditions.elementToBeClickable(ANALYSES_NAV));
        link.click();
        return waitUntilLoaded();
    }

    public AnalysesListPage openDirectly() {
        driver.navigate().to(PortalConfig.baseUrl() + "analysis");
        return waitUntilLoaded();
    }

    public AnalysesListPage waitUntilLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(
                        d -> {
                            // Home also renders «آخر التحليلات» headings and analysis cards: require the listing route.
                            if (!isOnAnalysesListingRoute()) {
                                return false;
                            }
                            // The toolbar/CTA render before the cards; wait for cards or an explicit empty state.
                            if (!d.findElements(PAGE_TITLE).isEmpty()
                                    && d.findElement(PAGE_TITLE).isDisplayed()) {
                                return d.findElements(ANALYSIS_CARD_ROOT).stream().anyMatch(WebElement::isDisplayed)
                                        || pageSourceContainsNoResults(d);
                            }
                            return false;
                        });
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return this;
    }

    private static boolean pageSourceContainsNoResults(WebDriver d) {
        String source = d.getPageSource();
        return source.contains(AnalysesListingMessages.EMPTY_TITLE)
                || source.contains("لا توجد");
    }

    private Optional<WebElement> resolveSearchField() {
        if (!driver.findElements(SEARCH_FIELD_PRIMARY).isEmpty()) {
            WebElement hit = driver.findElement(SEARCH_FIELD_PRIMARY);
            if (hit.isDisplayed()) {
                return Optional.of(hit);
            }
        }
        for (By locator : SEARCH_FIELD_FALLBACKS) {
            for (WebElement hit : driver.findElements(locator)) {
                if (hit.isDisplayed()) {
                    return Optional.of(hit);
                }
            }
        }
        return Optional.empty();
    }

    private WebElement requireSearchField() {
        return resolveSearchField()
                .orElseThrow(() -> new IllegalStateException("Analysis list search field not found"));
    }

    /** Waits until loading skeleton clears and listing shows cards or an empty/no-results state. */
    public AnalysesListPage waitUntilListingResultsStable() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(
                        d -> {
                            if (!d.findElements(LOADING_HINT).isEmpty()) {
                                boolean anyLoadingDisplayed =
                                        d.findElements(LOADING_HINT).stream()
                                                .anyMatch(WebElement::isDisplayed);
                                if (anyLoadingDisplayed) {
                                    return false;
                                }
                            }
                            return !d.findElements(ANALYSIS_CARD_ROOT).isEmpty()
                                    || isNoResultsStateVisible()
                                    || isGlobalEmptyStateVisible();
                        });
        return this;
    }

    public AnalysesListPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public boolean isOnAnalysesRoute() {
        return driver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("analysis");
    }

    /** Listing grid — excludes create route and numeric detail routes such as {@code /analysis/18}. */
    public boolean isOnAnalysesListingRoute() {
        String url = driver.getCurrentUrl();
        if (!url.toLowerCase(Locale.ROOT).contains("analysis")) {
            return false;
        }
        if (ANALYSIS_CREATE_ROUTE.matcher(url).find()) {
            return false;
        }
        if (ANALYSIS_DETAIL_ROUTE.matcher(url).find()) {
            return false;
        }
        return true;
    }

    public AnalysesListPage openListingEnsuringLoaded() {
        if (!isOnAnalysesListingRoute()) {
            return openDirectly();
        }
        return waitUntilLoaded();
    }

    public boolean isPageTitleVisible() {
        if (!driver.findElements(PAGE_TITLE).isEmpty()) {
            return driver.findElement(PAGE_TITLE).isDisplayed();
        }
        return pageContains(AnalysesListingMessages.PAGE_TITLE);
    }

    public boolean isPageDescriptionVisible() {
        return pageContains(AnalysesListingMessages.PAGE_DESCRIPTION);
    }

    public boolean isPrimaryCtaVisible() {
        return !driver.findElements(PRIMARY_CTA).isEmpty()
                && driver.findElement(PRIMARY_CTA).isDisplayed();
    }

    public CreateAnalysisPage clickPrimaryNewAnalysisCta() {
        WebElement cta =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(PRIMARY_CTA));
        cta.click();
        return new CreateAnalysisPage(driver).waitForCreatePopupReady();
    }

    public boolean isSearchFieldVisible() {
        return resolveSearchField().isPresent();
    }

    public String readSearchValue() {
        WebElement field = requireSearchField();
        String value = field.getAttribute("value");
        return value == null ? "" : value.trim();
    }

    public AnalysesListPage clearSearch() {
        WebElement field =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> resolveSearchField().orElse(null));
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);
        field.sendKeys(Keys.TAB);
        waitForSearchFieldSettled();
        return this;
    }

    public AnalysesListPage searchByAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return this;
        }
        return searchByMatchName(analysisId.trim());
    }

    public boolean containsAnalysisIdentityMarker(String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        String needle = marker.trim();
        String source = driver.getPageSource();
        if (source.contains("/analysis/" + needle) || source.contains("analysisId=" + needle)) {
            return true;
        }
        for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
            if (card.rawText().contains(needle) || card.matchTitle().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean pageSourceContainsFixture(String fixtureLabel) {
        return pageSourceContainsFixtureInternal(fixtureLabel);
    }

    public AnalysesListPage searchByMatchName(String matchNameFragment) {
        WebElement field =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> resolveSearchField().orElse(null));
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);
        if (matchNameFragment != null && !matchNameFragment.isBlank()) {
            field.sendKeys(matchNameFragment.trim());
            String needle = matchNameFragment.trim();
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .ignoring(StaleElementReferenceException.class)
                    .until(
                            d ->
                                    pageSourceContainsFixtureInternal(needle)
                                            || isNoResultsStateVisible()
                                            || containsVisibleMatchTitle(needle));
        }
        return this;
    }

    public boolean containsVisibleMatchTitle(String fixtureLabel) {
        if (fixtureLabel == null || fixtureLabel.isBlank()) {
            return false;
        }
        String normalizedFixture = normalizeFixtureLabel(fixtureLabel);
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                for (WebElement heading : driver.findElements(ANALYSIS_CARD_HEADING)) {
                    if (!heading.isDisplayed()) {
                        continue;
                    }
                    String title = normalizeFixtureLabel(heading.getText());
                    if (title.contains(normalizedFixture) || normalizedFixture.contains(title)) {
                        return true;
                    }
                    if (teamsMatch(title, normalizedFixture)) {
                        return true;
                    }
                }
                return false;
            } catch (StaleElementReferenceException stale) {
                waitForAnalysisCardHeadingsStable();
            }
        }
        return false;
    }

    public boolean pageShowsOfficialMatchType() {
        if (sourceContainsOfficialMatchType(driver.getPageSource())) {
            return true;
        }
        try {
            waitUntilListingResultsStable();
        } catch (TimeoutException listingBusy) {
            // Continue with best-effort card scan when Vue re-renders slowly.
        }
        for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
            if (sourceContainsOfficialMatchType(card.rawText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sourceContainsOfficialMatchType(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        return source.contains("مباراة رسمية")
                || source.contains(CreateAnalysisPage.MATCH_TYPE_OFFICIAL);
    }

    public boolean containsDescriptionMarker(String marker) {
        return marker != null && !marker.isBlank() && driver.getPageSource().contains(marker.trim());
    }

    public List<String> listVisibleMatchTitles() {
        List<String> titles = new ArrayList<>();
        for (WebElement heading : driver.findElements(ANALYSIS_CARD_HEADING)) {
            if (!heading.isDisplayed()) {
                continue;
            }
            String text = heading.getText().trim();
            if (!text.isEmpty()) {
                titles.add(text);
            }
        }
        return titles;
    }

    public List<AnalysesListCardSnapshot> readVisibleAnalysisCards() {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return readVisibleAnalysisCardsOnce();
            } catch (StaleElementReferenceException stale) {
                waitUntilListingResultsStable();
            }
        }
        return List.of();
    }

    public List<AnalysesListCardSnapshot> findVisibleCardsContaining(String needle) {
        if (needle == null || needle.isBlank()) {
            return List.of();
        }
        String n = needle.trim();
        List<AnalysesListCardSnapshot> hits = new ArrayList<>();
        for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
            if (card.rawText().contains(n) || card.matchTitle().contains(n)) {
                hits.add(card);
            }
        }
        if (hits.isEmpty() && pageSourceContainsFixtureInternal(n)) {
            for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
                hits.add(card);
            }
        }
        return hits;
    }

    /**
     * Read-only: resolves analysis id from visible card link href (fresh query; no submit).
     */
    public Optional<Long> tryReadAnalysisIdFromFirstMatchingCardLink(String titleFragment) {
        if (titleFragment == null || titleFragment.isBlank()) {
            return Optional.empty();
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                for (WebElement card : driver.findElements(ANALYSIS_CARD_ROOT)) {
                    if (!card.isDisplayed()) {
                        continue;
                    }
                    String text = card.getText() == null ? "" : card.getText();
                    if (!text.contains(titleFragment.trim())) {
                        continue;
                    }
                    for (WebElement link : card.findElements(By.cssSelector("a[href*='/analysis/']"))) {
                        String href = link.getAttribute("href");
                        if (href == null) {
                            continue;
                        }
                        Matcher m =
                                Pattern.compile("/analysis/(\\d+)").matcher(href);
                        if (m.find()) {
                            return Optional.of(Long.parseLong(m.group(1)));
                        }
                    }
                }
                return Optional.empty();
            } catch (StaleElementReferenceException stale) {
                waitUntilListingResultsStable();
            }
        }
        return Optional.empty();
    }

    private List<AnalysesListCardSnapshot> readVisibleAnalysisCardsOnce() {
        List<AnalysesListCardSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        int i = 0;
        while (true) {
            List<WebElement> cards = driver.findElements(ANALYSIS_CARD_ROOT);
            if (i >= cards.size()) {
                break;
            }
            WebElement card = cards.get(i);
            i++;
            if (!card.isDisplayed()) {
                continue;
            }
            String text = card.getText() == null ? "" : card.getText().trim();
            if (text.isBlank() || !looksLikeAnalysisCard(text)) {
                continue;
            }
            String status = resolveStatus(text);
            String title = extractMatchTitle(card, text);
            String dateToken = extractDateToken(text);
            String videoSource = AnalysesListCardRules.detectVideoSource(text).orElse("");
            snapshots.add(
                    new AnalysesListCardSnapshot(
                            index++,
                            text,
                            title,
                            dateToken,
                            videoSource,
                            status,
                            hasAction(card, AnalysesListingMessages.ACTION_VIEW_DETAILS),
                            isActionEnabled(card, AnalysesListingMessages.ACTION_VIEW_DETAILS),
                            hasAction(card, AnalysesListingMessages.ACTION_VIEW_DASHBOARD),
                            isActionEnabled(card, AnalysesListingMessages.ACTION_VIEW_DASHBOARD)));
        }
        return snapshots;
    }

    public int countVisibleAnalysisCards() {
        return readVisibleAnalysisCards().size();
    }

    public boolean visibleCardsAppearNewestFirst() {
        List<String> rawTexts =
                readVisibleAnalysisCards().stream().map(AnalysesListCardSnapshot::rawText).toList();
        return AnalysesListCardRules.looksNewestFirst(rawTexts);
    }

    public boolean isGlobalEmptyStateVisible() {
        return pageContains(AnalysesListingMessages.EMPTY_TITLE)
                && pageContains(AnalysesListingMessages.EMPTY_DESCRIPTION);
    }

    public boolean isNoResultsStateVisible() {
        return pageContains(AnalysesListingMessages.NO_RESULTS_TITLE)
                && pageContains(AnalysesListingMessages.NO_RESULTS_DESCRIPTION);
    }

    public boolean isLoadErrorVisible() {
        return pageContains(AnalysesListingMessages.LOAD_ERROR);
    }

    public boolean isRetryActionVisible() {
        return !driver.findElements(RETRY_BUTTON).isEmpty()
                && driver.findElement(RETRY_BUTTON).isDisplayed();
    }

    public boolean isLoadingStateVisible() {
        return !driver.findElements(LOADING_HINT).isEmpty();
    }

    public boolean pageContainsDeleteAction() {
        return pageContains(AnalysesListingMessages.DELETE_ACTION);
    }

    public Optional<String> readFirstVisibleCardStatus() {
        for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
            if (!card.status().isBlank()) {
                return Optional.of(card.status());
            }
        }
        return Optional.empty();
    }

    public AnalysesListPage clickFirstVisibleViewDetails() {
        if (clickFirstEnabledCardAction(AnalysisProcessingStatusMessages.ACTION_VIEW_DETAILS)) {
            return this;
        }
        // US-FAP-ANL-003 AC-10: «عرض التفاصيل» must stay enabled while processing — a fully disabled list is a product finding.
        throw new AssertionError(
                "No enabled «" + AnalysisProcessingStatusMessages.ACTION_VIEW_DETAILS + "» on list (AC-10 requires it"
                        + " enabled for «" + AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS + "» analyses)");
    }

    /** Clicks the first enabled {@code label} action of a visible card; the grid re-renders, so the scan is retried. */
    private boolean clickFirstEnabledCardAction(String label) {
        return UiRetry.onStale(() -> {
            for (WebElement card : driver.findElements(ANALYSIS_CARD_ROOT)) {
                if (!card.isDisplayed()) {
                    continue;
                }
                WebElement action = findActionInCard(card, label);
                if (action != null && isElementEnabled(action)) {
                    action.click();
                    return true;
                }
            }
            return false;
        });
    }

    public AnalysesListPage clickFirstEnabledDashboard() {
        if (clickFirstEnabledCardAction(AnalysisProcessingStatusMessages.ACTION_VIEW_DASHBOARD)) {
            return this;
        }
        // Dashboards are disabled until an analysis completes: no completed analysis is a data precondition.
        throw new IllegalStateException(
                "No enabled «" + AnalysisProcessingStatusMessages.ACTION_VIEW_DASHBOARD + "» on list");
    }

    public int countVisibleCardsMatchingTitleFragment(String titleFragment) {
        if (titleFragment == null || titleFragment.isBlank()) {
            return countVisibleAnalysisCards();
        }
        int count = 0;
        String needle = titleFragment.trim();
        for (AnalysesListCardSnapshot card : readVisibleAnalysisCards()) {
            if (card.rawText().contains(needle) || card.matchTitle().contains(needle)) {
                count++;
            }
        }
        return count;
    }

    public String searchEvidenceSummary(String fixtureFragment) {
        if (fixtureFragment == null || fixtureFragment.isBlank()) {
            return "No search performed";
        }
        searchByMatchName(fixtureFragment);
        if (containsVisibleMatchTitle(fixtureFragment)) {
            return "Visible analysis card title matches fixture fragment in current listing view";
        }
        if (pageSourceContainsFixtureInternal(fixtureFragment)) {
            return "Page source contains fixture fragment but no visible card title matched";
        }
        return "No matching analysis evidence in current listing view after search";
    }

    private static boolean looksLikeAnalysisCard(String text) {
        return text.contains(AnalysesListingMessages.STATUS_IN_PROGRESS)
                || text.contains(AnalysesListingMessages.STATUS_COMPLETED)
                || text.contains(AnalysesListingMessages.ACTION_VIEW_DETAILS)
                || text.toLowerCase(Locale.ROOT).contains("youtube")
                || text.toLowerCase(Locale.ROOT).contains("drive");
    }

    private static String resolveStatus(String text) {
        if (text.contains(AnalysesListingMessages.STATUS_COMPLETED)) {
            return AnalysesListingMessages.STATUS_COMPLETED;
        }
        if (text.contains(AnalysesListingMessages.STATUS_IN_PROGRESS)) {
            return AnalysesListingMessages.STATUS_IN_PROGRESS;
        }
        return "";
    }

    private static String extractMatchTitle(WebElement card, String fallbackText) {
        List<WebElement> headings = card.findElements(By.cssSelector("h3, h4, .font-bold"));
        for (WebElement heading : headings) {
            if (!heading.isDisplayed()) {
                continue;
            }
            String title = heading.getText().trim();
            if (!title.isEmpty()) {
                return title;
            }
        }
        String[] lines = fallbackText.split("\\R");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (line.contains(AnalysesListingMessages.STATUS_IN_PROGRESS)
                    || line.contains(AnalysesListingMessages.STATUS_COMPLETED)
                    || line.contains(AnalysesListingMessages.ACTION_VIEW_DETAILS)) {
                continue;
            }
            return line.trim();
        }
        return "";
    }

    private static String extractDateToken(String rawText) {
        // Cards render «٢٠ أكتوبر ٢٠٢٦ في ١٠:٠٤ ص» (Arabic-Indic digits): normalise before matching.
        String text = PortalDom.normalize(rawText);
        int slash = text.indexOf('/');
        if (slash > 0) {
            int start = Math.max(0, slash - 3);
            int end = Math.min(text.length(), slash + 12);
            String candidate = text.substring(start, end).trim();
            if (MatchDateDisplayRules.looksLikeCalendarDateDisplay(candidate)) {
                return candidate;
            }
        }
        return AnalysesListCardRules.parseSubmissionDate(text).map(date -> date.toString()).orElse("");
    }

    private static boolean hasAction(WebElement card, String label) {
        return findActionInCard(card, label) != null;
    }

    private static boolean isActionEnabled(WebElement card, String label) {
        WebElement action = findActionInCard(card, label);
        return action != null && isElementEnabled(action);
    }

    private static String normalizeFixtureLabel(String label) {
        return label.replace('×', '×').replace(" x ", " × ").replaceAll("\\s+", " ").trim();
    }

    private static boolean teamsMatch(String listTitle, String selectedFixture) {
        String a = listTitle.replace("×", " ").replace("-", " ");
        String b = selectedFixture.replace("×", " ").replace("-", " ");
        return !a.isBlank() && b.contains(a.substring(0, Math.min(8, a.length())));
    }

    private static WebElement findActionInCard(WebElement card, String label) {
        List<WebElement> hits =
                card.findElements(
                        By.xpath(
                                ".//a[contains(normalize-space(.),'"
                                        + label
                                        + "')]"
                                        + "|.//button[contains(normalize-space(.),'"
                                        + label
                                        + "')"
                                        + " or contains(@aria-label,'"
                                        + label
                                        + "')]"));
        for (WebElement hit : hits) {
            if (hit.isDisplayed()) {
                return hit;
            }
        }
        return null;
    }

    private static boolean isElementEnabled(WebElement element) {
        if (!element.isEnabled()) {
            return false;
        }
        String disabled = element.getAttribute("disabled");
        if (disabled != null && !disabled.isBlank() && !"false".equalsIgnoreCase(disabled)) {
            return false;
        }
        if ("true".equalsIgnoreCase(element.getAttribute("aria-disabled"))) {
            return false;
        }
        String cls = element.getAttribute("class");
        return cls == null || !cls.toLowerCase(Locale.ROOT).contains("disabled");
    }

    public String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    private boolean pageContains(String fragment) {
        return fragment != null && !fragment.isBlank() && driver.getPageSource().contains(fragment);
    }

    private boolean pageSourceContainsFixtureInternal(String fixtureLabel) {
        String source = driver.getPageSource();
        if (fixtureLabel.contains("×")) {
            String[] parts = fixtureLabel.split("×");
            if (parts.length >= 2) {
                return source.contains(parts[0].trim()) && source.contains(parts[1].trim());
            }
        }
        return source.contains(fixtureLabel.trim());
    }

    private void waitForSearchFieldSettled() {
        new WebDriverWait(driver, Duration.ofSeconds(8))
                .ignoring(StaleElementReferenceException.class)
                .until(
                        d -> {
                            var field = resolveSearchField();
                            if (field.isEmpty()) {
                                return true;
                            }
                            String value = field.get().getAttribute("value");
                            return value == null || value.isBlank();
                        });
        try {
            waitUntilListingResultsStable();
        } catch (TimeoutException ignored) {
            // Best-effort after clear; listing may already be stable.
        }
    }

    /**
     * Best-effort settle before reading card titles. A search with no hits may render an empty-state copy that differs
     * from the story wording; callers decide from the titles they read, so a timeout here must not fail them.
     */
    private void waitForAnalysisCardHeadingsStable() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .ignoring(StaleElementReferenceException.class)
                    .until(
                            d ->
                                    !d.findElements(ANALYSIS_CARD_HEADING).isEmpty()
                                            || isNoResultsStateVisible()
                                            || isGlobalEmptyStateVisible());
        } catch (TimeoutException noCardsYet) {
            // no cards and no recognised empty state: the caller sees an empty title list
        }
    }
}
