package com.taf.systems.zakaadash.pages.dashboard;

import com.taf.systems.zakaadash.utils.AllureDomUtil;
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.ScreenshotUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Final dashboard-wizard stage: indicator groups of recommended charts, per-chart inclusion
 * checkboxes, per-chart star switches, and a summary counter for starred charts.
 *
 * <p>Structure verified against the live DOM attached to the 2026-10-12 16:59 run: the stage lives in
 * {@code [role=dialog]} whose {@code .p-dialog-content[data-pc-section=content]} holds one
 * {@code section} per indicator group (heading in {@code h3}, group control
 * {@code button[role=checkbox]} outside the cards) and chart cards that each own a
 * {@code button[role=checkbox]} (title in its {@code span}) plus a
 * {@code button[role=switch]} star labelled "إضافة للمفضلة" (switches to "إزالة من المفضلة" once starred,
 * re-verified 2026-11-07). Chart titles and group names are generated per run, so every lookup is structural and
 * state is read only from {@code aria-checked}.
 */
final class RecommendedChartSelectionStage {

    static final String SAVE_LABEL = "حفظ";
    static final String STAR_ARIA_LABEL = "إضافة للمفضلة";
    /** aria-label the same switch carries once starred (with aria-checked="true"). */
    static final String STARRED_ARIA_LABEL = "إزالة من المفضلة";
    static final String SEED_PROPERTY = "dashboard.chart.selection.seed";

    private static final String SELECT_ALL = "تحديد الكل";
    private static final String DESELECT_ALL = "إلغاء تحديد الكل";
    private static final String EXPAND_MORE = "عرض المزيد";
    private static final String SUMMARY_COUNTER_LABEL = "عدد الرسومات في الملخص";
    private static final int TARGET_SUMMARY_STARS = 1;
    private static final int EXPAND_MAX_CLICKS = 8;
    private static final int SCROLL_MAX_ATTEMPTS = 4;
    private static final int VIEWPORT_TOP_OFFSET = 96;

    /** Structural lookups shared by every script; {@code arguments[0]} is the star label. */
    private static final String DOM_HELPERS_TEMPLATE = """
            function clean(text) {
                return (text || '').replace(/\\s+/g, ' ').trim();
            }
            function stageContent(dialog) {
                return dialog.querySelector('[data-pc-section="content"]');
            }
            function saveButton(dialog, saveLabel) {
                return Array.from(dialog.querySelectorAll('button')).find(button =>
                    clean(button.getAttribute('aria-label')) === saveLabel
                        || clean(button.textContent) === saveLabel) || null;
            }
            // Live DOM 2026-11-07: a starred switch renames itself from STAR_ARIA_LABEL to STARRED_ARIA_LABEL.
            function starButtons(root, starLabel) {
                return Array.from(root.querySelectorAll('button[role="switch"]'))
                    .filter(button => [starLabel, '__STARRED__'].includes(clean(button.getAttribute('aria-label'))));
            }
            function cardsIn(root, starLabel) {
                const blockCards = Array.from(root.querySelectorAll('div.group.block')).filter(card => {
                    const inclusion = card.querySelector('button[role="checkbox"]');
                    const star = starButtons(card, starLabel)[0];
                    return inclusion && star && card.contains(star) && card.contains(inclusion);
                });
                if (blockCards.length > 0) {
                    return blockCards;
                }
                const cards = [];
                starButtons(root, starLabel).forEach(star => {
                    let node = star.parentElement;
                    while (node && root.contains(node)) {
                        const inclusion = node.querySelector('button[role="checkbox"]');
                        const ownStar = starButtons(node, starLabel)[0];
                        if (inclusion && ownStar && node.contains(inclusion) && node.contains(ownStar)) {
                            cards.push(node);
                            return;
                        }
                        node = node.parentElement;
                    }
                });
                return cards;
            }
            function scrollChartStarIntoContent(dialog, group, title, ordinal, starLabel, offset) {
                const content = stageContent(dialog);
                const card = cardAt(dialog, group, title, ordinal, starLabel);
                if (!content || !card) return false;
                const star = starButtons(card, starLabel)[0];
                if (!star) return false;
                for (let attempt = 0; attempt < 8; attempt++) {
                    const starRect = star.getBoundingClientRect();
                    const contentRect = content.getBoundingClientRect();
                    const topOk = starRect.top >= contentRect.top + offset - 4;
                    const bottomOk = starRect.bottom <= contentRect.bottom - 4;
                    if (topOk && bottomOk && starRect.width > 0 && starRect.height > 0) {
                        return true;
                    }
                    content.scrollTop += (starRect.top - contentRect.top) - offset;
                }
                return star.getBoundingClientRect().height > 0;
            }
            function stageDialog(starLabel, saveLabel) {
                const dialogs = document.querySelectorAll(
                    '[role="dialog"], [aria-modal="true"], div.p-dialog.p-component');
                for (const dialog of dialogs) {
                    const rect = dialog.getBoundingClientRect();
                    if (rect.width < 120 || rect.height < 120) continue;
                    const content = stageContent(dialog);
                    if (!content) continue;
                    if (!saveButton(dialog, saveLabel)) continue;
                    if (cardsIn(content, starLabel).length === 0) continue;
                    return dialog;
                }
                return null;
            }
            function groupSections(dialog) {
                const content = stageContent(dialog);
                if (!content) return [];
                return Array.from(content.querySelectorAll('section'))
                    .filter(section => section.querySelector('h3'));
            }
            function groupName(section) {
                const heading = section.querySelector('h3');
                return heading ? clean(heading.textContent) : '';
            }
            function sectionOf(dialog, group) {
                return groupSections(dialog).find(section => groupName(section) === group) || null;
            }
            function cardTitle(card) {
                const inclusion = card.querySelector('button[role="checkbox"]');
                if (!inclusion) return '';
                const span = inclusion.querySelector('span');
                return clean(span ? span.textContent : inclusion.textContent);
            }
            function cardStates(dialog, starLabel) {
                const states = [];
                const seen = {};
                groupSections(dialog).forEach(section => {
                    const group = groupName(section);
                    cardsIn(section, starLabel).forEach(card => {
                        const title = cardTitle(card);
                        if (!group || !title) return;
                        const key = group + '::' + title;
                        const ordinal = seen[key] === undefined ? 0 : seen[key] + 1;
                        seen[key] = ordinal;
                        const inclusion = card.querySelector('button[role="checkbox"]');
                        const star = starButtons(card, starLabel)[0] || null;
                        states.push({
                            group: group,
                            title: title,
                            ordinal: ordinal,
                            inclusion: inclusion ? inclusion.getAttribute('aria-checked') : null,
                            star: star ? star.getAttribute('aria-checked') : null
                        });
                    });
                });
                return states;
            }
            function cardAt(dialog, group, title, ordinal, starLabel) {
                const section = sectionOf(dialog, group);
                if (!section) return null;
                const matches = cardsIn(section, starLabel).filter(card => cardTitle(card) === title);
                return matches[ordinal] || null;
            }
            function groupControl(dialog, group, starLabel, selectAll, deselectAll) {
                const section = sectionOf(dialog, group);
                if (!section) return null;
                const cards = cardsIn(section, starLabel);
                return Array.from(section.querySelectorAll('button[role="checkbox"]')).find(button => {
                    if (cards.some(card => card.contains(button))) return false;
                    const label = controlLabel(button);
                    return label === selectAll || label === deselectAll;
                }) || null;
            }
            function controlLabel(button) {
                const aria = clean(button.getAttribute('aria-label'));
                return aria || clean(button.textContent);
            }
            """;
    private static final String DOM_HELPERS = DOM_HELPERS_TEMPLATE.replace("__STARRED__", STARRED_ARIA_LABEL);

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final WaitUtil wait;

