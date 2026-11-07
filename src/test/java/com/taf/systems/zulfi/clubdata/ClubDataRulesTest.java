package com.taf.systems.zulfi.clubdata;

import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.SquadPlayer;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.validations.AnalysesListCardRules;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

/** Offline contract tests for the club-data rules (no browser). Values mirror the live 2026-10-22 snapshot. */
public class ClubDataRulesTest {

    private static final String CLUB = "نادي الزلفي";

    private static final List<Fixture> SCHEDULE = List.of(
            played(3, "نادي العروبة", 1, 1, CLUB, "الثلاثاء، 1 سبتمبر"),
            played(4, CLUB, 0, 0, "نادي هجر", "الثلاثاء، 8 سبتمبر"),
            played(5, "نادي الطائي", 0, 0, CLUB, "الأحد، 13 سبتمبر"),
            played(6, CLUB, 4, 0, "نادي الرائد", "السبت، 19 سبتمبر"),
            played(7, "نادي البكيرية", 0, 2, CLUB, "الثلاثاء، 13 أكتوبر"),
            played(8, CLUB, 1, 1, "نادي العلا", "الاثنين، 19 أكتوبر"),
            new Fixture("", 9, "النجمة", CLUB, null, null, "الأربعاء، 28 أكتوبر", "03:05 م", ""));

    @Test
    public void seasonStatsFromFixtures() {
        SeasonStats stats = SeasonStats.fromFixtures(CLUB, SCHEDULE);
        Assert.assertEquals(stats, new SeasonStats(6, 2, 4, 0, 8, 2, 6, 10));
        Assert.assertTrue(stats.internalProblems().isEmpty());
    }

    @Test
    public void internalProblemsDetectArithmeticErrors() {
        Assert.assertEquals(new SeasonStats(8, 3, 5, 0, 10, 2, 8, 14).internalProblems(), List.of());
        Assert.assertEquals(new SeasonStats(8, 3, 4, 0, 10, 2, 7, 15).internalProblems().size(), 3);
    }

    @Test
    public void differencesUseArabicLabels() {
        SeasonStats standings = new SeasonStats(8, 3, 5, 0, 10, 2, 8, 14);
        List<String> diffs = standings.differencesFrom(SeasonStats.fromFixtures(CLUB, SCHEDULE));
        Assert.assertTrue(diffs.contains("لعب: 8 ≠ 6"), diffs.toString());
        Assert.assertTrue(diffs.contains("النقاط: 14 ≠ 10"), diffs.toString());
        Assert.assertEquals(diffs.size(), 6);
    }

    @Test
    public void clubFirstScoreAndOutcomeForAwayWin() {
        Fixture away = SCHEDULE.get(4);
        Assert.assertEquals(away.clubFirstScore(CLUB), "2 - 0");
        Assert.assertEquals(away.outcome(CLUB), 'W');
        Assert.assertEquals(away.opponent(CLUB), "نادي البكيرية");
    }

    @Test
    public void lastPlayedNewestFirst() {
        List<Fixture> last = ClubDataFlow.lastPlayedNewestFirst(ClubDataFlow.clubFixtures(SCHEDULE, CLUB), 5);
        Assert.assertEquals(last.stream().map(Fixture::round).toList(), List.of(8, 7, 6, 5, 4));
        Assert.assertEquals(ClubDataFlow.upcoming(SCHEDULE).getFirst().round(), 9);
    }

    @Test
    public void teamNamesCompareCanonically() {
        Assert.assertTrue(Fixture.sameTeam("نادي الزلفي", "الزلفي"));
        Assert.assertTrue(Fixture.sameTeam("نادي الأنوار", "نادي الانوار"));
        Assert.assertFalse(Fixture.sameTeam("نادي الرائد", "نادي الرياض"));
    }

    @Test
    public void arabicDatesOrderAcrossTheSeasonAndDigitSystems() {
        Assert.assertTrue(ArabicDates.seasonOrderKey("الاثنين، 17 أغسطس") < ArabicDates.seasonOrderKey("الأحد، 3 يناير"));
        Assert.assertTrue(ArabicDates.sameDisplayedDate("الأربعاء، ٢٨ أكتوبر", "الأربعاء، 28 أكتوبر"));
        Assert.assertEquals(ArabicDates.seasonOrderKey("بدون تاريخ"), Integer.MAX_VALUE);
    }

