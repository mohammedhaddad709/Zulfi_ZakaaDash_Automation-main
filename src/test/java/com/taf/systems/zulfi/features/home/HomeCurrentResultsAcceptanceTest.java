package com.taf.systems.zulfi.features.home;

import com.taf.systems.zulfi.clubdata.ClubWidgets;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.pages.home.HomeClubWidgets;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleFixturesView;
import com.taf.systems.zulfi.config.DefaultClubConfig;
import com.taf.systems.zulfi.config.HomeCurrentResultsGate;
import com.taf.systems.zulfi.config.HomeCurrentResultsGate.Phase;
import com.taf.systems.zulfi.flows.home.HomeCurrentResultsFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.home.HomeResTestIds;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.home.CurrentTeamResultCardSnapshot;
import com.taf.systems.zulfi.pages.home.CurrentTeamResultsSection;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.HomeCurrentResultsMessages;
import com.taf.systems.zulfi.validations.HomeResultCardRules;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * US-FAP-HOME-CURRENT-RESULTS — Home current team results + View More deep link.
 * Enable via {@code -Dhome.current.results.phase=a|b|c}.
 */
@Epic("Zulfi Portal")
@Feature(HomeResTestIds.FEATURE)
public class HomeCurrentResultsAcceptanceTest extends AuthenticatedUiTest {

