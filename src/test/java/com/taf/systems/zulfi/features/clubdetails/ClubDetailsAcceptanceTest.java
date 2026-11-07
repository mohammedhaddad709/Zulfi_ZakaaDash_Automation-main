package com.taf.systems.zulfi.features.clubdetails;

import com.taf.systems.zulfi.clubdata.ClubDataComparisons;
import com.taf.systems.zulfi.clubdata.ClubFeatureTestIds;
import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.SquadPlayer;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.clubdata.SeasonStats;
import com.taf.systems.zulfi.config.FeaturePhaseGate;
import com.taf.systems.zulfi.config.FeaturePhaseGate.Phase;
import com.taf.systems.zulfi.features.clubdata.ClubDataUiTest;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.pages.clubdetails.ClubDetailsPage;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage.LeagueClubCard;
import com.taf.systems.zulfi.reporting.DefectClassification;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * US-FAP-CLUB-DETAILS — {@code /clubs/{id}} for the Default Club: header, tabs, overview statistics, leaderboards,
 * fixtures, squad (photos, localisation, data sanity), about, and layout. Enable via {@code -Dclub.details.phase=a|b|c}.
 */
@Epic("Zulfi Portal")
@Feature(ClubFeatureTestIds.FEATURE_CLUB_DETAILS)
public class ClubDetailsAcceptanceTest extends ClubDataUiTest {

    private static final Pattern RANK_BADGE = Pattern.compile("ترتيب رقم \\d+ في دوري الدرجة الأولى");
    private static final Pattern SCORE = Pattern.compile("\\d+ - \\d+");
    private static final Pattern RATING = Pattern.compile("\\d+\\.\\d{1,2}");
    private static final Pattern LATIN_ONLY = Pattern.compile("^[A-Za-z]{1,3}$");
    private static final String STAFF_GROUP = "الإدارة";
    private static final String SQUAD_EMPTY_STATE = "لا يوجد أعضاء في هذا التصنيف";
    private static final String LEAGUE_WORD = "دوري";
    /** «VS» on the next-match card is a graphic convention of Arabic sports UIs, not untranslated copy. */
    private static final Set<String> LATIN_ALLOWED = Set.of("VS");

    @Override
    protected FeaturePhaseGate gate() {
        return FeaturePhaseGate.CLUB_DETAILS;
    }

    @Override
    protected String story() {
        return ClubFeatureTestIds.STORY_CLUB_DETAILS;
    }

    @Override
    protected String feature() {
        return ClubFeatureTestIds.FEATURE_CLUB_DETAILS;
    }

