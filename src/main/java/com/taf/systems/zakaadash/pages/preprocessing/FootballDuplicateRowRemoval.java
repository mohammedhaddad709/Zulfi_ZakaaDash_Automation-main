package com.taf.systems.zakaadash.pages.preprocessing;

import com.taf.systems.zakaadash.utils.AllureDomUtil;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enables full-row duplicate removal when the wizard exposes it as a suggestion card control.
 * Step 4 cleaning uses PrimeNG {@code data-pc-name='checkbox'} ({@code option-drop_*}) for column drops only.
 * Step 2 system suggestions use PrimeNG {@code data-pc-name='toggleswitch'} on each rule card (see Allure capture).
 */
public final class FootballDuplicateRowRemoval {

    private static final By SUGGESTION_CARD = By.cssSelector(
            "div.border.rounded-lg.p-4, div[class*='rounded-lg'][class*='p-4'][class*='border']");

    private static final By CARD_CONTROL_ROOT = By.cssSelector(
            "[data-pc-name='checkbox'][data-pc-section='root'], "
                    + "[data-pc-name='toggleswitch'][data-pc-section='root']");

    private static final String[] ROW_DUPLICATE_FRAGMENTS = {
            "duplicate rows",
            "duplicate row",
            "full duplicate",
            "exact duplicate",
            "Duplicate rows",
            "حذف الصفوف المكررة",
            "إزالة الصفوف المكررة",
            "إزالة الصفوف المتكررة",
            "حذف الصفوف المتكررة",
            "الصفوف المكررة",
            "الصفوف المتكررة",
            "صفوف مكررة",
            "صفوف متكررة",
            "صفوف تكرار",
            "تكرار صفوف",
            "تكرار الصفوف",
            "صف مكرر",
            "صفوف مكرر"
    };

    private static final String[] COLUMN_DROP_FRAGMENTS = {
            "duplicate column",
            "تكرار العمود",
            "الأعمدة المكررة",
            "duplicate columns",
            "حذف العمود",
            "حذف عمود"
    };

    private static volatile ControlMatch lastEnabledDuplicate;

    private FootballDuplicateRowRemoval() {
    }

    public static boolean enableFullRowDuplicateRemoval(WebDriver driver, String stageLabel) {
        return enableFullRowDuplicateRemoval(driver, wizardDialogRoot(driver), stageLabel);
    }

    public static boolean enableFullRowDuplicateRemoval(WebDriver driver, WebElement scope, String stageLabel) {
        scrollSuggestionsIntoView(driver, scope);
        List<ControlMatch> matches = scanSelectableControls(scope);
        attachScanReport(stageLabel, matches);
        for (ControlMatch match : matches) {
            if (!match.fullRowDuplicate()) {
                continue;
            }
            setSelected(match.control(), true);
            if (isSelected(match.control())) {
                lastEnabledDuplicate = match;
                attachEnabledControl(stageLabel, match);
                return true;
            }
        }
        return false;
    }

    public static boolean enableFullRowDuplicateRemoval(WebDriver driver, List<ControlMatch> matches, String stageLabel) {
        attachScanReport(stageLabel, matches);
        for (ControlMatch match : matches) {
            if (!match.fullRowDuplicate()) {
                continue;
            }
            setSelected(match.control(), true);
            if (isSelected(match.control())) {
                lastEnabledDuplicate = match;
                attachEnabledControl(stageLabel, match);
                return true;
            }
        }
        return false;
    }

    public static boolean isFullRowDuplicateRemovalActive(WebDriver driver) {
        if (lastEnabledDuplicate != null && isSelected(lastEnabledDuplicate.control())) {
            return true;
        }
        for (ControlMatch match : scanSelectableControls(wizardDialogRoot(driver))) {
            if (match.fullRowDuplicate() && isSelected(match.control())) {
                lastEnabledDuplicate = match;
                return true;
            }
        }
        return false;
    }

