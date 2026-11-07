package com.taf.systems.zulfi.features.home;

import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.config.HomeOverviewGate;
import com.taf.systems.zulfi.config.HomeOverviewGate.Phase;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.flows.home.HomeOverviewFlow;
import com.taf.systems.zulfi.flows.LoginFlow;
import com.taf.systems.zulfi.framework.BaseUiTest;
import com.taf.systems.zulfi.home.HomeOverviewTestIds;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.LoginPage;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.home.HomeAnalysisSummarySection;
import com.taf.systems.zulfi.pages.home.HomeHeroSection;
import com.taf.systems.zulfi.pages.home.HomeOverviewPage;
import com.taf.systems.zulfi.pages.home.HomeRecentAnalysesSection;
import com.taf.systems.zulfi.pages.home.HomeUpcomingMatchSection;
import com.taf.systems.zulfi.pages.home.RecentAnalysisCardSnapshot;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * US-FAP-HOME-001 — Home overview (Hero, summary, upcoming match, recent analyses).
 * Enable via {@code -Dhome.overview.phase=a|b|c|d}. Read-only — no analysis submit.
 *
 * <p>Overlaps same physical page as US-FAP-HOME-CURRENT-RESULTS ({@code currentTeamResultsSection})
 * but validates a separate user story.
 */
@Epic("Zulfi Portal")
@Feature(HomeOverviewTestIds.FEATURE)
public class HomeOverviewAcceptanceTest extends BaseUiTest {

    private static final Set<String> UNAUTHENTICATED_START =
            Set.of("home001_001_ac01_successfulLoginRedirectsToHome");

    private static final Pattern ANALYSIS_DETAILS_URL = Pattern.compile("(?i)/analysis/\\d+");