    @Test
    public void analysisCardDateWithArabicIndicDigits() {
        Assert.assertEquals(ArabicDates.parseDayMonthYear("مباراة رسمية\n٢٠ أكتوبر ٢٠٢٦ في ١٠:٠٤ ص").orElse(null),
                LocalDate.of(2026, 10, 20));
        Assert.assertEquals(AnalysesListCardRules
                .parseSubmissionDate("نادي جدة × نادي العروبة\n٢٠ أكتوبر ٢٠٢٦ في ١٠:٠٢ ص").orElse(null),
                LocalDate.of(2026, 10, 20));
        Assert.assertTrue(ArabicDates.parseDayMonthYear("الأحد، 3 يناير").isEmpty(), "no year → not a full date");
    }

    @Test
    public void readerHelpers() {
        Assert.assertEquals(ClubWidgetReader.roundNumber("الجولة 6"), 6);
        Assert.assertEquals(ClubWidgetReader.roundNumber("جولة 12"), 12);
        Assert.assertEquals(ClubWidgetReader.roundNumber("Round of 32"), 0);
        Assert.assertEquals(ClubWidgetReader.outcomeFromColor("p-tag bg-green-400 text-white"), 'W');
        Assert.assertEquals(ClubWidgetReader.outcomeFromColor("p-tag bg-neutral-200"), 'D');
        Assert.assertEquals(ClubWidgetReader.outcomeFromColor("bg-red-500 text-white"), 'L');
        Assert.assertEquals(PortalDom.parseIntOr("+8", -1), 8);
        Assert.assertEquals(PortalDom.parseIntOr("-4", 0), -4);
        Assert.assertEquals(PortalDom.parseIntOr("—", -1), -1);
    }

    @Test
    public void resultCardsComparedNewestFirstWithClubFirstScore() {
        List<Fixture> expected = ClubDataFlow.lastPlayedNewestFirst(ClubDataFlow.clubFixtures(SCHEDULE, CLUB), 2);
        List<ResultCard> same = List.of(new ResultCard("العلا", "1 - 1", 1, 1, ""), new ResultCard("البكيرية", "2 - 0", 2, 0, ""));
        Assert.assertEquals(ClubDataComparisons.resultCards(same, expected, CLUB, "fixtures tab"), List.of());
        List<ResultCard> swapped = List.of(same.get(1), same.get(0));
        Assert.assertEquals(ClubDataComparisons.resultCards(swapped, expected, CLUB, "fixtures tab").size(), 2);
        Assert.assertTrue(ClubDataComparisons.resultCards(same.subList(0, 1), expected, CLUB, "fixtures tab").getFirst()
                .contains("1 cards but fixtures tab has 2"));
    }

    @Test
    public void leaderboardNamesMatchSquadByNormalisedContainment() {
        List<SquadPlayer> squad = List.of(player("عبدالله بن سعد الشهري", ""), player("فهد الأحمد", ""));
        List<LeaderboardEntry> leaders = List.of(leader("عبدالله الشهري"), leader("عبدالله بن سعد الشهري"),
                leader("فهد الاحمد"), leader("ماجد سالم"));
        Assert.assertEquals(ClubDataComparisons.notInSquad(leaders, squad), List.of("عبدالله الشهري", "ماجد سالم"));
    }

    @Test
    public void overAgeLimitIgnoresBlankAges() {
        List<SquadPlayer> u18 = List.of(player("A", "17"), player("B", "18"), player("C", "19"), player("D", ""));
        Assert.assertEquals(ClubDataComparisons.overAgeLimit(u18, 18), List.of("C (19)"));
    }

    private static Fixture played(int round, String home, int hg, int ag, String away, String date) {
        return new Fixture("دوري يلو", round, home, away, hg, ag, date, "", "");
    }

    private static SquadPlayer player(String name, String age) {
        return new SquadPlayer(name, "", "", "", "", "", age, "");
    }

    private static LeaderboardEntry leader(String name) {
        return new LeaderboardEntry(name, "", "1", "", "");
    }
}
