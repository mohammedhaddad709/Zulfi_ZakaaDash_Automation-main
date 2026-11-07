package com.taf.systems.zakaadash.pages.dashboard;

import com.taf.systems.zakaadash.pages.preprocessing.PreprocessingDialog;
import com.taf.systems.zakaadash.utils.AllureDomUtil;
import com.taf.systems.zakaadash.utils.ApplicationReadiness;
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.ScreenshotUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Wizard modal "إنشاء لوحة بيانات" opened from a processed data version row. */
public class DashboardCreationModal extends PreprocessingDialog {

    private static final String MODAL_TITLE = "إنشاء لوحة بيانات";
    private static final String STEP_DATA_ENTRY = "إدخال البيانات";
    private static final String STEP_GRAPHICS = "اختيار الرسومات";
    private static final String STEP_DESIGN = "تخصيص التصميم";
    private static final String NEXT = "التالي";
    private static final String DATA_SOURCE_LABEL = "مصدر البيانات";
    private static final String DASHBOARD_NAME_LABEL = "اسم لوحة البيانات";
    private static final String DASHBOARD_TYPE_LABEL = "نوع لوحة البيانات";
    private static final String DEPARTMENT_LABEL = "اختر القسم";
    private static final String JOB_ROLE_LABEL = "الدور الوظيفي";
    private static final String MAIN_INDICATORS_LABEL = "المؤشرات الرئيسية";
    private static final String GOALS_LABEL = "حدد أهدافك";
    private static final String DATA_SCOPE_LABEL = "نطاق البيانات";
    private static final String CHART_TYPES_LABEL = "أنواع الرسوم البيانية";
    private static final String SCOPE_TARGET = "تحليل البيانات والمؤشرات";
    private static final String CHART_BUBBLE = "مخطط فقاعي";
    private static final String CHART_HISTOGRAM = "المدرج التكراري";

    private static final int SCOPE_SCROLL_MAX_ATTEMPTS = 12;
    private static final int STEP2_SCROLL_MAX_PASSES = 24;
    private static final String CHART_EXPAND_LABEL = "عرض الكل";
    private static final String CHART_CARD_SELECTED_CLASS = "text-default";
    private static final String CHART_CARD_DESELECTED_CLASS = "text-neutral-500";
    /** Step 3 color customization (live wizard screenshot 2026-10-12). */
    private static final String DESIGN_PALETTE_TAB = "لوحات جاهزة";
    private static final String DESIGN_SUMMARY_LABEL = "ملخص لوحة البيانات";
    /** Final chart-selection stage controls (live DOM 2026-10-12 16:59 failure attachment). */
    private static final String FINAL_STAGE_SAVE_LABEL = RecommendedChartSelectionStage.SAVE_LABEL;
    private static final String FINAL_STAGE_STAR_ARIA_LABEL = RecommendedChartSelectionStage.STAR_ARIA_LABEL;
    /** Max wait after design Next for overlay to clear and recommended charts to appear. */
    private static final int DESIGN_SUBMIT_RESPONSE_WAIT_SECONDS = 60;

    private final JavascriptExecutor js;
    /** Set only during {@link #completeGraphicsAfterGoals()} for inner-content scrolling. */
    private WebElement step2ScrollContainer;
    /** Dashboard dialog that contains the goals field; set only during {@link #completeGraphicsAfterGoals()}. */
    private WebElement step2Modal;

    public DashboardCreationModal(WebDriver driver) {
        super(driver, MODAL_TITLE);
        this.js = (JavascriptExecutor) driver;
    }

    @Override
    protected String getDialogXpath() {
        return dashboardWizardShellXpath();
    }

    public DashboardCreationModal waitUntilLoaded() {
        wait.waitForVisible(By.xpath(getDialogXpath()));
        wait.waitForBlockingOverlayToDisappear("Dashboard wizard ready");
        return this;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(By.xpath(getDialogXpath()));
    }

    @Override
    public void waitUntilClosed() {
        wait.waitForInvisible(By.xpath(getDialogXpath()));
    }

    public void verifyDataSourceIsProcessedVersion(String processedVersionName) {
        wait.waitUntilTrue(
                driver -> readDropdownValue(DATA_SOURCE_LABEL).contains(processedVersionName),
                "Processed version not shown in data source field");
        String value = readDropdownValue(DATA_SOURCE_LABEL);
        if (!value.contains(processedVersionName)) {
            attachFailureDiagnostics("data-source-mismatch");
            Assert.fail("Expected data source '" + processedVersionName + "' but saw: " + value);
        }
    }

    public void completeDataEntryStep(
            String dashboardName,
            String dashboardType,
            String department,
            String jobRole,
            String mainIndicators) {
        waitForStepActive(STEP_DATA_ENTRY);
        fillTextAfterLabel(DASHBOARD_NAME_LABEL, dashboardName);
        selectDropdownOption(DASHBOARD_TYPE_LABEL, dashboardType);
        selectDropdownOption(DEPARTMENT_LABEL, department);
        waitForJobRoleOptionsLoaded();
        selectDropdownOption(JOB_ROLE_LABEL, jobRole);
        fillTextareaAfterLabel(MAIN_INDICATORS_LABEL, mainIndicators);
        verifyDataEntryValues(dashboardName, dashboardType, department, jobRole, mainIndicators);
        clickNextInModal();
        waitForGoalsFieldVisible();
    }

    public void enterGoals(String goalsText) {
        fillTextareaForGoals(goalsText);
        Assert.assertTrue(readGoalsText().contains(trimSnippet(goalsText, 40)),
                "Goals field should contain entered text");
    }

    /** Step 2 after goals: scope, charts, then Next — scrolls inside the modal content only. */
    public void completeGraphicsAfterGoals() {
        try {
            WebElement goals = requireFilledGoalsTextarea();
            step2Modal = closestDashboardShell(goals);
            if (step2Modal == null) {
                attachVisibleDialogsEvidence("step2-modal-missing");
                throw new NoSuchElementException("Dashboard dialog containing the goals field not found");
            }
            step2ScrollContainer = resolveScrollContainerFromGoals(goals);
            scrollModalContentUntilLabelInViewport(DATA_SCOPE_LABEL);
            selectDataScopeChip(SCOPE_TARGET);
            ZakaaDashLog.info("scope selected");
            scrollModalContentUntilExpandControlInViewport();
            expandChartOptionsIfCollapsed();
            ZakaaDashLog.info("charts expanded");
            scrollModalContentUntilChartCardInViewport(CHART_BUBBLE);
            ensureChartDeselected(CHART_BUBBLE);
            ZakaaDashLog.info("bubble unchecked");
            scrollModalContentUntilChartCardInViewport(CHART_HISTOGRAM);
            ensureChartDeselected(CHART_HISTOGRAM);
            ZakaaDashLog.info("histogram unchecked");
            int designKeywordsInStep2 = countDisplayedDesignKeywordElements();
            scrollModalContentToFooter();
            clickNextInModal();
            ZakaaDashLog.info("Next clicked");
            waitForDesignStepContent(designKeywordsInStep2);
            ZakaaDashLog.info("design step verified");
        } finally {
            step2ScrollContainer = null;
            step2Modal = null;
        }
    }

    public void selectDataScopeChip(String scopeLabel) {
        revealScopeChip(scopeLabel);
        WebElement chip = findScopeChipInViewport(scopeLabel);
        if (chip == null) {
            attachStep2FailureDiagnostics("scope-chip-not-in-viewport");
            Assert.fail("Scope chip not in modal viewport: " + scopeLabel);
        }
        if (!isScopeChipSelected(chip)) {
            scrollElementIntoModalViewport(chip);
            wait.waitForClickable(chip).click();
            HumanPaceUtil.pausePreprocessingAction();
        }
        WebElement selectedChip = findScopeChipInViewport(scopeLabel);
        Assert.assertTrue(selectedChip != null && isScopeChipSelected(selectedChip),
                "Scope chip should be selected: " + scopeLabel);
    }