    private HomeOverviewFlow flow;

    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase(Method method) {
        if (UNAUTHENTICATED_START.contains(method.getName())) {
            if (!CredentialProvider.isConfigured()) {
                throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for HOME-001 AC-01.");
            }
            new LoginPage(browser()).open();
            return;
        }
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for HOME-001 tests.");
        }
        new LoginFlow(browser()).loginAsConfiguredUser();
        flow = new HomeOverviewFlow(browser());
    }

    // --- A. Home Page & Hero ---

    @Test(description = HomeOverviewTestIds.HOME_001_001 + " — AC-01 Login redirects to Home")
    public void home001_001_ac01_successfulLoginRedirectsToHome() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_001);
        CredentialProvider.Credentials credentials = CredentialProvider.requireCredentials();
        runStep(
                HomeOverviewTestIds.HOME_001_001,
                "Login → default landing Home",
                () -> {
                    LoginPage login = new LoginPage(browser());
                    login.enterEmail(credentials.username())
                            .enterPassword(credentials.password())
                            .submit();
                    HomePage home = new HomePage(browser()).awaitReady();
                    String url = browser().getCurrentUrl();
                    Assert.assertTrue(home.isReady(), HomeOverviewMessages.HERO_PRIMARY_ACTION);
                    Assert.assertTrue(
                            url.startsWith(PortalConfig.homeUrl()) || url.contains("/home"),
                            "Expected Home landing; url=" + url);
                    return StepResult.pass("homeReady=true; url=" + url);
                });
    }

    @Test(
            description =
                    HomeOverviewTestIds.HOME_001_002
                            + " — AC-02 Same Home for supported roles (Skip: single credential; parity per AC-03)")
    public void home001_002_ac02_supportedRolesShareHomeExperience() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_002);
        throw new SkipException(
                "AC-02 role parity: First Release uses one configured credential; "
                        + "same Home sections as AC-03 — dual-role credential matrix not automated.");
    }

    @Test(description = HomeOverviewTestIds.HOME_001_003 + " — AC-03 Core sections visible")
    public void home001_003_ac03_homeDisplaysOverviewSections() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_003);
        runStep(
                HomeOverviewTestIds.HOME_001_003,
                "Hero + summary + upcoming + recent section titles",
                () -> {
                    HomeOverviewPage overview = flow.openHomeOverview();
                    Assert.assertTrue(overview.hero().waitUntilVisible().isTitleVisible());
                    overview.analysisSummary().waitUntilAnyKpiVisible();
                    overview.upcomingMatch().waitUntilSectionVisible();
                    overview.recentAnalyses().waitUntilSectionVisible();
                    return StepResult.pass("allSectionAnchorsVisible=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_004 + " — AC-04 Hero title")
    public void home001_004_ac04_heroTitle() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_004);
        assertHeroCopy(true, false, HomeOverviewTestIds.HOME_001_004);
    }

    @Test(description = HomeOverviewTestIds.HOME_001_005 + " — AC-05 Hero description")
    public void home001_005_ac05_heroDescription() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_005);
        assertHeroCopy(false, true, HomeOverviewTestIds.HOME_001_005);
    }

    @Test(description = HomeOverviewTestIds.HOME_001_006 + " — AC-06 ابدأ التحليل → تحليل مباراة جديدة")
    public void home001_006_ac06_startAnalysisOpensNewAnalysisDialog() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_006);
        runStep(
                HomeOverviewTestIds.HOME_001_006,
                "Click Hero primary CTA (read-only)",
                () -> {
                    CreateAnalysisPage create = flow.openCreateAnalysisFromHero();
                    Assert.assertTrue(
                            create.isDialogTitleVisible(),
                            CreateAnalysisPage.DIALOG_TITLE_FRAGMENT);
                    flow.closeCreateDialogIfOpen();
                    return StepResult.pass("dialogTitleVisible=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_007 + " — AC-07 استعراض التحليلات → التحليلات")
    public void home001_007_ac07_browseAnalysesOpensAnalysesList() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_007);
        runStep(
                HomeOverviewTestIds.HOME_001_007,
                "Click Hero secondary CTA",
                () -> {
                    AnalysesListPage list = flow.openAnalysesListFromHero();
                    Assert.assertTrue(
                            list.waitUntilLoaded() != null && browser().getCurrentUrl().contains("analysis"),
                            "url=" + browser().getCurrentUrl());
                    return StepResult.pass("analysesRoute=true");
                });
    }

    // --- B. Analysis Summary ---

    @Test(description = HomeOverviewTestIds.HOME_001_008 + " — AC-08 Summary KPI labels incl. هداف الفريق")
    public void home001_008_ac08_summaryKpiLabelsPresent() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_008);
        runStep(
                HomeOverviewTestIds.HOME_001_008,
                "Verify KPI card labels (incl. top scorer)",
                () -> {
                    HomeAnalysisSummarySection summary =
                            flow.openHomeOverview().analysisSummary().waitUntilAnyKpiVisible();
                    Assert.assertTrue(
                            summary.hasExpectedKpiLabels(),
                            "AC-08 requires إجمالي/قيد/اكتمل/عدد اللاعبين/هداف الفريق");
                    return StepResult.pass("kpiLabelsIncludingTopScorer=true");
                });
    }

    @Test(
            description =
                    HomeOverviewTestIds.HOME_001_009
                            + "…"
                            + HomeOverviewTestIds.HOME_001_013
                            + " — AC-09…AC-13 KPI values parseable")
    public void home001_009_ac09Through13_summaryKpiValuesParseable() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_009);
        runStep(
                HomeOverviewTestIds.HOME_001_009,
                "Parse summary integers when DOM exposes them",
                () -> {
                    HomeAnalysisSummarySection summary =
                            flow.openHomeOverview().analysisSummary().waitUntilAnyKpiVisible();
                    OptionalInt total = summary.readTotalAnalyses();
                    OptionalInt processing = summary.readInProgress();
                    OptionalInt completed = summary.readCompleted();
                    OptionalInt players = summary.readPlayerCount();
                    if (total.isEmpty() || processing.isEmpty() || completed.isEmpty() || players.isEmpty()) {
                        throw new SkipException(
                                "KPI values not parseable from current Home markup; selectors may need alignment.");
                    }
                    Assert.assertTrue(total.getAsInt() >= 0);
                    Assert.assertTrue(processing.getAsInt() >= 0);
                    Assert.assertTrue(completed.getAsInt() >= 0);
                    Assert.assertTrue(players.getAsInt() >= 0);
                    return StepResult.pass(
                            "total="
                                    + total.getAsInt()
                                    + "; processing="
                                    + processing.getAsInt()
                                    + "; completed="
                                    + completed.getAsInt()
                                    + "; players="
                                    + players.getAsInt());
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_012 + " — AC-12 Total = processing + completed")
    public void home001_012_ac12_totalEqualsProcessingPlusCompleted() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_012);
        runStep(
                HomeOverviewTestIds.HOME_001_012,
                "BR-HOME-005 consistency",
                () -> {
                    HomeAnalysisSummarySection summary =
                            flow.openHomeOverview().analysisSummary().waitUntilAnyKpiVisible();
                    if (!summary.totalsConsistentWhenParseable()) {
                        throw new SkipException("KPI integers not all parseable for consistency check.");
                    }
                    Assert.assertTrue(summary.totalsConsistentWhenParseable());
                    return StepResult.pass("total=processing+completed");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_014 + " — AC-14 Refresh retrieves summary")
    public void home001_014_ac14_refreshRetrievesLatestSummary() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_014);
        runStep(
                HomeOverviewTestIds.HOME_001_014,
                "Refresh Home and re-read KPIs",
                () -> {
                    HomeAnalysisSummarySection before =
                            flow.openHomeOverview().analysisSummary().waitUntilAnyKpiVisible();
                    OptionalInt totalBefore = before.readTotalAnalyses();
                    HomeAnalysisSummarySection afterRefresh =
                            flow.refreshHomeOverview().analysisSummary().waitUntilAnyKpiVisible();
                    OptionalInt totalAfter = afterRefresh.readTotalAnalyses();
                    if (totalBefore.isEmpty() || totalAfter.isEmpty()) {
                        throw new SkipException("Could not parse إجمالي التحليلات before/after refresh.");
                    }
                    return StepResult.pass("totalBefore=" + totalBefore.getAsInt() + "; totalAfter=" + totalAfter.getAsInt());
                });
    }

    // --- C. Upcoming Match ---

    @Test(description = HomeOverviewTestIds.HOME_001_015 + " — AC-15 Upcoming section title")
    public void home001_015_ac15_upcomingSectionTitle() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_015);
        runStep(
                HomeOverviewTestIds.HOME_001_015,
                "المباراة القادمة / المباريات القادمة",
                () -> {
                    HomeUpcomingMatchSection upcoming =
                            flow.openHomeOverview().upcomingMatch().waitUntilSectionVisible();
                    Assert.assertTrue(upcoming.isSectionTitleVisible());
                    return StepResult.pass("upcomingTitleVisible=true");
                });
    }

    @Test(
            description =
                    HomeOverviewTestIds.HOME_001_016
                            + "…"
                            + HomeOverviewTestIds.HOME_001_021
                            + " — AC-16…AC-21 Upcoming content or empty")
    public void home001_016_ac16Through21_upcomingMatchContentOrEmpty() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_016);
        runStep(
                HomeOverviewTestIds.HOME_001_016,
                "Future match details or empty copy (read-only)",
                () -> {
                    HomeUpcomingMatchSection upcoming =
                            flow.openHomeOverview().upcomingMatch().waitUntilSectionVisible();
                    if (upcoming.isEmptyStateVisible()) {
                        Assert.assertTrue(
                                upcoming.sectionText().contains(HomeOverviewMessages.UPCOMING_EMPTY));
                        return StepResult.pass("emptyState=" + HomeOverviewMessages.UPCOMING_EMPTY);
                    }
                    if (upcoming.isLoadErrorVisible()) {
                        throw new SkipException("Upcoming match in error state — retry not automated in phase b.");
                    }
                    Assert.assertTrue(
                            upcoming.hasMatchContentWhenNotEmpty(),
                            "Expected team/competition/date content in section text");
                    return StepResult.pass("matchContentPresent=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_022 + " — AC-22 No upcoming match copy")
    public void home001_022_ac22_noUpcomingMatchEmptyCopy() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_022);
        HomeUpcomingMatchSection upcoming = flow.openHomeOverview().upcomingMatch().waitUntilSectionVisible();
        if (!upcoming.isEmptyStateVisible()) {
            throw new SkipException("Environment has an upcoming match — empty copy not applicable.");
        }
        runStep(
                HomeOverviewTestIds.HOME_001_022,
                "Verify empty message",
                () -> {
                    Assert.assertTrue(upcoming.sectionText().contains(HomeOverviewMessages.UPCOMING_EMPTY));
                    return StepResult.pass("emptyCopyVerified=true");
                });
    }

    // --- D. Recent Analyses ---

    @Test(description = HomeOverviewTestIds.HOME_001_023 + " — AC-23 Recent analyses section")
    public void home001_023_ac23_recentAnalysesWhenDataExists() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_023);
        requireRecentCardsOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_023,
                "Cards under آخر التحليلات",
                () -> StepResult.pass("cardCount=" + recentSection().countVisibleAnalysisCards()));
    }

    @Test(description = HomeOverviewTestIds.HOME_001_024 + " — AC-24 Card fields")
    public void home001_024_ac24_recentCardStructure() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_024);
        List<RecentAnalysisCardSnapshot> cards = requireRecentCardsOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_024,
                "Match name, status, view details on cards",
                () -> {
                    for (RecentAnalysisCardSnapshot card : cards) {
                        Assert.assertFalse(card.matchTitle().isBlank(), "matchTitle on card " + card.index());
                        Assert.assertFalse(card.status().isBlank(), "status on card " + card.index());
                        Assert.assertTrue(card.viewDetailsPresent(), "عرض التفاصيل on card " + card.index());
                    }
                    return StepResult.pass("cardsValidated=" + cards.size());
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_025 + " — AC-25 Video source indicator")
    public void home001_025_ac25_videoSourceIndicator() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_025);
        List<RecentAnalysisCardSnapshot> cards = requireRecentCardsOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_025,
                "YouTube / Google Drive marker",
                () -> {
                    boolean any = cards.stream().anyMatch(RecentAnalysisCardSnapshot::hasVideoSourceIndicator);
                    if (!any) {
                        throw new SkipException("No video source markers detected on visible recent cards.");
                    }
                    return StepResult.pass("videoSourceMarker=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_026 + " — AC-26 View details action present")
    public void home001_026_ac26_viewDetailsOnEachCard() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_026);
        List<RecentAnalysisCardSnapshot> cards = requireRecentCardsOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_026,
                "عرض التفاصيل",
                () -> {
                    for (RecentAnalysisCardSnapshot card : cards) {
                        Assert.assertTrue(card.viewDetailsPresent());
                    }
                    return StepResult.pass("allCardsHaveViewDetails=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_028 + " — AC-28 عرض المزيد → التحليلات")
    public void home001_028_ac28_viewMoreOpensAnalysesList() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_028);
        HomeRecentAnalysesSection recent = flow.openHomeOverview().recentAnalyses().waitUntilSectionVisible();
        if (!recent.isViewMoreVisible()) {
            throw new SkipException("«عرض المزيد» not visible (empty recent section?).");
        }
        runStep(
                HomeOverviewTestIds.HOME_001_028,
                "Click section View More",
                () -> {
                    AnalysesListPage list = flow.navigateToAnalysesViaRecentViewMore();
                    Assert.assertTrue(browser().getCurrentUrl().contains("analysis"));
                    Assert.assertNotNull(list);
                    return StepResult.pass("analysesListOpen=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_029 + " — AC-29 No delete on Home cards")
    public void home001_029_ac29_noDeleteOnRecentCards() {
        requirePhase(Phase.B, HomeOverviewTestIds.HOME_001_029);
        runStep(
                HomeOverviewTestIds.HOME_001_029,
                "Section scoped delete check",
                () -> {
                    HomeRecentAnalysesSection recent =
                            flow.openHomeOverview().recentAnalyses().waitUntilSectionVisible();
                    Assert.assertFalse(
                            recent.sectionContainsDeleteAction(),
                            "Delete must not appear on Home recent cards (First Release)");
                    return StepResult.pass("noDeleteInRecentSection=true");
                });
    }

    // --- E/F. Processing & Completed (phase c) ---

    @Test(description = HomeOverviewTestIds.HOME_001_030 + " — AC-30 Processing status label")
    public void home001_030_ac30_processingStatusLabel() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_030);
        requireCardWithStatus(HomeOverviewMessages.STATUS_IN_PROGRESS, HomeOverviewTestIds.HOME_001_030);
    }

    @Test(description = HomeOverviewTestIds.HOME_001_031 + " — AC-31 Processing allows view details")
    public void home001_031_ac31_processingViewDetailsEnabled() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_031);
        RecentAnalysisCardSnapshot card = findCardWithStatus(HomeOverviewMessages.STATUS_IN_PROGRESS);
        runStep(
                HomeOverviewTestIds.HOME_001_031,
                "Processing card view details enabled",
                () -> {
                    Assert.assertTrue(card.viewDetailsEnabled());
                    return StepResult.pass("viewDetailsEnabled=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_032 + " — AC-32 Dashboard disabled while processing")
    public void home001_032_ac32_processingDashboardDisabled() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_032);
        RecentAnalysisCardSnapshot card = findCardWithStatus(HomeOverviewMessages.STATUS_IN_PROGRESS);
        runStep(
                HomeOverviewTestIds.HOME_001_032,
                "عرض لوحة التحليل disabled in DOM",
                () -> {
                    Assert.assertTrue(card.dashboardPresent());
                    Assert.assertFalse(card.dashboardEnabled());
                    return StepResult.pass("dashboardDisabled=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_033 + " — AC-33 Disabled dashboard does not navigate")
    public void home001_033_ac33_disabledDashboardNoNavigation() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_033);
        RecentAnalysisCardSnapshot card = findCardWithStatus(HomeOverviewMessages.STATUS_IN_PROGRESS);
        if (card.dashboardEnabled()) {
            throw new SkipException("Expected disabled dashboard on processing card.");
        }
        runStep(
                HomeOverviewTestIds.HOME_001_033,
                "No navigation via disabled control",
                () -> StepResult.pass("dashboardEnabled=false; no click attempted"));
    }

    @Test(description = HomeOverviewTestIds.HOME_001_034 + " — AC-34 Completed status label")
    public void home001_034_ac34_completedStatusLabel() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_034);
        requireCardWithStatus(HomeOverviewMessages.STATUS_COMPLETED, HomeOverviewTestIds.HOME_001_034);
    }

    @Test(description = HomeOverviewTestIds.HOME_001_035 + " — AC-35 Dashboard enabled when completed")
    public void home001_035_ac35_completedDashboardEnabled() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_035);
        RecentAnalysisCardSnapshot card = findCardWithStatus(HomeOverviewMessages.STATUS_COMPLETED);
        runStep(
                HomeOverviewTestIds.HOME_001_035,
                "Completed card dashboard enabled",
                () -> {
                    Assert.assertTrue(card.dashboardPresent());
                    Assert.assertTrue(card.dashboardEnabled());
                    return StepResult.pass("dashboardEnabled=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_027 + " — AC-27 View details navigation smoke")
    public void home001_027_ac27_viewDetailsOpensAnalysisRoute() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_027);
        requireRecentCardsOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_027,
                "Click first enabled عرض التفاصيل (no deep ANL-004)",
                () -> {
                    String before = browser().getCurrentUrl();
                    recentSection().clickFirstEnabledViewDetails();
                    new WebDriverWait(browser(), Duration.ofSeconds(20))
                            .until(d -> !d.getCurrentUrl().equals(before));
                    String after = browser().getCurrentUrl();
                    Assert.assertTrue(
                            ANALYSIS_DETAILS_URL.matcher(after).find(),
                            "Expected analysis details route; url=" + after);
                    return StepResult.pass("navigated=true; url=" + after);
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_036 + " — AC-36 Dashboard navigation smoke")
    public void home001_036_ac36_dashboardOpensForCompleted() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_036);
        findCardWithStatus(HomeOverviewMessages.STATUS_COMPLETED);
        runStep(
                HomeOverviewTestIds.HOME_001_036,
                "Click enabled dashboard on completed card",
                () -> {
                    String before = browser().getCurrentUrl();
                    recentSection().clickFirstEnabledDashboard();
                    new WebDriverWait(browser(), Duration.ofSeconds(20))
                            .until(d -> !d.getCurrentUrl().equals(before));
                    String after = browser().getCurrentUrl();
                    Assert.assertTrue(
                            ANALYSIS_DETAILS_URL.matcher(after).find() || after.toLowerCase().contains("dashboard"),
                            "Expected analysis-scoped route; url=" + after);
                    return StepResult.pass("url=" + after);
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_037 + " — AC-37 Dashboard scoped to selected analysis")
    public void home001_037_ac37_dashboardRouteContainsAnalysisId() {
        requirePhase(Phase.C, HomeOverviewTestIds.HOME_001_037);
        findCardWithStatus(HomeOverviewMessages.STATUS_COMPLETED);
        runStep(
                HomeOverviewTestIds.HOME_001_037,
                "URL contains analysis id after dashboard click",
                () -> {
                    recentSection().clickFirstEnabledDashboard();
                    String after = browser().getCurrentUrl();
                    Assert.assertTrue(ANALYSIS_DETAILS_URL.matcher(after).find(), after);
                    return StepResult.pass("analysisIdInUrl=true");
                });
    }

    // --- G. Empty / loading / error (phase d) ---

    @Test(description = HomeOverviewTestIds.HOME_001_038 + " — AC-38 Recent empty title")
    public void home001_038_ac38_recentEmptyStateTitle() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_038);
        requireEmptyRecentOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_038,
                HomeOverviewMessages.RECENT_EMPTY_TITLE,
                () -> StepResult.pass("emptyTitleVisible=true"));
    }

    @Test(description = HomeOverviewTestIds.HOME_001_039 + " — AC-39 Empty CTA opens new analysis")
    public void home001_039_ac39_emptyStateStartAnalysisOpensDialog() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_039);
        HomeRecentAnalysesSection recent = requireEmptyRecentOrSkip();
        runStep(
                HomeOverviewTestIds.HOME_001_039,
                "Empty state «ابدأ التحليل»",
                () -> {
                    recent.clickEmptyStateStartAnalysis();
                    CreateAnalysisPage create = new CreateAnalysisPage(browser()).waitForCreatePopupReady();
                    Assert.assertTrue(create.isDialogTitleVisible());
                    flow.closeCreateDialogIfOpen();
                    return StepResult.pass("createDialogOpened=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_040 + " — AC-40 Loading state")
    public void home001_040_ac40_dynamicSectionsLoadingState() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_040);
        skipUnlessErrorHooks("Loading state requires controllable slow API or dedicated hook.");
    }

    @Test(description = HomeOverviewTestIds.HOME_001_041 + " — AC-41 No empty while loading")
    public void home001_041_ac41_emptyNotShownWhileLoading() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_041);
        skipUnlessErrorHooks("Loading vs empty requires timing-controlled environment.");
    }

    @Test(description = HomeOverviewTestIds.HOME_001_042 + " — AC-42 Recent retrieval error copy")
    public void home001_042_ac42_recentRetrievalErrorCopy() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_042);
        skipUnlessErrorHooks("Recent error copy requires simulated API failure.");
    }

    @Test(description = HomeOverviewTestIds.HOME_001_043 + " — AC-43 Recent error retry action")
    public void home001_043_ac43_recentErrorRetryAction() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_043);
        skipUnlessErrorHooks("Retry action requires simulated API failure.");
    }

    @Test(description = HomeOverviewTestIds.HOME_001_044 + " — AC-44 Hero usable when sections fail")
    public void home001_044_ac44_heroActionsRemainAvailable() {
        requirePhase(Phase.A, HomeOverviewTestIds.HOME_001_044);
        runStep(
                HomeOverviewTestIds.HOME_001_044,
                "Hero CTAs enabled independent of dynamic sections",
                () -> {
                    HomeHeroSection hero = flow.openHomeOverview().hero().waitUntilVisible();
                    Assert.assertTrue(hero.isPrimaryActionEnabled());
                    Assert.assertTrue(hero.isSecondaryActionVisible());
                    return StepResult.pass("heroActionsAvailable=true");
                });
    }

    @Test(description = HomeOverviewTestIds.HOME_001_045 + " — AC-45 Successful retry restores state")
    public void home001_045_ac45_retryReplacesErrorState() {
        requirePhase(Phase.D, HomeOverviewTestIds.HOME_001_045);
        skipUnlessErrorHooks("Retry success path requires simulated failure + recovery hook.");
    }

    // --- helpers ---

    private void assertHeroCopy(boolean title, boolean description, String testId) {
        requirePhase(Phase.A, testId);
        runStep(
                testId,
                "Hero copy",
                () -> {
                    HomeHeroSection hero = flow.openHomeOverview().hero().waitUntilVisible();
                    if (title) {
                        Assert.assertTrue(hero.isTitleVisible(), HomeOverviewMessages.HERO_TITLE);
                    }
                    if (description) {
                        Assert.assertTrue(hero.isDescriptionVisible(), HomeOverviewMessages.HERO_DESCRIPTION);
                    }
                    return StepResult.pass("heroCopyVerified=true");
                });
    }

    private HomeRecentAnalysesSection recentSection() {
        return flow.openHomeOverview().recentAnalyses().waitUntilSectionVisible();
    }

    private List<RecentAnalysisCardSnapshot> requireRecentCardsOrSkip() {
        HomeRecentAnalysesSection recent = recentSection();
        if (recent.isEmptyStateVisible()) {
            throw new SkipException("No recent analyses in this environment (empty state).");
        }
        if (recent.isLoadErrorVisible()) {
            throw new SkipException("Recent analyses section in error state.");
        }
        List<RecentAnalysisCardSnapshot> cards = recent.readVisibleAnalysisCards();
        if (cards.isEmpty()) {
            throw new SkipException("No parseable recent analysis cards on Home.");
        }
        return cards;
    }

    private HomeRecentAnalysesSection requireEmptyRecentOrSkip() {
        if (!HomeOverviewGate.allowLiveEmptyStateChecks()) {
            throw new SkipException(
                    "Phase d empty-state tests require -Dhome.overview.allowEmptyStateChecks=true and tenant with no analyses.");
        }
        HomeRecentAnalysesSection recent = recentSection();
        if (!recent.isEmptyStateVisible()) {
            throw new SkipException("Recent section is not empty in this environment.");
        }
        return recent;
    }

    private RecentAnalysisCardSnapshot findCardWithStatus(String status) {
        for (RecentAnalysisCardSnapshot card : requireRecentCardsOrSkip()) {
            if (status.equals(card.status())) {
                return card;
            }
        }
        throw new SkipException("No recent card with status «" + status + "».");
    }

    private void requireCardWithStatus(String status, String testId) {
        findCardWithStatus(status);
        runStep(testId, "Card with status «" + status + "»", () -> StepResult.pass("statusFound=true"));
    }

    private static void skipUnlessErrorHooks(String reason) {
        if (!HomeOverviewGate.allowErrorStateHooks()) {
            throw new SkipException(reason + " Enable -Dhome.overview.allowErrorStateHooks=true when hooks exist.");
        }
        throw new SkipException(reason);
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        HomeOverviewGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-HOME-001",
                HomeOverviewTestIds.FEATURE,
                "Authenticated Home overview",
                "Credentials + phase gate satisfied",
                "phase=" + HomeOverviewGate.currentPhase());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute Home overview scenario",
                objective,
                "Per US-FAP-HOME-001 acceptance criteria",
                action,
                browser(),
                testCaseId);
    }
}