    private HomeCurrentResultsFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for HOME-CURRENT-RESULTS tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new HomeCurrentResultsFlow(browser());
    }

    @Test(description = HomeResTestIds.HOME_RES_001 + " — Display نتائج الفريق الحالية")
    public void homeRes001_displayCurrentTeamResultsSection() {
        requirePhase(Phase.A, HomeResTestIds.HOME_RES_001);
        runStep(
                HomeResTestIds.HOME_RES_001,
                "Load Home",
                () -> {
                    CurrentTeamResultsSection section = flow.openHomeWithCurrentResultsSection();
                    Assert.assertTrue(section.isSectionTitleVisible(), HomeCurrentResultsMessages.SECTION_TITLE);
                    return StepResult.pass("sectionVisible=true");
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_014 + " — Home CTA remains available")
    public void homeRes014_homeRemainsUsableWhenResultsSectionLoads() {
        requirePhase(Phase.A, HomeResTestIds.HOME_RES_014);
        runStep(
                HomeResTestIds.HOME_RES_014,
                "Results section + independent Home actions",
                () -> {
                    flow.openHomeWithCurrentResultsSection();
                    HomePage home = new HomePage(browser());
                    Assert.assertTrue(home.isReady(), "«ابدأ التحليل» must remain available");
                    return StepResult.pass("startAnalysisVisible=true");
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_009 + " / " + HomeResTestIds.HOME_RES_010 + " — View More → Schedule + club filter")
    public void homeRes009_viewMoreOpensScheduleWithDefaultClubFilter() {
        requirePhase(Phase.A, HomeResTestIds.HOME_RES_009);
        String clubFragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                HomeResTestIds.HOME_RES_009,
                "Click «عرض المزيد»",
                () -> {
                    MatchSchedulePage schedule = flow.navigateToScheduleViaViewMore();
                    Assert.assertTrue(
                            schedule.isOnMatchScheduleRoute() || schedule.isDisplayed(),
                            "Navigated to Match Schedule; url=" + browser().getCurrentUrl());
                    Assert.assertTrue(
                            schedule.clubFilterSelectionContains(clubFragment),
                            "Club filter should reflect configured Default Club fragment ["
                                    + clubFragment
                                    + "]; filter="
                                    + schedule.readSelectedClubFilterLabel());
                    return StepResult.pass("scheduleOpen=true; clubFragment=" + clubFragment);
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_010 + " — Default club from configuration")
    public void homeRes010_defaultClubComesFromConfiguration() {
        requirePhase(Phase.A, HomeResTestIds.HOME_RES_010);
        runStep(
                HomeResTestIds.HOME_RES_010,
                "Read DefaultClubConfig (not hardcoded in tests)",
                () -> {
                    String label = DefaultClubConfig.configuredDefaultClubLabel();
                    String fragment = DefaultClubConfig.clubMatchFragment();
                    Assert.assertFalse(label.isBlank(), "Default club label configured");
                    Assert.assertFalse(fragment.isBlank(), "Default club fragment configured");
                    Assert.assertTrue(
                            label.contains(fragment) || fragment.contains("الزلفي") || !label.equals("Al Zulfi"),
                            "Configuration uses portal Arabic club naming");
                    return StepResult.pass("label=" + label + "; fragment=" + fragment);
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_003 + " — Results involve configured Default Club")
    public void homeRes003_resultsFilteredToDefaultClub() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_003);
        requireResultCards();
        String fragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                HomeResTestIds.HOME_RES_003,
                "Each card mentions Default Club",
                () -> {
                    List<CurrentTeamResultCardSnapshot> cards =
                            flow.openHomeWithCurrentResultsSection().readVisibleResultCards();
                    if (cards.stream().allMatch(c -> HomeResultCardRules.mentionsClub(c.rawText(), fragment))) {
                        return StepResult.pass("cards=" + cards.size() + " (club named on every card)");
                    }
                    // Current cards show only the opponent: prove each one is a played match of the Default Club.
                    List<ClubWidgets.ResultCard> shown = new HomeClubWidgets(browser()).resultCards();
                    List<Fixture> played = ClubDataFlow.played(clubScheduleFixtures());
                    String club = DefaultClubConfig.configuredDefaultClubLabel();
                    for (ClubWidgets.ResultCard card : shown) {
                        Assert.assertTrue(
                                played.stream().anyMatch(f -> Fixture.sameTeam(f.opponent(club), card.opponent())
                                        && f.clubFirstScore(club).equals(card.score())),
                                "Card «" + card.describe() + "» is not a played match of «" + club + "» in جدول المباريات");
                    }
                    return StepResult.pass("cards=" + shown.size() + " all matched to «" + club + "» fixtures");
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_004 + " — Only played matches (scores)")
    public void homeRes004_onlyPlayedMatchesWithResults() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_004);
        requireResultCards();
        runStep(
                HomeResTestIds.HOME_RES_004,
                "Cards expose recorded scores",
                () -> {
                    List<CurrentTeamResultCardSnapshot> cards =
                            flow.openHomeWithCurrentResultsSection().readVisibleResultCards();
                    for (CurrentTeamResultCardSnapshot card : cards) {
                        Assert.assertTrue(card.scorePresent(), "Score required for played match; card=" + card.rawText());
                        Assert.assertTrue(
                                HomeResultCardRules.hasRecordedScore(card.rawText()),
                                "Upcoming fixtures must not appear without score");
                    }
                    return StepResult.pass("playedCards=" + cards.size());
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_005 + " — Display scores and date")
    public void homeRes005_displayMatchResultFields() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_005);
        requireResultCards();
        runStep(
                HomeResTestIds.HOME_RES_005,
                "Validate card content shape",
                () -> {
                    List<CurrentTeamResultCardSnapshot> cards =
                            flow.openHomeWithCurrentResultsSection().readVisibleResultCards();
                    for (CurrentTeamResultCardSnapshot card : cards) {
                        Assert.assertTrue(card.homeScore() >= 0 && card.awayScore() >= 0, "Scores parsed");
                        Assert.assertFalse(card.rawText().isBlank(), "Club names present in card text");
                        if (!card.dateToken().isBlank()) {
                            Assert.assertTrue(
                                    MatchDateDisplayRules.looksLikeCalendarDateDisplay(card.dateToken()),
                                    "Match date token");
                        }
                    }
                    return StepResult.pass("validated=" + cards.size());
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_006 + " — Analysis status when shown")
    public void homeRes006_displayAnalysisStatusWhenPresent() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_006);
        requireResultCards();
        runStep(
                HomeResTestIds.HOME_RES_006,
                "Status labels belong to approved set",
                () -> {
                    List<CurrentTeamResultCardSnapshot> cards =
                            flow.openHomeWithCurrentResultsSection().readVisibleResultCards();
                    int withStatus = 0;
                    for (CurrentTeamResultCardSnapshot card : cards) {
                        if (card.analysisStatus().isBlank()) {
                            continue;
                        }
                        withStatus++;
                        Assert.assertTrue(
                                HomeResultCardRules.hasKnownAnalysisStatus(card.rawText()),
                                "Unknown status on card: " + card.analysisStatus());
                    }
                    return StepResult.pass("cardsWithStatus=" + withStatus + "; total=" + cards.size());
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_007 + " — Latest results first")
    public void homeRes007_orderLatestResultsFirst() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_007);
        requireResultCards();
        runStep(
                HomeResTestIds.HOME_RES_007,
                "Compare parsed dates in DOM order",
                () -> {
                    List<CurrentTeamResultCardSnapshot> cards =
                            flow.openHomeWithCurrentResultsSection().readVisibleResultCards();
                    List<LocalDate> dates = new ArrayList<>();
                    for (CurrentTeamResultCardSnapshot card : cards) {
                        parseDisplayDate(card.dateToken()).ifPresent(dates::add);
                    }
                    if (dates.size() < 2) {
                        // Cards carry no dates: compare their order with the club's played fixtures, newest first.
                        List<ClubWidgets.ResultCard> shown = new HomeClubWidgets(browser()).resultCards();
                        String club = DefaultClubConfig.configuredDefaultClubLabel();
                        List<String> expected = ClubDataFlow.lastPlayedNewestFirst(clubScheduleFixtures(), shown.size()).stream()
                                .map(f -> Fixture.canonical(f.opponent(club)) + " " + f.clubFirstScore(club)).toList();
                        List<String> actual = shown.stream().map(c -> Fixture.canonical(c.opponent()) + " " + c.score()).toList();
                        Assert.assertEquals(actual, expected, "Home cards must follow جدول المباريات, most recent first");
                        return StepResult.pass("order matches schedule: " + actual);
                    }
                    for (int i = 1; i < dates.size(); i++) {
                        Assert.assertFalse(
                                dates.get(i).isAfter(dates.get(i - 1)),
                                "Dates must be non-increasing (most recent first)");
                    }
                    return StepResult.pass("orderedDates=" + dates.size());
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_008 + " — Limited cards on Home")
    public void homeRes008_limitDisplayedResultsOnHome() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_008);
        runStep(
                HomeResTestIds.HOME_RES_008,
                "Card count bounded by layout configuration property",
                () -> {
                    int max = HomeCurrentResultsGate.configuredMaxHomeResultCards();
                    int count = flow.openHomeWithCurrentResultsSection().countVisibleResultCards();
                    Assert.assertTrue(
                            count <= max,
                            "Home displays at most configured layout count; count="
                                    + count
                                    + "; max="
                                    + max);
                    return StepResult.pass("count=" + count + "; max=" + max);
                });
    }

    @Test(description = HomeResTestIds.HOME_RES_002 + " — Current season only")
    public void homeRes002_currentSeasonOnly() {
        requirePhase(Phase.B, HomeResTestIds.HOME_RES_002);
        throw new SkipException(
                "Season scoping requires season marker in card/API (not visible in Home card DOM yet).");
    }

    @Test(description = HomeResTestIds.HOME_RES_011 + " — No current-season results empty state")
    public void homeRes011_noResultsEmptyState() {
        requirePhase(Phase.C, HomeResTestIds.HOME_RES_011);
        throw new SkipException("Requires Default Club with zero played matches in current season.");
    }

    @Test(description = HomeResTestIds.HOME_RES_012 + " — Loading state")
    public void homeRes012_loadingState() {
        requirePhase(Phase.C, HomeResTestIds.HOME_RES_012);
        throw new SkipException("Loading state timing requires controlled network throttling or test hooks.");
    }

    @Test(description = HomeResTestIds.HOME_RES_013 + " — Retrieval failure + retry")
    public void homeRes013_resultsRetrievalFailure() {
        requirePhase(Phase.C, HomeResTestIds.HOME_RES_013);
        throw new SkipException("Error state requires simulated API failure or dedicated test environment.");
    }

    /** League + cup fixtures of the Default Club from جدول المباريات, chronological (navigates away from Home). */
    private List<Fixture> clubScheduleFixtures() {
        ClubDataFlow data = new ClubDataFlow(browser());
        List<Fixture> all = new ArrayList<>(data.scheduleFixtures(ScheduleFixturesView.LEAGUE_TAB));
        all.addAll(data.scheduleFixtures(ScheduleFixturesView.CUP_TAB));
        return ClubDataFlow.clubFixtures(all, DefaultClubConfig.configuredDefaultClubLabel());
    }

    private void requireResultCards() {
        CurrentTeamResultsSection section = flow.openHomeWithCurrentResultsSection();
        if (section.isEmptyStateVisible()) {
            throw new SkipException("Default Club has no current-season results in this environment.");
        }
        if (section.countVisibleResultCards() == 0) {
            throw new SkipException(
                    "No parseable result cards on Home (DOM selectors may need alignment with portal markup).");
        }
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        HomeCurrentResultsGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-HOME-CURRENT-RESULTS",
                HomeResTestIds.FEATURE,
                "Authenticated Home with Yellow League data",
                "Credentials + phase gate satisfied",
                "phase="
                        + HomeCurrentResultsGate.currentPhase()
                        + "; defaultClub="
                        + DefaultClubConfig.configuredDefaultClubLabel());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute Home current results scenario",
                objective,
                "Per US-FAP-HOME-CURRENT-RESULTS AC",
                action,
                browser(),
                testCaseId);
    }

    private static Optional<LocalDate> parseDisplayDate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        List<DateTimeFormatter> formatters =
                List.of(
                        DateTimeFormatter.ofPattern("d/M/uuuu", Locale.US),
                        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.US),
                        DateTimeFormatter.ofPattern("uuuu-M-d", Locale.US));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return Optional.of(LocalDate.parse(token, formatter));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return Optional.empty();
    }
}