    public void expandChartOptionsIfCollapsed() {
        WebElement expand = findChartExpandControl();
        if (expand == null) {
            ZakaaDashLog.info("Chart list already expanded (no '" + CHART_EXPAND_LABEL + "' control shown)");
            return;
        }
        int chartCardsBefore = allChartTypeCards().size();
        scrollElementIntoModalViewport(expand);
        wait.waitForClickable(expand).click();
        HumanPaceUtil.pausePreprocessingAction();
        wait.waitForBlockingOverlayToDisappear("Expand chart list");
        try {
            wait.waitUntilTrue(
                    driver -> findChartExpandControl() == null
                            || allChartTypeCards().size() > chartCardsBefore,
                    "Chart list should expand after clicking " + CHART_EXPAND_LABEL);
        } catch (TimeoutException ex) {
            attachStep2FailureDiagnostics("chart-expand-no-change");
            throw ex;
        }
    }

    public void ensureChartDeselected(String chartLabel) {
        String diagnosticId = chartDiagnosticId(chartLabel);
        List<String> otherChartStatesBefore = chartSelectionStatesOutside(chartLabel);
        WebElement card = waitForVisibleChartCard(chartLabel);
        scrollElementIntoModalViewport(card);

        ChartSelectionEvidence before = readChartSelectionEvidence(chartLabel, diagnosticId);
        attachChartToggleEvidence(diagnosticId, "before-click", before);
        if (before.state.equals("NOT_FOUND")) {
            throw new NoSuchElementException("Chart card not found in chart grid: " + chartLabel);
        }
        if (before.state.equals("UNKNOWN")) {
            Assert.fail("Cannot determine chart selection before click (" + diagnosticId + "): "
                    + before.marker + "\n" + before.controlOuterHtml);
        }
        if (before.state.equals("DESELECTED")) {
            Assert.assertEquals(chartSelectionStatesOutside(chartLabel), otherChartStatesBefore,
                    "Other chart selections should stay unchanged while unchecking: " + chartLabel);
            return;
        }

        WebElement control = findChartCardButton(chartLabel);
        if (control == null) {
            throw new NoSuchElementException("Chart card control missing before click: " + chartLabel);
        }
        wait.waitForClickable(control).click();
        HumanPaceUtil.pausePreprocessingAction();
        wait.waitForBlockingOverlayToDisappear("Deselect chart " + chartLabel);

        ChartSelectionEvidence afterClick = readChartSelectionEvidence(chartLabel, diagnosticId);
        attachChartToggleEvidence(diagnosticId, "after-one-click", afterClick);

        try {
            wait.waitUntilTrue(driver -> {
                ChartSelectionEvidence current = readChartSelectionEvidence(chartLabel, diagnosticId);
                if ("UNKNOWN".equals(current.state) || "NOT_FOUND".equals(current.state)) {
                    throw new IllegalStateException("Cannot determine chart selection while waiting ("
                            + diagnosticId + "): " + current.marker);
                }
                return "DESELECTED".equals(current.state);
            }, "Chart card should deselect after click: " + chartLabel);
        } catch (TimeoutException ex) {
            ChartSelectionEvidence stuck = readChartSelectionEvidence(chartLabel, diagnosticId);
            attachChartToggleEvidence(diagnosticId, "wait-timeout", stuck);
            throw ex;
        }

        ChartSelectionEvidence after = readChartSelectionEvidence(chartLabel, diagnosticId);
        if (!"DESELECTED".equals(after.state)) {
            Assert.fail("Chart should be deselected (" + diagnosticId + ") but read " + after.state
                    + " — " + after.marker + "\n" + after.controlOuterHtml);
        }
        Assert.assertEquals(chartSelectionStatesOutside(chartLabel), otherChartStatesBefore,
                "Other chart selections should stay unchanged while unchecking: " + chartLabel);
    }

    public void clickNextToDesignStep() {
        int designKeywordsInStep2 = countDisplayedDesignKeywordElements();
        scrollModalContentToFooter();
        clickNextInModal();
        waitForDesignStepContent(designKeywordsInStep2);
    }

    /** Step 3: keep the default palette, click Next, wait for the AI recommended-charts response. */
    public void submitDesignStepAndWaitForRecommendedCharts() {
        Assert.assertTrue(isDesignCustomizationPanelVisible(),
                "Design color customization should be visible before clicking Next");
        scrollModalContentToFooter();
        clickNextInModalWithoutOverlayWait();
        waitForRecommendedChartsResponse();
        Assert.assertTrue(isRecommendedChartsContentVisible(),
                "Recommended charts panel should be visible after design submission");
        ZakaaDashLog.info("recommended charts — VERIFIED " + probeFinalStageReadiness());
    }

    public void closeWizardHandlingDiscard() {
        WebElement close = wait.waitForClickable(inDialog(
                "//button[contains(@class,'p-dialog-header-close') or @aria-label='Close' or @aria-label='إغلاق']"));
        close.click();
        HumanPaceUtil.pauseNormalAction();
        confirmDiscardIfPresent();
        waitUntilClosed();
    }

    private void waitForDesignStepContent(int designKeywordsInStep2) {
        try {
            wait.waitUntilTrue(
                    driver -> isDesignStepActive(designKeywordsInStep2),
                    "Dashboard design step content should become active");
        } catch (TimeoutException ex) {
            attachStep2FailureDiagnostics("design-step-not-active");
            throw ex;
        }
        Assert.assertFalse(isGoalsFieldVisible(), "Step 2 goals field should not remain the active panel");
        Assert.assertTrue(isDesignStepActive(designKeywordsInStep2), "Step 3 customization controls should be visible");
    }

    /**
     * Step 3 is active when step-2 panels are gone and the color-customization controls from the live wizard
     * (palette tab or dashboard summary preview) are displayed.
     */
    private boolean isDesignStepActive(int designKeywordsInStep2) {
        return isDesignCustomizationPanelVisible() && isStepMarkerActive(STEP_DESIGN);
    }

    private boolean isDesignCustomizationPanelVisible() {
        if (isGoalsFieldVisible()
                || findFirstDisplayedInModal(By.xpath(labelXpath(DATA_SCOPE_LABEL))) != null
                || findFirstDisplayedInModal(By.xpath(labelXpath(CHART_TYPES_LABEL))) != null) {
            return false;
        }
        return findFirstDisplayedInModal(By.xpath(".//*[normalize-space(.)='" + DESIGN_PALETTE_TAB + "']")) != null
                || findFirstDisplayedInModal(By.xpath(labelXpath(DESIGN_SUMMARY_LABEL))) != null;
    }

    private void waitForRecommendedChartsResponse() {
        WebDriverWait responseWait = new WebDriverWait(driver,
                Duration.ofSeconds(DESIGN_SUBMIT_RESPONSE_WAIT_SECONDS));
        try {
            responseWait.withMessage("Recommended charts should appear within one minute after design Next")
                    .until(driver -> isRecommendedChartsContentVisible());
        } catch (TimeoutException ex) {
            attachRecommendedChartsTimeoutDiagnostics();
            throw ex;
        }
    }

    private void clickNextInModalWithoutOverlayWait() {
        WebElement next = findVisibleNextButton();
        scrollElementIntoModalViewport(next);
        wait.waitForClickable(next).click();
        HumanPaceUtil.pauseWizardTransition();
    }

