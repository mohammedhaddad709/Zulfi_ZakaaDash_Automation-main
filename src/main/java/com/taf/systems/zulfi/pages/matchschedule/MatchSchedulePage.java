package com.taf.systems.zulfi.pages.matchschedule;

import com.taf.systems.zulfi.ui.PrimeVueSelect;
import com.taf.systems.zulfi.ui.UiRetry;import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import com.taf.core.pacing.PacedElementInteraction;
import com.taf.core.utils.WaitManager;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import com.taf.systems.zulfi.validations.MatchSchedulePageMessages;
import com.taf.systems.zulfi.validations.MatchScheduleRowRules;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Match Schedule (Yellow League) — US-FAP-MCH browse/filter and US-FAP-MCH-ANALYSIS entry points.
 */
public final class MatchSchedulePage {

    public static final String STATUS_NOT_ANALYZED = MatchSchedulePageMessages.STATUS_NOT_ANALYZED;
    public static final String ACTION_START_ANALYSIS =
            MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT;

    private static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");

    private static final By SCHEDULE_NAV =
            By.cssSelector(
                    "a[aria-label='جدول المباريات'],"
                            + " a[href*='matches'],"
                            + " a[href*='match-schedule'],"
                            + " a[href*='schedule']");
    private static final By SCHEDULE_ROOT =
            By.cssSelector("[data-testid='match-schedule'], .match-schedule, main");
    private static final By PAGE_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2 or self::h3]"
                            + "[contains(normalize-space(.),'"
                            + MatchSchedulePageMessages.PAGE_TITLE
                            + "')]");
    private static final By PAGE_DESCRIPTION =
            By.xpath(
                    "//*[self::p or self::span or self::div]"
                            + "[contains(normalize-space(.),'"
                            + MatchSchedulePageMessages.pageDescriptionFragment()
                            + "')]");
    // Filter toolbar = container of the search field; the team/club select is its last combobox (after round/type).
    private static final By CLUB_FILTER_COMBO =
            By.xpath(
                    "(//*[contains(@class,'p-iconfield')]/parent::*//*[contains(@class,'p-select ')"
                            + " or @data-pc-name='select']//*[@role='combobox'])[last()]"
                            + "|//*[@id='club-filter']//*[@role='combobox']");
    private static final By ANALYSIS_ACTION =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + ACTION_START_ANALYSIS
                            + "') or contains(normalize-space(.),'"
                            + ACTION_START_ANALYSIS
                            + "')]");
    private static final By RETRY_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + MatchSchedulePageMessages.RETRY_ACTION
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + MatchSchedulePageMessages.RETRY_ACTION
                            + "')]");
    private static final By LOADING_HINT =
            By.cssSelector(
                    ".p-skeleton, [data-testid='match-schedule-loading'],"
                            + " [aria-busy='true'], .p-progress-spinner");

    private final WebDriver driver;
    private final ElementActions actions;
    private final WaitManager waits;

    public MatchSchedulePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.waits = new WaitManager(driver);
    }

    public MatchSchedulePage openFromSidebar() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(25))
                        .until(ExpectedConditions.elementToBeClickable(SCHEDULE_NAV));
        link.click();
        return waitUntilLoaded();
    }

    public MatchSchedulePage waitUntilLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(
                        d ->
                                d.getCurrentUrl().contains("schedule")
                                        || d.getCurrentUrl().contains("match")
                                        || !d.findElements(SCHEDULE_ROOT).isEmpty());
        waitForScheduleRowsSettled();
        return this;
    }

    public boolean isDisplayed() {
        return !driver.findElements(SCHEDULE_ROOT).isEmpty();
    }

    public boolean isOnMatchScheduleRoute() {
        String url = driver.getCurrentUrl().toLowerCase();
        return url.contains("/matches") || url.contains("schedule") || url.contains("match");
    }

    public boolean isPageTitleVisible() {
        if (!driver.findElements(PAGE_TITLE).isEmpty()) {
            return driver.findElement(PAGE_TITLE).isDisplayed();
        }
        return pageContains(MatchSchedulePageMessages.PAGE_TITLE);
    }

    public boolean isPageDescriptionVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(
                            d ->
                                    !d.findElements(PAGE_DESCRIPTION).isEmpty()
                                            && d.findElement(PAGE_DESCRIPTION).isDisplayed());
            return true;
        } catch (TimeoutException notFound) {
            return pageContains(MatchSchedulePageMessages.PAGE_DESCRIPTION)
                    || pageContains(MatchSchedulePageMessages.pageDescriptionFragment());
        }
    }

    public String readSelectedClubFilterLabel() {
        if (driver.findElements(CLUB_FILTER_COMBO).isEmpty()) {
            return "";
        }
        WebElement combo = driver.findElement(CLUB_FILTER_COMBO);
        if (!combo.isDisplayed()) {
            return "";
        }
        String aria = combo.getAttribute("aria-label");
        if (aria != null && !aria.isBlank()) {
            return aria.trim();
        }
        WebElement labelEl = firstDisplayed(combo, By.cssSelector(".p-select-label, [data-pc-section='label']"));
        if (labelEl != null) {
            String labelText = labelEl.getText();
            if (labelText != null && !labelText.isBlank()) {
                return labelText.trim();
            }
        }
        return combo.getText() == null ? "" : combo.getText().trim();
    }

    public boolean clubFilterSelectionContains(String clubFragment) {
        if (clubFragment == null || clubFragment.isBlank()) {
            return false;
        }
        // Only the filter's own label counts: the page header always contains the club name («نادي الزلفي | …»),
        // so a page-source fallback made this check pass even when no filter was applied.
        return readSelectedClubFilterLabel().contains(clubFragment);
    }

    public MatchSchedulePage selectClubByFragment(String clubFragment) {
        if (clubFragment == null || clubFragment.isBlank()) {
            throw new IllegalArgumentException("clubFragment required");
        }
        // The schedule auto-scrolls to the current round, parking the toolbar under the sticky top bar.
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        clubSelect().pick(clubFragment);
        waitForScheduleRowsSettled();
        return this;
    }

    /**
     * Story option «جميع الأندية». The live select has no such option: an empty (cleared) team filter lists every club
     * (re-baselined 2026-10-22). Whether the «جميع الأندية» wording exists is asserted separately (MCH-SCHED-013).
     */
    public MatchSchedulePage selectAllClubs() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        PrimeVueSelect select = clubSelect();
        boolean hasAllOption = select.open().options().stream().anyMatch(o -> o.contains(MatchSchedulePageMessages.FILTER_ALL_CLUBS));
        select.close();
        if (hasAllOption) {
            select.pick(MatchSchedulePageMessages.FILTER_ALL_CLUBS);
        } else {
            select.clear();
        }
        waitForScheduleRowsSettled();
        return this;
    }

    private PrimeVueSelect clubSelect() {
        return new PrimeVueSelect(driver, CLUB_FILTER_COMBO, MatchSchedulePageMessages.CLUB_FILTER_LABEL);
    }

    public List<MatchScheduleRowSnapshot> readMatchRows() {
        waitForScheduleRowsSettled();
        // Round rows re-render while the schedule settles; read every visible row's text in one script call
        // (no element handles → no stale storms that used to return an empty list).
        List<String> roundRowTexts = visibleRoundRowTexts();
        if (!roundRowTexts.isEmpty()) {
            List<MatchScheduleRowSnapshot> snapshots = new ArrayList<>();
            for (String text : roundRowTexts) {
                if (looksLikeScheduleRow(text)) {
                    snapshots.add(snapshotOf(snapshots.size(), text));
                }
            }
            return snapshots;
        }
        List<MatchScheduleRowSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        for (int staleAttempt = 0; staleAttempt < 4; staleAttempt++) {
            try {
                snapshots.clear();
                index = 0;
                for (WebElement candidate : locateStableRowElements()) {
                    if (!candidate.isDisplayed()) {
                        continue;
                    }
                    String text = readRowTextSafely(candidate);
                    if (!looksLikeScheduleRow(text)) {
                        continue;
                    }
                    boolean played = MatchScheduleRowRules.isPlayedRow(text);
                    boolean unplayed = MatchScheduleRowRules.isUnplayedRow(text);
                    int[] score = MatchScheduleRowRules.parseFirstScore(text);
                    snapshots.add(
                            new MatchScheduleRowSnapshot(
                                    index++,
                                    text,
                                    played,
                                    unplayed,
                                    score.length == 2 ? score[0] : -1,
                                    score.length == 2 ? score[1] : -1,
                                    MatchScheduleRowRules.extractDateToken(text),
                                    extractKickoffTime(text),
                                    readAnalysisStatusOnRow(text),
                                    MatchScheduleRowRules.mentionsMatchId(text)));
                }
                return snapshots;
            } catch (StaleElementReferenceException stale) {
                waitForScheduleRowsSettled();
            }
        }
        return snapshots;
    }

    private List<String> visibleRoundRowTexts() {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll(\"section[id^='round-'] article\")]"
                        + ".filter(a => a.getClientRects().length > 0).map(a => a.innerText.trim());");
        return texts == null ? List.of() : texts;
    }

    private MatchScheduleRowSnapshot snapshotOf(int index, String text) {
        int[] score = MatchScheduleRowRules.parseFirstScore(text);
        return new MatchScheduleRowSnapshot(
                index,
                text,
                MatchScheduleRowRules.isPlayedRow(text),
                MatchScheduleRowRules.isUnplayedRow(text),
                score.length == 2 ? score[0] : -1,
                score.length == 2 ? score[1] : -1,
                MatchScheduleRowRules.extractDateToken(text),
                extractKickoffTime(text),
                readAnalysisStatusOnRow(text),
                MatchScheduleRowRules.mentionsMatchId(text));
    }

    public String readAnalysisStatusOnRow(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return "";
        }
        if (MatchScheduleRowRules.isUnplayedRow(rowText)) {
            return "";
        }
        return MatchScheduleRowRules.findKnownAnalysisStatus(rowText);
    }

    public boolean isEmptyFilterStateVisible() {
        // No readMatchRows() here: it waits on this very check (mutual recursion).
        return (pageContains(MatchSchedulePageMessages.EMPTY_FILTER_TITLE)
                        && pageContains(MatchSchedulePageMessages.EMPTY_FILTER_DESCRIPTION))
                || (pageContains(MatchSchedulePageMessages.EMPTY_FILTER_TITLE)
                        && locateStableRowElements().stream().noneMatch(WebElement::isDisplayed));
    }

    public boolean isLoadErrorVisible() {
        return pageContains(MatchSchedulePageMessages.LOAD_ERROR);
    }

    public boolean isRetryActionVisible() {
        return !driver.findElements(RETRY_BUTTON).isEmpty()
                && driver.findElement(RETRY_BUTTON).isDisplayed();
    }

    public boolean isLoadingStateVisible() {
        return !driver.findElements(LOADING_HINT).isEmpty();
    }

    /** Yellow League schedule should not list friendly fixtures (heuristic on row copy). */
    public boolean hasFriendlyMatchRowsHeuristic() {
        for (MatchScheduleRowSnapshot row : readMatchRows()) {
            if (MatchScheduleRowRules.appearsFriendlyMatch(row.rawText())) {
                return true;
            }
        }
        return false;
    }

    public MatchSchedulePage openDirectly() {
        driver.navigate().to(PortalConfig.baseUrl() + "matches");
        return waitUntilLoaded();
    }

    public boolean isStartAnalysisActionVisibleForEligibleRow() {
        return findFirstEligibleAnalysisRowContext().isPresent();
    }

    public MatchSchedulePage clickStartAnalysisOnFirstEligibleRow() {
        MatchScheduleAnalysisRowContext context =
                findFirstEligibleAnalysisRowContext()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No played «"
                                                        + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                                                        + "» row with «"
                                                        + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                                                        + "»."));
        return clickStartAnalysisForRowContext(context);
    }

    /**
     * Finds first eligible row using text snapshots (Vue-safe) and scrolls the grid. Does not return
     * stale {@link WebElement} handles.
     */
    public Optional<MatchScheduleAnalysisRowContext> findFirstEligibleAnalysisRowContext() {
        return findEligibleAnalysisRowContext(Optional.empty());
    }

    public Optional<MatchScheduleAnalysisRowContext> findEligibleAnalysisRowContext(
            Optional<String> targetExternalEventId) {
        waitForScheduleRowsSettled();
        WebElement root = resolveScheduleRoot();
        for (int scrollPass = 0; scrollPass <= 14; scrollPass++) {
            Optional<MatchScheduleAnalysisRowContext> found =
                    scanVisibleRowsForEligibleContext(targetExternalEventId);
            if (found.isPresent()) {
                scrollIntoView(findRowElementForContext(found.get()).orElse(root));
                return found;
            }
            scrollScheduleViewportDown(root);
            waitForScheduleRowsSettled();
        }
        return Optional.empty();
    }

    /**
     * Read-only schedule scan (includes rows already analyzed — no «بدء تحليل» required). Matches
     * {@code externalEventId} on any row link, or home/away/score text when provided.
     */
    public Optional<MatchScheduleAnalysisRowContext> findRowContextByFixtureIdentityReadOnly(
            String externalEventId, String homeTeam, String awayTeam, String scoreDisplay) {
        waitForScheduleRowsSettled();
        WebElement root = resolveScheduleRoot();
        String extNeedle = externalEventId == null ? "" : externalEventId.trim();
        for (int scrollPass = 0; scrollPass <= 14; scrollPass++) {
            Optional<MatchScheduleAnalysisRowContext> found =
                    scanVisibleRowsForFixtureIdentity(extNeedle, homeTeam, awayTeam, scoreDisplay);
            if (found.isPresent()) {
                scrollIntoView(findRowElementForContext(found.get()).orElse(root));
                return found;
            }
            scrollScheduleViewportDown(root);
            waitForScheduleRowsSettled();
        }
        return Optional.empty();
    }

    /** Waits for «بدء تحليل المباراة» on the scoped row (same locators as click). */
    public void waitForStartAnalysisActionVisibleForContext(MatchScheduleAnalysisRowContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context required");
        }
        waitForScheduleRowsSettled();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(22))
                    .until(
                            d -> {
                                Optional<WebElement> action = findStartAnalysisActionForContext(context);
                                if (action.isEmpty()) {
                                    return false;
                                }
                                WebElement rowHost = resolveRowHost(action.get());
                                scrollIntoView(rowHost);
                                hoverRow(rowHost);
                                WebElement link = action.get();
                                return link.isEnabled()
                                        && (link.isDisplayed()
                                                || hasAnalysisCreateHref(link));
                            });
        } catch (TimeoutException timeout) {
            throw new IllegalStateException(
                    "«"
                            + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                            + "» not visible for schedule row: "
                            + context.rowText(),
                    timeout);
        }
    }

    public boolean isStartAnalysisActionVisibleAfterHover(MatchScheduleAnalysisRowContext context) {
        try {
            waitForStartAnalysisActionVisibleForContext(context);
            return true;
        } catch (IllegalStateException notVisible) {
            return false;
        }
    }

    /** Immediate scan (no long wait) — used after submit when «بدء تحليل» must be absent. */
    public boolean lacksStartAnalysisAction(MatchScheduleAnalysisRowContext context) {
        return findStartAnalysisActionForContext(context).isEmpty();
    }

    /** Hover a played «غير محللة» row until «بدء تحليل المباراة» is interactable. */
    public MatchSchedulePage hoverNotAnalyzedRowToRevealStartAnalysis(MatchScheduleAnalysisRowContext context) {
        waitForStartAnalysisActionVisibleForContext(context);
        return this;
    }

    private static boolean hasAnalysisCreateHref(WebElement action) {
        String href = action.getAttribute("href");
        return href != null && href.contains("/analysis/create");
    }

    public MatchSchedulePage clickStartAnalysisForRowContext(MatchScheduleAnalysisRowContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context required");
        }
        waitForScheduleRowsSettled();
        scrollIntoView(findRowElementForContext(context).orElse(resolveScheduleRoot()));
        clickStartAnalysisActionScopedToContext(context);
        return this;
    }

    public MatchSchedulePage clickStartAnalysisOnRow(WebElement row) {
        clickRowActionWithStaleRetry(row, MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS);
        return this;
    }

    /** @deprecated Prefer {@link #findFirstEligibleAnalysisRowContext()} to avoid stale rows. */
    @Deprecated
    public Optional<WebElement> findFirstEligibleAnalysisRowElement() {
        return findFirstEligibleAnalysisRowContext().flatMap(this::findRowElementForContext);
    }

    public MatchScheduleAnalysisRowContext captureAnalysisRowContext(WebElement row) {
        String text = readRowTextSafely(row);
        return buildAnalysisRowContextFromText(text, readExternalEventIdFromRow(row));
    }

    public String readOfficialMatchIdFromRow(WebElement row) {
        if (row == null) {
            return "";
        }
        return MatchScheduleRowRules.extractFirstMatchIdNumeric(readRowTextSafely(row)).orElse("");
    }

    public MatchScheduleAnalysisRowContext buildAnalysisRowContextFromText(String text) {
        return buildAnalysisRowContextFromText(text, "");
    }

    public MatchScheduleAnalysisRowContext buildAnalysisRowContextFromText(
            String text, String externalEventId) {
        String normalized = text == null ? "" : text.trim();
        int[] score = MatchScheduleRowRules.parseFirstScore(normalized);
        int home = score.length == 2 ? score[0] : -1;
        int away = score.length == 2 ? score[1] : -1;
        String scoreDisplay = score.length == 2 ? home + "-" + away : "";
        return new MatchScheduleAnalysisRowContext(
                normalized,
                readAnalysisStatusOnRow(normalized),
                MatchScheduleRowRules.extractFirstMatchIdNumeric(normalized).orElse(""),
                externalEventId == null ? "" : externalEventId.trim(),
                home,
                away,
                scoreDisplay,
                MatchScheduleRowRules.extractDateToken(normalized),
                MatchScheduleRowRules.isPlayedRow(normalized));
    }

    private String readExternalEventIdFromRow(WebElement row) {
        String fromStart = readExternalEventIdFromRowLinks(row, findStartAnalysisActions(row));
        if (!fromStart.isBlank()) {
            return fromStart;
        }
        return readExternalEventIdFromRowLinks(row, row.findElements(By.cssSelector("a[href*='externalEventId=']")));
    }

    private static String readExternalEventIdFromRowLinks(
            WebElement row, List<WebElement> links) {
        for (WebElement link : links) {
            String href = link.getAttribute("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            Matcher matcher = Pattern.compile("externalEventId=(\\d+)").matcher(href);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private Optional<MatchScheduleAnalysisRowContext> scanVisibleRowsForFixtureIdentity(
            String externalEventId, String homeTeam, String awayTeam, String scoreDisplay) {
        for (int staleAttempt = 0; staleAttempt < 4; staleAttempt++) {
            try {
                for (WebElement row : locateStableRowElements()) {
                    String text = readRowTextSafely(row);
                    if (!looksLikeScheduleRow(text)) {
                        continue;
                    }
                    String ext = readExternalEventIdFromRow(row);
                    boolean extMatch =
                            !externalEventId.isBlank() && externalEventId.equals(ext);
                    boolean teamMatch =
                            homeTeam != null
                                    && awayTeam != null
                                    && !homeTeam.trim().isBlank()
                                    && !awayTeam.trim().isBlank()
                                    && text.contains(homeTeam.trim())
                                    && text.contains(awayTeam.trim());
                    boolean scoreMatch =
                            scoreDisplay == null
                                    || scoreDisplay.isBlank()
                                    || text.contains(scoreDisplay.trim());
                    if (!externalEventId.isBlank()) {
                        if (extMatch || (teamMatch && scoreMatch)) {
                            return Optional.of(captureAnalysisRowContext(row));
                        }
                        continue;
                    }
                    if (teamMatch && scoreMatch) {
                        return Optional.of(captureAnalysisRowContext(row));
                    }
                }
                return Optional.empty();
            } catch (StaleElementReferenceException ignored) {
                // re-scan
            }
        }
        return Optional.empty();
    }

    private Optional<MatchScheduleAnalysisRowContext> scanVisibleRowsForEligibleContext(
            Optional<String> targetExternalEventId) {
        for (int staleAttempt = 0; staleAttempt < 4; staleAttempt++) {
            try {
                for (WebElement row : locateStableRowElements()) {
                    String text = readRowTextSafely(row);
                    if (!looksLikeScheduleRow(text)) {
                        continue;
                    }
                    if (!isEligibleAnalysisRowText(text)) {
                        continue;
                    }
                    if (!hasStartAnalysisControl(row)) {
                        continue;
                    }
                    MatchScheduleAnalysisRowContext context = captureAnalysisRowContext(row);
                    if (targetExternalEventId.isPresent()) {
                        String needle = targetExternalEventId.get().trim();
                        if (!needle.isBlank()
                                && !needle.equals(context.externalEventId())
                                && !needle.equals(context.matchIdNumeric())) {
                            continue;
                        }
                    }
                    return Optional.of(context);
                }
                return Optional.empty();
            } catch (StaleElementReferenceException ignored) {
                // Vue re-render — re-scan
            }
        }
        return Optional.empty();
    }

    private boolean isEligibleAnalysisRowText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (!MatchScheduleRowRules.isPlayedRow(text)) {
            return false;
        }
        if (MatchScheduleRowRules.appearsFriendlyMatch(text)) {
            return false;
        }
        String status = readAnalysisStatusOnRow(text);
        if (!status.contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED)) {
            return false;
        }
        return true;
    }

    private boolean hasStartAnalysisControl(WebElement row) {
        return !findStartAnalysisActions(row).isEmpty();
    }

    /** One schedule row with the start-analysis availability the user would get after hovering it. */
    public record RowActionAvailability(
            String rowText,
            boolean played,
            String analysisStatus,
            boolean startAnalysisAvailable,
            boolean viewDetailsAvailable) {

        public String shortLabel() {
            String single = rowText == null ? "" : rowText.replaceAll("\\s+", " ").trim();
            return single.length() > 90 ? single.substring(0, 90) + "…" : single;
        }
    }

    /**
     * Read-only scan of up to {@code maxRows} distinct rows accepted by {@code rowTextFilter}: hovers each row (the action only appears on hover) and
     * records whether «بدء تحليل المباراة» / «عرض التفاصيل» are present. Never clicks anything.
     */
    public List<RowActionAvailability> scanRowActionAvailability(
            int maxRows, Predicate<String> rowTextFilter) {
        waitForScheduleRowsSettled();
        WebElement root = resolveScheduleRoot();
        List<RowActionAvailability> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Only rows accepted by the filter are hovered (cheap text check first), so long schedules are scanned fully.
        for (int scrollPass = 0; scrollPass <= 40 && result.size() < maxRows; scrollPass++) {
            for (WebElement row : listScheduleDataRows()) {
                try {
                    String text = readRowTextSafely(row);
                    if (!seen.add(text) || result.size() >= maxRows || !rowTextFilter.test(text)) {
                        continue;
                    }
                    scrollIntoView(row);
                    hoverRow(row);
                    result.add(
                            new RowActionAvailability(
                                    text,
                                    MatchScheduleRowRules.isPlayedRow(text),
                                    readAnalysisStatusOnRow(text),
                                    hasStartAnalysisControl(row),
                                    isViewDetailsVisible(row)));
                } catch (StaleElementReferenceException staleRow) {
                    // Vue re-rendered the grid; the next pass re-reads this row.
                }
            }
            scrollScheduleViewportDown(root);
            waitForScheduleRowsSettled();
        }
        return result;
    }

    private Optional<WebElement> findRowElementForContext(MatchScheduleAnalysisRowContext context) {
        if (context == null) {
            return Optional.empty();
        }
        if (!context.externalEventId().isBlank()) {
            for (WebElement row : locateStableRowElements()) {
                if (context.externalEventId().equals(readExternalEventIdFromRow(row))) {
                    return Optional.of(row);
                }
            }
            for (WebElement row : locateStableRowElements()) {
                for (WebElement action : findStartAnalysisActions(row)) {
                    String href = action.getAttribute("href");
                    if (href != null
                            && href.contains("externalEventId=" + context.externalEventId())) {
                        return Optional.of(row);
                    }
                }
            }
        }
        if (!context.matchIdNumeric().isBlank()) {
            Optional<WebElement> byId = getRowByMatchIdFragment(context.matchIdNumeric());
            if (byId.isPresent()) {
                return byId;
            }
        }
        String rowSignature = context.rowText() == null ? "" : context.rowText().trim();
        if (!rowSignature.isBlank()) {
            for (WebElement row : locateStableRowElements()) {
                String text = readRowTextSafely(row);
                if (text.equals(rowSignature)) {
                    return Optional.of(row);
                }
            }
        }
        if (!context.scoreDisplay().isBlank()) {
            for (WebElement row : locateStableRowElements()) {
                String text = readRowTextSafely(row);
                if (text.contains(context.scoreDisplay())
                        && text.contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED)
                        && (rowSignature.isBlank() || text.contains(rowSignature.substring(0, Math.min(24, rowSignature.length()))))) {
                    return Optional.of(row);
                }
            }
        }
        return Optional.empty();
    }

    /** Deep link (href) of the «بدء تحليل المباراة» control of a row, e.g. /analysis/create?externalEventId=123. */
    public Optional<String> readStartAnalysisHrefForContext(MatchScheduleAnalysisRowContext context) {
        return findStartAnalysisActionForContext(context)
                .map(action -> action.getAttribute("href"))
                .filter(href -> href != null && !href.isBlank());
    }

    private Optional<WebElement> findStartAnalysisActionForContext(MatchScheduleAnalysisRowContext context) {
        Optional<WebElement> row = findRowElementForContext(context);
        if (row.isPresent()) {
            scrollIntoView(row.get());
            hoverRow(row.get());
            for (WebElement action : findStartAnalysisActions(row.get())) {
                if (action.isEnabled()) {
                    return Optional.of(action);
                }
            }
        }
        for (By locator : startAnalysisLocatorsForContext(context)) {
            for (WebElement action : driver.findElements(locator)) {
                if (action.isEnabled()) {
                    return Optional.of(action);
                }
            }
        }
        return Optional.empty();
    }

    private List<By> startAnalysisLocatorsForContext(MatchScheduleAnalysisRowContext context) {
        String actionFragment = MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT;
        List<By> locators = new ArrayList<>();
        if (!context.externalEventId().isBlank()) {
            locators.add(
                    By.xpath(
                            "//a[contains(@href,'externalEventId="
                                    + context.externalEventId()
                                    + "')]"
                                    + "[contains(@aria-label,'"
                                    + actionFragment
                                    + "')]"));
        }
        String matchId = context.matchIdNumeric();
        if (matchId != null && !matchId.isBlank()) {
            locators.add(
                    By.xpath(
                            "//article[contains(normalize-space(.),'#"
                                    + matchId
                                    + "') or contains(normalize-space(.),'مباراة #"
                                    + matchId
                                    + "')]"
                                    + startAnalysisActionXPathSuffix(actionFragment)));
            locators.add(
                    By.xpath(
                            "//*[@data-testid='match-schedule-row' or @data-testid='schedule-row']"
                                    + "[contains(normalize-space(.),'#"
                                    + matchId
                                    + "')]"
                                    + startAnalysisActionXPathSuffix(actionFragment)));
        }
        if (!context.scoreDisplay().isBlank()) {
            String scoreNeedle = context.scoreDisplay().replace("-", " - ");
            locators.add(
                    By.xpath(
                            "//article[contains(normalize-space(.),'"
                                    + context.scoreDisplay()
                                    + "') or contains(normalize-space(.),'"
                                    + scoreNeedle
                                    + "')]"
                                    + "[contains(normalize-space(.),'"
                                    + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                                    + "')]"
                                    + startAnalysisActionXPathSuffix(actionFragment)));
        }
        locators.add(
                By.xpath(
                        "//article[contains(normalize-space(.),'"
                                + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                                + "')]"
                                + startAnalysisActionXPathSuffix(actionFragment)));
        return locators;
    }

    private void clickStartAnalysisActionScopedToContext(MatchScheduleAnalysisRowContext context) {
        Optional<WebElement> row = findRowElementForContext(context);
        if (row.isPresent()) {
            scrollIntoView(row.get());
            hoverRow(row.get());
            try {
                clickStartAnalysisViaScriptInRow(row.get());
                return;
            } catch (IllegalStateException scriptMiss) {
                clickRowActionWithStaleRetry(
                        row.get(), MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS);
                waitForScheduleAnalysisNavigationAfterClick();
                return;
            }
        }
        Optional<WebElement> action = findStartAnalysisActionForContext(context);
        if (action.isPresent()) {
            WebElement rowHost = resolveRowHost(action.get());
            clickStartAnalysisElement(rowHost, action.get());
            return;
        }
        throw new IllegalStateException(
                "Could not locate start-analysis control for schedule row matchId="
                        + context.matchIdNumeric());
    }

    /** Clicks hover-revealed «بدء تحليل المباراة» inside the schedule row (Vue router link). */
    private void clickStartAnalysisViaScriptInRow(WebElement rowHost) {
        Object clicked =
                ((JavascriptExecutor) driver)
                        .executeScript(
                                """
                                const row = arguments[0];
                                const fragment = arguments[1];
                                const nodes = row.querySelectorAll(
                                    'a[href*="analysis/create"], a, button');
                                for (const el of nodes) {
                                  const href = el.href || el.getAttribute('href') || '';
                                  const label = ((el.getAttribute('aria-label') || '') + ' '
                                      + (el.innerText || el.textContent || '')).trim();
                                  if (href.includes('analysis/create')
                                      || label.includes(fragment)
                                      || label.includes('بدء تحليل')) {
                                    el.click();
                                    return true;
                                  }
                                }
                                return false;
                                """,
                                rowHost,
                                MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT);
        if (!Boolean.TRUE.equals(clicked)) {
            throw new IllegalStateException(
                    "Start-analysis control not found inside schedule row for script click");
        }
        waitForScheduleAnalysisNavigationAfterClick();
    }

    private static String startAnalysisActionXPathSuffix(String actionFragment) {
        return "//a[contains(@aria-label,'"
                + actionFragment
                + "') or contains(normalize-space(.),'"
                + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                + "')]"
                + "|//button[contains(@aria-label,'"
                + actionFragment
                + "') or contains(normalize-space(.),'"
                + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                + "')]";
    }

    private List<WebElement> locateStableRowElements() {
        // Round sections are page-level; the title-based root can resolve to the breadcrumb and miss them.
        List<WebElement> pageRounds = driver.findElements(By.cssSelector("section[id^='round-'] article"));
        if (!pageRounds.isEmpty()) {
            return pageRounds;
        }
        WebElement root = resolveScheduleRoot();
        List<WebElement> roundArticles =
                root.findElements(By.cssSelector("section[id^='round-'] article"));
        if (!roundArticles.isEmpty()) {
            return roundArticles;
        }
        List<WebElement> listArticles = root.findElements(By.cssSelector("ul article"));
        if (!listArticles.isEmpty()) {
            return listArticles;
        }
        List<WebElement> structured =
                root.findElements(
                        By.cssSelector("[data-testid='match-schedule-row'], [data-testid='schedule-row']"));
        if (!structured.isEmpty()) {
            return structured;
        }
        return locateRowElements(root);
    }

    private static String readRowTextSafely(WebElement row) {
        if (row == null) {
            return "";
        }
        try {
            String text = row.getText();
            return text == null ? "" : text.trim();
        } catch (StaleElementReferenceException stale) {
            throw stale;
        }
    }

    private void scrollScheduleViewportDown(WebElement root) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy(0, Math.min(arguments[0].scrollHeight, 720));", root);
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 480);");
    }

    // --- US-FAP-ANL-SYNC (row status + actions; MCH-ANL pending methods above unchanged) ---

    public MatchSchedulePage openScheduleAndRefresh() {
        openDirectly();
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public List<WebElement> listScheduleDataRows() {
        waitForScheduleRowsSettled();
        List<WebElement> rows = new ArrayList<>();
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                rows.clear();
                for (WebElement candidate : locateStableRowElements()) {
                    if (!candidate.isDisplayed()) {
                        continue;
                    }
                    String text = readRowTextSafely(candidate);
                    if (looksLikeScheduleRow(text)) {
                        rows.add(candidate);
                    }
                }
                return rows;
            } catch (StaleElementReferenceException stale) {
                waitForScheduleRowsSettled();
            }
        }
        return rows;
    }

    public Optional<WebElement> getRowByMatchIdFragment(String matchIdFragment) {
        if (matchIdFragment == null || matchIdFragment.isBlank()) {
            return Optional.empty();
        }
        String needle = matchIdFragment.trim();
        WebElement root = resolveScheduleRoot();
        for (int scrollPass = 0; scrollPass <= 14; scrollPass++) {
            for (WebElement row : listScheduleDataRows()) {
                String text = row.getText() == null ? "" : row.getText();
                if (text.contains(needle) || text.contains("#" + needle)) {
                    return Optional.of(row);
                }
            }
            scrollScheduleViewportDown(root);
            waitForScheduleRowsSettled();
        }
        return Optional.empty();
    }

    public Optional<WebElement> findFirstRowWithAnalysisStatus(String statusFragment) {
        if (statusFragment == null || statusFragment.isBlank()) {
            return Optional.empty();
        }
        for (WebElement row : listScheduleDataRows()) {
            String status = readAnalysisStatusForRow(row);
            if (status.contains(statusFragment.trim())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public String readAnalysisStatusForRow(WebElement row) {
        if (row == null) {
            return "";
        }
        String fromText = readAnalysisStatusOnRow(row.getText());
        if (!fromText.isBlank()) {
            return fromText;
        }
        return readAnalysisStatusFromDom(row);
    }

    public boolean isStartAnalysisVisible(WebElement row) {
        return rowActionVisible(row, MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS);
    }

    public boolean isViewDetailsVisible(WebElement row) {
        return rowActionVisible(row, MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DETAILS);
    }

    public boolean isViewDashboardVisible(WebElement row) {
        return rowActionVisible(row, MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DASHBOARD);
    }

    public MatchSchedulePage clickViewDetailsOnRow(WebElement row) {
        clickRowAction(row, MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DETAILS);
        return this;
    }

    public MatchSchedulePage clickViewDashboardOnRow(WebElement row) {
        clickRowAction(row, MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DASHBOARD);
        return this;
    }

    public Optional<MatchResultScores> parseMatchResultScores(WebElement row) {
        if (row == null) {
            return Optional.empty();
        }
        int[] parsed = MatchScheduleRowRules.parseFirstScore(row.getText());
        if (parsed.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new MatchResultScores(parsed[0], parsed[1], parsed[0] + "-" + parsed[1]));
    }

    public record MatchResultScores(int homeGoals, int awayGoals, String raw) {}

    public String resolveAnalysisStatusForContext(MatchScheduleAnalysisRowContext context) {
        if (context == null) {
            return "";
        }
        if (context.analysisStatus() != null && !context.analysisStatus().isBlank()) {
            return context.analysisStatus();
        }
        Optional<WebElement> row = findRowElementForContext(context);
        if (row.isEmpty()) {
            return "";
        }
        String fromDom = readAnalysisStatusFromDom(row.get());
        if (!fromDom.isBlank()) {
            return fromDom;
        }
        return readAnalysisStatusOnRow(readRowTextSafely(row.get()));
    }

    private String readAnalysisStatusFromDom(WebElement row) {
        String rowText = row.getText() == null ? "" : row.getText();
        if (rowText.contains(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED)) {
            return MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED;
        }
        if (rowText.contains(MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS)) {
            return MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS;
        }
        if (rowText.contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED)) {
            return MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED;
        }
        return "";
    }

    private boolean rowActionVisible(WebElement row, String label) {
        if (row == null || label == null || label.isBlank()) {
            return false;
        }
        if (MatchScheduleAnalysisStatusMessages.isStartAnalysisActionLabel(label)
                || label.contains(MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT)) {
            return hasStartAnalysisControl(row);
        }
        // row actions («عرض التفاصيل» / «عرض لوحة التحليل») may only render while the row is hovered
        if (findRowActions(row, label).stream().noneMatch(WebElement::isEnabled)) {
            hoverRow(row);
        }
        for (WebElement action : findRowActions(row, label)) {
            if (action.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private void clickRowAction(WebElement row, String label) {
        clickRowActionWithStaleRetry(row, label);
    }

    private void clickRowActionWithStaleRetry(WebElement row, String label) {
        StaleElementReferenceException lastStale = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                scrollIntoView(row);
                hoverRow(row); // row actions may only render while the row is hovered
                boolean startAnalysis = MatchScheduleAnalysisStatusMessages.isStartAnalysisActionLabel(label)
                        || label.contains(MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT);
                WebElement target;
                try {
                    target = new WebDriverWait(driver, Duration.ofSeconds(20))
                            .until(d -> {
                                for (WebElement action : findRowActions(row, label)) {
                                    if (action.isEnabled()) {
                                        return action;
                                    }
                                }
                                // only «بدء تحليل المباراة» may fall back to its alternative markup; never click it
                                // in place of another action (it opens the create flow)
                                if (startAnalysis) {
                                    for (WebElement action : findStartAnalysisActions(row)) {
                                        if (action.isEnabled()) {
                                            return action;
                                        }
                                    }
                                }
                                return null;
                            });
                } catch (TimeoutException absent) {
                    throw new IllegalStateException("Row action «" + label + "» not shown on row: "
                            + readRowTextSafely(row).replace('\n', ' '), absent);
                }
                WebElement rowHost = resolveRowHost(target);
                if (startAnalysis) {
                    clickStartAnalysisElement(rowHost, target);
                } else {
                    scrollIntoView(target);
                    PacedElementInteraction.click(target);
                }
                return;
            } catch (StaleElementReferenceException stale) {
                lastStale = stale;
            }
        }
        if (lastStale != null) {
            throw lastStale;
        }
        throw new IllegalStateException("Row action not clickable: " + label);
    }

    private WebElement resolveRowHost(WebElement action) {
        List<WebElement> articles = action.findElements(By.xpath("./ancestor::article[1]"));
        if (!articles.isEmpty()) {
            return articles.getFirst();
        }
        List<WebElement> rows =
                action.findElements(
                        By.xpath(
                                "./ancestor::*[@data-testid='match-schedule-row' or @data-testid='schedule-row'][1]"));
        if (!rows.isEmpty()) {
            return rows.getFirst();
        }
        return action;
    }

    private void clickStartAnalysisElement(WebElement rowHost, WebElement action) {
        scrollIntoView(rowHost);
        hoverRow(rowHost);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(
                            d -> {
                                hoverRow(rowHost);
                                return action.isDisplayed();
                            });
        } catch (TimeoutException hoverOnlyControl) {
            // Control may stay CSS-hidden until hover; JS click still triggers router navigation.
        }
        try {
            new Actions(driver)
                    .moveToElement(rowHost)
                    .pause(Duration.ofMillis(350))
                    .moveToElement(action)
                    .pause(Duration.ofMillis(250))
                    .click()
                    .perform();
        } catch (ElementNotInteractableException notInteractable) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", action);
        }
        waitForScheduleAnalysisNavigationAfterClick();
    }

    /** After «بدء تحليل المباراة», SPA routes to full-page create or opens modal form. */
    private void waitForScheduleAnalysisNavigationAfterClick() {
        By createForm = By.id("analysis-create-form");
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(
                        d ->
                                d.getCurrentUrl().contains("/analysis/create")
                                        || (!d.findElements(createForm).isEmpty()
                                                && d.findElement(createForm).isDisplayed()));
    }

    private List<WebElement> findStartAnalysisActions(WebElement row) {
        if (row == null) {
            return List.of();
        }
        String fragment = MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT;
        return row.findElements(
                By.cssSelector(
                        "a[aria-label*='"
                                + fragment
                                + "'], button[aria-label*='"
                                + fragment
                                + "'],"
                                + " a[aria-label='"
                                + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                                + "'],"
                                + " button[aria-label='"
                                + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS
                                + "']"));
    }

    private List<WebElement> findRowActions(WebElement row, String label) {
        By scoped =
                By.xpath(
                        ".//button[contains(normalize-space(.),'"
                                + label
                                + "') or contains(@aria-label,'"
                                + label
                                + "')]"
                                + "|.//a[contains(normalize-space(.),'"
                                + label
                                + "') or contains(@aria-label,'"
                                + label
                                + "')]");
        return row.findElements(scoped);
    }

    private void hoverRow(WebElement row) {
        new Actions(driver).moveToElement(row).pause(Duration.ofMillis(400)).perform();
    }

    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    private void waitForScheduleRowsSettled() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        // The portal flashes «لا توجد مباريات» before the rounds arrive, so only rendered rows end the wait early,
        // and only once the visible row count stops changing between two polls (Vue renders rounds in batches).
        int[] lastCount = {-1};
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(700))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        if (isLoadingStateVisible()) {
                            return false;
                        }
                        int count = visibleRoundRowTexts().size();
                        if (count == 0) {
                            count = locateStableRowElements().size();
                        }
                        boolean stable = count > 0 && count == lastCount[0];
                        lastCount[0] = count;
                        return stable;
                    });
        } catch (TimeoutException ignored) {
            // filter may legitimately yield empty — callers assert
        }
    }

    private WebElement resolveScheduleRoot() {
        if (!driver.findElements(SCHEDULE_ROOT).isEmpty()) {
            return driver.findElement(SCHEDULE_ROOT);
        }
        if (!driver.findElements(PAGE_TITLE).isEmpty()) {
            WebElement title = driver.findElement(PAGE_TITLE);
            List<WebElement> section = title.findElements(By.xpath("./ancestor::section[1]"));
            if (!section.isEmpty()) {
                return section.getFirst();
            }
        }
        return driver.findElement(By.tagName("body"));
    }

    private List<WebElement> locateRowElements(WebElement root) {
        List<WebElement> structured =
                root.findElements(
                        By.cssSelector(
                                "[data-testid='match-schedule-row'],"
                                        + " [data-testid='schedule-row'],"
                                        + " tr[data-testid*='match'],"
                                        + " tbody tr"));
        if (!structured.isEmpty()) {
            return structured;
        }
        return root.findElements(By.cssSelector("table tbody tr, [role='row'], article, li"));
    }

    private static boolean looksLikeScheduleRow(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains(MatchSchedulePageMessages.CLUB_FILTER_LABEL)
                && text.length() < 40) {
            return false;
        }
        if (text.contains(MatchSchedulePageMessages.PAGE_TITLE) && text.length() < 60) {
            return false;
        }
        return MatchScheduleRowRules.hasRecordedScore(text)
                || MatchScheduleRowRules.hasKickoffTime(text)
                || MatchScheduleRowRules.extractDateToken(text).length() > 0;
    }

    private static String extractKickoffTime(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = TIME_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private static WebElement firstDisplayed(WebElement root, By selector) {
        for (WebElement candidate : root.findElements(selector)) {
            if (candidate.isDisplayed()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean pageContains(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return false;
        }
        return driver.getPageSource().contains(fragment);
    }

    private void pendingDom(String step) {
        throw new UnsupportedOperationException(
                "Match Schedule analysis DOM not wired yet: "
                        + step
                        + ". Complete DOM review per docs/matchschedule/US-FAP-MCH-ANALYSIS_TEST_STRATEGY.md");
    }
}
