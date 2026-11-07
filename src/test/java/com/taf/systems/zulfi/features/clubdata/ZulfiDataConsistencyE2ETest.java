package com.taf.systems.zulfi.features.clubdata;

import com.taf.systems.zulfi.ui.PortalDom;
import org.openqa.selenium.By;
import com.taf.systems.zulfi.clubdata.ClubFeatureTestIds;
import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.clubdata.ClubDataComparisons;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.clubdata.SeasonStats;
import com.taf.systems.zulfi.clubdata.StandingsRow;
import com.taf.systems.zulfi.config.FeaturePhaseGate;
import com.taf.systems.zulfi.config.FeaturePhaseGate.Phase;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.clubdetails.ClubDetailsPage;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage.LeagueClubCard;
import com.taf.systems.zulfi.pages.home.HomeClubWidgets;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleFixturesView;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.HomeCurrentResultsMessages;
import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * US-FAP-ZULFI-DATA — the Default Club's numbers must agree everywhere they appear: Home (results, goals, top scorer,
 * upcoming, standings snippet), Match Schedule (source of match results), «ترتيب الدوري», «الأندية» and the club page.
 * Read-only E2E. Enable via {@code -Dclub.data.phase=b}.
 */
@Epic("Zulfi Portal")
@Feature(ClubFeatureTestIds.FEATURE_CLUB_DATA)
public class ZulfiDataConsistencyE2ETest extends ClubDataUiTest {

    private static final List<String> ANALYSIS_STATUSES = List.of(
            HomeCurrentResultsMessages.STATUS_NOT_ANALYZED,
            HomeCurrentResultsMessages.STATUS_IN_PROGRESS,
            HomeCurrentResultsMessages.STATUS_COMPLETED);

    @Override
    protected FeaturePhaseGate gate() {
        return FeaturePhaseGate.CLUB_DATA;
    }

    @Override
    protected String story() {
        return ClubFeatureTestIds.STORY_CLUB_DATA;
    }

    @Override
    protected String feature() {
        return ClubFeatureTestIds.FEATURE_CLUB_DATA;
    }

    @Test(description = ClubFeatureTestIds.ZDC_001 + " — Home last-5 results = last 5 played matches in Match Schedule")
    public void zdc001_homeResultsMatchSchedule() {
        String id = ClubFeatureTestIds.ZDC_001;
        requirePhase(Phase.B, id, "Home «نتائج الفريق للموسم الحالي» vs Match Schedule");
        String club = ClubDataFlow.defaultClub();
        List<ResultCard> home = flow().openHome().resultCards();
        List<Fixture> expected = ClubDataFlow.lastPlayedNewestFirst(allClubFixtures(club), home.size());
        step(id, "Compare card by card (newest first)",
                "Opponent and club-first score = " + expected.stream().map(f -> f.opponent(club) + " " + f.clubFirstScore(club)).toList(),
                () -> verdict(home.stream().map(ResultCard::describe).toList().toString(), ClubDataComparisons.resultCards(home, expected, club, "schedule")));
    }

    @Test(description = ClubFeatureTestIds.ZDC_002 + " — Home results = club page results")
    public void zdc002_homeResultsMatchClubOverview() {
        String id = ClubFeatureTestIds.ZDC_002;
        requirePhase(Phase.B, id, "Home results widget vs club «نظرة عامة» results widget");
        List<String> home = flow().openHome().resultCards().stream().map(ResultCard::describe).toList();
        List<String> club = flow().openDefaultClubDetails().resultCards().stream().map(ResultCard::describe).toList();
        step(id, "Compare widgets", "Same opponents, scores and order: " + home, () -> compare("club page results", home, club));
    }