    public static void disableColumnDropSuggestions(WebElement scope) {
        for (ControlMatch match : scanSelectableControls(scope)) {
            if (match.columnDropSuggestion() && isSelected(match.control())) {
                setSelected(match.control(), false);
            }
        }
    }

    public static void attachScanReport(String stageLabel, List<ControlMatch> matches) {
        StringBuilder report = new StringBuilder();
        report.append("Stage: ").append(stageLabel).append('\n');
        report.append("Selectable controls scanned: ").append(matches.size()).append("\n\n");
        for (int index = 0; index < matches.size(); index++) {
            ControlMatch match = matches.get(index);
            report.append("--- Control ").append(index + 1).append(" ---\n");
            if (match.categoryLabel() != null && !match.categoryLabel().isBlank()) {
                report.append("category=").append(match.categoryLabel()).append('\n');
            }
            report.append("optionId=").append(match.optionId()).append('\n');
            report.append("controlType=").append(match.controlType()).append('\n');
            report.append("selected=").append(isSelected(match.control())).append('\n');
            report.append("disabled=").append(isDisabled(match.control())).append('\n');
            report.append("rowText=").append(match.rowText()).append('\n');
            report.append("fullRowDuplicate=").append(match.fullRowDuplicate()).append('\n');
            report.append("columnDropSuggestion=").append(match.columnDropSuggestion()).append('\n');
            report.append("outerHTML=").append(truncate(AllureDomUtil.outerHtml(match.control()), 1200)).append("\n\n");
        }
        Allure.addAttachment("Football duplicate-row scan — " + stageLabel, "text/plain", report.toString(), ".txt");
    }

    public static void attachSuggestionsPanel(String attachmentName, String categoryLabel, WebElement panel) {
        StringBuilder body = new StringBuilder();
        body.append("category=").append(categoryLabel).append("\n\n");
        body.append("panelOuterHTML=").append(truncate(panel.getAttribute("outerHTML"), 120_000));
        Allure.addAttachment(attachmentName, "text/plain", body.toString(), ".txt");
    }

    private static void attachEnabledControl(String stageLabel, ControlMatch match) {
        Allure.addAttachment("Football duplicate-row enabled — " + stageLabel, "text/plain",
                "optionId=" + match.optionId() + "\nrowText=" + match.rowText() + "\n"
                        + AllureDomUtil.outerHtml(match.control()), ".txt");
    }

    public static WebElement wizardDialogRoot(WebDriver driver) {
        return driver.findElement(By.xpath(PreprocessingDialog.dialogXpath("معالجة البيانات المتقدمة")));
    }

    public static List<WebElement> findSelectableControls(WebElement scope) {
        List<WebElement> controls = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (WebElement card : scope.findElements(SUGGESTION_CARD)) {
            WebElement root = findControlRootInCard(card);
            if (root == null || !isControlInteractable(root)) {
                continue;
            }
            String key = dedupeKey(root);
            if (!seen.add(key)) {
                continue;
            }
            controls.add(root);
        }
        return controls;
    }

    public static List<ControlMatch> scanSelectableControls(WebElement scope) {
        return scanSelectableControls(scope, null);
    }

    public static List<ControlMatch> scanSelectableControls(WebElement scope, String categoryLabel) {
        List<ControlMatch> matches = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (WebElement card : scope.findElements(SUGGESTION_CARD)) {
            WebElement root = findControlRootInCard(card);
            if (root == null || !isControlInteractable(root)) {
                continue;
            }
            String key = dedupeKey(root);
            if (!seen.add(key)) {
                continue;
            }
            String rowText = card.getText().replaceAll("\\s+", " ").trim();
            matches.add(new ControlMatch(
                    root,
                    rowText,
                    optionIdForControl(root),
                    root.getAttribute("data-pc-name"),
                    categoryLabel));
        }
        return matches;
    }

