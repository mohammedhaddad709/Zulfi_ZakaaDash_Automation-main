package com.taf.systems.zulfi.pages.clubdetails;

import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.SquadPlayer;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.clubdata.SeasonStats;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.ui.PrimeVueSelect;
import com.taf.systems.zulfi.ui.UiRetry;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Club details at {@code /clubs/{externalTeamId}}: header, four tabs and their widgets (read-only). */
public final class ClubDetailsPage {

    public static final String TAB_OVERVIEW = "نظرة عامة";
    public static final String TAB_FIXTURES = "جدول المباريات والنتائج";
    public static final String TAB_SQUAD = "قائمة الفريق";
    public static final String TAB_ABOUT = "عن النادي";
    public static final List<String> TABS = List.of(TAB_OVERVIEW, TAB_FIXTURES, TAB_SQUAD, TAB_ABOUT);

    public static final String STATS_HEADING = "إحصائيات الفريق";
    public static final List<String> STAT_LABELS = List.of("لعب", "فوز", "تعادل", "خسارة", "له", "عليه", "الفارق", "النقاط");
    public static final String RESULTS_HEADING = "نتائج الفريق للموسم الحالي";
    public static final String NEXT_MATCH_HEADING = "المباراة القادمة";
    public static final String TOP_RATED = "أعلى تصنيف";
    public static final String TOP_SCORERS = "أفضل الهدافين";
    public static final String TOP_ASSISTS = "أفضل صانعي الأهداف";
    public static final String ALL_COMPETITIONS = "كل البطولات";
    public static final List<String> SQUAD_TEAMS = List.of("الفريق الأول", "فريق تحت 21 سنة", "فريق تحت 18 سنة");
    public static final List<String> SQUAD_GROUPS = List.of("المدافعون", "لاعبو الوسط", "المهاجمون", "حراس المرمى", "الإدارة");
    /** Player groups only («الإدارة» lists staff). */
    public static final List<String> PLAYER_GROUPS = SQUAD_GROUPS.subList(0, 4);

    private static final Pattern RANK = Pattern.compile("ترتيب رقم\\s*(\\d+)");
    private static final By HEADER = By.cssSelector("header h1");
    private static final By TAB = By.cssSelector("[role='tablist'] [role='tab']");
    private static final By BREADCRUMB = By.cssSelector(".p-breadcrumb-list");
    private static final By COMPETITION_COMBO = By.cssSelector("[role='tabpanel'][id$='_tabpanel_fixtures'] [role='combobox']");

    private static final String READ_SQUAD =
            "const panel = arguments[0];"
                    + "return [...panel.querySelectorAll('tbody tr[role=row]')].filter(tr => tr.getClientRects().length > 0).map(tr => {"
                    + "  const td = [...tr.querySelectorAll(':scope > td')];"
                    + "  const ps = td[0] ? [...td[0].querySelectorAll('p')].map(p => p.textContent.trim()) : [];"
                    + "  const t = i => td[i] ? td[i].innerText.trim() : '';"
                    + "  return { name: ps[0] || '', position: ps[1] || '', apps: t(1), minutes: t(2), value: t(3),"
                    + "    nationality: t(4), age: t(5), number: t(6) };"
                    + "});";

    private final WebDriver driver;

    public ClubDetailsPage(WebDriver driver) {
        this.driver = driver;
    }

    public ClubDetailsPage open(String externalTeamId) {
        driver.navigate().to(PortalConfig.baseUrl() + "clubs/" + externalTeamId);
        return waitUntilLoaded();
    }