    @Test(description = ClubFeatureTestIds.ZDC_003 + " — Standings «آخر 5 مباريات» = last 5 league outcomes in Match Schedule")
    public void zdc003_standingsFormMatchesSchedule() {
        String id = ClubFeatureTestIds.ZDC_003;
        requirePhase(Phase.B, id, "Standings form badges vs Match Schedule outcomes");
        String club = ClubDataFlow.defaultClub();
        StandingsRow row = ownRow(flow().openStandings().table().rows(), club);
        List<Fixture> league = ClubDataFlow.played(ClubDataFlow.clubFixtures(
                flow().scheduleFixtures(ScheduleFixturesView.LEAGUE_TAB), club));
        List<Fixture> lastFive = league.subList(Math.max(0, league.size() - row.form().size()), league.size());
        String expected = lastFive.stream().map(f -> String.valueOf(f.outcome(club))).collect(Collectors.joining());
        step(id, "Compare form (oldest → newest)",
                "Form = " + expected + " from " + lastFive.stream().map(Fixture::describe).toList(),
                () -> compare("standings form", expected, String.join("", row.form())));
    }

    @Test(description = ClubFeatureTestIds.ZDC_004 + " — Club season totals recomputed from Match Schedule = standings row")
    public void zdc004_standingsMatchScheduleTotals() {
        String id = ClubFeatureTestIds.ZDC_004;
        requirePhase(Phase.B, id, "P/W/D/L/GF/GA/Pts from every played league match vs «ترتيب الدوري»");
        String club = ClubDataFlow.defaultClub();
        List<Fixture> league = ClubDataFlow.clubFixtures(flow().scheduleFixtures(ScheduleFixturesView.LEAGUE_TAB), club);
        SeasonStats fromSchedule = SeasonStats.fromFixtures(club, league);
        StandingsRow row = ownRow(flow().openStandings().table().rows(), club);
        TestStepReporter.recordTestData("scheduleMatches",
                ClubDataFlow.played(league).stream().map(Fixture::describe).toList().toString());
        step(id, "Recompute from schedule and compare",
                "Standings «" + club + "» = " + fromSchedule.describe() + " (from " + ClubDataFlow.played(league).size() + " played matches)",
                () -> verdict("standings " + row.stats().describe() + " = schedule",
                        row.stats().differencesFrom(fromSchedule).stream()
                                .map(d -> "standings vs schedule — " + d).toList()));
    }

    @Test(description = ClubFeatureTestIds.ZDC_005 + " — Standings row = club page «إحصائيات الفريق»")
    public void zdc005_standingsMatchClubStats() {
        String id = ClubFeatureTestIds.ZDC_005;
        requirePhase(Phase.B, id, "«ترتيب الدوري» row vs club statistics cards");
        String club = ClubDataFlow.defaultClub();
        SeasonStats standings = ownRow(flow().openStandings().table().rows(), club).stats();
        SeasonStats clubPage = flow().openDefaultClubDetails().seasonStats();
        step(id, "Compare 8 figures", "Club cards = " + standings.describe(), () ->
                verdict(clubPage.describe(), clubPage.differencesFrom(standings).stream().map(d -> "club page vs standings — " + d).toList()));
    }