    public static void scrollSuggestionsIntoView(WebDriver driver, WebElement scope) {
        if (!(driver instanceof JavascriptExecutor js)) {
            return;
        }
        js.executeScript(
                "arguments[0].scrollTop = 0;"
                        + "arguments[0].querySelectorAll('[data-pc-name=\"checkbox\"], [data-pc-name=\"toggleswitch\"]')"
                        + ".forEach(function(el){ el.scrollIntoView({block:'nearest', inline:'nearest'});});",
                scope);
    }

    private static WebElement findControlRootInCard(WebElement card) {
        for (WebElement root : card.findElements(CARD_CONTROL_ROOT)) {
            if (isControlInteractable(root)) {
                return root;
            }
        }
        return null;
    }

    private static String dedupeKey(WebElement root) {
        String id = root.getAttribute("id");
        if (id != null && !id.isBlank()) {
            return id;
        }
        String describedBy = root.getAttribute("aria-describedby");
        if (describedBy != null && !describedBy.isBlank()) {
            return describedBy;
        }
        return root.toString();
    }

    private static String optionIdForControl(WebElement control) {
        String id = control.getAttribute("id");
        return id == null ? "" : id;
    }

    private static boolean isControlInteractable(WebElement root) {
        if ("true".equalsIgnoreCase(root.getAttribute("data-p-disabled"))) {
            return false;
        }
        if ("true".equalsIgnoreCase(root.getAttribute("aria-disabled"))) {
            return false;
        }
        return root.isDisplayed();
    }

    static boolean isDisabled(WebElement control) {
        if ("true".equalsIgnoreCase(control.getAttribute("data-p-disabled"))) {
            return true;
        }
        return "true".equalsIgnoreCase(control.getAttribute("aria-disabled"));
    }

    static boolean isSelected(WebElement control) {
        if (control.isSelected()) {
            return true;
        }
        if ("true".equalsIgnoreCase(control.getAttribute("aria-checked"))) {
            return true;
        }
        if ("true".equalsIgnoreCase(control.getAttribute("data-p-checked"))) {
            return true;
        }
        String dataChecked = control.getAttribute("data-p");
        return dataChecked != null && dataChecked.contains("checked");
    }

    static void setSelected(WebElement control, boolean selected) {
        if (isSelected(control) == selected) {
            return;
        }
        String pcName = control.getAttribute("data-pc-name");
        try {
            if ("toggleswitch".equals(pcName)) {
                control.findElement(By.cssSelector("[data-pc-section='slider'], .p-toggleswitch-slider")).click();
                return;
            }
            if ("checkbox".equals(pcName)) {
                control.findElement(By.cssSelector("[data-pc-section='box'], .p-checkbox-box")).click();
                return;
            }
        } catch (RuntimeException ignored) {
            // fall through to root click
        }
        control.click();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max) + "...";
    }

    public static boolean isFullRowDuplicateRemovalText(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return false;
        }
        ControlMatch match = new ControlMatch(null, rowText.replaceAll("\\s+", " ").trim(), "", "", null);
        return match.fullRowDuplicate();
    }

    public record ControlMatch(
            WebElement control,
            String rowText,
            String optionId,
            String controlType,
            String categoryLabel) {

        boolean columnDropSuggestion() {
            if (optionId != null && optionId.startsWith("option-drop_")) {
                return true;
            }
            return matchesAny(rowText, COLUMN_DROP_FRAGMENTS);
        }

        boolean fullRowDuplicate() {
            if (columnDropSuggestion()) {
                return false;
            }
            if (matchesAny(rowText, ROW_DUPLICATE_FRAGMENTS)) {
                return true;
            }
            String lower = rowText.toLowerCase(Locale.ROOT);
            return lower.contains("duplicate") && lower.contains("row")
                    || rowText.contains("صف") && rowText.contains("تكرار")
                    || rowText.contains("صف") && rowText.contains("مكرر");
        }
    }

    private static boolean matchesAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isBlank() && text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
