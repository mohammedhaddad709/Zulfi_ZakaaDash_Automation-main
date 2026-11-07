package com.taf.systems.zulfi.pages.analysis;

import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.validations.AnalysisProcessingStatusMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Analysis Details view — US-FAP-ANL-002 / US-FAP-ANL-004 overlap (status + metadata read). */
public final class AnalysisDetailsPage {

    private static final Pattern ANALYSIS_ID_IN_URL =
            Pattern.compile("(?i)/analysis/(\\d+)(?:[/?#]|$)");

    private final WebDriver driver;

    public AnalysisDetailsPage(WebDriver driver) {
        this.driver = driver;
    }

    public AnalysisDetailsPage openById(long analysisId) {
        driver.navigate().to(PortalConfig.baseUrl() + "analysis/" + analysisId);
        return waitUntilLoaded();
    }

    public AnalysisDetailsPage waitUntilLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(
                        d -> {
                            String url = d.getCurrentUrl();
                            if (!ANALYSIS_ID_IN_URL.matcher(url).find()) {
                                return false;
                            }
                            String source = d.getPageSource();
                            return source.contains(AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS)
                                    || source.contains(AnalysisProcessingStatusMessages.STATUS_COMPLETED)
                                    || source.contains(AnalysisProcessingStatusMessages.BREADCRUMB_DETAILS)
                                    || source.contains(AnalysisProcessingStatusMessages.PROCESSING_TITLE)
                                    || source.contains(AnalysisProcessingStatusMessages.INTERACTIVE_DASHBOARD_LABEL);
                        });
        return this;
    }

    public AnalysisDetailsPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public Optional<Long> readAnalysisIdFromUrl() {
        Matcher matcher = ANALYSIS_ID_IN_URL.matcher(driver.getCurrentUrl());
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(matcher.group(1)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public boolean breadcrumbShowsApprovedTrail() {
        String page = visibleText();
        return page.contains(AnalysisProcessingStatusMessages.BREADCRUMB_ANALYSES)
                && page.contains(AnalysisProcessingStatusMessages.BREADCRUMB_DETAILS);
    }

    public boolean pageContainsLegacyBreadcrumbMarkers() {
        String page = visibleText();
        return page.contains(AnalysisProcessingStatusMessages.LEGACY_BREADCRUMB_MODELING)
                || page.contains(AnalysisProcessingStatusMessages.LEGACY_BREADCRUMB_VIDEO);
    }

    public String readLifecycleStatusLabel() {
        String page = visibleText();
        if (page.contains(AnalysisProcessingStatusMessages.STATUS_COMPLETED)) {
            return AnalysisProcessingStatusMessages.STATUS_COMPLETED;
        }
        if (page.contains(AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS)) {
            return AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS;
        }
        return "";
    }

    public boolean isProcessingTitleVisible() {
        return pageContains(AnalysisProcessingStatusMessages.PROCESSING_TITLE);
    }

    public boolean isProcessingDescriptionVisible() {
        return pageContains(AnalysisProcessingStatusMessages.PROCESSING_DESCRIPTION);
    }

    public boolean isInteractiveDashboardActionPresent() {
        return findActionByLabelFragment(AnalysisProcessingStatusMessages.INTERACTIVE_DASHBOARD_LABEL) != null
                || findActionByLabelFragment(AnalysisProcessingStatusMessages.ACTION_VIEW_DASHBOARD) != null
                || findActionByLabelFragment("لوحة التحليل") != null;
    }

    public boolean isInteractiveDashboardActionEnabled() {
        WebElement action = resolveDashboardAction();
        return action != null && isElementEnabled(action);
    }

    public boolean isPdfReportActionPresent() {
        return findActionByLabelFragment(AnalysisProcessingStatusMessages.PDF_REPORT_LABEL) != null
                || findActionByLabelFragment("PDF") != null
                || findActionByLabelFragment("تقرير التحليل") != null;
    }

    public boolean isPdfReportActionEnabled() {
        WebElement action = resolvePdfAction();
        return action != null && isElementEnabled(action);
    }

    public boolean pageExposesDeleteAction() {
        return visibleText().contains(AnalysisProcessingStatusMessages.DELETE_ACTION);
    }

    public boolean pageExposesFailedLifecycle() {
        String page = visibleText();
        return page.contains(AnalysisProcessingStatusMessages.FAILED_STATUS)
                || page.contains(AnalysisProcessingStatusMessages.RETRY_ANALYSIS_ACTION);
    }

    public String readMatchName() {
        return readLabeledValue(AnalysisProcessingStatusMessages.MATCH_NAME_LABEL)
                .or(() -> readLabeledValue(AnalysisProcessingStatusMessages.LEGACY_VIDEO_NAME_LABEL))
                .orElse("");
    }

    public String readVideoSource() {
        return readLabeledValue(AnalysisProcessingStatusMessages.VIDEO_SOURCE_LABEL).orElse("");
    }

    public String readVideoUrl() {
        Optional<String> labeled = readLabeledValue(AnalysisProcessingStatusMessages.VIDEO_URL_LABEL);
        if (labeled.isPresent() && !labeled.get().isBlank()) {
            return labeled.get();
        }
        for (WebElement link : driver.findElements(By.cssSelector("a[href*='youtu'], a[href*='drive.google']"))) {
            if (!link.isDisplayed()) {
                continue;
            }
            String href = link.getAttribute("href");
            if (href != null && !href.isBlank()) {
                return href.trim();
            }
        }
        return "";
    }

    public String readDescription() {
        return readLabeledValue(AnalysisProcessingStatusMessages.DESCRIPTION_LABEL).orElse("");
    }

    public String readSubmissionDate() {
        return readLabeledValue(AnalysisProcessingStatusMessages.SUBMISSION_DATE_LABEL).orElse("");
    }

    public boolean videoSourceMatchesUrl(String source, String url) {
        if (source == null || url == null || source.isBlank() || url.isBlank()) {
            return true;
        }
        String lowerSource = source.toLowerCase(Locale.ROOT);
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        boolean youtubeSource = lowerSource.contains("youtube") || lowerSource.contains("يوتيوب");
        boolean driveSource = lowerSource.contains("drive") || lowerSource.contains("جوجل");
        boolean youtubeUrl = lowerUrl.contains("youtu");
        boolean driveUrl = lowerUrl.contains("drive.google");
        if (youtubeSource && driveUrl) {
            return false;
        }
        if (driveSource && youtubeUrl && !driveUrl) {
            return false;
        }
        return true;
    }

    public boolean pageContainsDashboardOrPdfContentWhileProcessing() {
        if (!AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS.equals(readLifecycleStatusLabel())) {
            return false;
        }
        String lower = visibleText().toLowerCase(Locale.ROOT);
        return lower.contains("dashboard") && isInteractiveDashboardActionEnabled();
    }

    private WebElement resolveDashboardAction() {
        WebElement hit = findActionByLabelFragment(AnalysisProcessingStatusMessages.INTERACTIVE_DASHBOARD_LABEL);
        if (hit != null) {
            return hit;
        }
        hit = findActionByLabelFragment(AnalysisProcessingStatusMessages.ACTION_VIEW_DASHBOARD);
        if (hit != null) {
            return hit;
        }
        return findActionByLabelFragment("لوحة التحليل");
    }

    private WebElement resolvePdfAction() {
        WebElement hit = findActionByLabelFragment(AnalysisProcessingStatusMessages.PDF_REPORT_LABEL);
        if (hit != null) {
            return hit;
        }
        return findActionByLabelFragment("تقرير التحليل");
    }

    private WebElement findActionByLabelFragment(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return null;
        }
        List<WebElement> hits =
                driver.findElements(
                        By.xpath(
                                "//a[contains(normalize-space(.),'"
                                        + fragment
                                        + "')]"
                                        + "|//button[contains(normalize-space(.),'"
                                        + fragment
                                        + "')"
                                        + " or contains(@aria-label,'"
                                        + fragment
                                        + "')]"));
        for (WebElement hit : hits) {
            if (hit.isDisplayed()) {
                return hit;
            }
        }
        return null;
    }

    private Optional<String> readLabeledValue(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        List<WebElement> hits =
                driver.findElements(
                        By.xpath(
                                "//*[contains(normalize-space(.),'"
                                        + label
                                        + "')]/following::*[self::p or self::span or self::div or self::a][1]"));
        for (WebElement hit : hits) {
            if (!hit.isDisplayed()) {
                continue;
            }
            String text = hit.getText().trim();
            if (!text.isBlank() && !text.contains(label)) {
                return Optional.of(text);
            }
        }
        String page = visibleText();
        if (!page.contains(label)) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private boolean pageContains(String fragment) {
        return fragment != null && visibleText().contains(fragment);
    }

    private String visibleText() {
        WebElement body =
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        return body.getText();
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
