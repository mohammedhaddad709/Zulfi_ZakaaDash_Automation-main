package com.taf.systems.zulfi.validations;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Row texts as rendered by Match Schedule on 2026-10-22 (played: score + Arabic date; upcoming: kickoff time). */
public class MatchScheduleRowRulesTest {

    private static final String PLAYED = "نادي الزلفي\n4 - 0\nالسبت، 19 سبتمبر\nنادي الرائد\nقيد التحليل";
    private static final String UPCOMING = "نادي الطائي\n12:30 م\nالأحد، 3 يناير\nنادي العدالة";

    @Test
    public void kickoffTimeIsNotAScore() {
        Assert.assertFalse(MatchScheduleRowRules.hasRecordedScore(UPCOMING));
        Assert.assertTrue(MatchScheduleRowRules.isUnplayedRow(UPCOMING));
        Assert.assertFalse(MatchScheduleRowRules.isPlayedRow(UPCOMING));
        Assert.assertFalse(HomeResultCardRules.hasRecordedScore("03:05 م"));
    }

    @Test
    public void playedRowScoreAndDate() {
        Assert.assertTrue(MatchScheduleRowRules.isPlayedRow(PLAYED));
        Assert.assertEquals(MatchScheduleRowRules.parseFirstScore(PLAYED), new int[] {4, 0});
        Assert.assertEquals(MatchScheduleRowRules.extractDateToken(PLAYED), "19 سبتمبر");
        Assert.assertEquals(MatchScheduleRowRules.extractDateToken(UPCOMING), "3 يناير");
    }

    @Test
    public void numericDatesAreNotScores() {
        Assert.assertFalse(MatchScheduleRowRules.hasRecordedScore("2026-10-19"));
        Assert.assertFalse(MatchScheduleRowRules.hasRecordedScore("19/10/2026"));
        Assert.assertTrue(MatchDateDisplayRules.looksLikeCalendarDateDisplay("19/10/2026"));
    }
}
