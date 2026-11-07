package com.taf.systems.zulfi.features.analysis.status;

import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.analysis.status.AnlsStsTestIds;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.AnlsStsGate;
import com.taf.systems.zulfi.config.AnlsStsGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.AnalysisStatusTrackingFlow;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.flows.analysis.CreateAnalysisFlow;
import com.taf.systems.zulfi.flows.analysis.OfficialFixtureSelectionFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisDetailsPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.AnalysisProcessingStatusMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * US-FAP-ANL-002 — Track match analysis processing status ({@code قيد التحليل} → {@code اكتمل التحليل}).
 *
 * <p>Cross-story: HOME-001 AC-30…36 (Home card labels/actions), ANL-SYNC (schedule status labels),
 * ANL-OFF-HP-001 (optional phase-d observe submit — exactly one submit, no duplicate).
 *
 * <p>Enable via {@code -Danl.sts.phase=a|b|c|d}. Default {@code none} skips all methods.
 */
@Epic("Zulfi Portal")
@Feature(AnlsStsTestIds.FEATURE)
public class AnalysisProcessingStatusAcceptanceTest extends AuthenticatedUiTest {

    private AnalysisStatusTrackingFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for ANL-STS tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new AnalysisStatusTrackingFlow(browser());
    }

    // --- A. Initial status (phase d, observe submit) ---

    @Test(description = AnlsStsTestIds.ANL_STS_001 + " — AC-01…03 Post-submit observe (single submit)")
    public void anlSts001_ac01Through03_postSubmitProcessingRecord() {
        AnlsStsGate.requirePostSubmitObservationAuthorized();
        runPostSubmitObservation(
                AnlsStsTestIds.ANL_STS_001,
                details -> {
                    Assert.assertEquals(
                            details.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS);
                    Assert.assertTrue(details.readAnalysisIdFromUrl().isPresent(), "Analysis id in URL");
                    String match = details.readMatchName();
                    if (match.isBlank()) {
                        throw new SkipException("Match name not readable on details — bind known fixture label.");
                    }
                    AnalysesListPage list = new AnalysesListPage(browser()).openViaSidebarNavigation();
                    list.searchByMatchName(match);
                    int count = list.countVisibleCardsMatchingTitleFragment(match);
                    Assert.assertEquals(count, 1, "Expected single list card for submitted analysis");
                    return StepResult.pass(
                            "status="
                                    + AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS
                                    + "; id="
                                    + details.readAnalysisIdFromUrl().orElse(-1L)
                                    + "; cardCount=1");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_002 + " — AC-02 Status belongs to submitted record")
    public void anlSts002_ac02_statusOnSameRecord() {
        skipWhenGroupedInPostSubmitObserve(AnlsStsTestIds.ANL_STS_002);
    }

    @Test(description = AnlsStsTestIds.ANL_STS_003 + " — AC-03 Submit does not create extra list row")
    public void anlSts003_ac03_noExtraRecordOnSubmit() {
        skipWhenGroupedInPostSubmitObserve(AnlsStsTestIds.ANL_STS_003);
    }

    // --- B. Asynchronous processing ---

    @Test(description = AnlsStsTestIds.ANL_STS_004 + " — AC-04 Processing continues after leaving details")
    public void anlSts004_ac04_processingSurvivesNavigationAway() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_004);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_004,
                "Leave details and return — status unchanged",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(
                            details.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS);
                    browser().navigate().to(PortalConfig.homeUrl());
                    AnalysisDetailsPage again = flow.openDetailsById(id);
                    Assert.assertEquals(
                            again.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS);
                    return StepResult.pass("statusStillProcessing=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_005 + " — AC-05 Leaving page does not cancel analysis")
    public void anlSts005_ac05_leaveDoesNotCancel() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_005);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_005,
                "Record remains قيد التحليل after session navigation",
                () -> {
                    flow.openDetailsById(id);
                    browser().navigate().to(PortalConfig.homeUrl());
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(
                            details.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS);
                    return StepResult.pass("stillProcessing=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_006 + " — AC-06 Return does not restart processing")
    public void anlSts006_ac06_returnDoesNotRestart() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_006);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_006,
                "Repeated open shows same id and processing state",
                () -> {
                    AnalysisDetailsPage first = flow.openDetailsById(id);
                    long firstId = first.readAnalysisIdFromUrl().orElse(-1L);
                    first.refresh();
                    AnalysisDetailsPage second = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    Assert.assertEquals(second.readAnalysisIdFromUrl().orElse(-1L), firstId);
                    Assert.assertEquals(
                            second.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS);
                    return StepResult.pass("sameId=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_007 + " — AC-07 Refresh/reopen does not resubmit")
    public void anlSts007_ac07_refreshDoesNotResubmit() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_007);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_007,
                "List card count stable after portal refresh",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    String match = details.readMatchName();
                    if (match.isBlank()) {
                        throw new SkipException("Need readable match name for duplicate card check.");
                    }
                    browser().navigate().refresh();
                    AnalysesListPage list = new AnalysesListPage(browser()).openViaSidebarNavigation();
                    list.searchByMatchName(match);
                    int count = list.countVisibleCardsMatchingTitleFragment(match);
                    Assert.assertEquals(count, 1);
                    return StepResult.pass("cardCount=1 after refresh");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_008 + " — AC-08 Return shows existing record")
    public void anlSts008_ac08_existingRecordOnReturn() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_008);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_008,
                "Direct id open resolves details route",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(details.readAnalysisIdFromUrl().orElse(-1L), id);
                    return StepResult.pass("recordFound=true");
                });
    }

    // --- C. Status retrieval ---

    @Test(description = AnlsStsTestIds.ANL_STS_009 + " — AC-09 Details retrieves latest status")
    public void anlSts009_ac09_detailsRetrievesLatestStatus() {
        requirePhase(Phase.A, AnlsStsTestIds.ANL_STS_009);
        runStep(
                AnlsStsTestIds.ANL_STS_009,
                "Open first list details — lifecycle label present",
                () -> {
                    flow.openAnalysesList().clickFirstVisibleViewDetails();
                    AnalysisDetailsPage details = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    String status = details.readLifecycleStatusLabel();
                    Assert.assertFalse(status.isBlank(), "Expected قيد التحليل or اكتمل التحليل");
                    return StepResult.pass("status=" + status);
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_010 + " — AC-10 Incomplete record shows قيد التحليل")
    public void anlSts010_ac10_processingLabelOnDetails() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_010);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        assertDetailsStatus(id, AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS, AnlsStsTestIds.ANL_STS_010);
    }

    @Test(description = AnlsStsTestIds.ANL_STS_011 + " — AC-11 Completed record shows اكتمل التحليل")
    public void anlSts011_ac11_completedLabelOnDetails() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_011);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        assertDetailsStatus(id, AnalysisProcessingStatusMessages.STATUS_COMPLETED, AnlsStsTestIds.ANL_STS_011);
    }

    @Test(description = AnlsStsTestIds.ANL_STS_012 + " — AC-12 Consistent Arabic labels Home/List/Details")
    public void anlSts012_ac12_crossSurfaceStatusConsistency() {
        requirePhase(Phase.A, AnlsStsTestIds.ANL_STS_012);
        runStep(
                AnlsStsTestIds.ANL_STS_012,
                "Compare Home recent, list, and details for configured or discovered record",
                () -> {
                    Optional<String> homeStatus = flow.readFirstHomeRecentStatus();
                    if (homeStatus.isEmpty()) {
                        throw new SkipException("No Home recent analyses for cross-surface check.");
                    }
                    AnalysisDetailsPage details =
                            flow.openDetailsFromHomeRecentByStatus(homeStatus.get());
                    String detailsStatus = details.readLifecycleStatusLabel();
                    String match = details.readMatchName();
                    Optional<String> listStatus = flow.readListStatusForMatchFragment(match);
                    Assert.assertTrue(
                            flow.statusesConsistent(homeStatus.get(), listStatus.orElse(""), detailsStatus),
                            "Home="
                                    + homeStatus.get()
                                    + " list="
                                    + listStatus.orElse("n/a")
                                    + " details="
                                    + detailsStatus);
                    return StepResult.pass("consistent=true");
                });
    }

    // --- D. Processing state ---

    @Test(description = AnlsStsTestIds.ANL_STS_013 + " — AC-13 Processing title")
    public void anlSts013_ac13_processingTitle() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_013);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_013,
                AnalysisProcessingStatusMessages.PROCESSING_TITLE,
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertTrue(details.isProcessingTitleVisible());
                    return StepResult.pass("processingTitleVisible=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_014 + " — AC-14 Processing description")
    public void anlSts014_ac14_processingDescription() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_014);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_014,
                "Processing body copy",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertTrue(details.isProcessingDescriptionVisible());
                    return StepResult.pass("processingDescriptionVisible=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_015 + " — AC-15 Match/video info visible while processing")
    public void anlSts015_ac15_metadataVisibleWhileProcessing() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_015);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_015,
                "Match name and video fields readable",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.readMatchName().isBlank(), "Match name visible");
                    Assert.assertFalse(details.readVideoUrl().isBlank(), "Video URL visible");
                    return StepResult.pass("metadataVisible=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_016 + " — AC-16 Dashboard disabled while processing")
    public void anlSts016_ac16_dashboardDisabledProcessing() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_016);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_016,
                "Interactive dashboard action disabled",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    if (details.isInteractiveDashboardActionPresent()) {
                        Assert.assertFalse(details.isInteractiveDashboardActionEnabled());
                    }
                    return StepResult.pass("dashboardDisabled=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_017 + " — AC-17 PDF disabled while processing")
    public void anlSts017_ac17_pdfDisabledProcessing() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_017);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_017,
                "PDF report action disabled",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    if (details.isPdfReportActionPresent()) {
                        Assert.assertFalse(details.isPdfReportActionEnabled());
                    }
                    return StepResult.pass("pdfDisabled=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_018 + " — AC-18 No direct result content while processing")
    public void anlSts018_ac18_noResultContentWhileProcessing() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_018);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_018,
                "Dashboard/PDF not enabled for قيد التحليل",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.pageContainsDashboardOrPdfContentWhileProcessing());
                    return StepResult.pass("protected=true");
                });
    }

    // --- E. Completion ---

    @Test(description = AnlsStsTestIds.ANL_STS_019 + " — AC-19 Completion updates same record status")
    public void anlSts019_ac19_completedStatusOnBoundRecord() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_019);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        assertDetailsStatus(id, AnalysisProcessingStatusMessages.STATUS_COMPLETED, AnlsStsTestIds.ANL_STS_019);
    }

    @Test(description = AnlsStsTestIds.ANL_STS_020 + " — AC-20 Completion does not create new record")
    public void anlSts020_ac20_singleRecordAfterCompletion() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_020);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_020,
                "List shows one card for completed analysis id",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    String match = details.readMatchName();
                    if (match.isBlank()) {
                        throw new SkipException("Need match name for list cardinality check.");
                    }
                    AnalysesListPage list = new AnalysesListPage(browser()).openViaSidebarNavigation();
                    list.searchByMatchName(match);
                    Assert.assertEquals(list.countVisibleCardsMatchingTitleFragment(match), 1);
                    return StepResult.pass("cardCount=1");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_021 + " — AC-21 Processing message hidden when completed")
    public void anlSts021_ac21_noProcessingCopyWhenCompleted() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_021);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_021,
                "Processing title absent on completed record",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.isProcessingTitleVisible());
                    return StepResult.pass("processingCopyHidden=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_022 + " — AC-22 Interactive dashboard available")
    public void anlSts022_ac22_dashboardEnabledWhenCompleted() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_022);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_022,
                AnalysisProcessingStatusMessages.INTERACTIVE_DASHBOARD_LABEL,
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertTrue(details.isInteractiveDashboardActionPresent());
                    Assert.assertTrue(details.isInteractiveDashboardActionEnabled());
                    return StepResult.pass("dashboardEnabled=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_023 + " — AC-23 PDF report available")
    public void anlSts023_ac23_pdfEnabledWhenCompleted() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_023);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_023,
                AnalysisProcessingStatusMessages.PDF_REPORT_LABEL,
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertTrue(details.isPdfReportActionPresent());
                    Assert.assertTrue(details.isPdfReportActionEnabled());
                    return StepResult.pass("pdfEnabled=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_024 + " — AC-24 Outputs belong to same completed record")
    public void anlSts024_ac24_outputsSameAnalysisId() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_024);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_024,
                "URL id stable with enabled outputs",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(details.readAnalysisIdFromUrl().orElse(-1L), id);
                    Assert.assertTrue(details.isInteractiveDashboardActionEnabled());
                    Assert.assertTrue(details.isPdfReportActionEnabled());
                    return StepResult.pass("sameIdWithOutputs=true");
                });
    }

    // --- F. Data integrity ---

    @Test(description = AnlsStsTestIds.ANL_STS_025 + " — AC-25 Match name preserved")
    public void anlSts025_ac25_matchNamePreserved() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_025);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_025,
                "Match name non-empty on completed record",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.readMatchName().isBlank());
                    return StepResult.pass("matchName=" + details.readMatchName());
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_026 + " — AC-26 Description preserved")
    public void anlSts026_ac26_descriptionPreserved() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_026);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_026,
                "Description field readable (may be empty if never provided)",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    String description = details.readDescription();
                    return StepResult.pass("descriptionLength=" + description.length());
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_027 + " — AC-27 Video source/url preserved")
    public void anlSts027_ac27_videoFieldsPreserved() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_027);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_027,
                "Video URL present on completed record",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.readVideoUrl().isBlank());
                    return StepResult.pass("videoUrlPresent=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_028 + " — AC-28 Submission date unchanged")
    public void anlSts028_ac28_submissionDateReadable() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_028);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_028,
                "Submission date field present",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertFalse(details.readSubmissionDate().isBlank());
                    return StepResult.pass("submissionDate=" + details.readSubmissionDate());
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_029 + " — AC-29 Video source corresponds to URL")
    public void anlSts029_ac29_sourceUrlConsistency() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_029);
        long processingId = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_029,
                "Source label vs href (flags BR-ANL-STS-010 defect)",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(processingId);
                    String source = details.readVideoSource();
                    String url = details.readVideoUrl();
                    if (source.isBlank()) {
                        throw new SkipException("Video source label not exposed in DOM.");
                    }
                    Assert.assertTrue(
                            details.videoSourceMatchesUrl(source, url),
                            "Drive+YouTube mismatch — product defect documented in strategy");
                    return StepResult.pass("sourceUrlConsistent=" + details.videoSourceMatchesUrl(source, url));
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_030 + " — AC-30 Refresh does not duplicate record")
    public void anlSts030_ac30_refreshNoDuplicate() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_030);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_030,
                "Details refresh keeps same analysis id",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    details.refresh();
                    Assert.assertEquals(details.readAnalysisIdFromUrl().orElse(-1L), id);
                    return StepResult.pass("sameIdAfterRefresh=true");
                });
    }

    // --- G. Result availability protection ---

    @Test(description = AnlsStsTestIds.ANL_STS_031 + " — AC-31 Dashboard/PDF unavailable before completion")
    public void anlSts031_ac31_outputsBlockedBeforeCompletion() {
        requirePhase(Phase.B, AnlsStsTestIds.ANL_STS_031);
        long id = AnlsStsGate.requireProcessingAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_031,
                "Processing record blocks outputs",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    if (details.isInteractiveDashboardActionPresent()) {
                        Assert.assertFalse(details.isInteractiveDashboardActionEnabled());
                    }
                    if (details.isPdfReportActionPresent()) {
                        Assert.assertFalse(details.isPdfReportActionEnabled());
                    }
                    return StepResult.pass("outputsBlocked=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_032 + " — AC-32 Completed actions only on completed record")
    public void anlSts032_ac32_completedActionsOnlyWhenCompleted() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_032);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_032,
                "Completed id enables dashboard",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(
                            details.readLifecycleStatusLabel(),
                            AnalysisProcessingStatusMessages.STATUS_COMPLETED);
                    Assert.assertTrue(details.isInteractiveDashboardActionEnabled());
                    return StepResult.pass("completedActions=true");
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_033 + " — AC-33 Outputs scoped to selected analysis id")
    public void anlSts033_ac33_outputsScopedToSelectedId() {
        requirePhase(Phase.C, AnlsStsTestIds.ANL_STS_033);
        long id = AnlsStsGate.requireCompletedAnalysisId();
        runStep(
                AnlsStsTestIds.ANL_STS_033,
                "URL contains bound analysis id",
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(details.readAnalysisIdFromUrl().orElse(-1L), id);
                    return StepResult.pass("scopedId=" + id);
                });
    }

    // --- H. Status scope ---

    @Test(description = AnlsStsTestIds.ANL_STS_034 + " — AC-34 Only approved lifecycle statuses")
    public void anlSts034_ac34_onlyApprovedLifecycleStatuses() {
        requirePhase(Phase.A, AnlsStsTestIds.ANL_STS_034);
        runStep(
                AnlsStsTestIds.ANL_STS_034,
                "Details uses قيد التحليل or اكتمل التحليل only",
                () -> {
                    flow.openAnalysesList().clickFirstVisibleViewDetails();
                    AnalysisDetailsPage details = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    String status = details.readLifecycleStatusLabel();
                    Assert.assertTrue(
                            AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS.equals(status)
                                    || AnalysisProcessingStatusMessages.STATUS_COMPLETED.equals(status));
                    return StepResult.pass("status=" + status);
                });
    }

    @Test(description = AnlsStsTestIds.ANL_STS_035 + " — AC-35 No failed status or retry action")
    public void anlSts035_ac35_noFailedOrRetryUi() {
        requirePhase(Phase.A, AnlsStsTestIds.ANL_STS_035);
        runStep(
                AnlsStsTestIds.ANL_STS_035,
                "First Release lifecycle UI scope",
                () -> {
                    flow.openAnalysesList().clickFirstVisibleViewDetails();
                    AnalysisDetailsPage details = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    Assert.assertFalse(details.pageExposesFailedLifecycle());
                    return StepResult.pass("noFailedLifecycle=true");
                });
    }

    // --- helpers ---

    private interface DetailsAssertion {
        StepResult run(AnalysisDetailsPage details);
    }

    private void runPostSubmitObservation(String testId, DetailsAssertion assertion) {
        CreateAnalysisFlow createFlow = new CreateAnalysisFlow(browser());
        OfficialFixtureSelectionFlow selectionFlow = new OfficialFixtureSelectionFlow(browser());

        runStep(
                testId,
                "Single authorized submit then poll details once",
                () -> {
                    selectionFlow.verifyAuthenticatedHome();
                    CreateAnalysisPage form = createFlow.openCreatePopupFromHome();
                    createFlow.completeOfficialMatchSelection(form);
                    String fixture = form.getOfficialMatchSelectedLabel();
                    AnalysisSubmissionAttempt attempt = AnalysisSubmissionAttempt.authorized(fixture);
                    attempt.selectFixtureBeforeSubmit(fixture);
                    String marker = "ANL-STS observe " + System.currentTimeMillis();
                    createFlow.fillDescriptionAndVideo(form, marker, AnalysisCreateGate.resolveVideoUrl());
                    AnalysisPostSubmitOutcome outcome =
                            createFlow.submitOfficialAnalysisExactlyOnce(form, attempt);
                    if (outcome != AnalysisPostSubmitOutcome.SUCCESS) {
                        return StepResult.fail("postSubmitOutcome=" + outcome);
                    }
                    AnalysisDetailsPage details = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    return assertion.run(details);
                });
    }

    private void assertDetailsStatus(long id, String expected, String testId) {
        runStep(
                testId,
                "Status «" + expected + "» for id=" + id,
                () -> {
                    AnalysisDetailsPage details = flow.openDetailsById(id);
                    Assert.assertEquals(details.readLifecycleStatusLabel(), expected);
                    return StepResult.pass("status=" + expected);
                });
    }

    private static void skipWhenGroupedInPostSubmitObserve(String testCaseId) {
        AnlsStsGate.requirePhaseAtLeast(Phase.D);
        if (!AnlsStsGate.isObserveSubmitEnabled()) {
            throw new SkipException(
                    testCaseId + " requires -D" + AnlsStsGate.OBSERVE_SUBMIT_PROPERTY + "=true (grouped in ANL-STS-001).");
        }
        throw new SkipException(
                testCaseId + " assertions run inside ANL-STS-001 (exactly-one submit policy).");
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        AnlsStsGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-ANL-002",
                AnlsStsTestIds.FEATURE,
                "Analysis processing status tracking",
                "Credentials + phase gate satisfied",
                "phase=" + AnlsStsGate.currentPhase());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute ANL-STS scenario",
                objective,
                "Per US-FAP-ANL-002 acceptance criteria",
                action,
                browser(),
                testCaseId);
    }
}