    // ---------- phase a: smoke ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_001 + " — «عرض تفاصيل النادي» opens the club by ID")
    public void clubDet001_detailsButtonOpensClubById() {
        String id = ClubFeatureTestIds.CLUB_DET_001;
        requirePhase(Phase.A, id, "«عرض تفاصيل النادي» opens /clubs/{id}");
        LeagueClubCard card = flow().defaultClubCard();
        String expectedId = ClubsListPage.extractClubIdFromHref(card.detailsHref());
        step(id, "Click «عرض تفاصيل النادي» on «" + card.name() + "»", "URL /clubs/" + expectedId + " and header = club name", () -> {
            WebElement link = browser().findElement(By.cssSelector("a[href='" + card.detailsHref() + "']"));
            ((JavascriptExecutor) browser()).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
            PortalOverlaySupport.dismissVisibleToasts(browser());
            link.click();
            ClubDetailsPage page = new ClubDetailsPage(browser()).waitUntilLoaded();
            boolean ok = expectedId.equals(page.clubIdFromUrl()) && Fixture.sameTeam(page.clubName(), card.name());
            return ok ? StepResult.pass("url=" + browser().getCurrentUrl() + "; h1=" + page.clubName())
                    : StepResult.fail("url=" + browser().getCurrentUrl() + "; h1=" + page.clubName());
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_002 + " — Header: name, rank badge, logo")
    public void clubDet002_header() {
        String id = ClubFeatureTestIds.CLUB_DET_002;
        requirePhase(Phase.A, id, "Club header name, «ترتيب رقم N» badge and logo");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Club name", "h1 = «" + ClubDataFlow.defaultClub() + "»", () ->
                compare("h1", Fixture.canonical(ClubDataFlow.defaultClub()), Fixture.canonical(page.clubName())));
        step(id, "Rank badge", "Badge reads «# ترتيب رقم N في دوري الدرجة الأولى»", () ->
                RANK_BADGE.matcher(page.rankBadge()).find()
                        ? StepResult.pass(page.rankBadge()) : StepResult.fail("badge=«" + page.rankBadge() + "»"));
        step(id, "Header logo", "Club logo image loads", () ->
                verdict("logo loaded", page.brokenHeaderImages().stream().map(b -> "broken " + b).toList()));
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_003 + " — Four tabs in order, overview active by default")
    public void clubDet003_tabs() {
        String id = ClubFeatureTestIds.CLUB_DET_003;
        requirePhase(Phase.A, id, "Tabs " + ClubDetailsPage.TABS);
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Tab labels", "Tabs " + ClubDetailsPage.TABS, () -> compare("tabs", ClubDetailsPage.TABS, page.tabLabels()));
        step(id, "Default tab", "«" + ClubDetailsPage.TAB_OVERVIEW + "» active", () ->
                compare("active tab", ClubDetailsPage.TAB_OVERVIEW, page.activeTab()));
        step(id, "Every tab opens", "Each tab becomes active and shows content", () -> {
            List<String> problems = new ArrayList<>();
            for (String tab : ClubDetailsPage.TABS) {
                page.selectTab(tab);
                if (page.activePanel().getText().isBlank()) {
                    problems.add("«" + tab + "» panel is empty");
                }
            }
            return verdict("all tabs render", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_004 + " — Breadcrumb trail and navigation")
    public void clubDet004_breadcrumb() {
        String id = ClubFeatureTestIds.CLUB_DET_004;
        requirePhase(Phase.A, id, "Breadcrumb الرئيسية › الأندية › club");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        List<String> expected = List.of("الرئيسية", "الأندية", page.clubName());
        step(id, "Breadcrumb items", "Trail " + expected, () -> compare("breadcrumb", expected, page.breadcrumb()));
        step(id, "Click «الأندية»", "Returns to /clubs", () -> {
            page.clickBreadcrumb("الأندية");
            new WebDriverWait(browser(), Duration.ofSeconds(15)).until(d -> d.getCurrentUrl().endsWith("/clubs"));
            return StepResult.pass("url=" + browser().getCurrentUrl());
        });
    }

    // ---------- phase b: overview ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_005 + " — Season statistics cards are complete and consistent")
    public void clubDet005_seasonStats() {
        String id = ClubFeatureTestIds.CLUB_DET_005;
        requirePhase(Phase.B, id, "«إحصائيات الفريق» 8 cards; P=W+D+L, Pts=3W+D, GD=GF−GA");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        Map<String, List<String>> cards = page.statCards();
        step(id, "Card labels", "Cards " + ClubDetailsPage.STAT_LABELS, () ->
                compare("labels", ClubDetailsPage.STAT_LABELS, List.copyOf(cards.keySet())));
        step(id, "Arithmetic", "Totals agree", () -> {
            SeasonStats stats = page.seasonStats();
            TestStepReporter.recordTestData("clubStats", stats.describe());
            return verdict(stats.describe(), stats.internalProblems());
        });
        step(id, "Goal difference sign", "Positive «الفارق» rendered with «+»", () -> {
            String gd = cards.getOrDefault("الفارق", List.of("")).getLast();
            int value = PortalDom.parseIntOr(gd, 0);
            return value > 0 && !gd.startsWith("+") ? StepResult.fail("الفارق shows «" + gd + "»")
                    : StepResult.pass("الفارق «" + gd + "»");
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_006 + " — «نتائج الفريق للموسم الحالي» cards")
    public void clubDet006_seasonResultCards() {
        String id = ClubFeatureTestIds.CLUB_DET_006;
        requirePhase(Phase.B, id, "Up to 5 result cards: score format, opponent logo, colour = outcome");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        List<ResultCard> cards = page.resultCards();
        step(id, "Card count", "1..5 cards", () -> cards.isEmpty() || cards.size() > 5
                ? StepResult.fail(cards.size() + " cards") : StepResult.pass(cards.size() + " cards"));
        step(id, "Each card", "«x - y» score, opponent named, tag colour W green / D neutral / L red", () -> {
            List<String> problems = new ArrayList<>();
            for (ResultCard c : cards) {
                if (!SCORE.matcher(c.score()).matches()) {
                    problems.add(c.describe() + ": score format");
                }
                if (c.opponent().isBlank()) {
                    problems.add(c.describe() + ": no opponent name/alt");
                }
                if (ClubWidgetReader.outcomeFromColor(c.tagClass()) != c.outcome()) {
                    problems.add(c.describe() + ": outcome " + c.outcome() + " but colour " + c.tagClass());
                }
            }
            return verdict(cards.stream().map(ResultCard::describe).toList().toString(), problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_007 + " — Every image on the overview loads")
    public void clubDet007_overviewImagesLoad() {
        String id = ClubFeatureTestIds.CLUB_DET_007;
        requirePhase(Phase.B, id, "Opponent logos and player photos on «نظرة عامة» load");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Images", "No broken image among " + page.imageCountInActivePanel(), () ->
                verdict(page.imageCountInActivePanel() + " images loaded",
                        page.brokenImagesInActivePanel().stream().map(b -> "broken " + b).toList()));
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_008 + " — «أعلى تصنيف» leaderboard")
    public void clubDet008_topRated() {
        assertLeaderboard(ClubFeatureTestIds.CLUB_DET_008, ClubDetailsPage.TOP_RATED, true);
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_009 + " — «أفضل الهدافين» leaderboard")
    public void clubDet009_topScorers() {
        assertLeaderboard(ClubFeatureTestIds.CLUB_DET_009, ClubDetailsPage.TOP_SCORERS, false);
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_010 + " — «أفضل صانعي الأهداف» leaderboard")
    public void clubDet010_topAssists() {
        assertLeaderboard(ClubFeatureTestIds.CLUB_DET_010, ClubDetailsPage.TOP_ASSISTS, false);
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_011 + " — Leaderboard player opens the player profile")
    public void clubDet011_playerProfileNavigation() {
        String id = ClubFeatureTestIds.CLUB_DET_011;
        requirePhase(Phase.B, id, "Player name link → /clubs/players/{id} showing that player");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        LeaderboardEntry first = page.leaderboard(ClubDetailsPage.TOP_RATED).getFirst();
        step(id, "Open «" + first.name() + "»", "Navigates to " + first.profileHref() + " and shows the player name", () -> {
            page.openLeaderboardProfile(ClubDetailsPage.TOP_RATED, 0);
            new WebDriverWait(browser(), Duration.ofSeconds(20)).until(d -> d.getCurrentUrl().contains("/clubs/players/"));
            PortalOverlaySupport.waitForBlockingLoaderToClear(browser());
            boolean named = new WebDriverWait(browser(), Duration.ofSeconds(20)).ignoring(RuntimeException.class)
                    .until(d -> PortalDom.normalize(d.findElement(By.tagName("main")).getText()).contains(first.name()));
            return named ? StepResult.pass("url=" + browser().getCurrentUrl())
                    : StepResult.fail("profile page does not show «" + first.name() + "»");
        });
    }

    // ---------- phase b: fixtures tab ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_012 + " — Fixtures tab: next match and result rows")
    public void clubDet012_fixturesTab() {
        String id = ClubFeatureTestIds.CLUB_DET_012;
        requirePhase(Phase.B, id, "«المباراة القادمة» + rows involving the club; played rows scored and dated");
        String club = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES);
        step(id, "Next match card", "Round, both teams (one is the club), date and time", () -> {
            UpcomingCard next = page.nextMatch();
            if (next == null) {
                return StepResult.fail("no «المباراة القادمة» card");
            }
            boolean ok = next.round() > 0 && (Fixture.sameTeam(next.homeTeam(), club) || Fixture.sameTeam(next.awayTeam(), club))
                    && !next.dateText().isBlank() && !next.timeText().isBlank();
            return ok ? StepResult.pass(next.describe()) : StepResult.fail(next.describe());
        });
        step(id, "Result rows", "Every row involves the club, has a date; played rows show a score and analysis status", () -> {
            List<Fixture> rows = page.fixturesTabRows();
            List<String> problems = new ArrayList<>();
            if (rows.isEmpty()) {
                problems.add("no rows");
            }
            for (Fixture f : rows) {
                if (!f.involves(club)) {
                    problems.add(f.describe() + ": club not involved");
                }
                if (f.dateText().isBlank()) {
                    problems.add(f.describe() + ": no date");
                }
                if (f.played() && f.analysisLabel().isBlank()) {
                    problems.add(f.describe() + ": no analysis status");
                }
            }
            for (int i = 1; i < rows.size(); i++) {
                if (rows.get(i).seasonOrderKey() < rows.get(i - 1).seasonOrderKey()) {
                    problems.add("order: " + rows.get(i - 1).describe() + " listed before " + rows.get(i).describe());
                }
            }
            return verdict(rows.size() + " rows", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_013 + " — Fixtures tab competition filter defaults to «كل البطولات»")
    public void clubDet013_competitionFilter() {
        String id = ClubFeatureTestIds.CLUB_DET_013;
        requirePhase(Phase.B, id, "Competition select shows «كل البطولات» and lists both competitions' rows");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES);
        step(id, "Default filter", "«" + ClubDetailsPage.ALL_COMPETITIONS + "»", () ->
                compare("filter", ClubDetailsPage.ALL_COMPETITIONS, page.competitionFilterLabel()));
        step(id, "All competitions listed", "League rounds and at least one cup row (round without league number) when the club played the cup", () -> {
            List<Fixture> rows = page.fixturesTabRows();
            long league = rows.stream().filter(f -> f.round() > 0).count();
            long other = rows.size() - league;
            return league > 0 ? StepResult.pass("league rows=" + league + "; other competitions=" + other)
                    : StepResult.fail("no league rows among " + rows.size());
        });
    }

    // ---------- phase b: squad tab ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_014 + " — Squad tab: team toggles and position groups")
    public void clubDet014_squadStructure() {
        String id = ClubFeatureTestIds.CLUB_DET_014;
        requirePhase(Phase.B, id, "Toggles " + ClubDetailsPage.SQUAD_TEAMS + "; groups " + ClubDetailsPage.SQUAD_GROUPS);
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_SQUAD);
        step(id, "Team toggles", "Toggles " + ClubDetailsPage.SQUAD_TEAMS, () ->
                compare("toggles", ClubDetailsPage.SQUAD_TEAMS, page.squadTeamToggles()));
        step(id, "Position groups", "Groups " + ClubDetailsPage.SQUAD_GROUPS, () ->
                compare("groups", ClubDetailsPage.SQUAD_GROUPS, page.squadGroupTabs()));
        step(id, "Players per group (first team)",
                "Every player group lists at least one row; «الإدارة» lists staff or shows its empty state", () -> {
            List<String> problems = new ArrayList<>();
            StringBuilder counts = new StringBuilder();
            for (String group : ClubDetailsPage.SQUAD_GROUPS) {
                int n = page.selectSquadGroup(group).squadRows().size();
                counts.append(group).append('=').append(n).append(' ');
                boolean emptyStateShown = PortalDom.normalize(page.activePanel().getText()).contains(SQUAD_EMPTY_STATE);
                if (n == 0 && !(STAFF_GROUP.equals(group) && emptyStateShown)) {
                    problems.add("«" + group + "» is empty" + (emptyStateShown ? " (empty state shown)" : ""));
                }
            }
            return verdict(counts.toString().trim(), problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_015 + " — Every squad photo loads")
    public void clubDet015_squadPhotosLoad() {
        String id = ClubFeatureTestIds.CLUB_DET_015;
        requirePhase(Phase.B, id, "No broken player photo in any first-team group");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_SQUAD);
        step(id, "Photos per group", "Every visible player photo has naturalWidth > 0", () -> {
            List<String> problems = new ArrayList<>();
            int images = 0;
            for (String group : ClubDetailsPage.SQUAD_GROUPS) {
                page.selectSquadGroup(group);
                images += page.imageCountInActivePanel();
                page.brokenImagesInActivePanel().stream()
                        .filter(b -> !b.contains("player-number-badge"))
                        .forEach(b -> problems.add(group + ": broken photo " + b));
            }
            return verdict(images + " images checked", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_016 + " — Player positions are localised to Arabic")
    public void clubDet016_positionsLocalised() {
        String id = ClubFeatureTestIds.CLUB_DET_016;
        requirePhase(Phase.B, id, "No raw position code (e.g. «D», «M», «F») under a player name");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_SQUAD);
        step(id, "Positions per group", "Every position label is Arabic text", () -> {
            List<String> problems = new ArrayList<>();
            int players = 0;
            for (String group : ClubDetailsPage.SQUAD_GROUPS) {
                for (SquadPlayer p : page.selectSquadGroup(group).squadRows()) {
                    players++;
                    if (p.position().isBlank() || LATIN_ONLY.matcher(p.position()).matches()) {
                        problems.add(group + ": «" + p.name() + "» position «" + p.position() + "»");
                    }
                }
            }
            return verdict(players + " players localised", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_017 + " — Squad data sanity (numbers, ages, minutes)")
    public void clubDet017_squadDataSanity() {
        String id = ClubFeatureTestIds.CLUB_DET_017;
        requirePhase(Phase.B, id, "Unique shirt numbers; ages 15–45; minutes ≤ appearances × 120");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_SQUAD);
        step(id, "First-team rows", "No duplicate number, implausible age or impossible minutes", () -> {
            List<String> problems = new ArrayList<>();
            Map<String, List<String>> owners = new TreeMap<>();
            int players = 0;
            for (String group : ClubDetailsPage.SQUAD_GROUPS) {
                if (STAFF_GROUP.equals(group)) {
                    continue;
                }
                for (SquadPlayer p : page.selectSquadGroup(group).squadRows()) {
                    players++;
                    if (!p.shirtNumber().isBlank()) {
                        owners.computeIfAbsent(p.shirtNumber(), k -> new ArrayList<>()).add(p.name());
                    }
                    int age = PortalDom.parseIntOr(p.age(), -1);
                    if (!p.age().isBlank() && (age < 15 || age > 45)) {
                        problems.add(p.name() + ": age " + p.age());
                    }
                    int apps = PortalDom.parseIntOr(p.appearances(), 0);
                    int minutes = PortalDom.parseIntOr(p.minutes(), 0);
                    if (minutes > apps * 120) {
                        problems.add(p.name() + ": " + minutes + " min in " + apps + " appearances");
                    }
                }
            }
            owners.forEach((number, names) -> {
                if (names.size() > 1) {
                    problems.add("shirt #" + number + " shared by " + names);
                }
            });
            return verdict(players + " players sane", problems);
        });
    }

    // ---------- phase b: about ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_018 + " — «عن النادي» biography blocks")
    public void clubDet018_aboutTab() {
        String id = ClubFeatureTestIds.CLUB_DET_018;
        requirePhase(Phase.B, id, "Biography headings each have text and name the club");
        String clubName = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_ABOUT);
        step(id, "Blocks", "Every heading has a non-empty paragraph", () -> {
            Map<String, String> blocks = page.aboutBlocks();
            List<String> problems = new ArrayList<>();
            if (blocks.isEmpty()) {
                problems.add("no biography blocks");
            }
            blocks.forEach((h, p) -> {
                if (p.isBlank()) {
                    problems.add("«" + h + "» has no text");
                }
            });
            String all = String.join(" ", blocks.values());
            if (!all.contains(Fixture.canonical(clubName))) {
                problems.add("biography never mentions «" + clubName + "»");
            }
            return verdict(blocks.keySet().toString(), problems);
        });
    }

    // ---------- phase b: read-only + refresh (migrated from CLUB-INF-008/023/024/025) ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_022 + " — Club page is read-only on every tab")
    public void clubDet022_readOnlyEverywhere() {
        String id = ClubFeatureTestIds.CLUB_DET_022;
        requirePhase(Phase.B, id, "No add/edit/delete/save control and no editable field on any tab (was CLUB-INF-008/023/024)");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Inspect each tab", "0 mutation controls and 0 editable fields per tab", () -> {
            List<String> problems = new ArrayList<>();
            for (String tab : ClubDetailsPage.TABS) {
                page.selectTab(tab);
                page.mutationControlsInActivePanel().forEach(c -> problems.add("«" + tab + "» control «" + c + "»"));
                int editable = page.editableFieldsInActivePanel();
                if (editable > 0) {
                    problems.add("«" + tab + "» has " + editable + " editable field(s)");
                }
            }
            return verdict("all tabs read-only", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_023 + " — Refresh shows the same club data")
    public void clubDet023_refreshKeepsClubData() {
        String id = ClubFeatureTestIds.CLUB_DET_023;
        requirePhase(Phase.B, id, "Reload re-renders identical stats and «عن النادي» headings (was CLUB-INF-025)");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        SeasonStats before = page.seasonStats();
        List<String> aboutBefore = List.copyOf(page.selectTab(ClubDetailsPage.TAB_ABOUT).aboutBlocks().keySet());
        step(id, "Reload the page", "Same stats " + before.describe() + " and headings " + aboutBefore, () -> {
            page.reload();
            SeasonStats after = page.seasonStats();
            List<String> aboutAfter = List.copyOf(page.selectTab(ClubDetailsPage.TAB_ABOUT).aboutBlocks().keySet());
            List<String> problems = new ArrayList<>(after.differencesFrom(before).stream().map(d -> "stats " + d).toList());
            if (!aboutAfter.equals(aboutBefore)) {
                problems.add("about headings " + aboutBefore + " → " + aboutAfter);
            }
            return verdict("stats and about unchanged after reload", problems);
        });
    }

    // ---------- phase b: tab-to-tab logic on the same page ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_024 + " — Overview results = latest played matches of the fixtures tab")
    public void clubDet024_overviewResultsMatchFixturesTab() {
        String id = ClubFeatureTestIds.CLUB_DET_024;
        requirePhase(Phase.B, id, "«" + ClubDetailsPage.RESULTS_HEADING + "» summarises «" + ClubDetailsPage.TAB_FIXTURES + "»");
        String club = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails();
        List<ResultCard> cards = page.resultCards();
        List<Fixture> tab = ClubDataFlow.clubFixtures(page.selectTab(ClubDetailsPage.TAB_FIXTURES).fixturesTabRows(), club);
        List<Fixture> expected = ClubDataFlow.lastPlayedNewestFirst(tab, 5);
        step(id, "Compare card by card (newest first)",
                "Opponent and club-first score = " + expected.stream().map(f -> f.opponent(club) + " " + f.clubFirstScore(club)).toList(),
                () -> verdict(cards.stream().map(ResultCard::describe).toList().toString(),
                        ClubDataComparisons.resultCards(cards, expected, club, "fixtures tab")));
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_025 + " — «إحصائيات الفريق» = totals recomputed from the league rows of the fixtures tab")
    public void clubDet025_statsMatchFixturesTabLeagueRows() {
        String id = ClubFeatureTestIds.CLUB_DET_025;
        requirePhase(Phase.B, id, "Overview league totals vs the league matches listed on the same page");
        String club = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails();
        SeasonStats shown = page.seasonStats();
        page.selectTab(ClubDetailsPage.TAB_FIXTURES);
        String league = leagueOption(page.competitionOptions());
        List<Fixture> leagueRows = ClubDataFlow.played(page.selectCompetition(league).fixturesTabRows());
        SeasonStats computed = SeasonStats.fromFixtures(club, leagueRows);
        step(id, "Recompute from «" + league + "» rows (" + leagueRows.size() + " played)", "Shown " + shown.describe(),
                () -> verdict(computed.describe(), computed.differencesFrom(shown).stream()
                        .map(d -> "fixtures tab vs card: " + d).toList()));
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_026 + " — «المباراة القادمة» follows the club's last played match")
    public void clubDet026_nextMatchFollowsLastResult() {
        String id = ClubFeatureTestIds.CLUB_DET_026;
        requirePhase(Phase.B, id, "Next-match card vs the matches listed on the same tab");
        String club = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES);
        UpcomingCard next = page.nextMatch();
        List<Fixture> rows = ClubDataFlow.clubFixtures(page.fixturesTabRows(), club);
        List<Fixture> upcoming = ClubDataFlow.upcoming(rows);
        List<Fixture> played = ClubDataFlow.played(rows);
        step(id, "Card involves the club and is not a played match", "«" + club + "» on the card; no listed result for the same teams and date", () -> {
            if (next == null) {
                return StepResult.fail("no «" + ClubDetailsPage.NEXT_MATCH_HEADING + "» card");
            }
            Fixture card = asFixture(next);
            List<String> problems = new ArrayList<>();
            if (!card.involves(club)) {
                problems.add("club not on the card");
            }
            played.stream().filter(f -> ClubDataComparisons.sameMatch(f, card)).findFirst()
                    .ifPresent(f -> problems.add("already played: " + f.describe()));
            return verdict(next.describe(), problems);
        });
        step(id, "Chronology", "Card is dated after the last result and its round follows the last league round played", () -> {
            if (next == null) {
                return StepResult.fail("no card");
            }
            Fixture card = asFixture(next);
            if (!upcoming.isEmpty()) {
                return verdict(next.describe(), ClubDataComparisons.upcoming(List.of(next), List.of(upcoming.getFirst()), "fixtures tab"));
            }
            List<String> problems = new ArrayList<>();
            played.stream().filter(f -> f.seasonOrderKey() > card.seasonOrderKey()).forEach(f ->
                    problems.add("result " + f.describe() + " is dated after the «next» match " + next.dateText()));
            int lastLeagueRound = played.stream().mapToInt(Fixture::round).max().orElse(0);
            if (next.round() > 0 && lastLeagueRound > 0 && next.round() != lastLeagueRound + 1) {
                problems.add("round " + next.round() + " but the last league round played is " + lastLeagueRound);
            }
            return verdict(next.describe() + " after round " + lastLeagueRound
                    + " (the tab lists results only; upcoming fixtures appear in the card)", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_027 + " — Competition filter narrows the fixtures tab and «كل البطولات» restores it")
    public void clubDet027_competitionFilterNarrowsRows() {
        String id = ClubFeatureTestIds.CLUB_DET_027;
        requirePhase(Phase.B, id, "Each competition shows only its matches; together they are exactly «كل البطولات»");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES);
        List<Fixture> all = page.fixturesTabRows();
        List<String> options = page.competitionOptions();
        List<String> competitions = options.stream().filter(o -> !o.contains(ClubDetailsPage.ALL_COMPETITIONS)).toList();
        step(id, "Options", "«" + ClubDetailsPage.ALL_COMPETITIONS + "» plus at least one competition", () ->
                options.stream().anyMatch(o -> o.contains(ClubDetailsPage.ALL_COMPETITIONS)) && !competitions.isEmpty()
                        ? StepResult.pass(options.toString()) : StepResult.fail(options.toString()));
        Map<String, List<Fixture>> byCompetition = new LinkedHashMap<>();
        for (String competition : competitions) {
            List<Fixture> rows = page.selectCompetition(competition).fixturesTabRows();
            byCompetition.put(competition, rows);
            boolean league = competition.contains(LEAGUE_WORD);
            step(id, "Filter «" + competition + "»", "Only rows from «كل البطولات»" + (league ? ", each with a league round" : ""), () -> {
                List<String> problems = new ArrayList<>();
                for (Fixture f : rows) {
                    if (all.stream().noneMatch(a -> ClubDataComparisons.sameMatch(a, f))) {
                        problems.add(f.describe() + ": not listed under «" + ClubDetailsPage.ALL_COMPETITIONS + "»");
                    }
                    if (league && f.round() <= 0) {
                        problems.add(f.describe() + ": no league round");
                    }
                }
                if (rows.size() == all.size() && competitions.size() > 1) {
                    problems.add("filter did not narrow the list (" + rows.size() + " rows, same as all)");
                }
                return verdict(rows.size() + " rows", problems);
            });
        }
        step(id, "Partition", "Competitions together = «كل البطولات» (" + all.size() + " rows), no match in two competitions", () -> {
            List<String> problems = new ArrayList<>();
            List<Fixture> union = byCompetition.values().stream().flatMap(List::stream).toList();
            for (Fixture f : all) {
                long hits = union.stream().filter(u -> ClubDataComparisons.sameMatch(u, f)).count();
                if (hits != 1) {
                    problems.add(f.describe() + ": in " + hits + " competitions");
                }
            }
            return verdict(byCompetition.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).toList().toString(), problems);
        });
        step(id, "Reset", "«" + ClubDetailsPage.ALL_COMPETITIONS + "» lists all " + all.size() + " rows again", () -> {
            List<Fixture> back = page.selectCompetition(ClubDetailsPage.ALL_COMPETITIONS).fixturesTabRows();
            return compare("rows", all.size(), back.size());
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_028 + " — «تحت 21» / «تحت 18» squads show their own players within the age limit")
    public void clubDet028_youthSquads() {
        String id = ClubFeatureTestIds.CLUB_DET_028;
        requirePhase(Phase.B, id, "Team toggles switch the roster; youth players are within the age group");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_SQUAD);
        Set<String> firstTeam = names(page.squadRoster());
        Map<String, Integer> limits = new LinkedHashMap<>();
        limits.put(ClubDetailsPage.SQUAD_TEAMS.get(1), 21);
        limits.put(ClubDetailsPage.SQUAD_TEAMS.get(2), 18);
        limits.forEach((team, limit) -> {
            Map<String, List<SquadPlayer>> roster = page.selectSquadTeam(team).squadRoster();
            List<SquadPlayer> players = roster.values().stream().flatMap(List::stream).toList();
            step(id, "«" + team + "»", "Own roster (not the first team's), every listed age ≤ " + limit, () -> {
                List<String> problems = new ArrayList<>();
                if (!players.isEmpty() && names(roster).equals(firstTeam)) {
                    problems.add("shows exactly the first-team roster (" + firstTeam.size() + " players)");
                }
                ClubDataComparisons.overAgeLimit(players, limit).forEach(p -> problems.add("over " + limit + ": " + p));
                String counts = roster.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).toList().toString();
                return verdict(players.isEmpty() ? "no players listed " + counts : players.size() + " players " + counts, problems);
            });
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_029 + " — Leaderboards agree with the squad and the club's goals")
    public void clubDet029_leaderboardsMatchSquadAndGoals() {
        String id = ClubFeatureTestIds.CLUB_DET_029;
        requirePhase(Phase.B, id, "Leaders are first-team players; scorer goals and assists fit within the club's goals");
        String club = ClubDataFlow.defaultClub();
        ClubDetailsPage page = flow().openDefaultClubDetails();
        List<LeaderboardEntry> rated = page.leaderboard(ClubDetailsPage.TOP_RATED);
        List<LeaderboardEntry> scorers = page.leaderboard(ClubDetailsPage.TOP_SCORERS);
        List<LeaderboardEntry> assists = page.leaderboard(ClubDetailsPage.TOP_ASSISTS);
        List<Fixture> played = ClubDataFlow.played(ClubDataFlow.clubFixtures(
                page.selectTab(ClubDetailsPage.TAB_FIXTURES).fixturesTabRows(), club));
        int goalsFor = played.stream().mapToInt(f -> f.goalsFor(club)).sum();
        List<SquadPlayer> squad = page.selectTab(ClubDetailsPage.TAB_SQUAD).squadRoster().values().stream()
                .flatMap(List::stream).toList();
        step(id, "Leaders in squad", "Every name on the three leaderboards is on «قائمة الفريق» (" + squad.size() + " players)", () -> {
            List<LeaderboardEntry> leaders = new ArrayList<>(rated);
            leaders.addAll(scorers);
            leaders.addAll(assists);
            return verdict(leaders.size() + " leaderboard rows found in squad",
                    ClubDataComparisons.notInSquad(leaders, squad).stream().map(n -> "«" + n + "» not in the squad").toList());
        });
        step(id, "Goals budget", "Top-3 goals and top-3 assists each ≤ club goals in all competitions (" + goalsFor + ")", () -> {
            int goals = scorers.stream().mapToInt(e -> (int) e.numericValue()).sum();
            int assisted = assists.stream().mapToInt(e -> (int) e.numericValue()).sum();
            List<String> problems = new ArrayList<>();
            if (goals > goalsFor) {
                problems.add("top scorers total " + goals + " > club goals " + goalsFor);
            }
            if (assisted > goalsFor) {
                problems.add("top assists total " + assisted + " > club goals " + goalsFor);
            }
            return verdict("goals " + goals + ", assists " + assisted + " of " + goalsFor, problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_030 + " — Fixtures tab labels are Arabic (rounds, statuses, filter)")
    public void clubDet030_fixturesTabLabelsArabic() {
        String id = ClubFeatureTestIds.CLUB_DET_030;
        requirePhase(Phase.B, id, "No English UI text on «" + ClubDetailsPage.TAB_FIXTURES + "»");
        ClubDetailsPage page = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES);
        step(id, "Visible text", "No Latin-script word (e.g. «Round of 32») among the tab's labels", () -> {
            List<String> latin = page.latinTextInActivePanel().stream().filter(t -> !LATIN_ALLOWED.contains(t)).toList();
            return verdict("all labels Arabic (allowed: " + LATIN_ALLOWED + ")", latin.stream().map(t -> "English text «" + t + "»").toList());
        });
    }

    // ---------- phase c: design ----------

    @Test(description = ClubFeatureTestIds.CLUB_DET_019 + " — Desktop layout: RTL, no horizontal scroll on any tab")
    public void clubDet019_desktopLayout() {
        String id = ClubFeatureTestIds.CLUB_DET_019;
        requirePhase(Phase.C, id, "dir=rtl and no page-level horizontal overflow per tab");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Direction", "Body direction rtl", () -> compare("direction", "rtl", PortalDom.documentDirection(browser())));
        step(id, "Overflow per tab", "scrollWidth ≤ clientWidth on every tab", () -> {
            List<String> problems = new ArrayList<>();
            for (String tab : ClubDetailsPage.TABS) {
                page.selectTab(tab);
                int overflow = PortalDom.horizontalOverflowPx(browser());
                if (overflow > 0) {
                    problems.add("«" + tab + "» overflows by " + overflow + "px");
                }
            }
            return verdict("no overflow at " + browser().manage().window().getSize(), problems);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_020 + " — Mobile layout (390px): no horizontal page scroll")
    public void clubDet020_mobileLayout() {
        String id = ClubFeatureTestIds.CLUB_DET_020;
        requirePhase(Phase.C, id, "At 390×844 the club page does not scroll horizontally");
        browser().manage().window().setSize(new Dimension(390, 844));
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Overflow per tab", "scrollWidth ≤ clientWidth on every tab at 390px", () -> {
            List<String> problems = new ArrayList<>();
            for (String tab : ClubDetailsPage.TABS) {
                page.selectTab(tab);
                int overflow = PortalDom.horizontalOverflowPx(browser());
                if (overflow > 0) {
                    problems.add("«" + tab + "» overflows by " + overflow + "px");
                }
            }
            return problems.isEmpty() ? StepResult.pass("no overflow at 390px")
                    : StepResult.fail(String.join(" ; ", problems), DefectClassification.PRODUCT_DEFECT);
        });
    }

    @Test(description = ClubFeatureTestIds.CLUB_DET_021 + " — No clipped text in stats and leaderboards")
    public void clubDet021_noClippedText() {
        String id = ClubFeatureTestIds.CLUB_DET_021;
        requirePhase(Phase.C, id, "Stat values and leaderboard names are fully visible (or deliberately ellipsised)");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        step(id, "Overview text", "No text cut without ellipsis", () ->
                verdict("no clipped text", PortalDom.clippedTexts(browser(), page.activePanel()).stream()
                        .map(t -> "clipped «" + t + "»").toList()));
    }

    // ---------- helpers ----------

    /** The league option of the fixtures tab competition filter (named «دوري …»). */
    private static String leagueOption(List<String> options) {
        return options.stream()
                .filter(o -> o.contains(LEAGUE_WORD) && !o.contains(ClubDetailsPage.ALL_COMPETITIONS))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No league option in the competition filter: " + options));
    }

    private static Fixture asFixture(UpcomingCard card) {
        return new Fixture("", card.round(), card.homeTeam(), card.awayTeam(), null, null, card.dateText(), card.timeText(), "");
    }

    private static Set<String> names(Map<String, List<SquadPlayer>> roster) {
        Set<String> names = new HashSet<>();
        roster.values().forEach(players -> players.forEach(p -> names.add(Fixture.canonical(p.name()))));
        return names;
    }

    private void assertLeaderboard(String id, String heading, boolean rating) {
        requirePhase(Phase.B, id, "«" + heading + "»: 3 players, sorted, valid values, photos load, profile links");
        ClubDetailsPage page = flow().openDefaultClubDetails();
        List<LeaderboardEntry> entries = page.leaderboard(heading);
        step(id, "Entries", "3 players with name, position and value", () -> {
            List<String> problems = new ArrayList<>();
            if (entries.size() != 3) {
                problems.add(entries.size() + " entries");
            }
            for (LeaderboardEntry e : entries) {
                if (e.name().isBlank() || e.position().isBlank()) {
                    problems.add("incomplete entry " + e);
                }
                boolean valueOk = rating ? RATING.matcher(e.value()).matches() : e.value().matches("\\d+");
                if (!valueOk) {
                    problems.add(e.name() + ": value «" + e.value() + "»");
                }
                if (!e.profileHref().contains("/clubs/players/")) {
                    problems.add(e.name() + ": no profile link");
                }
            }
            return verdict(entries.stream().map(e -> e.name() + "=" + e.value()).toList().toString(), problems);
        });
        step(id, "Sorted", "Values in descending order", () -> {
            List<String> problems = new ArrayList<>();
            for (int i = 1; i < entries.size(); i++) {
                if (entries.get(i).numericValue() > entries.get(i - 1).numericValue()) {
                    problems.add(entries.get(i).name() + " (" + entries.get(i).value() + ") below "
                            + entries.get(i - 1).name() + " (" + entries.get(i - 1).value() + ")");
                }
            }
            return verdict("descending", problems);
        });
        step(id, "Photos", "Every player photo loads", () ->
                verdict("photos loaded", PortalDom.brokenImages(browser(), page.section(heading)).stream()
                        .map(b -> "broken " + b).toList()));
    }
}
