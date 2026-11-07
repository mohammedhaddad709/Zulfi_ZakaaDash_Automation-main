package com.taf.systems.zulfi.features.matchschedule;


import com.taf.systems.zulfi.ui.UiRetry;import org.openqa.selenium.StaleElementReferenceException;
import com.taf.systems.zulfi.config.AnlsSyncGate;
import com.taf.systems.zulfi.config.AnlsSyncGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.matchschedule.AnalysisStatusSyncFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.matchschedule.AnlsSyncTestIds;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage.MatchResultScores;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * US-FAP-ANL-SYNC acceptance tests. Enable phases via {@code -Danl.sync.phase=a|b|c|d}. Default {@code none}
 * skips all methods.
 */
@Epic("Zulfi Portal")
@Feature(AnlsSyncTestIds.FEATURE)
public class AnalysisStatusSyncAcceptanceTest extends AuthenticatedUiTest {

    private AnalysisStatusSyncFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for ANL-SYNC tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new AnalysisStatusSyncFlow(browser());
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_001 + " — Played match without analysis")
    public void anlSync001_playedMatchWithoutAnalysis() {
        requirePhase(Phase.A, AnlsSyncTestIds.ANL_SYNC_001);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_001,
                "Locate played row «غير محللة»",
                () -> {
                    flow.openMatchSchedule();
                    // Rows re-render while the schedule settles: re-locate the row if it goes stale.
                    return UiRetry.onStale(() -> {
                        Optional<WebElement> row =
                                flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED);
                        if (row.isEmpty()) {
                            throw new SkipException(
                                    "No schedule row with «"
                                            + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                                            + "» — need played official data.");
                        }
                        Assert.assertTrue(
                                flow.rowMatchesStatusActionRules(
                                        row.get(), MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED),
                                "Expected «تحليل المباراة» without details/dashboard actions");
                        return StepResult.pass("status=غير محللة; startAnalysisVisible=true");
                    });
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_002 + " — Sync status after analysis creation")
    public void anlSync002_synchronizeStatusAfterAnalysisCreation() {
        AnlsSyncGate.requireLifecycleSubmitAuthorized();
        AnlsSyncGate.requirePhaseDFixtureBinding();
        requirePhase(Phase.D, AnlsSyncTestIds.ANL_SYNC_002);
        throw new SkipException(
                "Lifecycle creation observation not automated — authorize with -Danl.sync.lifecycle.enabled=true, "
                        + "-Danl.sync.creation.enabled=true, -Danalysis.create.enabled=true and bind fixture index; "
                        + "then observe row transition غير محللة → قيد التحليل after MCH-ANL submit.");
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_003 + " — Hide تحليل المباراة while processing")
    public void anlSync003_removeStartAnalysisDuringProcessing() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_003);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_003,
                "Row «قيد التحليل» exposes عرض التفاصيل only",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row = requireRow(MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS);
                    Assert.assertFalse(
                            new MatchSchedulePage(browser()).isStartAnalysisVisible(row),
                            MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS + " must be hidden");
                    Assert.assertTrue(
                            new MatchSchedulePage(browser()).isViewDetailsVisible(row),
                            MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DETAILS + " must be visible");
                    return StepResult.pass("startAnalysis=false; viewDetails=true");
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_004 + " — Sync completed analysis status")
    public void anlSync004_synchronizeCompletedAnalysis() {
        requirePhase(Phase.D, AnlsSyncTestIds.ANL_SYNC_004);
        AnlsSyncGate.requirePhaseDFixtureBinding();
        runStep(
                AnlsSyncTestIds.ANL_SYNC_004,
                "Fixture row shows «اكتمل التحليل» when analysis completed in environment",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row = requireRow(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    Assert.assertEquals(
                            new MatchSchedulePage(browser()).readAnalysisStatusForRow(row),
                            MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    return StepResult.pass("status=اكتمل التحليل");
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_005 + " — Completed match actions")
    public void anlSync005_completedMatchActions() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_005);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_005,
                "Completed row actions",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row = requireRow(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    Assert.assertTrue(
                            flow.rowMatchesStatusActionRules(
                                    row, MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED));
                    return StepResult.pass("details+dashboard visible; startAnalysis hidden");
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_006 + " — Open correct analysis details")
    public void anlSync006_openCorrectAnalysisDetails() {
        requirePhase(Phase.C, AnlsSyncTestIds.ANL_SYNC_006);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_006,
                "عرض التفاصيل navigates away from schedule (read-only open)",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row =
                            requireRowAny(
                                    MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS,
                                    MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    // a missing button is the product behaviour under test (DEF-ZUL-008 family), not a click timeout
                    Assert.assertTrue(
                            new MatchSchedulePage(browser()).isViewDetailsVisible(row),
                            MatchScheduleAnalysisStatusMessages.ACTION_VIEW_DETAILS + " must be offered on an analysed row (row: "
                                    + PortalDom.normalize(row.getText()).replace('\n', ' ') + ")");
                    String scheduleUrl = browser().getCurrentUrl();
                    String detailsUrl = flow.openViewDetailsFromRow(row);
                    Assert.assertFalse(detailsUrl.isBlank(), "Details navigation should resolve a URL");
                    Assert.assertNotEquals(detailsUrl, scheduleUrl, "Details view should leave schedule grid");
                    return StepResult.pass("detailsUrl=" + detailsUrl);
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_007 + " — Open correct completed dashboard")
    public void anlSync007_openCorrectCompletedDashboard() {
        requirePhase(Phase.C, AnlsSyncTestIds.ANL_SYNC_007);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_007,
                "عرض لوحة التحليل opens dashboard route (read-only)",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row = requireRow(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    String scheduleUrl = browser().getCurrentUrl();
                    String dashboardUrl = flow.openViewDashboardFromRow(row);
                    Assert.assertFalse(dashboardUrl.isBlank());
                    Assert.assertNotEquals(dashboardUrl, scheduleUrl);
                    return StepResult.pass("dashboardUrl=" + dashboardUrl);
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_008 + " — Latest analysis status on refresh")
    public void anlSync008_useLatestAnalysisStatusOnRefresh() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_008);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_008,
                "Refresh schedule and re-read row status",
                () -> {
                    flow.openMatchSchedule();
                    Optional<WebElement> before =
                            flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS);
                    if (before.isEmpty()) {
                        before = flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    }
                    if (before.isEmpty()) {
                        before = flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED);
                    }
                    if (before.isEmpty()) {
                        throw new SkipException("No played rows with analysis status in schedule grid.");
                    }
                    WebElement row = before.get();
                    String statusBefore = new MatchSchedulePage(browser()).readAnalysisStatusForRow(row);
                    flow.refreshMatchSchedule();
                    Optional<WebElement> afterRow =
                            flow.findRowWithStatus(statusBefore);
                    Assert.assertTrue(afterRow.isPresent(), "Row still present after refresh");
                    String statusAfter =
                            new MatchSchedulePage(browser()).readAnalysisStatusForRow(afterRow.get());
                    Assert.assertEquals(
                            statusAfter,
                            statusBefore,
                            "Refresh must show latest known status (no stale downgrade in same session)");
                    return StepResult.pass("statusBeforeAfter=" + statusBefore);
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_009 + " — No infer unanalyzed on retrieval failure")
    public void anlSync009_doNotInferUnanalyzedFromRetrievalFailure() {
        requirePhase(Phase.D, AnlsSyncTestIds.ANL_SYNC_009);
        throw new SkipException(
                "API/analysis retrieval failure simulation requires controlled backend hook or fault injection — "
                        + "not available in default UI automation.");
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_010 + " — Do not falsely mark analysis complete")
    public void anlSync010_doNotFalselyMarkAnalysisComplete() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_010);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_010,
                "Non-completed rows must not show «اكتمل التحليل»",
                () -> {
                    flow.openMatchSchedule();
                    Optional<WebElement> inProgress =
                            flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS);
                    if (inProgress.isPresent()) {
                        String status =
                                new MatchSchedulePage(browser())
                                        .readAnalysisStatusForRow(inProgress.get());
                        Assert.assertFalse(
                                status.contains(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED),
                                "Processing row must not display completed status");
                    }
                    Optional<WebElement> notAnalyzed =
                            flow.findRowWithStatus(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED);
                    if (notAnalyzed.isPresent()) {
                        String status =
                                new MatchSchedulePage(browser())
                                        .readAnalysisStatusForRow(notAnalyzed.get());
                        Assert.assertFalse(
                                status.contains(MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED),
                                "Unanalyzed row must not display completed status");
                    }
                    if (inProgress.isEmpty() && notAnalyzed.isEmpty()) {
                        throw new SkipException("Need in-progress or not-analyzed row to assert negative completion.");
                    }
                    return StepResult.pass("noFalseComplete=true");
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_011 + " — Preserve match result on status change")
    public void anlSync011_preserveMatchResult() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_011);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_011,
                "Scores unchanged across refresh for same row",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row =
                            requireRowAny(
                                    MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED,
                                    MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS,
                                    MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    Optional<MatchResultScores> before = flow.readScores(row);
                    if (before.isEmpty()) {
                        throw new SkipException("Selected row has no parseable match result score.");
                    }
                    // Read the status before refreshing: the old row element is gone afterwards.
                    String status = new MatchSchedulePage(browser()).readAnalysisStatusForRow(row);
                    flow.refreshMatchSchedule();
                    Optional<WebElement> refreshed = flow.findRowWithStatus(status);
                    Assert.assertTrue(refreshed.isPresent());
                    Optional<MatchResultScores> after = flow.readScores(refreshed.get());
                    Assert.assertTrue(after.isPresent());
                    Assert.assertEquals(after.get().homeGoals(), before.get().homeGoals());
                    Assert.assertEquals(after.get().awayGoals(), before.get().awayGoals());
                    return StepResult.pass("scores=" + before.get().raw());
                });
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_012 + " — Friendly analysis must not sync to schedule")
    public void anlSync012_preventFriendlyAnalysisSynchronization() {
        requirePhase(Phase.D, AnlsSyncTestIds.ANL_SYNC_012);
        throw new SkipException(
                "Friendly vs official schedule isolation requires paired friendly submit + schedule row assertion — "
                        + "run under dedicated data setup (see ANL-FRD suite), not default ANL-SYNC grid pass.");
    }

    @Test(description = AnlsSyncTestIds.ANL_SYNC_013 + " — Prevent duplicate analysis action after sync")
    public void anlSync013_preventDuplicateAnalysisAfterSynchronization() {
        requirePhase(Phase.B, AnlsSyncTestIds.ANL_SYNC_013);
        runStep(
                AnlsSyncTestIds.ANL_SYNC_013,
                "Synced rows hide «تحليل المباراة»",
                () -> {
                    flow.openMatchSchedule();
                    WebElement row =
                            requireRowAny(
                                    MatchScheduleAnalysisStatusMessages.STATUS_IN_PROGRESS,
                                    MatchScheduleAnalysisStatusMessages.STATUS_COMPLETED);
                    Assert.assertFalse(
                            new MatchSchedulePage(browser()).isStartAnalysisVisible(row),
                            "Duplicate start action must not appear after synchronization");
                    return StepResult.pass("startAnalysisHidden=true");
                });
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        AnlsSyncGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-ANL-SYNC",
                AnlsSyncTestIds.FEATURE,
                "Authorized user; Yellow League schedule; phased automation",
                "Credentials configured; phase gate satisfied",
                "phase=" + AnlsSyncGate.currentPhase());
    }

    private WebElement requireRow(String status) {
        Optional<WebElement> row = flow.findRowWithStatus(status);
        if (row.isEmpty()) {
            throw new SkipException("No schedule row with status «" + status + "».");
        }
        return row.get();
    }

    private WebElement requireRowAny(String... statuses) {
        for (String status : statuses) {
            Optional<WebElement> row = flow.findRowWithStatus(status);
            if (row.isPresent()) {
                return row.get();
            }
        }
        throw new SkipException("No schedule row matching any of configured analysis statuses.");
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute acceptance scenario",
                objective,
                "Expected per US-FAP-ANL-SYNC AC",
                action,
                browser(),
                testCaseId);
    }
}