    @Test(description = ClubFeatureTestIds.ZDC_006 + " — Home «عدد الأهداف» = standings GF = club «له»")
    public void zdc006_teamGoalsEverywhere() {
        String id = ClubFeatureTestIds.ZDC_006;
        requirePhase(Phase.B, id, "Team goals on Home KPI, standings and club page");
        String club = ClubDataFlow.defaultClub();
        int home = flow().openHome().teamGoals();
        int standings = ownRow(flow().openStandings().table().rows(), club).stats().goalsFor();
        int clubPage = flow().openDefaultClubDetails().seasonStats().goalsFor();
        step(id, "Compare goals", "Home = standings GF = club «له» = " + standings, () -> {
            List<String> problems = new ArrayList<>();
            if (home != standings) {
                problems.add("Home «عدد الأهداف» " + home + " ≠ standings GF " + standings);
            }
            if (clubPage != standings) {
                problems.add("club «له» " + clubPage + " ≠ standings GF " + standings);
            }
            return verdict("goals=" + home, problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_007 + " — Home «هداف الفريق» = club «أفضل الهدافين» leader")
    public void zdc007_topScorerConsistent() {
        String id = ClubFeatureTestIds.ZDC_007;
        requirePhase(Phase.B, id, "Top scorer name and goals on Home vs club leaderboard");
        List<String> home = flow().openHome().topScorer();
        List<LeaderboardEntry> board = flow().openDefaultClubDetails().leaderboard(ClubDetailsPage.TOP_SCORERS);
        step(id, "Compare", "Home goals = leaderboard max; Home name among players with that max", () -> {
            if (home.size() < 2 || board.isEmpty()) {
                return StepResult.fail("home=" + home + "; leaderboard=" + board);
            }
            String name = home.getFirst();
            int goals = Integer.parseInt(home.getLast().replaceAll("\\D", ""));
            int max = board.stream().mapToInt(e -> (int) e.numericValue()).max().orElse(-1);
            List<String> leaders = board.stream().filter(e -> (int) e.numericValue() == max).map(LeaderboardEntry::name).toList();
            List<String> problems = new ArrayList<>();
            if (goals != max) {
                problems.add("Home goals " + goals + " ≠ leaderboard max " + max);
            }
            if (leaders.stream().noneMatch(l -> l.equals(name))) {
                problems.add("Home «" + name + "» not among leaders " + leaders);
            }
            return verdict(name + " " + goals + " ∈ " + leaders, problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_008 + " — Rank: club badge = standings position = «الأندية» #N")
    public void zdc008_rankConsistent() {
        String id = ClubFeatureTestIds.ZDC_008;
        requirePhase(Phase.B, id, "League position on club badge, standings and clubs grid");
        String club = ClubDataFlow.defaultClub();
        LeagueClubCard card = flow().defaultClubCard();
        int standings = ownRow(flow().openStandings().table().rows(), club).position();
        int badge = flow().openDefaultClubDetails().rankFromBadge();
        step(id, "Compare positions", "All three = #" + standings, () -> {
            List<String> problems = new ArrayList<>();
            if (badge != standings) {
                problems.add("club badge #" + badge + " ≠ standings #" + standings);
            }
            if (card.rank() != standings) {
                problems.add("«الأندية» card #" + card.rank() + " ≠ standings #" + standings);
            }
            return verdict("#" + standings, problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_009 + " — Home «المباريات القادمة» = next fixtures in Match Schedule")
    public void zdc009_homeUpcomingMatchesSchedule() {
        String id = ClubFeatureTestIds.ZDC_009;
        requirePhase(Phase.B, id, "Home upcoming cards vs the club's next unplayed fixtures");
        String club = ClubDataFlow.defaultClub();
        List<UpcomingCard> home = flow().openHome().upcomingCards();
        List<Fixture> next = ClubDataFlow.upcoming(allClubFixtures(club)).stream().limit(home.size()).toList();
        step(id, "Compare card by card", "Next " + home.size() + ": " + next.stream().map(Fixture::describe).toList(), () ->
                verdict(home.stream().map(UpcomingCard::describe).toList().toString(), ClubDataComparisons.upcoming(home, next, "schedule")));
    }

    @Test(description = ClubFeatureTestIds.ZDC_010 + " — Club «المباراة القادمة» = first upcoming fixture = Home first card")
    public void zdc010_nextMatchConsistent() {
        String id = ClubFeatureTestIds.ZDC_010;
        requirePhase(Phase.B, id, "Next match on club page vs Match Schedule vs Home");
        String club = ClubDataFlow.defaultClub();
        UpcomingCard homeFirst = flow().openHome().upcomingCards().getFirst();
        UpcomingCard clubNext = flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES).nextMatch();
        Fixture scheduleNext = ClubDataFlow.upcoming(allClubFixtures(club)).getFirst();
        step(id, "Compare", "All equal " + scheduleNext.describe(), () -> {
            List<String> problems = new ArrayList<>();
            problems.addAll(ClubDataComparisons.upcoming(List.of(clubNext), List.of(scheduleNext), "schedule").stream().map(p -> "club page: " + p).toList());
            problems.addAll(ClubDataComparisons.upcoming(List.of(homeFirst), List.of(scheduleNext), "schedule").stream().map(p -> "Home: " + p).toList());
            return verdict(clubNext == null ? "none" : clubNext.describe(), problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_011 + " — Club fixtures tab = Match Schedule rows for the club")
    public void zdc011_clubFixturesTabMatchesSchedule() {
        String id = ClubFeatureTestIds.ZDC_011;
        requirePhase(Phase.B, id, "Every played match on the club tab exists in Match Schedule with the same score, and vice versa");
        String club = ClubDataFlow.defaultClub();
        List<Fixture> tab = ClubDataFlow.played(flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES).fixturesTabRows());
        List<Fixture> schedule = ClubDataFlow.played(allClubFixtures(club));
        step(id, "Cross-match by teams and date", tab.size() + " tab rows vs " + schedule.size() + " schedule rows", () -> {
            List<String> problems = new ArrayList<>();
            for (Fixture t : tab) {
                Fixture s = schedule.stream().filter(x -> ClubDataComparisons.sameMatch(x, t)).findFirst().orElse(null);
                if (s == null) {
                    problems.add("club tab " + t.describe() + " missing in Match Schedule");
                } else if (!t.homeGoals().equals(s.homeGoals()) || !t.awayGoals().equals(s.awayGoals())) {
                    problems.add("score differs: tab " + t.describe() + " vs schedule " + s.describe());
                }
            }
            for (Fixture s : schedule) {
                if (tab.stream().noneMatch(t -> ClubDataComparisons.sameMatch(s, t))) {
                    problems.add("schedule " + s.describe() + " missing on club tab");
                }
            }
            return verdict(tab.size() + " matches agree", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_012 + " — Home results «عرض المزيد» opens Match Schedule filtered to the club")
    public void zdc012_resultsViewMoreFiltersSchedule() {
        assertViewMoreFiltersSchedule(ClubFeatureTestIds.ZDC_012, HomeCurrentResultsMessages.SECTION_TITLE);
    }

    @Test(description = ClubFeatureTestIds.ZDC_013 + " — Home upcoming «عرض المزيد» opens Match Schedule filtered to the club")
    public void zdc013_upcomingViewMoreFiltersSchedule() {
        assertViewMoreFiltersSchedule(ClubFeatureTestIds.ZDC_013, HomeOverviewMessages.UPCOMING_SECTION_TITLE_PLURAL);
    }

    @Test(description = ClubFeatureTestIds.ZDC_014 + " — Home «آخر التحليلات» «عرض المزيد» lists the same latest analyses")
    public void zdc014_recentAnalysesViewMore() {
        String id = ClubFeatureTestIds.ZDC_014;
        requirePhase(Phase.B, id, "Home latest analyses = first cards on «التحليلات»");
        HomeClubWidgets home = flow().openHome();
        List<String> homeTitles = home.recentAnalysisTitles();
        step(id, "Click «عرض المزيد»", "Opens /analysis whose first " + homeTitles.size() + " cards = " + homeTitles, () -> {
            home.clickViewMore(home.recentAnalysesSection());
            new AnalysesListPage(browser()).waitUntilLoaded();
            new WebDriverWait(browser(), Duration.ofSeconds(20)).until(d ->
                    d.findElements(By.cssSelector("article")).stream()
                            .filter(WebElement::isDisplayed).count() >= homeTitles.size());
            List<String> listTitles = PortalDom.rows(browser(),
                            "return [...document.querySelectorAll('main article, article')].map(a => ({t: (a.innerText || '').split('\\n')[0].trim()}));")
                    .stream().map(r -> PortalDom.str(r, "t")).filter(t -> !t.isBlank())
                    .distinct().limit(homeTitles.size()).toList();
            return compare("first cards on «التحليلات»", homeTitles, listTitles);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_015 + " — Analysis status of each club match is the same on schedule and club tab")
    public void zdc015_analysisStatusConsistent() {
        String id = ClubFeatureTestIds.ZDC_015;
        requirePhase(Phase.B, id, "غير محللة / قيد التحليل / اكتمل التحليل per played match on both pages");
        String club = ClubDataFlow.defaultClub();
        List<Fixture> tab = ClubDataFlow.played(flow().openDefaultClubDetails().selectTab(ClubDetailsPage.TAB_FIXTURES).fixturesTabRows());
        List<Fixture> schedule = ClubDataFlow.played(allClubFixtures(club));
        step(id, "Compare status labels", "Same status for each of " + tab.size() + " matches", () -> {
            List<String> problems = new ArrayList<>();
            for (Fixture t : tab) {
                schedule.stream().filter(s -> ClubDataComparisons.sameMatch(s, t)).findFirst().ifPresent(s -> {
                    String a = status(t.analysisLabel());
                    String b = status(s.analysisLabel());
                    if (!a.equals(b)) {
                        problems.add(t.describe() + ": club tab «" + a + "» vs schedule «" + b + "»");
                    }
                });
            }
            return verdict("statuses agree", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.ZDC_016 + " — Whole league: every standings row = totals recomputed from Match Schedule")
    public void zdc016_wholeLeagueStandingsMatchSchedule() {
        String id = ClubFeatureTestIds.ZDC_016;
        requirePhase(Phase.B, id, "All clubs: «ترتيب الدوري» vs played league matches in Match Schedule");
        List<Fixture> league = flow().scheduleFixtures(ScheduleFixturesView.LEAGUE_TAB);
        List<Integer> rounds = new ScheduleFixturesView(browser()).roundsRendered();
        List<StandingsRow> rows = flow().openStandings().table().rows();
        TestStepReporter.recordTestData("roundsRendered", rounds.stream().sorted().toList().toString());
        step(id, "Recompute every club", rows.size() + " standings rows equal schedule totals", () -> {
            List<String> problems = new ArrayList<>();
            for (StandingsRow row : rows) {
                SeasonStats computed = SeasonStats.fromFixtures(row.team(), league);
                List<String> diffs = row.stats().differencesFrom(computed);
                if (!diffs.isEmpty()) {
                    problems.add(row.team() + " [standings " + row.stats().describe() + " | schedule " + computed.describe() + "]");
                }
            }
            return verdict(rows.size() + " clubs agree", problems);
        });
    }

    // ---------- helpers ----------

    /** League + cup fixtures of the club, chronological. */
    private List<Fixture> allClubFixtures(String club) {
        List<Fixture> league = flow().scheduleFixtures(ScheduleFixturesView.LEAGUE_TAB);
        List<Fixture> cup = flow().scheduleFixtures(ScheduleFixturesView.CUP_TAB);
        return ClubDataFlow.clubFixtures(Stream.concat(league.stream(), cup.stream()).toList(), club);
    }

    private static StandingsRow ownRow(List<StandingsRow> rows, String club) {
        return rows.stream()
                .filter(r -> Fixture.sameTeam(r.team(), club))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("«" + club + "» not on standings"));
    }

    private static String status(String label) {
        return ANALYSIS_STATUSES.stream().filter(label::contains).findFirst().orElse("(none)");
    }

    private void assertViewMoreFiltersSchedule(String id, String sectionTitle) {
        requirePhase(Phase.B, id, "«" + sectionTitle + "» → «عرض المزيد» → schedule shows only the club");
        String club = ClubDataFlow.defaultClub();
        HomeClubWidgets home = flow().openHome();
        WebElement section = sectionTitle.equals(HomeCurrentResultsMessages.SECTION_TITLE)
                ? home.resultsSection() : home.upcomingSection();
        String href = home.viewMoreHref(section);
        TestStepReporter.recordTestData("viewMoreHref", href);
        step(id, "Click «عرض المزيد»", "Match Schedule opens", () -> {
            home.clickViewMore(section);
            new WebDriverWait(browser(), Duration.ofSeconds(20)).until(d -> d.getCurrentUrl().contains("/matches"));
            new ScheduleFixturesView(browser()).waitUntilLoaded();
            return StepResult.pass("url=" + browser().getCurrentUrl());
        });
        step(id, "Club filter applied", "«الفريق» = «" + club + "» and every listed match involves the club", () -> {
            ScheduleFixturesView schedule = new ScheduleFixturesView(browser());
            List<Fixture> rows = schedule.fixtures();
            List<Fixture> foreign = rows.stream().filter(f -> !f.involves(club)).toList();
            List<String> problems = new ArrayList<>();
            if (!Fixture.sameTeam(schedule.teamFilterLabel(), club)) {
                problems.add("team filter shows «" + schedule.teamFilterLabel() + "»");
            }
            if (!foreign.isEmpty()) {
                problems.add(foreign.size() + " of " + rows.size() + " rows do not involve «" + club + "», e.g. "
                        + foreign.stream().limit(3).map(Fixture::describe).toList());
            }
            return verdict(rows.size() + " rows, all «" + club + "»", problems);
        });
    }
}