    public ClubDetailsPage waitUntilLoaded() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .ignoring(RuntimeException.class)
                .until(d -> d.findElement(HEADER).isDisplayed() && !d.findElements(TAB).isEmpty());
        PortalDom.waitForSection(driver, STATS_HEADING);
        return this;
    }

    public String clubIdFromUrl() {
        Matcher m = Pattern.compile("/clubs/(\\d+)").matcher(driver.getCurrentUrl());
        return m.find() ? m.group(1) : "";
    }

    // --- header ---

    public String clubName() {
        return PortalDom.normalize(driver.findElement(HEADER).getText());
    }

    public String rankBadge() {
        return driver.findElement(HEADER).findElements(By.xpath("./following-sibling::span[1]")).stream()
                .map(e -> PortalDom.normalize(e.getText()))
                .findFirst()
                .orElse("");
    }

    public int rankFromBadge() {
        Matcher m = RANK.matcher(rankBadge());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public List<String> brokenHeaderImages() {
        return PortalDom.brokenImages(driver, driver.findElement(HEADER).findElement(By.xpath("./ancestor::header[1]")));
    }

    public List<String> breadcrumb() {
        return driver.findElements(BREADCRUMB).stream()
                .flatMap(ol -> ol.findElements(By.cssSelector("li.p-breadcrumb-item")).stream())
                .map(li -> PortalDom.normalize(li.getText()))
                .toList();
    }

    public void clickBreadcrumb(String label) {
        WebElement link = driver.findElement(BREADCRUMB)
                .findElement(By.xpath(".//a[contains(normalize-space(.),'" + label + "')]"));
        PortalOverlaySupport.dismissVisibleToasts(driver);
        link.click();
    }

    // --- tabs ---

    public List<String> tabLabels() {
        return driver.findElements(TAB).stream()
                .filter(WebElement::isDisplayed)
                .map(t -> PortalDom.normalize(t.getText()))
                .filter(t -> TABS.contains(t))
                .toList();
    }

    public String activeTab() {
        return driver.findElements(TAB).stream()
                .filter(t -> "true".equals(t.getAttribute("data-p-active")) || "true".equals(t.getAttribute("aria-selected")))
                .map(t -> PortalDom.normalize(t.getText()))
                .filter(TABS::contains)
                .findFirst()
                .orElse("");
    }

    public ClubDetailsPage selectTab(String label) {
        WebElement tab = driver.findElements(TAB).stream()
                .filter(t -> PortalDom.normalize(t.getText()).equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Club tab not found: " + label));
        PortalOverlaySupport.dismissVisibleToasts(driver);
        tab.click();
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> label.equals(activeTab()));
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return this;
    }

    /** The visible panel of the club-level tabs (nested squad tabs live inside it). */
    public WebElement activePanel() {
        return driver.findElements(By.cssSelector("[role='tabpanel'][id$='_tabpanel_overview'], [role='tabpanel'][id$='_tabpanel_fixtures'],"
                        + " [role='tabpanel'][id$='_tabpanel_squad'], [role='tabpanel'][id$='_tabpanel_about']"))
                .stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No visible club tab panel"));
    }

    // --- overview ---

    public Map<String, List<String>> statCards() {
        return ClubWidgetReader.kpis(driver, PortalDom.waitForSection(driver, STATS_HEADING));
    }

    public SeasonStats seasonStats() {
        Map<String, List<String>> cards = statCards();
        return new SeasonStats(
                stat(cards, "لعب"), stat(cards, "فوز"), stat(cards, "تعادل"), stat(cards, "خسارة"),
                stat(cards, "له"), stat(cards, "عليه"), stat(cards, "الفارق"), stat(cards, "النقاط"));
    }

    public List<ResultCard> resultCards() {
        return ClubWidgetReader.resultCards(driver, PortalDom.waitForSection(driver, RESULTS_HEADING));
    }

    public WebElement section(String heading) {
        return PortalDom.waitForSection(driver, heading);
    }

    public List<LeaderboardEntry> leaderboard(String heading) {
        return ClubWidgetReader.leaderboard(driver, PortalDom.waitForSection(driver, heading));
    }

    public void openLeaderboardProfile(String heading, int index) {
        List<WebElement> links = PortalDom.waitForSection(driver, heading).findElements(By.cssSelector("a[href]"));
        WebElement link = links.get(index);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        PortalOverlaySupport.dismissVisibleToasts(driver);
        link.click();
    }

    // --- fixtures tab ---

    public UpcomingCard nextMatch() {
        WebElement section = PortalDom.waitForSection(driver, NEXT_MATCH_HEADING);
        // the heading renders before the fixture data arrives
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(RuntimeException.class)
                .until(d -> !section.findElements(By.cssSelector("article span.truncate")).isEmpty());
        List<Map<String, Object>> rows = PortalDom.rows(driver,
                "const a = arguments[0].querySelector('article'); if (!a) return [];"
                        + "const names = [...a.querySelectorAll('span.truncate')].map(s => s.textContent.trim());"
                        + "const spans = [...a.querySelectorAll('span')].map(s => s.textContent.trim());"
                        + "const t = a.querySelector('time');"
                        + "return [{ round: spans.find(x => /الجولة|جولة/.test(x)) || '', home: names[0] || '', away: names[1] || '',"
                        + "  date: t ? t.textContent.trim() : '', time: spans.find(x => /\\d{1,2}:\\d{2}/.test(x)) || '' }];",
                section);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.getFirst();
        return new UpcomingCard(ClubWidgetReader.roundNumber(PortalDom.str(r, "round")), PortalDom.str(r, "home"),
                PortalDom.str(r, "away"), PortalDom.str(r, "date"), PortalDom.str(r, "time"));
    }

    public List<Fixture> fixturesTabRows() {
        return ClubWidgetReader.fixtures(driver, PortalDom.waitForSection(driver, TAB_FIXTURES), "");
    }

    /** Options of the fixtures tab competition select (e.g. «كل البطولات», league, cup). */
    public List<String> competitionOptions() {
        PrimeVueSelect select = competitionSelect().open();
        List<String> options = select.options();
        select.close();
        return options;
    }

    /** Picks a competition and waits until the fixture rows stop changing. */
    public ClubDetailsPage selectCompetition(String option) {
        String before = fixtureSignature();
        competitionSelect().pick(option);
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        waitForChangeThenStable(before, this::fixtureSignature);
        return this;
    }

    private PrimeVueSelect competitionSelect() {
        return new PrimeVueSelect(driver, COMPETITION_COMBO, "competition filter («" + ALL_COMPETITIONS + "»)");
    }

    private String fixtureSignature() {
        return UiRetry.onStale(() -> fixturesTabRows().toString());
    }

    public String competitionFilterLabel() {
        return PortalDom.waitForSection(driver, TAB_FIXTURES).findElements(By.cssSelector(".p-select-label")).stream()
                .map(e -> PortalDom.normalize(e.getText()))
                .findFirst()
                .orElse("");
    }

    // --- squad tab ---

    public List<String> squadTeamToggles() {
        return activePanel().findElements(By.cssSelector("button")).stream()
                .map(b -> PortalDom.normalize(b.getText()))
                .filter(SQUAD_TEAMS::contains)
                .toList();
    }

    public List<String> squadGroupTabs() {
        return activePanel().findElements(By.cssSelector("[role='tab']")).stream()
                .map(b -> PortalDom.normalize(b.getText()))
                .filter(t -> !t.isBlank())
                .toList();
    }

    public ClubDetailsPage selectSquadTeam(String label) {
        WebElement button = activePanel().findElements(By.cssSelector("button")).stream()
                .filter(b -> PortalDom.normalize(b.getText()).equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Squad team toggle not found: " + label));
        String before = squadSignature();
        button.click();
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        waitForChangeThenStable(before, this::squadSignature);
        return this;
    }

    /** Every player row of the selected team across {@link #PLAYER_GROUPS}, tagged with its group. */
    public Map<String, List<SquadPlayer>> squadRoster() {
        Map<String, List<SquadPlayer>> roster = new LinkedHashMap<>();
        for (String group : PLAYER_GROUPS) {
            roster.put(group, selectSquadGroup(group).squadRows());
        }
        return roster;
    }

    private String squadSignature() {
        return UiRetry.onStale(() -> squadRows().toString());
    }

    /**
     * Waits for content to differ from {@code before} (the new data arrived) and then hold still for one poll.
     * If it never changes the content may legitimately be identical, so the wait ends without failing and the
     * caller's assertion decides.
     */
    private void waitForChangeThenStable(String before, Supplier<String> signature) {
        String[] last = {null};
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(400))
                    .ignoring(RuntimeException.class)
                    .until(d -> {
                        String now = signature.get();
                        boolean settled = !now.equals(before) && now.equals(last[0]);
                        last[0] = now;
                        return settled;
                    });
        } catch (TimeoutException unchanged) {
            // unchanged for 10 s: nothing new is coming; the test compares what is shown
        }
    }

    public ClubDetailsPage selectSquadGroup(String label) {
        WebElement tab = activePanel().findElements(By.cssSelector("[role='tab']")).stream()
                .filter(b -> PortalDom.normalize(b.getText()).equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Squad group tab not found: " + label));
        tab.click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> "true".equals(tab.getAttribute("data-p-active")));
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return this;
    }

    public List<SquadPlayer> squadRows() {
        return PortalDom.rows(driver, READ_SQUAD, activePanel()).stream()
                .map(r -> new SquadPlayer(
                        PortalDom.str(r, "name"), PortalDom.str(r, "position"), dash(r, "apps"), dash(r, "minutes"),
                        dash(r, "value"), dash(r, "nationality"), dash(r, "age"), dash(r, "number")))
                .toList();
    }

    public List<String> brokenImagesInActivePanel() {
        return PortalDom.brokenImages(driver, activePanel());
    }

    /** Distinct visible text fragments of the active panel that contain a Latin-script word (untranslated UI text). */
    public List<String> latinTextInActivePanel() {
        return PortalDom.rows(driver,
                        "const out = new Set(); const walker = document.createTreeWalker(arguments[0], NodeFilter.SHOW_TEXT);"
                                + "while (walker.nextNode()) { const n = walker.currentNode, t = n.textContent.trim();"
                                + "  if (!/[A-Za-z]{2,}/.test(t) || !n.parentElement || n.parentElement.getClientRects().length === 0) continue;"
                                + "  out.add(t.slice(0, 80)); }"
                                + "return [...out].slice(0, 30).map(t => ({t}));",
                        activePanel())
                .stream().map(r -> PortalDom.str(r, "t")).toList();
    }

    public int imageCountInActivePanel() {
        return PortalDom.visibleImageCount(driver, activePanel());
    }

    // --- read-only checks ---

    /** Visible add / edit / delete controls inside the active tab panel (the club page is read-only). */
    public List<String> mutationControlsInActivePanel() {
        return PortalDom.rows(driver,
                        "return [...arguments[0].querySelectorAll('button, a, [role=button]')]"
                                + ".filter(e => e.getClientRects().length > 0)"
                                + ".map(e => ((e.getAttribute('aria-label') || '') + ' ' + e.innerText).trim())"
                                + ".filter(t => /إضافة|اضافة|تعديل|حذف|حفظ/.test(t)).map(t => ({t}));",
                        activePanel())
                .stream().map(r -> PortalDom.str(r, "t")).toList();
    }

    /** Editable fields in the active panel; filters/selects of the fixtures tab are excluded by the caller. */
    public int editableFieldsInActivePanel() {
        Object count = ((JavascriptExecutor) driver).executeScript(
                "return [...arguments[0].querySelectorAll('textarea, [contenteditable=true], input:not([type=hidden]):not([readonly])')]"
                        + ".filter(e => e.getClientRects().length > 0 && !e.closest('.p-select, .p-datepicker')).length;",
                activePanel());
        return count == null ? 0 : ((Number) count).intValue();
    }

    public ClubDetailsPage reload() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    // --- about tab ---

    /** heading → paragraph text for each «عن النادي» block. */
    public Map<String, String> aboutBlocks() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> r : PortalDom.rows(driver,
                "return [...arguments[0].querySelectorAll('section')].map(s => ({h: (s.querySelector('h1,h2,h3')||{textContent:''}).textContent.trim(),"
                        + " p: [...s.querySelectorAll('p')].map(p => p.textContent.trim()).join(' ')}));",
                activePanel())) {
            String heading = PortalDom.str(r, "h");
            if (!heading.isBlank()) {
                out.put(heading, PortalDom.str(r, "p"));
            }
        }
        return out;
    }

    private static int stat(Map<String, List<String>> cards, String label) {
        List<String> values = cards.getOrDefault(label, List.of());
        return values.isEmpty() ? Integer.MIN_VALUE : PortalDom.parseIntOr(values.getLast(), Integer.MIN_VALUE);
    }

    private static String dash(Map<String, Object> row, String key) {
        String value = PortalDom.str(row, key);
        return value.equals("—") || value.equals("-") ? "" : value;
    }
}