    private String currentAction = "stage-entry";
    private ChartIdentity currentChart;

    RecommendedChartSelectionStage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.wait = new WaitUtil(driver);
    }

    void run(String expectedDashboardName) {
        try {
            runStage(expectedDashboardName);
        } catch (RuntimeException | AssertionError failure) {
            attachFailureEvidence(failure);
            throw failure;
        }
    }

    private void runStage(String expectedDashboardName) {
        requireStageDialog();
        ZakaaDashLog.info("final chart-selection stage — ENTERED");

        long seed = resolveSeed();
        Random random = new Random(seed);
        ZakaaDashLog.info("Chart selection seed: " + seed);

        currentAction = "discover-and-expand";
        List<String> groups = discoverGroupNames();
        Assert.assertFalse(groups.isEmpty(), "At least one indicator group should be discovered");
        for (String group : groups) {
            scrollGroupIntoView(group);
            expandGroupFully(group);
        }
        groups = discoverGroupNames();
        for (String group : groups) {
            scrollGroupIntoView(group);
            expandGroupFully(group);
        }
        List<ChartIdentity> charts = discoverCharts();
        ZakaaDashLog.info("Discovered " + groups.size() + " groups and " + charts.size() + " chart cards");
        AllureDomUtil.attachText("Final stage discovery",
                "groups=" + groups.size() + "\ncards=" + charts.size() + "\n"
                        + charts.stream().map(ChartIdentity::label).collect(Collectors.joining("\n")));
        Assert.assertFalse(charts.isEmpty(), "At least one chart card should be discovered");

        currentAction = "select-all-checks";
        verifySelectAllForEveryGroup(groups);

        Map<ChartIdentity, Boolean> inclusionBeforeStars = readInclusionSnapshot();

        List<ChartIdentity> eligible = readStates().stream()
                .filter(ChartState::included)
                .map(ChartState::identity)
                .toList();
        Assert.assertFalse(eligible.isEmpty(),
                "At least one included chart is required to star before save");

        int activeBefore = readSummaryCounter()[0];
        currentAction = "ensure-one-star";
        ChartIdentity chosenStar = ensureExactlyOneSummaryStar(eligible, random);
        verifyExactlyOneSummaryStar(chosenStar);

        int activeAfter = readSummaryCounter()[0];
        ZakaaDashLog.info("Summary stars: before=" + activeBefore + ", after=" + activeAfter
                + ", chosen=" + chosenStar.label());

        currentAction = "scroll-for-save";
        scrollModalContentToBottomAndVerify();

        Map<ChartIdentity, Boolean> inclusionAfterStars = readInclusionSnapshot();
        for (Map.Entry<ChartIdentity, Boolean> entry : inclusionBeforeStars.entrySet()) {
            Assert.assertEquals(inclusionAfterStars.get(entry.getKey()), entry.getValue(),
                    "Star clicks should not change chart inclusion: " + entry.getKey().label());
        }
        AllureDomUtil.attachText("One-star selection before save",
                "seed=" + seed + "\nactiveBefore=" + activeBefore
                        + "\nchosenStar=" + chosenStar.label());
        ZakaaDashLog.info("One starred chart before save (seed=" + seed + "): " + chosenStar.label());

        currentAction = "click-save";
        saveDashboardAndVerifyCreated(expectedDashboardName);
        currentAction = "stage-complete";
    }

    private long resolveSeed() {
        String configured = System.getProperty(SEED_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Long.parseLong(configured.trim());
        }
        return System.nanoTime();
    }

    // --- A. discovery and expansion -------------------------------------------------------------

    private void expandGroupFully(String group) {
        for (int click = 0; click < EXPAND_MAX_CLICKS; click++) {
            WebElement expand = collapsedExpandControl(group);
            if (expand == null) {
                return;
            }
            int cardsBefore = statesInGroup(group).size();
            clickControl(expand);
            wait.waitUntilTrue(
                    webDriver -> statesInGroup(group).size() > cardsBefore
                            || collapsedExpandControl(group) == null,
                    "Group '" + group + "' should expand after clicking '" + EXPAND_MORE + "'");
        }
    }

    private WebElement collapsedExpandControl(String group) {
        Object control = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const section = sectionOf(dialog, arguments[2]);
                if (!section) return null;
                const expandLabel = arguments[3];
                return Array.from(section.querySelectorAll('button[aria-expanded]')).find(button =>
                    button.getAttribute('aria-expanded') === 'false'
                        && controlLabel(button).includes(expandLabel)) || null;
                """, group, EXPAND_MORE);
        return control instanceof WebElement element ? element : null;
    }

    private void scrollGroupIntoView(String group) {
        runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return;
                const content = stageContent(dialog);
                const section = sectionOf(dialog, arguments[2]);
                if (!content || !section) return;
                const contentRect = content.getBoundingClientRect();
                const sectionRect = section.getBoundingClientRect();
                content.scrollTop += (sectionRect.top - contentRect.top) - 48;
                """, group);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private List<String> discoverGroupNames() {
        Object raw = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return [];
                return groupSections(dialog).map(groupName).filter(name => name.length > 0);
                """);
        List<String> groups = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                groups.add(String.valueOf(item));
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(groups));
    }

    /** One round-trip snapshot of every card with its own {@code aria-checked} values. */
    private List<ChartState> readStates() {
        Object raw = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return [];
                return cardStates(dialog, arguments[0]);
                """);
        List<ChartState> states = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    ChartIdentity identity = new ChartIdentity(
                            Objects.toString(map.get("group"), ""),
                            Objects.toString(map.get("title"), ""),
                            ((Number) map.get("ordinal")).intValue());
                    states.add(new ChartState(
                            identity,
                            "true".equals(Objects.toString(map.get("inclusion"), "")),
                            "true".equals(Objects.toString(map.get("star"), ""))));
                }
            }
        }
        return states;
    }

    private List<ChartIdentity> discoverCharts() {
        return readStates().stream().map(ChartState::identity).toList();
    }

    private List<ChartState> statesInGroup(String group) {
        return readStates().stream()
                .filter(state -> state.identity().group().equals(group))
                .toList();
    }

    // --- B. select-all behaviour ----------------------------------------------------------------

    private void verifySelectAllForEveryGroup(List<String> groups) {
        for (String group : groups) {
            if (statesInGroup(group).isEmpty()) {
                continue;
            }
            verifySelectAllForGroup(group);
        }
        for (ChartState state : readStates()) {
            Assert.assertTrue(state.included(),
                    "Every chart should stay included before the star checks: " + state.identity().label());
        }
    }

    private void verifySelectAllForGroup(String group) {
        currentAction = "select-all-check:" + group;
        Map<ChartIdentity, Boolean> otherGroupsBefore = inclusionOutsideGroup(group);

        if (!statesInGroup(group).stream().allMatch(ChartState::included)) {
            clickGroupControl(group, DESELECT_ALL);
            waitUntilGroupInclusion(group, true);
        }

        clickGroupControl(group, SELECT_ALL);
        waitUntilGroupInclusion(group, false);
        Assert.assertEquals(groupControlLabel(group), SELECT_ALL,
                "Group control should offer '" + SELECT_ALL + "' after deselecting all: " + group);
        assertInclusionUnchanged(otherGroupsBefore, "after deselect-all in " + group);

        clickGroupControl(group, DESELECT_ALL);
        waitUntilGroupInclusion(group, true);
        Assert.assertEquals(groupControlLabel(group), DESELECT_ALL,
                "Group control should offer '" + DESELECT_ALL + "' after selecting all: " + group);
        assertInclusionUnchanged(otherGroupsBefore, "after select-all in " + group);

        ZakaaDashLog.info("Select-all verified for group: " + group
                + " (cards=" + statesInGroup(group).size() + ")");
    }

    /** Clicks the group control and waits until its label flips to {@code expectedLabelAfterClick}. */
    private void clickGroupControl(String group, String expectedLabelAfterClick) {
        WebElement control = groupControl(group);
        clickControl(control);
        wait.waitUntilTrue(
                webDriver -> expectedLabelAfterClick.equals(groupControlLabel(group)),
                "Group control of '" + group + "' should change to '" + expectedLabelAfterClick + "'");
    }

    private WebElement groupControl(String group) {
        Object control = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                return groupControl(dialog, arguments[2], arguments[0], arguments[3], arguments[4]);
                """, group, SELECT_ALL, DESELECT_ALL);
        if (control instanceof WebElement element) {
            return element;
        }
        throw new NoSuchElementException("Group select-all control not found for group: " + group);
    }

    private String groupControlLabel(String group) {
        Object label = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return '';
                const control = groupControl(dialog, arguments[2], arguments[0], arguments[3], arguments[4]);
                return control ? controlLabel(control) : '';
                """, group, SELECT_ALL, DESELECT_ALL);
        return String.valueOf(label).trim();
    }

    private void waitUntilGroupInclusion(String group, boolean included) {
        wait.waitUntilTrue(webDriver -> {
            List<ChartState> states = statesInGroup(group);
            return !states.isEmpty() && states.stream().allMatch(state -> state.included() == included);
        }, "All charts in group '" + group + "' should have inclusion aria-checked=" + included);
    }

    private Map<ChartIdentity, Boolean> readInclusionSnapshot() {
        Map<ChartIdentity, Boolean> states = new LinkedHashMap<>();
        for (ChartState state : readStates()) {
            states.put(state.identity(), state.included());
        }
        return states;
    }

    private Map<ChartIdentity, Boolean> inclusionOutsideGroup(String group) {
        Map<ChartIdentity, Boolean> states = new LinkedHashMap<>();
        for (ChartState state : readStates()) {
            if (!state.identity().group().equals(group)) {
                states.put(state.identity(), state.included());
            }
        }
        return states;
    }

    private void assertInclusionUnchanged(Map<ChartIdentity, Boolean> expected, String context) {
        Map<ChartIdentity, Boolean> actual = readInclusionSnapshot();
        for (Map.Entry<ChartIdentity, Boolean> entry : expected.entrySet()) {
            Assert.assertEquals(actual.get(entry.getKey()), entry.getValue(),
                    "Inclusion of other group should stay unchanged " + context + ": "
                            + entry.getKey().label());
        }
    }

    // --- C. reproducible random choices ---------------------------------------------------------

    private List<ChartIdentity> pickAcrossGroups(List<ChartIdentity> pool, int count, Random random) {
        Map<String, List<ChartIdentity>> byGroup = new LinkedHashMap<>();
        for (ChartIdentity chart : pool) {
            byGroup.computeIfAbsent(chart.group(), key -> new ArrayList<>()).add(chart);
        }
        List<String> groups = new ArrayList<>(byGroup.keySet());
        Collections.shuffle(groups, random);
        byGroup.values().forEach(charts -> Collections.shuffle(charts, random));

        List<ChartIdentity> picked = new ArrayList<>();
        Set<ChartIdentity> used = new LinkedHashSet<>();
        boolean progressed = true;
        while (picked.size() < count && progressed) {
            progressed = false;
            for (String group : groups) {
                for (ChartIdentity chart : byGroup.get(group)) {
                    if (used.add(chart)) {
                        picked.add(chart);
                        progressed = true;
                        break;
                    }
                }
                if (picked.size() >= count) {
                    break;
                }
            }
        }
        Assert.assertEquals(picked.size(), count,
                "Should pick " + count + " distinct charts from " + pool.size() + " eligible charts");
        ZakaaDashLog.info("Random chart pool picks: " + picked.stream().map(ChartIdentity::label).toList());
        return picked;
    }

    // --- D. single summary star -----------------------------------------------------------------

    private ChartIdentity ensureExactlyOneSummaryStar(List<ChartIdentity> eligible, Random random) {
        List<ChartIdentity> activeStars = readStates().stream()
                .filter(ChartState::included)
                .filter(ChartState::starred)
                .map(ChartState::identity)
                .toList();

        if (activeStars.size() == TARGET_SUMMARY_STARS) {
            ChartIdentity retained = activeStars.get(0);
            ZakaaDashLog.info("Retaining existing summary star without click: " + retained.label());
            return retained;
        }

        if (activeStars.size() > TARGET_SUMMARY_STARS) {
            List<ChartIdentity> shuffled = new ArrayList<>(activeStars);
            Collections.shuffle(shuffled, random);
            ChartIdentity keep = shuffled.get(0);
            for (ChartIdentity chart : shuffled.subList(1, shuffled.size())) {
                unstarChartWithVerification(chart);
            }
            Assert.assertTrue(isStarred(keep),
                    "Retained summary star should stay aria-checked=true: " + keep.label());
            return keep;
        }

        return starFirstSuccessfulIncludedChart(eligible, random);
    }

    /** Tries shuffled included charts until one star reads aria-checked=true (handles clipped/off-screen picks). */
    private ChartIdentity starFirstSuccessfulIncludedChart(List<ChartIdentity> eligible, Random random) {
        List<ChartIdentity> candidates = new ArrayList<>(eligible);
        Collections.shuffle(candidates, random);
        List<String> attempts = new ArrayList<>();

        for (ChartIdentity chart : candidates) {
            if (!isIncluded(chart)) {
                continue;
            }
            if (isStarred(chart)) {
                return chart;
            }
            currentChart = chart;
            try {
                clickStarSwitchWhenOff(chart);
                if (waitUntilStarred(chart, 8)) {
                    ZakaaDashLog.info("Star succeeded on chart: " + chart.label());
                    currentChart = null;
                    return chart;
                }
                attempts.add(chart.label() + " — click sent but aria-checked stayed false");
            } catch (RuntimeException failure) {
                attempts.add(chart.label() + " — " + failure.getMessage());
            }
            currentChart = null;
        }

        throw new AssertionError("Could not activate any chart star before save. Attempts:\n"
                + String.join("\n", attempts));
    }

    private boolean waitUntilStarred(ChartIdentity chart, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(webDriver -> isStarred(chart));
            return true;
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    private void verifyExactlyOneSummaryStar(ChartIdentity chosenStar) {
        long activeStars = readStates().stream().filter(ChartState::starred).count();
        Assert.assertEquals(activeStars, TARGET_SUMMARY_STARS,
                "Exactly one star should be active across all chart cards");
        Assert.assertEquals(readSummaryCounter()[0], TARGET_SUMMARY_STARS,
                "Summary counter should read one before save");
        Assert.assertTrue(isStarred(chosenStar),
                "Chosen chart star should read aria-checked=true: " + chosenStar.label());
        Assert.assertTrue(isIncluded(chosenStar),
                "Chosen chart should remain included: " + chosenStar.label());
    }

    private void starChartWithVerification(ChartIdentity chart, int expectedCount) {
        currentChart = chart;
        int countBefore = readSummaryCounter()[0];
        ZakaaDashLog.info("Starring " + chart.label() + " — counter before=" + countBefore
                + ", expected after=" + expectedCount);

        Assert.assertTrue(isIncluded(chart), "Chart inclusion must be true before starring: " + chart.label());
        if (isStarred(chart)) {
            Assert.assertEquals(countBefore, expectedCount,
                    "Already starred chart should match expected global count: " + chart.label());
            currentChart = null;
            return;
        }

        clickStarSwitchWhenOff(chart);
        wait.waitUntilTrue(webDriver -> isStarred(chart),
                "Star should become aria-checked=true: " + chart.label());
        wait.waitUntilTrue(webDriver -> readSummaryCounter()[0] == expectedCount,
                "Summary counter should read " + expectedCount + " after starring " + chart.label());

        int countAfter = readSummaryCounter()[0];
        ZakaaDashLog.info("Starred " + chart.label() + " — counter after=" + countAfter);
        Assert.assertTrue(isIncluded(chart),
                "Inclusion should stay enabled after starring: " + chart.label());
        currentChart = null;
    }

    private void unstarChartWithVerification(ChartIdentity chart) {
        currentChart = chart;
        if (!isStarred(chart)) {
            currentChart = null;
            return;
        }
        int countBefore = readSummaryCounter()[0];
        clickStarSwitchWhenOn(chart);
        wait.waitUntilTrue(webDriver -> !isStarred(chart),
                "Star should become aria-checked=false: " + chart.label());
        wait.waitUntilTrue(webDriver -> readSummaryCounter()[0] == countBefore - 1,
                "Summary counter should decrease after unstarring " + chart.label());
        currentChart = null;
    }

    /** Toasts render outside the dialog, so they are read from the document with geometry checks. */
    private List<String> visibleWarnings() {
        Object raw = js.executeScript("""
                const clean = text => (text || '').replace(/\\s+/g, ' ').trim();
                const messages = [];
                document.querySelectorAll('.p-toast-message, [role="alert"], [role="status"]')
                    .forEach(element => {
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        if (rect.width < 2 || rect.height < 2) return;
                        if (style.display === 'none' || style.visibility === 'hidden') return;
                        if (Number(style.opacity || '1') <= 0.05) return;
                        const text = clean(element.innerText);
                        if (text) messages.push(text);
                    });
                return messages;
                """);
        List<String> messages = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                messages.add(String.valueOf(item));
            }
        }
        return messages;
    }

    private void scrollModalContentToTopAndVerify() {
        scrollModalContentToEdge(true);
        wait.waitUntilTrue(webDriver -> readModalContentScrollTop() <= 2,
                "Modal content should scroll to the top");
    }

    private void scrollModalContentToBottomAndVerify() {
        scrollModalContentToEdge(false);
        wait.waitUntilTrue(webDriver -> {
            int[] metrics = readModalContentScrollMetrics();
            return metrics[0] >= metrics[1] - 2;
        }, "Modal content should scroll to the bottom");
    }

    private void scrollModalContentToEdge(boolean top) {
        runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return;
                const content = stageContent(dialog);
                if (!content) return;
                content.scrollTop = arguments[2] ? 0 : content.scrollHeight;
                """, top);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private int readModalContentScrollTop() {
        Object raw = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return -1;
                const content = stageContent(dialog);
                return content ? content.scrollTop : -1;
                """);
        return raw instanceof Number number ? number.intValue() : -1;
    }

    /** Returns {@code [scrollTop, maxScrollTop]}. */
    private int[] readModalContentScrollMetrics() {
        Object raw = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const content = stageContent(dialog);
                if (!content) return null;
                const max = Math.max(0, content.scrollHeight - content.clientHeight);
                return { scrollTop: content.scrollTop, maxScrollTop: max };
                """);
        if (!(raw instanceof Map<?, ?> map)) {
            return new int[] { -1, -1 };
        }
        return new int[] {
                ((Number) map.get("scrollTop")).intValue(),
                ((Number) map.get("maxScrollTop")).intValue()
        };
    }

    private void saveDashboardAndVerifyCreated(String expectedDashboardName) {
        clickSaveFooter();
        handleSaveConfirmationDialogIfPresent();
        assertNoSaveErrorVisible();
        wait.waitUntilTrue(
                webDriver -> runScript("return stageDialog(arguments[0], arguments[1]);") == null,
                "Dashboard creation dialog should close after clicking '" + SAVE_LABEL + "'");
        assertNoSaveErrorVisible();
        wait.waitUntilTrue(
                webDriver -> isExactDashboardNameVisible(expectedDashboardName),
                "Created dashboard name should appear in the UI: " + expectedDashboardName);
        AllureDomUtil.attachText("Dashboard save verification",
                "dashboardName=" + expectedDashboardName + "\nurl=" + driver.getCurrentUrl());
        ScreenshotUtil.captureAndAttach(driver, "dashboard-created");
        ZakaaDashLog.info("Dashboard created — verified name: " + expectedDashboardName);
    }

    private void clickSaveFooter() {
        Object save = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                return saveButton(dialog, arguments[1]);
                """);
        if (!(save instanceof WebElement saveButton)) {
            throw new NoSuchElementException("Footer save button '" + SAVE_LABEL + "' not found");
        }
        scrollIntoClippingViewport(saveButton);
        wait.waitForClickable(saveButton).click();
        HumanPaceUtil.pauseWizardTransition();
        ZakaaDashLog.info("Clicked footer save on final chart-selection stage");
    }

    private void handleSaveConfirmationDialogIfPresent() {
        // Needs the shared helpers (clean, saveButton); runScript passes STAR_ARIA_LABEL and SAVE_LABEL.
        Object handled = runScript("""
                const labels = ['تأكيد', 'نعم', 'موافق'];
                const saveLabel = arguments[1];
                const dialogs = document.querySelectorAll('[role="dialog"], [aria-modal="true"]');
                for (const dialog of dialogs) {
                    const rect = dialog.getBoundingClientRect();
                    if (rect.width < 80 || rect.height < 80) continue;
                    const content = dialog.querySelector('[data-pc-section="content"]');
                    if (content && content.querySelector('button[role="switch"]')) continue;
                    if (saveButton(dialog, saveLabel)) continue;
                    for (const button of dialog.querySelectorAll('button')) {
                        const label = clean(button.getAttribute('aria-label')) || clean(button.textContent);
                        if (labels.includes(label)) {
                            button.click();
                            return label;
                        }
                    }
                }
                return null;
                """);
        if (handled != null) {
            ZakaaDashLog.info("Save confirmation dialog — clicked: " + handled);
            HumanPaceUtil.pauseWizardTransition();
        }
    }

    private void assertNoSaveErrorVisible() {
        List<String> errors = visibleSaveErrors();
        if (!errors.isEmpty()) {
            ScreenshotUtil.captureAndAttach(driver, "dashboard-save-error");
            AllureDomUtil.attachText("Dashboard save error", String.join("\n", errors));
            throw new AssertionError("Dashboard save reported an error: " + errors);
        }
    }

    private List<String> visibleSaveErrors() {
        List<String> messages = new ArrayList<>(visibleWarnings());
        Object dialogError = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const nodes = dialog.querySelectorAll('[role="alert"], .p-message-error, .p-inline-message');
                for (const node of nodes) {
                    const rect = node.getBoundingClientRect();
                    if (rect.width < 2 || rect.height < 2) continue;
                    const text = clean(node.innerText);
                    if (text) return text;
                }
                return null;
                """);
        if (dialogError != null) {
            messages.add(String.valueOf(dialogError));
        }
        return messages.stream()
                .filter(message -> message.contains("خطأ") || message.toLowerCase().contains("error"))
                .toList();
    }

    private boolean isExactDashboardNameVisible(String expectedDashboardName) {
        Object visible = js.executeScript("""
                const expected = arguments[0];
                const clean = text => (text || '').replace(/\\s+/g, ' ').trim();
                const nodes = document.querySelectorAll(
                    'h1, h2, h3, h4, [data-testid], [class*="title"], span, a, button, p');
                for (const node of nodes) {
                    const rect = node.getBoundingClientRect();
                    if (rect.width < 2 || rect.height < 2) continue;
                    const style = window.getComputedStyle(node);
                    if (style.display === 'none' || style.visibility === 'hidden') continue;
                    if (Number(style.opacity || '1') <= 0.05) continue;
                    const text = clean(node.textContent);
                    if (text === expected) return true;
                    if (clean(node.innerText) === expected) return true;
                }
                return false;
                """, expectedDashboardName);
        return Boolean.TRUE.equals(visible);
    }

    // --- state reading --------------------------------------------------------------------------

    private boolean isIncluded(ChartIdentity chart) {
        return "true".equals(readAriaChecked(chart, "checkbox"));
    }

    private boolean isStarred(ChartIdentity chart) {
        return "true".equals(readAriaChecked(chart, "switch"));
    }

    private String readAriaChecked(ChartIdentity chart, String role) {
        Object value = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const card = cardAt(dialog, arguments[2], arguments[3], arguments[4], arguments[0]);
                if (!card) return null;
                const control = arguments[5] === 'switch'
                    ? starButtons(card, arguments[0])[0]
                    : card.querySelector('button[role="checkbox"]');
                return control ? control.getAttribute('aria-checked') : null;
                """, chart.group(), chart.title(), chart.ordinal(), role);
        return value == null ? null : String.valueOf(value);
    }

    /** Summary counter rendered as {@code current / max} in DOM order, independent of RTL display. */
    private int[] readSummaryCounter() {
        Object raw = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const content = stageContent(dialog);
                const label = arguments[2];
                const nodes = Array.from(content.querySelectorAll('span, div, p, h1, h2, h3'));
                const holder = nodes.find(element => clean(element.textContent).startsWith(label)
                    && !Array.from(element.children).some(child =>
                        clean(child.textContent).startsWith(label)));
                if (!holder) return null;
                let scope = holder.parentElement;
                for (let depth = 0; depth < 3 && scope; depth++) {
                    const match = clean(scope.innerText).match(/(\\d+)\\s*\\/\\s*(\\d+)/);
                    if (match) {
                        return { current: Number(match[1]), max: Number(match[2]) };
                    }
                    scope = scope.parentElement;
                }
                return null;
                """, SUMMARY_COUNTER_LABEL);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new NoSuchElementException("Summary star counter '" + SUMMARY_COUNTER_LABEL + "' not readable");
        }
        return new int[] {
                ((Number) map.get("current")).intValue(),
                ((Number) map.get("max")).intValue()
        };
    }

    // --- interaction ----------------------------------------------------------------------------

    private void scrollChartStarIntoView(ChartIdentity chart) {
        scrollGroupIntoView(chart.group());
        runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return false;
                return scrollChartStarIntoContent(
                    dialog,
                    arguments[2],
                    arguments[3],
                    arguments[4],
                    arguments[0],
                    arguments[5]);
                """, chart.group(), chart.title(), chart.ordinal(), VIEWPORT_TOP_OFFSET);
        HumanPaceUtil.pausePreprocessingAction();
    }

    /** Re-resolves the star switch, scrolls it into the modal viewport, clicks only when off. */
    private void clickStarSwitchWhenOff(ChartIdentity chart) {
        Assert.assertFalse(isStarred(chart),
                "Star switch should be off before click: " + chart.label());
        for (int attempt = 0; attempt < SCROLL_MAX_ATTEMPTS; attempt++) {
            scrollChartStarIntoView(chart);
            WebElement star = resolveStarControl(chart);
            if (!isInsideClippingViewport(star)) {
                continue;
            }
            if (isStarred(chart)) {
                return;
            }
            WebElement freshStar = resolveStarControl(chart);
            wait.waitForClickable(freshStar);
            new Actions(driver).moveToElement(freshStar).click().perform();
            HumanPaceUtil.pausePreprocessingAction();
            return;
        }
        throw new NoSuchElementException(
                "Star switch not in modal viewport for chart: " + chart.label());
    }

    /** Re-resolves the star switch, scrolls it into the modal viewport, clicks only when on. */
    private void clickStarSwitchWhenOn(ChartIdentity chart) {
        Assert.assertTrue(isStarred(chart),
                "Star switch should be on before unstar click: " + chart.label());
        for (int attempt = 0; attempt < SCROLL_MAX_ATTEMPTS; attempt++) {
            scrollChartStarIntoView(chart);
            WebElement star = resolveStarControl(chart);
            if (!isInsideClippingViewport(star)) {
                continue;
            }
            if (!isStarred(chart)) {
                return;
            }
            WebElement freshStar = resolveStarControl(chart);
            wait.waitForClickable(freshStar);
            new Actions(driver).moveToElement(freshStar).click().perform();
            HumanPaceUtil.pausePreprocessingAction();
            return;
        }
        throw new NoSuchElementException(
                "Star switch not in modal viewport for unstar: " + chart.label());
    }

    private WebElement cardElement(ChartIdentity chart) {
        Object card = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                return cardAt(dialog, arguments[2], arguments[3], arguments[4], arguments[0]);
                """, chart.group(), chart.title(), chart.ordinal());
        return card instanceof WebElement element ? element : null;
    }

    private WebElement resolveStarControl(ChartIdentity chart) {
        Object control = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return null;
                const card = cardAt(dialog, arguments[2], arguments[3], arguments[4], arguments[0]);
                if (!card) return null;
                return starButtons(card, arguments[0])[0] || null;
                """, chart.group(), chart.title(), chart.ordinal());
        if (control instanceof WebElement element) {
            return element;
        }
        throw new NoSuchElementException("Chart star control not found: " + chart.label());
    }

    /** Freshly resolved control, scrolled inside its clipping container, then clicked normally. */
    private void clickControl(WebElement control) {
        for (int attempt = 0; attempt < SCROLL_MAX_ATTEMPTS && !isInsideClippingViewport(control); attempt++) {
            scrollIntoClippingViewport(control);
        }
        wait.waitForClickable(control).click();
        HumanPaceUtil.pausePreprocessingAction();
    }

    private void scrollIntoClippingViewport(WebElement target) {
        js.executeScript("""
                const element = arguments[0];
                const offset = arguments[1];
                let node = element.parentElement;
                while (node) {
                    const style = window.getComputedStyle(node);
                    const scrollable = (style.overflowY === 'auto' || style.overflowY === 'scroll')
                        && node.scrollHeight > node.clientHeight + 4;
                    if (scrollable) {
                        const elementRect = element.getBoundingClientRect();
                        const nodeRect = node.getBoundingClientRect();
                        node.scrollTop += (elementRect.top - nodeRect.top) - offset;
                    }
                    if (node.classList && node.classList.contains('p-dialog')) {
                        break;
                    }
                    node = node.parentElement;
                }
                """, target, VIEWPORT_TOP_OFFSET);
        HumanPaceUtil.pausePreprocessingAction();
    }

    private boolean isInsideClippingViewport(WebElement target) {
        Object inside = js.executeScript("""
                const element = arguments[0];
                const rect = element.getBoundingClientRect();
                if (rect.width < 1 || rect.height < 1) return false;
                let node = element.parentElement;
                while (node) {
                    const style = window.getComputedStyle(node);
                    const clips = style.overflowY === 'auto' || style.overflowY === 'scroll'
                        || style.overflow === 'hidden';
                    if (clips) {
                        const bounds = node.getBoundingClientRect();
                        if (rect.top < bounds.top + 2 || rect.bottom > bounds.bottom - 2) {
                            return false;
                        }
                    }
                    if (node.classList && node.classList.contains('p-dialog')) {
                        break;
                    }
                    node = node.parentElement;
                }
                return true;
                """, target);
        return Boolean.TRUE.equals(inside);
    }

    // --- stage lookup and evidence --------------------------------------------------------------

    private WebElement requireStageDialog() {
        Object dialog = runScript("return stageDialog(arguments[0], arguments[1]);");
        if (dialog instanceof WebElement element) {
            return element;
        }
        throw new NoSuchElementException(
                "Final chart-selection dialog with chart cards and the '" + SAVE_LABEL + "' footer not found");
    }

    private Object runScript(String body, Object... extraArguments) {
        Object[] arguments = new Object[extraArguments.length + 2];
        arguments[0] = STAR_ARIA_LABEL;
        arguments[1] = SAVE_LABEL;
        System.arraycopy(extraArguments, 0, arguments, 2, extraArguments.length);
        return js.executeScript(DOM_HELPERS + body, arguments);
    }

    private void attachFailureEvidence(Throwable failure) {
        StringBuilder report = new StringBuilder();
        report.append("action=").append(currentAction).append('\n');
        report.append("failure=").append(failure.getMessage()).append('\n');
        if (currentChart != null) {
            report.append("chart=").append(currentChart.label()).append('\n');
            report.append("inclusion aria-checked=").append(readAriaChecked(currentChart, "checkbox")).append('\n');
            report.append("star aria-checked=").append(readAriaChecked(currentChart, "switch")).append('\n');
        }
        try {
            report.append("summaryCounter=").append(readSummaryCounter()[0])
                    .append(" / ").append(readSummaryCounter()[1]).append('\n');
        } catch (RuntimeException ex) {
            report.append("summaryCounter unavailable: ").append(ex.getMessage()).append('\n');
        }
        try {
            report.append("visibleWarnings=").append(visibleWarnings()).append('\n');
            report.append(describeScrollMetrics()).append('\n');
            report.append("cards=").append(discoverCharts().stream()
                    .map(ChartIdentity::label).collect(Collectors.joining("\n"))).append('\n');
            if (currentChart != null) {
                report.append("card outerHTML:\n")
                        .append(AllureDomUtil.outerHtml(cardElement(currentChart)))
                        .append('\n');
            }
        } catch (RuntimeException ex) {
            report.append("stage diagnostics incomplete: ").append(ex.getMessage()).append('\n');
        }
        AllureDomUtil.attachText("Final chart-selection failure — " + currentAction, report.toString());
        ScreenshotUtil.captureAndAttach(driver, "chart-selection-" + currentAction);
    }

    private String describeScrollMetrics() {
        Object metrics = runScript("""
                const dialog = stageDialog(arguments[0], arguments[1]);
                if (!dialog) return 'stage dialog not found';
                const content = stageContent(dialog);
                if (!content) return 'stage content not found';
                return 'content scrollTop=' + content.scrollTop
                    + ' scrollHeight=' + content.scrollHeight
                    + ' clientHeight=' + content.clientHeight;
                """);
        return String.valueOf(metrics);
    }

    private record ChartState(ChartIdentity identity, boolean included, boolean starred) {
    }

    private record ChartIdentity(String group, String title, int ordinal) {

        String label() {
            return group + " / " + title + (ordinal == 0 ? "" : " #" + (ordinal + 1));
        }
    }
}