    /**
     * Final chart-selection stage is ready when the current dialog has footer {@code حفظ} and at least
     * one chart card that owns BOTH an inclusion {@code role=checkbox} and a star {@code role=switch}
     * with readable {@code aria-checked}. Labels are compared in JS, not via CSS attribute selectors,
     * because generated titles vary and Unicode {@code aria-label} CSS matching was returning false
     * while the stage was already on screen (Allure 2026-10-12 17:07). Viewport coverage is not required.
     */
    private boolean isRecommendedChartsContentVisible() {
        Map<String, Object> probe = probeFinalStageReadiness();
        Object ready = probe.get("ready");
        return Boolean.TRUE.equals(ready) || "true".equalsIgnoreCase(String.valueOf(ready));
    }

    private Map<String, Object> probeFinalStageReadiness() {
        Object raw = js.executeScript("""
                const starLabel = arguments[0];
                const saveLabel = arguments[1];
                const clean = text => (text || '').replace(/\\s+/g, ' ').trim();
                const result = { ready: false, saveCount: 0, starCount: 0, pairCount: 0, dialogs: 0 };
                const dialogs = document.querySelectorAll(
                    '[role="dialog"], [aria-modal="true"], div.p-dialog.p-component');
                result.dialogs = dialogs.length;
                for (const dialog of dialogs) {
                    const rect = dialog.getBoundingClientRect();
                    if (rect.width < 120 || rect.height < 120) {
                        continue;
                    }
                    const saves = Array.from(dialog.querySelectorAll('button')).filter(button =>
                        clean(button.getAttribute('aria-label')) === saveLabel
                            || clean(button.textContent) === saveLabel);
                    const stars = Array.from(dialog.querySelectorAll('button[role="switch"]'))
                        .filter(button => [starLabel, arguments[2]].includes(clean(button.getAttribute('aria-label'))));
                    let pairs = 0;
                    for (const star of stars) {
                        let card = star.parentElement;
                        while (card && card !== dialog) {
                            const inclusion = Array.from(card.querySelectorAll('button[role="checkbox"]'))
                                .find(button => button !== star);
                            if (inclusion && card.contains(star) && card.contains(inclusion)) {
                                const inc = inclusion.getAttribute('aria-checked');
                                const st = star.getAttribute('aria-checked');
                                if ((inc === 'true' || inc === 'false')
                                        && (st === 'true' || st === 'false')) {
                                    pairs++;
                                    break;
                                }
                            }
                            card = card.parentElement;
                        }
                    }
                    result.saveCount = Math.max(result.saveCount, saves.length);
                    result.starCount = Math.max(result.starCount, stars.length);
                    result.pairCount = Math.max(result.pairCount, pairs);
                    if (saves.length > 0 && pairs > 0) {
                        result.ready = true;
                        return result;
                    }
                }
                return result;
                """, FINAL_STAGE_STAR_ARIA_LABEL, FINAL_STAGE_SAVE_LABEL,
                RecommendedChartSelectionStage.STARRED_ARIA_LABEL);
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> probe = new java.util.LinkedHashMap<>();
            map.forEach((key, value) -> probe.put(String.valueOf(key), value));
            probe.put("overlayVisible", ApplicationReadiness.hasVisibleBlockingOverlay(driver));
            return probe;
        }
        return Map.of("ready", false, "error", String.valueOf(raw));
    }

    /** Final wizard stage: recommended chart groups, inclusion checkboxes, and star switches. */
    public void completeRecommendedChartSelectionStage(String dashboardName) {
        new RecommendedChartSelectionStage(driver).run(dashboardName);
    }

    private void attachRecommendedChartsTimeoutDiagnostics() {
        attachFailureDiagnostics("recommended-charts-timeout");
        AllureDomUtil.attachText("Final-stage readiness probe", probeFinalStageReadiness().toString());
        try {
            WebElement modal = modalRoot();
            Object snippet = js.executeScript("""
                    const modal = arguments[0];
                    const text = (modal.innerText || '').replace(/\\s+/g, ' ').trim();
                    return text.length > 2500 ? text.slice(0, 2500) + '...[truncated]' : text;
                    """, modal);
            AllureDomUtil.attachText("Step 3 recommended charts timeout — modal text", String.valueOf(snippet));
        } catch (RuntimeException ex) {
            ZakaaDashLog.info("Recommended charts timeout diagnostics skipped: " + ex.getMessage());
        }
        ScreenshotUtil.captureAndAttach(driver, "recommended-charts-timeout");
    }

    private int countDisplayedDesignKeywordElements() {
        String keywordMatch = "contains(normalize-space(.),'الألوان') or contains(normalize-space(.),'المظهر')"
                + " or contains(normalize-space(.),'الخلفية')";
        int count = 0;
        for (WebElement element : step2Modal().findElements(By.xpath(
                ".//*[(" + keywordMatch + ") and not(.//*[" + keywordMatch + "])]"))) {
            if (element.isDisplayed()) {
                count++;
            }
        }
        return count;
    }

    private void revealScopeChip(String scopeLabel) {
        for (int attempt = 1; attempt <= SCOPE_SCROLL_MAX_ATTEMPTS; attempt++) {
            if (findScopeChipInViewport(scopeLabel) != null) {
                return;
            }
            WebElement section = dataScopeSection();
            WebElement arrow = scopeScrollLeftArrow(section);
            if (arrow == null) {
                failScopeReveal("no left arrow found in the data-scope chip row (attempt " + attempt + ")", null);
            }
            String arrowHtml = AllureDomUtil.outerHtml(arrow);
            String chipPositionsBefore = scopeChipPositions(section);
            scrollElementIntoModalViewport(arrow);
            wait.waitForClickable(arrow).click();
            HumanPaceUtil.pausePreprocessingAction();
            if (!isStep2ModalOpen()) {
                AllureDomUtil.attachText("Step 2 clicked scope arrow", arrowHtml);
                attachVisibleDialogsEvidence("scope-arrow-closed-modal");
                Assert.fail("Dashboard modal closed after clicking the data-scope left arrow (attempt " + attempt + ")");
            }
            wait.waitForBlockingOverlayToDisappear("Scope chip scroll");
            String[] lastSeenPositions = {chipPositionsBefore};
            try {
                wait.waitUntilTrue(driver -> {
                    String current = scopeChipPositions(dataScopeSection());
                    boolean movedAndSettled = !current.equals(chipPositionsBefore) && current.equals(lastSeenPositions[0]);
                    lastSeenPositions[0] = current;
                    return movedAndSettled;
                }, "Data-scope chip row should move after clicking its left arrow");
            } catch (TimeoutException ex) {
                failScopeReveal("chip row did not move after clicking the left arrow (attempt " + attempt + ")", arrowHtml);
            }
        }
        if (findScopeChipInViewport(scopeLabel) == null) {
            failScopeReveal("chip still hidden after " + SCOPE_SCROLL_MAX_ATTEMPTS + " left-arrow clicks", null);
        }
    }

    private void failScopeReveal(String failedAction, String clickedArrowHtml) {
        attachStep2FailureDiagnostics("scope-chip-not-found");
        if (clickedArrowHtml != null) {
            AllureDomUtil.attachText("Step 2 clicked scope arrow", clickedArrowHtml);
        }
        try {
            WebElement section = dataScopeSection();
            AllureDomUtil.attachText("Step 2 data-scope section",
                    scopeChipPositions(section) + "\n" + AllureDomUtil.outerHtml(section));
        } catch (RuntimeException ex) {
            ZakaaDashLog.info("Step 2 data-scope section diagnostics skipped: " + ex.getMessage());
        }
        Assert.fail("Could not reveal scope chip '" + SCOPE_TARGET + "': " + failedAction);
    }

    /** Nearest block around the data-scope label that holds the chips (excludes goals above and charts below). */
    private WebElement dataScopeSection() {
        WebElement label = findFirstDisplayedInModal(By.xpath(labelXpath(DATA_SCOPE_LABEL)));
        if (label == null) {
            attachFailureDiagnostics("data-scope-label-missing");
            throw new NoSuchElementException("Data scope section label not visible");
        }
        List<WebElement> sections = label.findElements(By.xpath(
                "./ancestor::div[count(.//button) >= 2][not(.//textarea)]"
                        + "[not(.//*[contains(normalize-space(.),'" + CHART_TYPES_LABEL + "')])][1]"));
        if (sections.isEmpty()) {
            attachFailureDiagnostics("data-scope-section-missing");
            throw new NoSuchElementException("No chip container found around label " + DATA_SCOPE_LABEL);
        }
        return sections.get(0);
    }

    /** Horizontal scroller whose children are the PrimeVue chips ({@code data-pc-name='chip'}). */
    private WebElement scopeChipScroller(WebElement scopeSection) {
        return scopeSection.findElement(By.xpath(".//*[@data-pc-name='chip']/parent::div"));
    }

    /** Visible arrow button that is a sibling of the chip scroller and sits on its left half. */
    private WebElement scopeScrollLeftArrow(WebElement scopeSection) {
        WebElement chipScroller = scopeChipScroller(scopeSection);
        Rectangle scrollerRect = chipScroller.getRect();
        int scrollerCenterX = scrollerRect.getX() + scrollerRect.getWidth() / 2;
        for (WebElement button : chipScroller.findElements(By.xpath("../button"))) {
            Rectangle rect = button.getRect();
            if (button.isDisplayed() && rect.getX() + rect.getWidth() / 2 < scrollerCenterX) {
                return button;
            }
        }
        return null;
    }

    private String scopeChipPositions(WebElement scopeSection) {
        Object positions = js.executeScript("""
                const scroller = arguments[0];
                return scroller.scrollLeft + ' : ' + Array.from(scroller.querySelectorAll("[data-pc-name='chip']"))
                    .map(c => (c.innerText || '').trim() + '@' + Math.round(c.getBoundingClientRect().left))
                    .join(' | ');
                """, scopeChipScroller(scopeSection));
        return String.valueOf(positions);
    }

    private WebElement findVisibleScopeChip(String scopeLabel) {
        WebElement section = dataScopeSection();
        for (WebElement chip : section.findElements(By.xpath(
                ".//button[normalize-space(.)='" + scopeLabel + "']"
                        + " | .//*[@role='button'][normalize-space(.)='" + scopeLabel + "']"))) {
            if (chip.isDisplayed()) {
                return chip;
            }
        }
        return null;
    }

    /** Chip is inside the modal viewport and not clipped or covered at its centre. */
    private WebElement findScopeChipInViewport(String scopeLabel) {
        WebElement chip = findVisibleScopeChip(scopeLabel);
        if (chip != null && isWithinModalViewport(chip) && isTopmostAtCenter(chip)) {
            return chip;
        }
        return null;
    }

    private boolean isTopmostAtCenter(WebElement element) {
        Object topmost = js.executeScript("""
                const el = arguments[0];
                const r = el.getBoundingClientRect();
                const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
                return hit !== null && (hit === el || el.contains(hit));
                """, element);
        return Boolean.TRUE.equals(topmost);
    }

    private WebElement findChartExpandControl() {
        return findFirstDisplayedInModal(By.xpath(
                ".//button[contains(normalize-space(.),'" + CHART_EXPAND_LABEL + "')]"));
    }

    private WebElement waitForVisibleChartCard(String chartLabel) {
        try {
            wait.waitUntilTrue(driver -> findVisibleChartCard(chartLabel) != null,
                    "Chart card should be rendered: " + chartLabel);
        } catch (TimeoutException ex) {
            attachStep2FailureDiagnostics("chart-card-missing-" + chartLabel);
            throw ex;
        }
        return findVisibleChartCard(chartLabel);
    }

    /** PrimeVue chart card button in the chart-types grid, resolved live by exact title span. */
    private WebElement findVisibleChartCard(String chartLabel) {
        WebElement button = findChartCardButton(chartLabel);
        return button != null && button.isDisplayed() ? button : null;
    }

    private WebElement findChartCardButton(String chartLabel) {
        Object button = js.executeScript("""
                return findChartCardButton(arguments[0], arguments[1], arguments[2]);
                function findChartCardButton(modal, chartLabel, sectionLabel) {
                    const grid = chartTypesGrid(modal, sectionLabel);
                    if (!grid) return null;
                    return Array.from(grid.querySelectorAll("button[data-pc-name='button']"))
                        .find(btn => chartTitle(btn) === chartLabel) || null;
                }
                function chartTypesGrid(modal, sectionLabel) {
                    const labels = Array.from(modal.querySelectorAll('*')).filter(el => {
                        const text = (el.innerText || '').replace(/\\s+/g, ' ').trim();
                        return text === sectionLabel && !Array.from(el.children).some(child =>
                            (child.innerText || '').replace(/\\s+/g, ' ').trim() === sectionLabel);
                    });
                    if (!labels.length) return null;
                    const grid = modal.ownerDocument.evaluate(
                        "./following::div[contains(@class,'grid')][1]",
                        labels[0], null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    return grid;
                }
                function chartTitle(btn) {
                    const span = btn.querySelector('div.flex-col-center > span');
                    return span ? span.textContent.replace(/\\s+/g, ' ').trim() : '';
                }
                """, step2Modal(), chartLabel, CHART_TYPES_LABEL);
        return button instanceof WebElement webElement ? webElement : null;
    }

    private List<WebElement> allChartTypeCards() {
        WebElement label = findFirstDisplayedInModal(By.xpath(labelXpath(CHART_TYPES_LABEL)));
        if (label == null) {
            return List.of();
        }
        return label.findElements(By.xpath(
                "./following::div[contains(@class,'grid')][1]//button[@data-pc-name='button']")).stream()
                .filter(WebElement::isDisplayed)
                .toList();
    }

    private static String chartCardTitle(WebElement card) {
        return normalize(card.findElement(By.xpath(
                ".//div[contains(@class,'flex-col-center')]//span[normalize-space(.)!='']")).getText());
    }

    private static String chartDiagnosticId(String chartLabel) {
        if (CHART_BUBBLE.equals(chartLabel)) {
            return "bubble";
        }
        if (CHART_HISTOGRAM.equals(chartLabel)) {
            return "histogram";
        }
        return "chart";
    }

    /**
     * Reads the state the app itself puts on the chart card button: the card carries
     * {@code text-default} while selected and {@code text-neutral-500} while deselected
     * (captured card markup for مخطط فقاعي / المدرج التكراري across runs 15:08, 15:31 and 15:40).
     * The selection icon is the card's direct-child {@code svg}; the chart illustration sits deeper
     * inside {@code div.flex-col-center}, so neither element's tag name can decide the state.
     */
    private ChartSelectionEvidence readChartSelectionEvidence(String chartLabel, String diagnosticId) {
        Object raw = js.executeScript("""
                return readChartSelectionEvidence(
                    arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
                function findChartCardButton(modal, chartLabel, sectionLabel) {
                    const grid = chartTypesGrid(modal, sectionLabel);
                    if (!grid) return null;
                    return Array.from(grid.querySelectorAll("button[data-pc-name='button']"))
                        .find(btn => chartTitle(btn) === chartLabel) || null;
                }
                function chartTypesGrid(modal, sectionLabel) {
                    const labels = Array.from(modal.querySelectorAll('*')).filter(el => {
                        const text = (el.innerText || '').replace(/\\s+/g, ' ').trim();
                        return text === sectionLabel && !Array.from(el.children).some(child =>
                            (child.innerText || '').replace(/\\s+/g, ' ').trim() === sectionLabel);
                    });
                    if (!labels.length) return null;
                    return modal.ownerDocument.evaluate(
                        "./following::div[contains(@class,'grid')][1]",
                        labels[0], null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                }
                function chartTitle(btn) {
                    const span = btn.querySelector('div.flex-col-center > span');
                    return span ? span.textContent.replace(/\\s+/g, ' ').trim() : '';
                }
                function readChartSelectionEvidence(modal, chartLabel, sectionLabel,
                        selectedToken, deselectedToken) {
                    const btn = findChartCardButton(modal, chartLabel, sectionLabel);
                    if (!btn) {
                        return { chartName: chartLabel, state: 'NOT_FOUND', marker: 'no button in chart grid',
                            cardOuterHtml: '', controlOuterHtml: '' };
                    }
                    let state = 'UNKNOWN';
                    if (btn.classList.contains(deselectedToken)) {
                        state = 'DESELECTED';
                    } else if (btn.classList.contains(selectedToken)) {
                        state = 'SELECTED';
                    }
                    const indicator = btn.querySelector(':scope > svg');
                    return {
                        chartName: chartLabel,
                        state: state,
                        marker: 'cardStateClass=' + (state === 'UNKNOWN' ? 'none'
                                : state === 'DESELECTED' ? deselectedToken : selectedToken)
                            + '; indicator=' + (indicator
                                ? 'svg[' + (indicator.getAttribute('class') || '') + ']' : 'absent')
                            + '; cardClass=' + (btn.getAttribute('class') || ''),
                        cardOuterHtml: btn.outerHTML,
                        controlOuterHtml: btn.outerHTML
                    };
                }
                """, step2Modal(), chartLabel, CHART_TYPES_LABEL,
                CHART_CARD_SELECTED_CLASS, CHART_CARD_DESELECTED_CLASS);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Chart selection evidence unavailable for " + diagnosticId);
        }
        return new ChartSelectionEvidence(
                chartLabel,
                diagnosticId,
                Objects.toString(map.get("state"), "UNKNOWN"),
                Objects.toString(map.get("marker"), ""),
                Objects.toString(map.get("cardOuterHtml"), ""),
                Objects.toString(map.get("controlOuterHtml"), ""));
    }

    private void attachChartToggleEvidence(String diagnosticId, String phase, ChartSelectionEvidence evidence) {
        AllureDomUtil.attachText("Step 2 chart toggle — " + diagnosticId + " — " + phase,
                "chartName=" + evidence.chartName + "\n"
                        + "diagnosticId=" + evidence.diagnosticId + "\n"
                        + "state=" + evidence.state + "\n"
                        + "marker=" + evidence.marker + "\n"
                        + "control outerHTML:\n" + evidence.controlOuterHtml);
    }

    /** Selection state of every chart card except the one with {@code excludeLabel}. */
    private List<String> chartSelectionStatesOutside(String excludeLabel) {
        List<String> states = new ArrayList<>();
        for (WebElement card : allChartTypeCards()) {
            String title = chartCardTitle(card);
            if (!excludeLabel.equals(title)) {
                ChartSelectionEvidence evidence = readChartSelectionEvidence(title, chartDiagnosticId(title));
                if ("UNKNOWN".equals(evidence.state) || "NOT_FOUND".equals(evidence.state)) {
                    attachChartToggleEvidence(chartDiagnosticId(title), "peer-state-unreadable", evidence);
                    throw new IllegalStateException("Cannot read peer chart selection: " + title
                            + " — " + evidence.marker);
                }
                states.add(String.valueOf("SELECTED".equals(evidence.state)));
            }
        }
        return states;
    }

    private static final class ChartSelectionEvidence {
        private final String chartName;
        private final String diagnosticId;
        private final String state;
        private final String marker;
        private final String cardOuterHtml;
        private final String controlOuterHtml;

        private ChartSelectionEvidence(
                String chartName,
                String diagnosticId,
                String state,
                String marker,
                String cardOuterHtml,
                String controlOuterHtml) {
            this.chartName = chartName;
            this.diagnosticId = diagnosticId;
            this.state = state;
            this.marker = marker;
            this.cardOuterHtml = cardOuterHtml;
            this.controlOuterHtml = controlOuterHtml;
        }
    }

    private static String dashboardWizardShellXpath() {
        String markers = ".//*[contains(normalize-space(.),'" + DATA_SOURCE_LABEL + "')]"
                + " or .//*[contains(normalize-space(.),'" + DASHBOARD_NAME_LABEL + "')]"
                + " or .//*[contains(normalize-space(.),'" + GOALS_LABEL + "')]"
                + " or .//*[contains(normalize-space(.),'" + DATA_SCOPE_LABEL + "')]"
                + " or .//*[contains(normalize-space(.),'" + CHART_TYPES_LABEL + "')]"
                + " or .//button[contains(normalize-space(.),'" + NEXT + "')]"
                + " or .//textarea[contains(@placeholder,'هدف')]"
                // Final chart-selection stage has no 'التالي' and none of the step 1-2 fields.
                + " or .//button[@role='switch' and (@aria-label='" + FINAL_STAGE_STAR_ARIA_LABEL + "'"
                + " or @aria-label='" + RecommendedChartSelectionStage.STARRED_ARIA_LABEL + "')]";
        return "//div[contains(@class,'p-dialog') and contains(@class,'p-component')][" + markers + "]"
                + " | //*[@data-pc-name='dialog'][" + markers + "]"
                + " | //*[@role='dialog' or @aria-modal='true'][" + markers + "]";
    }

    /** Re-resolves the visible PrimeNG dashboard wizard shell (not preprocessing dialogs). */
    private WebElement modalRoot() {
        List<WebElement> shells = driver.findElements(By.xpath(getDialogXpath()));
        for (int i = shells.size() - 1; i >= 0; i--) {
            WebElement shell = shells.get(i);
            if (isDashboardShellCandidate(shell)) {
                return shell;
            }
        }
        attachVisibleDialogsEvidence("modal-root");
        throw new NoSuchElementException("Dashboard wizard shell not visible");
    }

    private WebElement findDisplayedGoalsTextarea() {
        for (WebElement area : driver.findElements(By.xpath(
                "//textarea[contains(@placeholder,'هدف') and not(@readonly) and not(@disabled)]"))) {
            if (!area.isDisplayed()) {
                continue;
            }
            String value = nullToEmpty(area.getAttribute("value"));
            if (value.isBlank()) {
                value = area.getText();
            }
            if (!value.isBlank()) {
                return area;
            }
        }
        return null;
    }

    private WebElement closestDashboardShell(WebElement goals) {
        Object shell = js.executeScript("""
                const t = arguments[0];
                if (!t) return null;
                return t.closest('div.p-dialog.p-component')
                    || t.closest('[data-pc-name=\"dialog\"]')
                    || t.closest('[role=\"dialog\"]');
                """, goals);
        return shell instanceof WebElement webElement ? webElement : null;
    }

    private WebElement resolveScrollContainerFromGoals(WebElement goals) {
        Object scroller = js.executeScript("""
                let node = arguments[0].parentElement;
                let best = null;
                let bestDelta = 0;
                while (node) {
                    const style = window.getComputedStyle(node);
                    const overflowY = style.overflowY;
                    if ((overflowY === 'auto' || overflowY === 'scroll')
                            && node.scrollHeight > node.clientHeight + 2) {
                        const delta = node.scrollHeight - node.clientHeight;
                        if (delta > bestDelta) {
                            bestDelta = delta;
                            best = node;
                        }
                    }
                    if (node.classList && node.classList.contains('p-dialog')) {
                        break;
                    }
                    node = node.parentElement;
                }
                return best;
                """, goals);
        if (scroller instanceof WebElement scrollElement) {
            return scrollElement;
        }
        WebElement shell = closestDashboardShell(goals);
        if (shell != null) {
            return shell.findElement(By.cssSelector(".p-dialog-content, [data-pc-section='content']"));
        }
        throw new NoSuchElementException("Scroll container not found from goals field");
    }

    private boolean isDashboardShellCandidate(WebElement shell) {
        Object visible = js.executeScript("""
                const root = arguments[0];
                if (!root || !root.isConnected || !root.offsetParent) return false;
                const rect = root.getBoundingClientRect();
                if (rect.width < 120 || rect.height < 120) return false;
                const style = window.getComputedStyle(root);
                if (style.visibility === 'hidden' || style.display === 'none') return false;
                if (Number(style.opacity || '1') <= 0.05) return false;
                return root.querySelector('.p-dialog-content, textarea, [role=\"combobox\"], button') !== null;
                """, shell);
        return Boolean.TRUE.equals(visible);
    }

    private boolean isElementConnected(WebElement element) {
        if (element == null) {
            return false;
        }
        return Boolean.TRUE.equals(js.executeScript("return arguments[0].isConnected;", element));
    }

    private WebElement modalScrollContainer() {
        if (isElementConnected(step2ScrollContainer)) {
            return step2ScrollContainer;
        }
        WebElement root = modalRoot();
        List<WebElement> candidates = root.findElements(By.cssSelector(
                ".p-dialog-content, [data-pc-section='content'], [class*='overflow-y-auto'], [class*='overflow-auto']"));
        return candidates.stream()
                .filter(WebElement::isDisplayed)
                .max(Comparator.comparingInt(el -> {
                    Object sh = js.executeScript("return arguments[0].scrollHeight;", el);
                    Object ch = js.executeScript("return arguments[0].clientHeight;", el);
                    int scrollHeight = sh instanceof Number number ? number.intValue() : 0;
                    int clientHeight = ch instanceof Number number ? number.intValue() : 0;
                    return scrollHeight - clientHeight;
                }))
                .orElse(root);
    }

    private WebElement requireFilledGoalsTextarea() {
        WebElement goals = findDisplayedGoalsTextarea();
        if (goals == null) {
            attachVisibleDialogsEvidence("goals-textarea-missing");
            throw new NoSuchElementException("Filled goals textarea not found for step 2 scrolling");
        }
        return goals;
    }

    private void scrollModalContentUntilLabelInViewport(String labelText) {
        scrollModalContentUntil(() -> {
            WebElement label = findFirstDisplayedInModal(By.xpath(labelXpath(labelText)));
            return label != null && isWithinModalViewport(label);
        }, "data-scope-section");
    }

    private void scrollModalContentUntilExpandControlInViewport() {
        scrollModalContentUntil(() -> {
            WebElement expand = findChartExpandControl();
            if (expand != null && isWithinModalViewport(expand)) {
                return true;
            }
            WebElement label = findFirstDisplayedInModal(By.xpath(labelXpath(CHART_TYPES_LABEL)));
            return label != null && isWithinModalViewport(label);
        }, "chart-expand-control");
    }

    private void scrollModalContentUntilChartCardInViewport(String chartLabel) {
        scrollModalContentUntil(() -> {
            WebElement card = findVisibleChartCard(chartLabel);
            return card != null && isWithinModalViewport(card);
        }, "chart-card-" + chartLabel);
    }

    private void scrollModalContentUntil(java.util.function.BooleanSupplier targetVisible, String phase) {
        WebElement scroller = modalScrollContainer();
        long scrollTopBefore = readScrollTop(scroller);
        for (int pass = 0; pass < STEP2_SCROLL_MAX_PASSES; pass++) {
            if (targetVisible.getAsBoolean()) {
                return;
            }
            long previousTop = readScrollTop(scroller);
            scrollModalContentDownOneStep(scroller);
            long currentTop = readScrollTop(scroller);
            if (currentTop <= previousTop && !targetVisible.getAsBoolean()) {
                break;
            }
        }
        if (!targetVisible.getAsBoolean()) {
            attachStep2FailureDiagnostics(phase);
            Assert.fail("Could not scroll modal content into view for: " + phase
                    + " (scrollTop before=" + scrollTopBefore + ", after=" + readScrollTop(scroller) + ")");
        }
    }

    private void scrollModalContentDownOneStep(WebElement scroller) {
        long previousTop = readScrollTop(scroller);
        try {
            new Actions(driver).scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(scroller), 0, 140).perform();
        } catch (RuntimeException ex) {
            js.executeScript("arguments[0].scrollTop += 140;", scroller);
        }
        HumanPaceUtil.pausePreprocessingAction();
        if (readScrollTop(scroller) <= previousTop) {
            js.executeScript("arguments[0].scrollTop += 140;", scroller);
        }
    }

    /** Innermost element containing the text, so its rect is the label itself rather than a wrapping panel. */
    private static String labelXpath(String labelText) {
        String textMatch = "contains(normalize-space(.),'" + labelText + "')";
        return ".//*[" + textMatch + " and not(.//*[" + textMatch + "])]";
    }

    private long readScrollTop(WebElement scroller) {
        Object value = js.executeScript("return arguments[0].scrollTop;", scroller);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String describeScrollContainer(WebElement scroller, long scrollTop) {
        Object summary = js.executeScript("""
                const el = arguments[0];
                const style = window.getComputedStyle(el);
                return {
                  tag: el.tagName,
                  className: el.className,
                  scrollTop: arguments[1],
                  scrollHeight: el.scrollHeight,
                  clientHeight: el.clientHeight,
                  overflowY: style.overflowY
                };
                """, scroller, scrollTop);
        String html = AllureDomUtil.outerHtml(scroller);
        if (html.length() > 2500) {
            html = html.substring(0, 2500) + "...[truncated]";
        }
        return summary + "\n" + html;
    }

    private void scrollModalContentToFooter() {
        WebElement scroller = modalScrollContainer();
        js.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", scroller);
        HumanPaceUtil.pausePreprocessingAction();
        WebElement next = findVisibleNextButton();
        scrollElementIntoModalViewport(next);
    }

    private WebElement findVisibleNextButton() {
        for (WebElement button : modalRoot().findElements(By.xpath(
                ".//button[contains(normalize-space(.),'" + NEXT + "') and not(@disabled)]"))) {
            if (button.isDisplayed()) {
                return button;
            }
        }
        throw new NoSuchElementException("Next button not found in dashboard modal");
    }

    private void scrollElementIntoModalViewport(WebElement target) {
        WebElement scroller = modalScrollContainer();
        js.executeScript("""
                const scroller = arguments[0];
                const el = arguments[1];
                if (!scroller || !el) return;
                const scrollerRect = scroller.getBoundingClientRect();
                const elRect = el.getBoundingClientRect();
                const delta = elRect.top - scrollerRect.top - 48;
                scroller.scrollTop += delta;
                """, scroller, target);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private boolean isWithinModalViewport(WebElement element) {
        Object visible = js.executeScript("""
                const el = arguments[0];
                const scroller = arguments[1];
                if (!el || !scroller) return false;
                const r = el.getBoundingClientRect();
                const s = scroller.getBoundingClientRect();
                return r.bottom > s.top + 4 && r.top < s.bottom - 4
                    && r.right > s.left + 4 && r.left < s.right - 4;
                """, element, modalScrollContainer());
        return Boolean.TRUE.equals(visible);
    }

    private void attachStep2FailureDiagnostics(String phase) {
        attachFailureDiagnostics(phase);
        try {
            WebElement scroller = modalScrollContainer();
            AllureDomUtil.attachText("Step 2 scroll container — " + phase,
                    describeScrollContainer(scroller, readScrollTop(scroller)));
        } catch (RuntimeException ex) {
            ZakaaDashLog.info("Step 2 scroll diagnostics skipped: " + ex.getMessage());
        }
        attachChartSectionEvidence(phase);
    }

    /** Markup of each target chart card (previews stripped) plus the titles of its sibling cards. */
    private void attachChartSectionEvidence(String phase) {
        for (String chartLabel : List.of(CHART_BUBBLE, CHART_HISTOGRAM)) {
            try {
                AllureDomUtil.attachText("Step 2 chart card markup — " + chartLabel + " — " + phase,
                        describeChartCard(chartLabel));
            } catch (RuntimeException ex) {
                ZakaaDashLog.info("Step 2 chart card diagnostics skipped: " + ex.getMessage());
            }
        }
    }

    private String describeChartCard(String chartLabel) {
        Object report = js.executeScript("""
                const label = arguments[1];
                const matches = Array.from(arguments[0].querySelectorAll('*'))
                    .filter(el => (el.innerText || '').replace(/\\s+/g, ' ').trim() === label)
                    .filter(el => !Array.from(el.children)
                        .some(child => (child.innerText || '').replace(/\\s+/g, ' ').trim() === label));
                if (!matches.length) {
                    return 'no element whose text is exactly: ' + label;
                }
                let card = matches[0];
                while (card.parentElement
                        && (card.parentElement.innerText || '').replace(/\\s+/g, ' ').trim() === label) {
                    card = card.parentElement;
                }
                const clone = card.cloneNode(true);
                clone.querySelectorAll('svg, img, canvas').forEach(node => node.remove());
                const grid = card.parentElement;
                const siblings = Array.from(grid ? grid.children : [])
                    .map(child => (child.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 40));
                return 'card outerHTML (previews removed):\\n' + clone.outerHTML
                    + '\\n\\ngrid class: ' + (grid ? grid.className : 'none')
                    + '\\nsibling card titles: ' + JSON.stringify(siblings);
                """, step2Modal(), chartLabel);
        return String.valueOf(report);
    }

    private WebElement findFirstDisplayedInModal(By relative) {
        for (WebElement element : step2Modal().findElements(relative)) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        return null;
    }

    /** The dialog holding the goals field while step 2 runs; otherwise the shared shell lookup. */
    private WebElement step2Modal() {
        if (step2Modal == null) {
            return modalRoot();
        }
        if (!isStep2ModalOpen()) {
            attachVisibleDialogsEvidence("step2-modal-closed");
            throw new NoSuchElementException("Dashboard dialog containing the goals field is no longer open");
        }
        return step2Modal;
    }

    private boolean isStep2ModalOpen() {
        return isElementConnected(step2Modal) && isDashboardShellCandidate(step2Modal);
    }

    private static boolean isScopeChipSelected(WebElement chip) {
        String clazz = nullToEmpty(chip.getAttribute("class"));
        if (clazz.contains("primary") || clazz.contains("selected") || clazz.contains("active")) {
            return true;
        }
        return "true".equalsIgnoreCase(chip.getAttribute("aria-pressed"))
                || "true".equalsIgnoreCase(chip.getAttribute("aria-selected"));
    }

    private void verifyDataEntryValues(
            String dashboardName,
            String dashboardType,
            String department,
            String jobRole,
            String mainIndicators) {
        Assert.assertEquals(readInputAfterLabel(DASHBOARD_NAME_LABEL), dashboardName);
        Assert.assertTrue(readDropdownValue(DASHBOARD_TYPE_LABEL).contains(dashboardType));
        Assert.assertTrue(readDropdownValue(DEPARTMENT_LABEL).contains(department));
        Assert.assertTrue(readDropdownValue(JOB_ROLE_LABEL).contains(jobRole));
        Assert.assertTrue(readTextareaAfterLabel(MAIN_INDICATORS_LABEL).contains(trimSnippet(mainIndicators, 30)));
    }

    private void waitForJobRoleOptionsLoaded() {
        wait.waitUntilTrue(driver -> {
            openDropdown(JOB_ROLE_LABEL);
            boolean loaded = visibleOptions().stream().anyMatch(o -> o.getText().contains("محلل"));
            dismissOpenOverlay();
            return loaded;
        });
    }

    private WebElement waitForVisibleOption(String optionText) {
        wait.waitUntilTrue(driver -> visibleOptions().stream()
                .anyMatch(o -> normalize(o.getText()).contains(normalize(optionText))));
        return visibleOptions().stream()
                .filter(WebElement::isDisplayed)
                .filter(o -> normalize(o.getText()).contains(normalize(optionText)))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Option not found: " + optionText));
    }

    private void selectDropdownOption(String labelText, String optionText) {
        openDropdown(labelText);
        WebElement option = waitForVisibleOption(optionText);
        wait.waitForClickable(option).click();
        HumanPaceUtil.pausePreprocessingAction();
        wait.waitForBlockingOverlayToDisappear("Select " + labelText);
        Assert.assertTrue(readDropdownValue(labelText).contains(optionText),
                "Dropdown " + labelText + " should show " + optionText);
    }

    private void openDropdown(String labelText) {
        WebElement trigger = wait.waitForClickable(
                waitForVisibleInDashboardModal(dropdownTriggerRelativeXpath(labelText)));
        trigger.click();
        HumanPaceUtil.pausePreprocessingAction();
        wait.waitUntilTrue(
                driver -> !visibleDropdownOptions().isEmpty(),
                "Dropdown list did not open for: " + labelText);
    }

    private List<WebElement> visibleDropdownOptions() {
        return driver.findElements(By.xpath(
                "//*[@role='listbox']//*[@role='option']"
                        + " | //*[@data-pc-section='list']//*[@role='option']"
                        + " | //ul[contains(@class,'p-select-list')]//li[@role='option']"
                        + " | //*[@role='option']")).stream()
                .filter(WebElement::isDisplayed)
                .toList();
    }

    private List<WebElement> visibleOptions() {
        return visibleDropdownOptions();
    }

    private void dismissOpenOverlay() {
        try {
            driver.findElement(By.tagName("body")).click();
        } catch (RuntimeException ignored) {
            // no-op
        }
    }

    private WebElement dropdownTrigger(String labelText) {
        return findInDashboardModal(dropdownTriggerRelativeXpath(labelText));
    }

    private static String dropdownTriggerRelativeXpath(String labelText) {
        String labelMatch = "contains(normalize-space(.),'" + labelText + "')";
        String comboboxInField = "//*[@role='combobox' or contains(@class,'p-select')][1]";
        String fieldAncestor = "/ancestor::*[contains(@class,'field') or contains(@class,'p-float-label')"
                + " or contains(@class,'flex-col') or contains(@class,'col-')][1]" + comboboxInField;
        return ".//*[(self::label or self::span) and " + labelMatch + "]" + fieldAncestor
                + " | .//*[" + labelMatch + " and not(.//*[" + labelMatch + "])]" + fieldAncestor;
    }

    private String readDropdownValue(String labelText) {
        WebElement trigger = dropdownTrigger(labelText);
        return normalize(trigger.getText());
    }

    private void fillTextAfterLabel(String label, String value) {
        WebElement input = waitForVisibleInDashboardModal(editableTextInputRelativeXpath(label));
        input.clear();
        input.sendKeys(value);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private String readInputAfterLabel(String label) {
        WebElement input = findInDashboardModal(editableTextInputRelativeXpath(label));
        String value = input.getAttribute("value");
        return value == null ? "" : value;
    }

    /** Finds inside {@link #modalRoot()} — avoids invalid XPath from union shell + relative concat in {@code inDialog}. */
    private WebElement findInDashboardModal(String relativeXpath) {
        return modalRoot().findElement(By.xpath(relativeXpath));
    }

    private WebElement waitForVisibleInDashboardModal(String relativeXpath) {
        wait.waitUntilTrue(driver -> {
            try {
                WebElement element = modalRoot().findElement(By.xpath(relativeXpath));
                return element.isDisplayed();
            } catch (RuntimeException ex) {
                return false;
            }
        });
        WebElement element = findInDashboardModal(relativeXpath);
        if (!element.isDisplayed()) {
            throw new NoSuchElementException("Element not visible in dashboard modal: " + relativeXpath);
        }
        return element;
    }

    /**
     * Plain text input in the same field group as the label (relative to wizard shell).
     * Avoids {@code following::input[1]} from a broad label match (readonly combobox for مصدر البيانات).
     */
    private static String editableTextInputRelativeXpath(String labelText) {
        String labelMatch = "contains(normalize-space(.),'" + labelText + "')";
        String editableInput = "input[(@type='text' or not(@type) or @type='search')"
                + " and not(@readonly) and not(@disabled) and not(@type='hidden')"
                + " and not(ancestor::*[@role='combobox' or contains(@class,'p-select')])]";
        String fieldAncestor = "/ancestor::*[contains(@class,'field') or contains(@class,'p-float-label')"
                + " or contains(@class,'flex-col') or contains(@class,'col-')][1]//" + editableInput;
        return ".//*[(self::label or self::span) and " + labelMatch + "]" + fieldAncestor
                + " | .//*[" + labelMatch + " and not(.//*[" + labelMatch + "])]" + fieldAncestor;
    }

    private void fillTextareaAfterLabel(String label, String value) {
        WebElement area = waitForVisibleInDashboardModal(editableTextareaRelativeXpath(label));
        area.clear();
        area.sendKeys(value);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private String readTextareaAfterLabel(String label) {
        WebElement area = findInDashboardModal(editableTextareaRelativeXpath(label));
        return area.getAttribute("value") != null ? area.getAttribute("value") : area.getText();
    }

    private void fillTextareaForGoals(String value) {
        WebElement area = waitForVisibleInDashboardModal(goalsTextareaRelativeXpath());
        area.clear();
        area.sendKeys(value);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private String readGoalsText() {
        WebElement area = findInDashboardModal(goalsTextareaRelativeXpath());
        return area.getText().isBlank() ? nullToEmpty(area.getAttribute("value")) : area.getText();
    }

    private void waitForGoalsFieldVisible() {
        waitForVisibleInDashboardModal(goalsTextareaRelativeXpath());
        waitForStepActive(STEP_GRAPHICS);
    }

    private boolean isGoalsFieldVisible() {
        try {
            WebElement area = findInDashboardModal(goalsTextareaRelativeXpath());
            return area.isDisplayed();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String editableTextareaRelativeXpath(String labelText) {
        String labelMatch = "contains(normalize-space(.),'" + labelText + "')";
        String editableTextarea = "textarea[not(@readonly) and not(@disabled)]";
        String fieldAncestor = "/ancestor::*[contains(@class,'field') or contains(@class,'p-float-label')"
                + " or contains(@class,'flex-col') or contains(@class,'col-')][1]//" + editableTextarea;
        return ".//*[(self::label or self::span) and " + labelMatch + "]" + fieldAncestor
                + " | .//*[" + labelMatch + " and not(.//*[" + labelMatch + "])]" + fieldAncestor;
    }

    private static String goalsTextareaRelativeXpath() {
        return editableTextareaRelativeXpath(GOALS_LABEL)
                + " | .//textarea[contains(@placeholder,'هدف') and not(@readonly) and not(@disabled)]";
    }

    private void waitForStepActive(String stepLabel) {
        wait.waitUntilTrue(driver -> isStepMarkerActive(stepLabel));
    }

    private boolean isStepMarkerActive(String stepLabel) {
        List<WebElement> markers = modalRoot().findElements(By.xpath(
                ".//*[contains(normalize-space(.),'" + stepLabel + "')]"));
        for (WebElement marker : markers) {
            if (!marker.isDisplayed()) {
                continue;
            }
            String clazz = nullToEmpty(marker.getAttribute("class"));
            if (clazz.contains("active") || clazz.contains("current") || "true".equals(marker.getAttribute("data-p-active"))) {
                return true;
            }
        }
        return false;
    }

    private void clickNextInModal() {
        WebElement next = findVisibleNextButton();
        scrollElementIntoModalViewport(next);
        wait.waitForClickable(next).click();
        HumanPaceUtil.pauseWizardTransition();
        wait.waitForBlockingOverlayToDisappear("Dashboard wizard next");
    }

    private void confirmDiscardIfPresent() {
        List<WebElement> dialogs = driver.findElements(By.xpath("//div[@role='dialog']"));
        for (WebElement dialog : dialogs) {
            if (!dialog.isDisplayed()) {
                continue;
            }
            for (WebElement button : dialog.findElements(By.xpath(
                    ".//button[contains(normalize-space(.),'ترك') or contains(normalize-space(.),'متابعة')"
                            + " or contains(normalize-space(.),'تأكيد') or contains(normalize-space(.),'نعم')]"))) {
                if (button.isDisplayed()) {
                    button.click();
                    HumanPaceUtil.pauseNormalAction();
                    return;
                }
            }
        }
    }

    public void attachFailureDiagnostics(String phase) {
        try {
            ScreenshotUtil.captureAndAttach(driver, "dashboard-modal-" + phase);
        } catch (RuntimeException ex) {
            ZakaaDashLog.info("Dashboard modal screenshot skipped: " + ex.getMessage());
        }
        try {
            AllureDomUtil.attachText("Dashboard modal failure — " + phase,
                    AllureDomUtil.outerHtml(modalRoot()));
        } catch (NoSuchElementException ex) {
            attachVisibleDialogsEvidence(phase);
        }
    }

    public void captureDashboardModalEvidence(String phase) {
        try {
            ScreenshotUtil.captureAndAttach(driver, "dashboard-modal-" + phase);
        } catch (RuntimeException ex) {
            ZakaaDashLog.info("Dashboard modal screenshot skipped: " + ex.getMessage());
        }
        try {
            AllureDomUtil.attachText("Dashboard modal — " + phase, AllureDomUtil.outerHtml(modalRoot()));
        } catch (RuntimeException ex) {
            attachVisibleDialogsEvidence(phase);
        }
    }

    private void attachVisibleDialogsEvidence(String phase) {
        StringBuilder report = new StringBuilder();
        String candidatesXpath = "//div[contains(@class,'p-dialog') and contains(@class,'p-component')]"
                + " | //*[@data-pc-name='dialog'] | //div[@role='dialog'] | //*[@aria-modal='true']";
        for (WebElement dialog : driver.findElements(By.xpath(candidatesXpath))) {
            Object summary = js.executeScript("""
                    const el = arguments[0];
                    if (!el) return null;
                    const rect = el.getBoundingClientRect();
                    const style = window.getComputedStyle(el);
                    return {
                      tag: el.tagName,
                      role: el.getAttribute('role'),
                      ariaModal: el.getAttribute('aria-modal'),
                      className: el.className,
                      displayed: el.offsetParent !== null,
                      rect: { w: rect.width, h: rect.height },
                      textSample: (el.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 400)
                    };
                    """, dialog);
            report.append("--- candidate shell ---\n").append(String.valueOf(summary)).append("\n");
            if (Boolean.TRUE.equals(js.executeScript("return arguments[0].offsetParent !== null;", dialog))) {
                report.append(AllureDomUtil.outerHtml(dialog)).append("\n");
            }
        }
        if (report.isEmpty()) {
            report.append("(no dialog shell candidates in DOM)");
        }
        AllureDomUtil.attachText("Dashboard modal failure — visible dialogs — " + phase, report.toString());
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String trimSnippet(String text, int length) {
        String normalized = normalize(text);
        return normalized.length() <= length ? normalized : normalized.substring(0, length);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
