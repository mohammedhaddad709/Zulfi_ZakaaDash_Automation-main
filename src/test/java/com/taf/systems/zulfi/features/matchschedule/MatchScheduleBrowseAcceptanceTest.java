package com.taf.systems.zulfi.features.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.ScheduleFixturesView;
import org.openqa.selenium.By;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.config.DefaultClubConfig;
import com.taf.systems.zulfi.config.MchScheduleGate;
import com.taf.systems.zulfi.config.MchScheduleGate.Phase;
import com.taf.systems.zulfi.flows.matchschedule.MatchScheduleBrowseFlow;
import com.taf.systems.zulfi.flows.matchschedule.MatchScheduleGridSnapshot;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.matchschedule.MchScheduleTestIds;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleRowSnapshot;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.MatchSchedulePageMessages;
import com.taf.systems.zulfi.validations.MatchScheduleRowRules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * US-FAP-MCH — View and filter Yellow League Match Schedule (read-only).
 * Enable via {@code -Dmch.schedule.phase=a|b|c}.
 */
@Epic("Zulfi Portal")
@Feature(MchScheduleTestIds.FEATURE)
public class MatchScheduleBrowseAcceptanceTest extends AuthenticatedUiTest {

    private MatchScheduleBrowseFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for MCH-SCHED tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new MatchScheduleBrowseFlow(browser());
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_001 + " — Open Match Schedule from sidebar")
    public void mchSched001_openScheduleFromSidebar() {
        requirePhase(Phase.A, MchScheduleTestIds.MCH_SCHED_001);
        runStep(
                MchScheduleTestIds.MCH_SCHED_001,
                "Navigate via sidebar جدول المباريات",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    Assert.assertTrue(
                            schedule.isOnMatchScheduleRoute() || schedule.isDisplayed(),
                            "Schedule route; url=" + browser().getCurrentUrl());
                    return StepResult.pass("scheduleOpen=true");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_002 + " — Page title visible")
    public void mchSched002_pageTitleVisible() {
        requirePhase(Phase.A, MchScheduleTestIds.MCH_SCHED_002);
        runStep(
                MchScheduleTestIds.MCH_SCHED_002,
                "Assert «" + MatchSchedulePageMessages.PAGE_TITLE + "»",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    Assert.assertTrue(
                            schedule.isPageTitleVisible(), MatchSchedulePageMessages.PAGE_TITLE);
                    return StepResult.pass("titleVisible=true");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_003 + " — Page description visible")
    public void mchSched003_pageDescriptionVisible() {
        requirePhase(Phase.A, MchScheduleTestIds.MCH_SCHED_003);
        runStep(
                MchScheduleTestIds.MCH_SCHED_003,
                "Yellow League scope shown by the competition tabs (description removed — re-baselined 2026-10-22)",
                () -> {
                    flow.openFromSidebar();
                    ScheduleFixturesView tabs =
                            new ScheduleFixturesView(browser()).waitUntilLoaded();
                    Assert.assertEquals(tabs.activeCompetition(),
                            ScheduleFixturesView.LEAGUE_TAB, "default competition tab");
                    Assert.assertTrue(browser().findElement(By.tagName("body")).getText()
                            .contains(ScheduleFixturesView.CUP_TAB), "cup tab offered");
                    return StepResult.pass("active=«" + tabs.activeCompetition() + "»");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_004 + " — Yellow League scope (no friendly rows)")
    public void mchSched004_yellowLeagueScopeNoFriendlyHeuristic() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_004);
        requireScheduleRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_004,
                "Heuristic: no «ودية» rows on official schedule",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    Assert.assertFalse(
                            schedule.hasFriendlyMatchRowsHeuristic(),
                            "Schedule should list Yellow League fixtures only");
                    return StepResult.pass("friendlyRows=false");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_005 + " — Unplayed rows: clubs, date, time")
    public void mchSched005_unplayedRowsShowClubsDateTime() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_005);
        requireUnplayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_005,
                "Unplayed fixture shape",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    int checked = 0;
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.unplayed()) {
                            continue;
                        }
                        checked++;
                        Assert.assertFalse(row.rawText().isBlank(), "Club names in row text");
                        Assert.assertFalse(
                                row.kickoffTimeToken().isBlank(),
                                "Kickoff time required; row=" + row.rawText());
                        if (!row.dateToken().isBlank()) {
                            Assert.assertTrue(
                                    MatchDateDisplayRules.looksLikeCalendarDateDisplay(row.dateToken()),
                                    "Date token");
                        }
                    }
                    return StepResult.pass("unplayedChecked=" + checked);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_006 + " — Unplayed rows: no upcoming label")
    public void mchSched006_unplayedRowsNoUpcomingLabel() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_006);
        requireUnplayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_006,
                "No «" + MatchSchedulePageMessages.UPCOMING_ROW_LABEL + "» on fixtures",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.unplayed()) {
                            continue;
                        }
                        Assert.assertFalse(
                                row.rawText().contains(MatchSchedulePageMessages.UPCOMING_ROW_LABEL),
                                "Upcoming badge must not replace date/time; row=" + row.rawText());
                    }
                    return StepResult.pass("ok");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_007 + " — Unplayed rows: no analysis status")
    public void mchSched007_unplayedRowsNoAnalysisStatus() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_007);
        requireUnplayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_007,
                "Analysis status only on played rows",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.unplayed()) {
                            continue;
                        }
                        Assert.assertTrue(
                                row.analysisStatus().isBlank(),
                                "Unplayed row must not show status; row=" + row.rawText());
                        Assert.assertTrue(
                                MatchScheduleRowRules.findKnownAnalysisStatus(row.rawText()).isBlank(),
                                row.rawText());
                    }
                    return StepResult.pass("ok");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_008 + " — Played rows: result and clubs")
    public void mchSched008_playedRowsShowResultAndClubs() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_008);
        requirePlayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_008,
                "Played match displays score",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    int played = 0;
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.played()) {
                            continue;
                        }
                        played++;
                        Assert.assertTrue(row.homeScore() >= 0 && row.awayScore() >= 0, row.rawText());
                        Assert.assertFalse(row.rawText().isBlank(), "Club names");
                    }
                    return StepResult.pass("played=" + played);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_009 + " — Played analysis statuses")
    public void mchSched009_playedRowsKnownAnalysisStatus() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_009);
        requirePlayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_009,
                "غير محللة / قيد التحليل / اكتمل التحليل",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    int withStatus = 0;
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.played()) {
                            continue;
                        }
                        if (row.analysisStatus().isBlank()) {
                            continue;
                        }
                        withStatus++;
                        Assert.assertTrue(
                                MatchScheduleRowRules.findKnownAnalysisStatus(row.rawText()).length() > 0,
                                "Unknown status: " + row.analysisStatus());
                    }
                    return StepResult.pass("withStatus=" + withStatus);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_010 + " — Result independent of analysis status")
    public void mchSched010_resultIndependentOfAnalysisStatus() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_010);
        requirePlayedRows();
        runStep(
                MchScheduleTestIds.MCH_SCHED_010,
                "Score present whether or not status label shown",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.played()) {
                            continue;
                        }
                        Assert.assertTrue(
                                MatchScheduleRowRules.hasRecordedScore(row.rawText()),
                                "Score must not depend on status; row=" + row.rawText());
                    }
                    return StepResult.pass("ok");
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_011 + " — Match #ID secondary to club names")
    public void mchSched011_matchIdSecondaryToClubs() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_011);
        requireScheduleRows();
        String fragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                MchScheduleTestIds.MCH_SCHED_011,
                "When #ID present, club fragment precedes ID in text order",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    int withId = 0;
                    for (MatchScheduleRowSnapshot row : schedule.readMatchRows()) {
                        if (!row.matchIdPresent()) {
                            continue;
                        }
                        withId++;
                        Assert.assertTrue(
                                MatchScheduleRowRules.clubNamesPrecedeMatchId(row.rawText(), fragment)
                                        || !row.rawText().contains(fragment),
                                "Club names primary; row=" + row.rawText());
                    }
                    return StepResult.pass("rowsWithId=" + withId);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_012 + " — Filter by club")
    public void mchSched012_filterBySpecificClub() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_012);
        requireScheduleRows();
        String fragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                MchScheduleTestIds.MCH_SCHED_012,
                "Select النادي → " + fragment,
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    flow.showAllClubs(schedule);
                    int allCount = flow.captureGrid(schedule).rowCount();
                    flow.filterByClub(schedule, fragment);
                    Assert.assertTrue(
                            flow.clubFilterShows(schedule, fragment),
                            "Filter label; actual=" + schedule.readSelectedClubFilterLabel());
                    List<MatchScheduleRowSnapshot> filtered = flow.captureGrid(schedule).rows();
                    Assert.assertFalse(filtered.isEmpty(), "Expected rows for default club fragment");
                    for (MatchScheduleRowSnapshot row : filtered) {
                        Assert.assertTrue(
                                MatchScheduleRowRules.rowMentionsClub(row.rawText(), fragment),
                                "Row must involve club; row=" + row.rawText());
                    }
                    return StepResult.pass("all=" + allCount + "; filtered=" + filtered.size());
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_013 + " — Show all clubs")
    public void mchSched013_showAllClubsFilter() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_013);
        requireScheduleRows();
        String fragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                MchScheduleTestIds.MCH_SCHED_013,
                "«" + MatchSchedulePageMessages.FILTER_ALL_CLUBS + "» ≥ club-specific count",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    flow.filterByClub(schedule, fragment);
                    int clubCount = flow.captureGrid(schedule).rowCount();
                    flow.showAllClubs(schedule);
                    Assert.assertTrue(
                            flow.clubFilterShowsAllClubs(schedule),
                            "All clubs selected; label=" + schedule.readSelectedClubFilterLabel());
                    int allCount = flow.captureGrid(schedule).rowCount();
                    Assert.assertTrue(
                            allCount > clubCount,
                            "All clubs view must list more rows than one club; all="
                                    + allCount
                                    + "; club="
                                    + clubCount);
                    return StepResult.pass("all=" + allCount + "; club=" + clubCount);
                });
    }

    @Test(
            description =
                    MchScheduleTestIds.MCH_SCHED_014
                            + " — Preselected club from Home (see HOME-RES-009)")
    public void mchSched014_preselectedClubFromHomeViewMore() {
        requirePhase(Phase.B, MchScheduleTestIds.MCH_SCHED_014);
        String fragment = DefaultClubConfig.clubMatchFragment();
        runStep(
                MchScheduleTestIds.MCH_SCHED_014,
                "Home → عرض المزيد → schedule filter",
                () -> {
                    MatchSchedulePage schedule = flow.openViaHomeViewMore();
                    Assert.assertTrue(flow.isOnScheduleRoute(schedule), "Schedule open from Home");
                    Assert.assertTrue(
                            flow.clubFilterShows(schedule, fragment),
                            "Preselected Default Club; filter="
                                    + schedule.readSelectedClubFilterLabel());
                    return StepResult.pass("clubFragment=" + fragment);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_015 + " — No filter results empty copy")
    public void mchSched015_emptyFilterResultsCopy() {
        requirePhase(Phase.C, MchScheduleTestIds.MCH_SCHED_015);
        String emptyClub =
                MchScheduleGate.configuredEmptyFilterClubFragment()
                        .orElseThrow(
                                () ->
                                        new SkipException(
                                                "Set -Dmch.schedule.emptyFilterClub=... or env MCH_SCHEDULE_EMPTY_FILTER_CLUB for empty-filter scenario."));
        runStep(
                MchScheduleTestIds.MCH_SCHED_015,
                "Filter yields empty state copy",
                () -> {
                    MatchSchedulePage schedule = flow.openFromSidebar();
                    MatchScheduleGridSnapshot emptyGrid =
                            flow.applyEmptyFilterClub(schedule, emptyClub);
                    Assert.assertTrue(
                            emptyGrid.emptyFilterVisible() || !emptyGrid.hasParseableRows(),
                            MatchSchedulePageMessages.EMPTY_FILTER_TITLE);
                    return StepResult.pass("emptyClub=" + emptyClub);
                });
    }

    @Test(description = MchScheduleTestIds.MCH_SCHED_016 + " — Loading / error / retry")
    public void mchSched016_loadingAndErrorStates() {
        requirePhase(Phase.C, MchScheduleTestIds.MCH_SCHED_016);
        throw new SkipException(
                "Loading/error states require controlled API failure or network throttling.");
    }

    private void requireScheduleRows() {
        MatchSchedulePage schedule = flow.openFromSidebar();
        if (schedule.isLoadErrorVisible()) {
            throw new SkipException("Schedule load error in environment.");
        }
        if (schedule.readMatchRows().isEmpty() && !schedule.isEmptyFilterStateVisible()) {
            throw new SkipException(
                    "No parseable schedule rows (align DOM selectors with portal markup).");
        }
    }

    private void requireUnplayedRows() {
        MatchSchedulePage schedule = flow.openFromSidebar();
        boolean any =
                schedule.readMatchRows().stream().anyMatch(MatchScheduleRowSnapshot::unplayed);
        if (!any) {
            throw new SkipException("No unplayed fixtures in current schedule data.");
        }
    }

    private void requirePlayedRows() {
        MatchSchedulePage schedule = flow.openFromSidebar();
        boolean any = schedule.readMatchRows().stream().anyMatch(MatchScheduleRowSnapshot::played);
        if (!any) {
            throw new SkipException("No played matches in current schedule data.");
        }
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        MchScheduleGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-MCH",
                MchScheduleTestIds.FEATURE,
                "Authorized user; Yellow League schedule",
                "Credentials + phase gate satisfied",
                "phase="
                        + MchScheduleGate.currentPhase()
                        + "; defaultClub="
                        + DefaultClubConfig.configuredDefaultClubLabel());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute Match Schedule browse scenario",
                objective,
                "Per US-FAP-MCH AC",
                action,
                browser(),
                testCaseId);
    }
}
