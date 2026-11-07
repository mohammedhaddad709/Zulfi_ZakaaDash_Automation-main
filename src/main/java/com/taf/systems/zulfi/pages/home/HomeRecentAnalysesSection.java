package com.taf.systems.zulfi.pages.home;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Home «آخر التحليلات» section. */
public final class HomeRecentAnalysesSection {

    private static final By SECTION_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span or self::p]"
                            + "[contains(normalize-space(.),'"
                            + HomeOverviewMessages.RECENT_SECTION_TITLE
                            + "')]");

    private final WebDriver driver;
    private final ElementActions actions;

    public HomeRecentAnalysesSection(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }

    public HomeRecentAnalysesSection waitUntilSectionVisible() {
        WebElement title =
                new WebDriverWait(driver, Duration.ofSeconds(25))
                        .until(ExpectedConditions.visibilityOfElementLocated(SECTION_TITLE));
        scrollIntoView(title);
        if (!isEmptyStateVisible() && !isLoadErrorVisible()) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(
                                d -> {
                                    scrollIntoView(title);
                                    return !readVisibleAnalysisCards().isEmpty();
                                });
            } catch (TimeoutException cardsSlow) {
                // Cards may hydrate after section title; callers assert card presence.
            }
        }
        return this;
    }

    public boolean isSectionTitleVisible() {
        return !driver.findElements(SECTION_TITLE).isEmpty()
                && driver.findElement(SECTION_TITLE).isDisplayed();
    }

    public boolean isEmptyStateVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.RECENT_EMPTY_TITLE)
                && HomeSectionSupport.pageContains(driver, HomeOverviewMessages.RECENT_EMPTY_DESCRIPTION);
    }

    public boolean isLoadErrorVisible() {
        return HomeSectionSupport.pageContains(driver, HomeOverviewMessages.RECENT_LOAD_ERROR);
    }

    public boolean isRetryVisible() {
        WebElement root = resolveSectionRoot();
        return root.getText().contains(HomeOverviewMessages.RETRY_ACTION);
    }

    public boolean isViewMoreVisible() {
        return !resolveSectionRoot().findElements(viewMoreInSection()).isEmpty();
    }

    public HomeRecentAnalysesSection clickViewMore() {
        // Scope to this section: Home has one «عرض المزيد» per section (results, upcoming, analyses).
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> resolveSectionRoot().findElements(viewMoreInSection()).stream()
                                .filter(WebElement::isDisplayed)
                                .findFirst()
                                .orElse(null));
        link.click();
        return this;
    }

    public HomeRecentAnalysesSection clickEmptyStateStartAnalysis() {
        By startInEmpty =
                By.xpath(
                        ".//button[contains(@aria-label,'"
                                + HomeOverviewMessages.HERO_PRIMARY_ACTION
                                + "')"
                                + " or contains(normalize-space(.),'"
                                + HomeOverviewMessages.HERO_PRIMARY_ACTION
                                + "')]");
        WebElement root = resolveSectionRoot();
        WebElement button =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> {
                            List<WebElement> hits = root.findElements(startInEmpty);
                            for (WebElement hit : hits) {
                                if (hit.isDisplayed() && hit.isEnabled()) {
                                    return hit;
                                }
                            }
                            return null;
                        });
        button.click();
        return this;
    }

    public List<RecentAnalysisCardSnapshot> readVisibleAnalysisCards() {
        WebElement root = resolveSectionRoot();
        List<WebElement> candidates =
                root.findElements(
                        By.cssSelector(
                                "[data-testid='home-recent-analysis-card'],"
                                        + " [data-testid='recent-analysis-card'],"
                                        + " article,"
                                        + " li"));
        List<RecentAnalysisCardSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        for (WebElement candidate : candidates) {
            if (!candidate.isDisplayed()) {
                continue;
            }
            String text = candidate.getText() == null ? "" : candidate.getText().trim();
            if (text.isBlank() || text.contains(HomeOverviewMessages.RECENT_SECTION_TITLE)) {
                continue;
            }
            if (!looksLikeAnalysisCard(text)) {
                continue;
            }
            String status = resolveStatus(text);
            snapshots.add(
                    new RecentAnalysisCardSnapshot(
                            index++,
                            text,
                            extractMatchTitle(candidate, text),
                            status,
                            hasVideoSourceIndicator(text),
                            hasAction(candidate, HomeOverviewMessages.ACTION_VIEW_DETAILS),
                            isActionEnabled(candidate, HomeOverviewMessages.ACTION_VIEW_DETAILS),
                            hasAction(candidate, HomeOverviewMessages.ACTION_VIEW_DASHBOARD),
                            isActionEnabled(candidate, HomeOverviewMessages.ACTION_VIEW_DASHBOARD)));
        }
        return snapshots;
    }

    public int countVisibleAnalysisCards() {
        return readVisibleAnalysisCards().size();
    }

    public boolean sectionContainsDeleteAction() {
        WebElement root = resolveSectionRoot();
        String sectionText = root.getText();
        return sectionText.contains(HomeOverviewMessages.DELETE_ACTION);
    }

    public String clickFirstEnabledViewDetails() {
        return clickActionOnFirstMatchingCard(HomeOverviewMessages.ACTION_VIEW_DETAILS, true);
    }

    /** Clicks «عرض التفاصيل» on the first visible card whose text includes {@code statusLabel}. */
    public String clickFirstEnabledViewDetailsForStatus(String statusLabel) {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement root = resolveSectionRoot();
        scrollIntoView(root);
        for (WebElement candidate : listAnalysisCardElements(root)) {
            scrollIntoView(candidate);
            if (!candidate.isDisplayed()) {
                continue;
            }
            String text = candidate.getText() == null ? "" : candidate.getText();
            if (statusLabel != null && !statusLabel.isBlank() && !text.contains(statusLabel)) {
                continue;
            }
            WebElement action = findActionElement(candidate, HomeOverviewMessages.ACTION_VIEW_DETAILS);
            if (action != null && isElementEnabled(action)) {
                String beforeUrl = driver.getCurrentUrl();
                clickWhenReady(action);
                return beforeUrl;
            }
        }
        // US-FAP-ANL-002/003: details stay reachable while processing — a disabled action is a product finding.
        throw new AssertionError("No enabled view-details for status «" + statusLabel + "» (details must be reachable"
                + " for every lifecycle status)");
    }

    public String clickFirstEnabledDashboard() {
        return clickActionOnFirstMatchingCard(HomeOverviewMessages.ACTION_VIEW_DASHBOARD, true);
    }

    private String clickActionOnFirstMatchingCard(String actionLabel, boolean requireEnabled) {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement root = resolveSectionRoot();
        scrollIntoView(root);
        List<WebElement> candidates = listAnalysisCardElements(root);
        for (WebElement candidate : candidates) {
            scrollIntoView(candidate);
            if (!candidate.isDisplayed()) {
                continue;
            }
            WebElement action = findActionElement(candidate, actionLabel);
            if (action == null) {
                continue;
            }
            if (requireEnabled && !isElementEnabled(action)) {
                continue;
            }
            String beforeUrl = driver.getCurrentUrl();
            clickWhenReady(action);
            return beforeUrl;
        }
        WebElement sectionAction = findFirstActionInSection(root, actionLabel, requireEnabled);
        if (sectionAction != null) {
            String beforeUrl = driver.getCurrentUrl();
            clickWhenReady(sectionAction);
            return beforeUrl;
        }
        if (HomeOverviewMessages.ACTION_VIEW_DETAILS.equals(actionLabel)) {
            WebElement detailsHref = findFirstAnalysisDetailsHrefInSection(root, requireEnabled);
            if (detailsHref != null) {
                String beforeUrl = driver.getCurrentUrl();
                clickWhenReady(detailsHref);
                return beforeUrl;
            }
            WebElement pageScoped = findViewDetailsNearRecentSectionTitle(requireEnabled);
            if (pageScoped != null) {
                String beforeUrl = driver.getCurrentUrl();
                clickWhenReady(pageScoped);
                return beforeUrl;
            }
            if (requireEnabled) {
                return clickActionOnFirstMatchingCard(actionLabel, false);
            }
        }
        throw new IllegalStateException("No clickable «" + actionLabel + "» in آخر التحليلات");
    }

    private WebElement findViewDetailsNearRecentSectionTitle(boolean requireEnabled) {
        By scoped =
                By.xpath(
                        "//*[contains(normalize-space(.),'"
                                + HomeOverviewMessages.RECENT_SECTION_TITLE
                                + "')]"
                                + "/following::*[contains(normalize-space(.),'"
                                + HomeOverviewMessages.ACTION_VIEW_DETAILS
                                + "') or contains(@aria-label,'"
                                + HomeOverviewMessages.ACTION_VIEW_DETAILS
                                + "')][1]"
                                + "/ancestor-or-self::*[self::a or self::button][1]");
        for (WebElement action : driver.findElements(scoped)) {
            if (!action.isDisplayed()) {
                continue;
            }
            if (requireEnabled && !isElementEnabled(action)) {
                continue;
            }
            return action;
        }
        return null;
    }

    private WebElement findFirstActionInSection(WebElement root, String actionLabel, boolean requireEnabled) {
        By anywhereInSection =
                By.xpath(
                        ".//*[self::a or self::button][contains(normalize-space(.),'"
                                + actionLabel
                                + "') or contains(@aria-label,'"
                                + actionLabel
                                + "')]"
                                + "|.//span[contains(@class,'p-button-label') and contains(normalize-space(.),'"
                                + actionLabel
                                + "')]/ancestor::*[self::a or self::button][1]");
        for (WebElement action : root.findElements(anywhereInSection)) {
            if (!action.isDisplayed()) {
                continue;
            }
            if (requireEnabled && !isElementEnabled(action)) {
                continue;
            }
            return action;
        }
        return null;
    }

    /** Fallback when CTA is a router link without visible label text on the anchor. */
    private static WebElement findFirstAnalysisDetailsHrefInSection(
            WebElement root, boolean requireEnabled) {
        for (WebElement link :
                root.findElements(
                        By.cssSelector(
                                "a[href*='/analysis/'], a[href*='analysis/']"))) {
            if (!link.isDisplayed()) {
                continue;
            }
            if (requireEnabled && !isElementEnabled(link)) {
                continue;
            }
            String href = link.getAttribute("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            if (href.contains("/analysis/create") || href.contains("عرض المزيد")) {
                continue;
            }
            if (href.matches("(?i).*/analysis/\\d+.*") || href.contains("/analysis/")) {
                return link;
            }
        }
        return null;
    }

    private List<WebElement> listAnalysisCardElements(WebElement root) {
        List<WebElement> merged = new ArrayList<>();
        for (By locator :
                List.of(
                        By.cssSelector("[data-testid='home-recent-analysis-card']"),
                        By.cssSelector("[data-testid='recent-analysis-card']"),
                        By.cssSelector("article"),
                        By.cssSelector("li"))) {
            merged.addAll(root.findElements(locator));
        }
        return merged;
    }

    private void clickWhenReady(WebElement action) {
        scrollIntoView(action);
        WebElement clickable =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(action));
        try {
            clickable.click();
        } catch (ElementClickInterceptedException intercepted) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", clickable);
        }
    }

    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    private WebElement resolveSectionRoot() {
        WebElement title = driver.findElement(SECTION_TITLE);
        List<String> ancestorXpaths =
                List.of(
                        "./ancestor::section[1]",
                        "./ancestor::*[@data-testid and contains(@data-testid,'recent')][1]",
                        "./ancestor::*[contains(@class,'recent')][1]",
                        "./ancestor::div[contains(@class,'card')][1]",
                        "./ancestor::div[2]",
                        "./ancestor::div[3]");
        for (String xpath : ancestorXpaths) {
            List<WebElement> found = title.findElements(By.xpath(xpath));
            if (found.isEmpty()) {
                continue;
            }
            WebElement candidate = found.getFirst();
            String text = candidate.getText();
            if (text != null
                    && text.contains(HomeOverviewMessages.RECENT_SECTION_TITLE)
                    && (text.contains(HomeOverviewMessages.ACTION_VIEW_DETAILS)
                            || text.contains(HomeOverviewMessages.STATUS_IN_PROGRESS)
                            || text.contains(HomeOverviewMessages.STATUS_COMPLETED))) {
                return candidate;
            }
        }
        return HomeSectionSupport.resolveSectionRoot(driver, SECTION_TITLE);
    }

    private By viewMoreInSection() {
        return By.xpath(
                ".//a[contains(normalize-space(.),'"
                        + HomeOverviewMessages.RECENT_VIEW_MORE
                        + "')]"
                        + "|.//button[contains(normalize-space(.),'"
                        + HomeOverviewMessages.RECENT_VIEW_MORE
                        + "')]");
    }

    private static boolean looksLikeAnalysisCard(String text) {
        return text.contains(HomeOverviewMessages.STATUS_IN_PROGRESS)
                || text.contains(HomeOverviewMessages.STATUS_COMPLETED)
                || text.contains(HomeOverviewMessages.ACTION_VIEW_DETAILS)
                || text.contains("YouTube")
                || text.contains("Google Drive")
                || text.contains("Drive");
    }

    private static String resolveStatus(String text) {
        if (text.contains(HomeOverviewMessages.STATUS_COMPLETED)) {
            return HomeOverviewMessages.STATUS_COMPLETED;
        }
        if (text.contains(HomeOverviewMessages.STATUS_IN_PROGRESS)) {
            return HomeOverviewMessages.STATUS_IN_PROGRESS;
        }
        return "";
    }

    private static boolean hasVideoSourceIndicator(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("youtube") || lower.contains("google drive") || lower.contains("drive");
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
            if (line.contains(HomeOverviewMessages.STATUS_IN_PROGRESS)
                    || line.contains(HomeOverviewMessages.STATUS_COMPLETED)
                    || line.contains(HomeOverviewMessages.ACTION_VIEW_DETAILS)) {
                continue;
            }
            return line.trim();
        }
        return "";
    }

    private static boolean hasAction(WebElement card, String label) {
        return findActionElement(card, label) != null;
    }

    private static boolean isActionEnabled(WebElement card, String label) {
        WebElement action = findActionElement(card, label);
        return action != null && isElementEnabled(action);
    }

    private static WebElement findActionElement(WebElement card, String label) {
        List<WebElement> hits =
                card.findElements(
                        By.xpath(
                                ".//a[contains(normalize-space(.),'"
                                        + label
                                        + "') or contains(@aria-label,'"
                                        + label
                                        + "')]"
                                        + "|.//button[contains(normalize-space(.),'"
                                        + label
                                        + "') or contains(@aria-label,'"
                                        + label
                                        + "')]"
                                        + "|.//*[@role='link' or @role='button'][contains(normalize-space(.),'"
                                        + label
                                        + "') or contains(@aria-label,'"
                                        + label
                                        + "')]"
                                        + "|.//span[contains(@class,'p-button-label') and contains(normalize-space(.),'"
                                        + label
                                        + "')]/ancestor::button[1]"
                                        + "|.//span[contains(@class,'p-button-label') and contains(normalize-space(.),'"
                                        + label
                                        + "')]/ancestor::a[1]"));
        for (WebElement hit : hits) {
            if (hit.isDisplayed()) {
                return hit;
            }
        }
        for (WebElement labelSpan :
                card.findElements(By.cssSelector(".p-button-label, [data-pc-section='label']"))) {
            if (!labelSpan.isDisplayed()) {
                continue;
            }
            String spanText = labelSpan.getText();
            if (spanText == null || !spanText.contains(label)) {
                continue;
            }
            try {
                WebElement host =
                        labelSpan.findElement(
                                By.xpath("./ancestor::*[self::a or self::button][1]"));
                if (host.isDisplayed()) {
                    return host;
                }
            } catch (NoSuchElementException ignored) {
                // label not inside button anchor
            }
        }
        for (WebElement link : card.findElements(By.cssSelector("a[href*='/analysis/']"))) {
            if (link.isDisplayed() && isElementEnabled(link)) {
                String text = link.getText() == null ? "" : link.getText();
                if (text.contains(label) || card.getText().contains(label)) {
                    return link;
                }
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
}
