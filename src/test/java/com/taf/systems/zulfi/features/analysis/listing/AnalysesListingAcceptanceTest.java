package com.taf.systems.zulfi.features.analysis.listing;

import com.taf.systems.zulfi.ui.PrimeVueDatePicker;
import com.taf.systems.zulfi.validations.AnalysesListCardRules;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.taf.systems.zulfi.analysis.listing.AnlsLstTestIds;
import com.taf.systems.zulfi.config.AnlsLstGate;
import com.taf.systems.zulfi.config.AnlsLstGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.AnalysesListingFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.AnalysesListCardSnapshot;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisDetailsPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.AnalysesListingMessages;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * US-FAP-ANL-003 — Browse, search, and filter match analyses (read-only).
 * Enable via {@code -Danl.lst.phase=a|b|c|d}.
 */
@Epic("Zulfi Portal")
@Feature(AnlsLstTestIds.FEATURE)
public class AnalysesListingAcceptanceTest extends AuthenticatedUiTest {

    private static final Pattern ANALYSIS_DETAILS_URL = Pattern.compile("(?i)/analysis/\\d+");
    private AnalysesListingFlow flow;

    @BeforeMethod(alwaysRun = true)
    @Override
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for ANL-LST tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new AnalysesListingFlow(browser());
    }

    // --- A. Page access & listing ---

    @Test(description = AnlsLstTestIds.ANL_LST_001 + " — AC-01 Sidebar → التحليلات")
    public void anlLst001_ac01_openAnalysesFromSidebar() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_001);
        runStep(
                AnlsLstTestIds.ANL_LST_001,
                "Navigate via sidebar",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertTrue(list.isOnAnalysesRoute(), "url=" + browser().getCurrentUrl());
                    return StepResult.pass("analysesRoute=true");
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_002
                            + "…"
                            + AnlsLstTestIds.ANL_LST_004
                            + " — AC-02…04 Title, description, primary CTA")
    public void anlLst002_ac02Through04_pageChromeAndPrimaryCta() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_002);
        runStep(
                AnlsLstTestIds.ANL_LST_002,
                "Page title + CTA (description removed — re-baselined 2026-10-22, docs/testing/REBASELINE_2026-10-22.md)",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertTrue(list.isPageTitleVisible(), AnalysesListingMessages.PAGE_TITLE);
                    Assert.assertTrue(list.isPrimaryCtaVisible(), AnalysesListingMessages.PRIMARY_CTA);
                    CreateAnalysisPage create = list.clickPrimaryNewAnalysisCta();
                    Assert.assertTrue(
                            create.isDialogTitleVisible(), CreateAnalysisPage.DIALOG_TITLE_FRAGMENT);
                    flow.closeCreateDialogIfOpen();
                    return StepResult.pass("pageChrome=true; createDialogOpened=true");
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_016 + " — AC-16 Search placeholder")
    public void anlLst016_ac16_searchFieldPlaceholder() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_016);
        runStep(
                AnlsLstTestIds.ANL_LST_016,
                "Search field visible",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertTrue(list.isSearchFieldVisible());
                    Assert.assertTrue(
                            browser().getPageSource().contains(AnalysesListingMessages.SEARCH_PLACEHOLDER));
                    return StepResult.pass("searchField=true");
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_005 + " — AC-05 Newest-first ordering")
    public void anlLst005_ac05_cardsOrderedNewestFirst() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_005);
        List<AnalysesListCardSnapshot> cards = requireListCardsOrSkip();
        runStep(
                AnlsLstTestIds.ANL_LST_005,
                "Compare parseable submission dates on visible cards",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertTrue(
                            list.visibleCardsAppearNewestFirst(),
                            "Expected non-increasing submission dates on visible cards");
                    if (cards.size() >= 2) {
                        Assert.assertFalse(cards.get(0).rawText().isBlank());
                    }
                    return StepResult.pass("cardCount=" + cards.size());
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_006 + " — AC-06 Card field structure")
    public void anlLst006_ac06_cardDisplaysRequiredFields() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_006);
        runStep(
                AnlsLstTestIds.ANL_LST_006,
                "Match name, date, source, status, actions",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    List<AnalysesListCardSnapshot> cards = requireListCardsOrSkip();
                    for (AnalysesListCardSnapshot card : cards) {
                        Assert.assertFalse(card.matchTitle().isBlank(), "Match name");
                        Assert.assertFalse(card.status().isBlank(), "Status");
                        Assert.assertTrue(card.viewDetailsPresent(), AnalysesListingMessages.ACTION_VIEW_DETAILS);
                        Assert.assertTrue(card.dashboardPresent(), AnalysesListingMessages.ACTION_VIEW_DASHBOARD);
                        if (!card.submissionDateToken().isBlank()) {
                            Assert.assertTrue(
                                    MatchDateDisplayRules.looksLikeCalendarDateDisplay(
                                                    card.submissionDateToken())
                                            || card.submissionDateToken().matches("\\d{4}-\\d{2}-\\d{2}"),
                                    "dateToken=" + card.submissionDateToken());
                        }
                    }
                    Assert.assertFalse(list.pageContainsDeleteAction(), "Delete must not appear");
                    return StepResult.pass("cardsChecked=" + cards.size());
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_007 + " — AC-07 Video source labels")
    public void anlLst007_ac07_videoSourceOnCards() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_007);
        runStep(
                AnlsLstTestIds.ANL_LST_007,
                "YouTube or Google Drive on cards when data exists",
                () -> {
                    flow.openFromSidebar();
                    List<AnalysesListCardSnapshot> cards = requireListCardsOrSkip();
                    int withSource = 0;
                    for (AnalysesListCardSnapshot card : cards) {
                        if (!card.videoSource().isBlank()) {
                            withSource++;
                            Assert.assertTrue(
                                    AnalysesListingMessages.VIDEO_SOURCE_YOUTUBE.equals(card.videoSource())
                                            || AnalysesListingMessages.VIDEO_SOURCE_GOOGLE_DRIVE.equals(
                                                    card.videoSource()));
                        }
                    }
                    if (withSource == 0) {
                        throw new SkipException("No video source labels parseable on current cards.");
                    }
                    return StepResult.pass("withSource=" + withSource);
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_008 + " — AC-08 Tenant scope (single credential)")
    public void anlLst008_ac08_authenticatedUserScopeOnly() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_008);
        runStep(
                AnlsLstTestIds.ANL_LST_008,
                "First Release: configured analyst credential models authorized scope",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertFalse(list.isLoadErrorVisible());
                    return StepResult.pass(
                            "Automated scope uses single tenant credential; cross-user negative not automated");
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_009
                            + "…"
                            + AnlsLstTestIds.ANL_LST_014
                            + " — AC-09…14 Processing/completed card actions")
    public void anlLst009_ac09Through14_statusDrivesDashboardAvailability() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_009);
        runStep(
                AnlsLstTestIds.ANL_LST_009,
                "Dashboard disabled for قيد; enabled for اكتمل",
                () -> {
                    flow.openFromSidebar();
                    AnalysesListCardSnapshot processing = findCardWithStatus(AnalysesListingMessages.STATUS_IN_PROGRESS);
                    AnalysesListCardSnapshot completed = findCardWithStatus(AnalysesListingMessages.STATUS_COMPLETED);
                    Assert.assertEquals(processing.status(), AnalysesListingMessages.STATUS_IN_PROGRESS);
                    Assert.assertTrue(processing.viewDetailsEnabled());
                    Assert.assertFalse(processing.dashboardEnabled());
                    Assert.assertEquals(completed.status(), AnalysesListingMessages.STATUS_COMPLETED);
                    Assert.assertTrue(completed.viewDetailsEnabled());
                    Assert.assertTrue(completed.dashboardEnabled());
                    return StepResult.pass("processingAndCompletedCardsVerified=true");
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_014 + " — AC-14 View details targets one record")
    public void anlLst014_ac14_viewDetailsNavigationSmoke() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_014);
        runStep(
                AnlsLstTestIds.ANL_LST_014,
                "Click first enabled عرض التفاصيل",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    requireListCardsOrSkip();
                    list.clickFirstVisibleViewDetails();
                    AnalysisDetailsPage details = new AnalysisDetailsPage(browser()).waitUntilLoaded();
                    Assert.assertTrue(
                            ANALYSIS_DETAILS_URL.matcher(browser().getCurrentUrl()).find(),
                            "url=" + browser().getCurrentUrl());
                    Assert.assertFalse(details.readLifecycleStatusLabel().isBlank());
                    return StepResult.pass("detailsUrl=" + browser().getCurrentUrl());
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_015 + " — AC-15 No delete on cards")
    public void anlLst015_ac15_noDeleteActionOnCards() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_015);
        runStep(
                AnlsLstTestIds.ANL_LST_015,
                "Page must not expose حذف on listing cards",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertFalse(list.pageContainsDeleteAction());
                    return StepResult.pass("deleteAbsent=true");
                });
    }

    // --- B. Search + status/source filters + reset ---

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_017
                            + "…"
                            + AnlsLstTestIds.ANL_LST_021
                            + " — AC-17…21 Search behavior")
    public void anlLst017_ac17Through21_searchMatchNameRules() {
        requirePhase(Phase.B, AnlsLstTestIds.ANL_LST_017);
        AnalysesListCardSnapshot sample = requireListCardsOrSkip().get(0);
        String fullTitle = sample.matchTitle();
        if (fullTitle.isBlank()) {
            throw new SkipException("No match title on first card.");
        }
        String partial =
                fullTitle.length() > 4 ? fullTitle.substring(0, Math.min(6, fullTitle.length())) : fullTitle;
        runStep(
                AnlsLstTestIds.ANL_LST_017,
                "Full + partial + trim + clear search",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    int baseline = list.countVisibleAnalysisCards();
                    list.searchByMatchName(fullTitle);
                    Assert.assertTrue(list.countVisibleCardsMatchingTitleFragment(fullTitle) >= 1);
                    list.clearSearch();
                    list.searchByMatchName("  " + partial + "  ");
                    Assert.assertTrue(
                            list.countVisibleCardsMatchingTitleFragment(partial) >= 1
                                    || list.isNoResultsStateVisible());
                    if (fullTitle.chars().anyMatch(c -> c >= 'A' && c <= 'z')) {
                        list.clearSearch();
                        list.searchByMatchName(fullTitle.toUpperCase(Locale.ROOT));
                        Assert.assertTrue(
                                list.countVisibleCardsMatchingTitleFragment(partial) >= 1
                                        || list.isNoResultsStateVisible());
                    }
                    list.clearSearch();
                    int afterClear = list.countVisibleAnalysisCards();
                    Assert.assertTrue(afterClear >= 1 || list.isGlobalEmptyStateVisible());
                    Assert.assertTrue(afterClear >= baseline || list.isNoResultsStateVisible());
                    return StepResult.pass("baseline=" + baseline + "; afterClear=" + afterClear);
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_022 + " — AC-22 Search read-only")
    public void anlLst022_ac22_searchDoesNotMutateRecords() {
        requirePhase(Phase.B, AnlsLstTestIds.ANL_LST_022);
        runStep(
                AnlsLstTestIds.ANL_LST_022,
                "Card count stable after search/clear",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    int before = list.countVisibleAnalysisCards();
                    if (before == 0) {
                        throw new SkipException("No cards to verify search integrity.");
                    }
                    String title = list.readVisibleAnalysisCards().get(0).matchTitle();
                    list.searchByMatchName(title);
                    list.clearSearch();
                    int after = list.countVisibleAnalysisCards();
                    Assert.assertEquals(after, before, "Search must not remove records from tenant");
                    return StepResult.pass("countStable=" + before);
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_029
                            + "…"
                            + AnlsLstTestIds.ANL_LST_033
                            + " — AC-29…33 Status filter")
    public void anlLst029_ac29Through33_statusFilter() {
        requirePhase(Phase.B, AnlsLstTestIds.ANL_LST_029);
        runStep(
                AnlsLstTestIds.ANL_LST_029,
                "الكل default; قيد; اكتمل",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    requireListCardsOrSkip();
                    // Re-baselined 2026-10-22: «الكل» = the cleared select showing its placeholder.
                    Assert.assertTrue(
                            list.filters().isCleared(AnalysesListingMessages.FILTER_STATUS_LABEL),
                            "default=" + list.filters().readStatusFilterSelection());
                    list.filters().selectStatus(AnalysesListingMessages.STATUS_IN_PROGRESS);
                    for (AnalysesListCardSnapshot card : list.readVisibleAnalysisCards()) {
                        Assert.assertEquals(card.status(), AnalysesListingMessages.STATUS_IN_PROGRESS);
                    }
                    list.filters().selectStatus(AnalysesListingMessages.STATUS_COMPLETED);
                    for (AnalysesListCardSnapshot card : list.readVisibleAnalysisCards()) {
                        Assert.assertEquals(card.status(), AnalysesListingMessages.STATUS_COMPLETED);
                    }
                    list.filters().selectStatusAll();
                    Assert.assertTrue(list.filters().isCleared(AnalysesListingMessages.FILTER_STATUS_LABEL), "status cleared");
                    waitForCards();
                    Assert.assertTrue(list.countVisibleAnalysisCards() >= 1, "all analyses listed again after clearing");
                    return StepResult.pass("statusFilter=true");
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_034
                            + "…"
                            + AnlsLstTestIds.ANL_LST_038
                            + " — AC-34…38 Video source filter")
    public void anlLst034_ac34Through38_videoSourceFilter() {
        requirePhase(Phase.B, AnlsLstTestIds.ANL_LST_034);
        runStep(
                AnlsLstTestIds.ANL_LST_034,
                "YouTube / Google Drive / الكل",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    requireListCardsOrSkip();
                    list.filters().selectVideoSource(AnalysesListingMessages.VIDEO_SOURCE_YOUTUBE);
                    assertAllCardsSourceIfAny(list, AnalysesListingMessages.VIDEO_SOURCE_YOUTUBE);
                    list.filters().selectVideoSource(AnalysesListingMessages.VIDEO_SOURCE_GOOGLE_DRIVE);
                    assertAllCardsSourceIfAny(list, AnalysesListingMessages.VIDEO_SOURCE_GOOGLE_DRIVE);
                    list.filters().selectVideoSourceAll();
                    waitForCards();
                    Assert.assertTrue(list.countVisibleAnalysisCards() >= 1, "all analyses listed again after clearing");
                    return StepResult.pass("videoSourceFilter=true");
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_043
                            + "…"
                            + AnlsLstTestIds.ANL_LST_048
                            + " — AC-43…48 Reset filters keeps search")
    public void anlLst043_ac43Through48_resetFiltersPreservesSearch() {
        requirePhase(Phase.B, AnlsLstTestIds.ANL_LST_043);
        runStep(
                AnlsLstTestIds.ANL_LST_043,
                "Apply filters + search then reset",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    AnalysesListCardSnapshot card = requireListCardsOrSkip().get(0);
                    String searchNeedle =
                            card.matchTitle().length() > 3
                                    ? card.matchTitle().substring(0, 3)
                                    : card.matchTitle();
                    list.searchByMatchName(searchNeedle);
                    list.filters().selectStatus(AnalysesListingMessages.STATUS_COMPLETED);
                    if (list.filters().isResetFiltersVisible()) {
                        list.filters().resetFilters();
                    } else {
                        list.filters().selectStatusAll().selectVideoSourceAll().clearDateRange();
                    }
                    Assert.assertEquals(list.readSearchValue(), searchNeedle.trim());
                    Assert.assertTrue(
                            list.filters().isCleared(AnalysesListingMessages.FILTER_STATUS_LABEL),
                            "status=" + list.filters().readStatusFilterSelection());
                    return StepResult.pass("searchPreserved=" + searchNeedle);
                });
    }

    // --- C. Date filter ---

    @Test(description = AnlsLstTestIds.ANL_LST_023 + " — AC-23 Date filter controls visible")
    public void anlLst023_ac23_dateFilterControlsPresent() {
        requirePhase(Phase.C, AnlsLstTestIds.ANL_LST_023);
        runStep(
                AnlsLstTestIds.ANL_LST_023,
                "«التاريخ» single-date filter (re-baselined 2026-10-22: replaces من تاريخ / إلى تاريخ)",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    Assert.assertTrue(list.filters().isDateFilterVisible(), "date filter visible");
                    Assert.assertEquals(list.filters().dateFilterPlaceholder(), AnalysesListingMessages.FILTER_DATE_LABEL);
                    LocalDate day = latestCardDate(list);
                    Assert.assertTrue(list.filters().pickDate(day), "day " + day + " selectable");
                    Assert.assertEquals(list.filters().readDateValue(), PrimeVueDatePicker.DISPLAY.format(day));
                    list.filters().clearDate();
                    Assert.assertTrue(list.filters().readDateValue().isBlank(), "date cleared");
                    return StepResult.pass("dateFilter=«" + AnalysesListingMessages.FILTER_DATE_LABEL + "»; picked " + day);
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_024
                            + "…"
                            + AnlsLstTestIds.ANL_LST_026
                            + " — AC-24…26 Date range filtering smoke")
    public void anlLst024_ac24Through26_dateRangeSmoketest() {
        requirePhase(Phase.C, AnlsLstTestIds.ANL_LST_024);
        requireListCardsOrSkip();
        runStep(
                AnlsLstTestIds.ANL_LST_024,
                "Pick the newest card's date: only analyses submitted that day remain (re-baselined 2026-10-22)",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    LocalDate day = latestCardDate(list);
                    Assert.assertTrue(list.filters().pickDate(day), "day " + day + " selectable");
                    List<AnalysesListCardSnapshot> filtered = list.readVisibleAnalysisCards();
                    Assert.assertFalse(filtered.isEmpty(), "analyses submitted on " + day + " must be listed");
                    for (AnalysesListCardSnapshot c : filtered) {
                        Assert.assertEquals(
                                AnalysesListCardRules.parseSubmissionDate(c.rawText()).orElse(null),
                                day, "card «" + c.matchTitle() + "» outside the selected day");
                    }
                    list.filters().clearDate();
                    return StepResult.pass(filtered.size() + " cards, all submitted on " + day);
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_027
                            + "…"
                            + AnlsLstTestIds.ANL_LST_028
                            + " — AC-27…28 Invalid date range message")
    public void anlLst027_ac27Through28_invalidDateRangeValidation() {
        requirePhase(Phase.C, AnlsLstTestIds.ANL_LST_027);
        runStep(
                AnlsLstTestIds.ANL_LST_027,
                "A day without analyses lists nothing and shows an empty state (re-baselined 2026-10-22: single-date"
                        + " filter, an inverted range can no longer be entered)",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    LocalDate emptyDay = latestCardDate(list).minusYears(1);
                    Assert.assertTrue(list.filters().pickDate(emptyDay), "day " + emptyDay + " selectable");
                    // Count rendered cards directly: the card reader waits for the story's no-results copy.
                    new WebDriverWait(browser(), Duration.ofSeconds(15))
                            .until(d -> d.findElement(By.tagName("body")).getText().contains("لا توجد")
                                    || visibleCardCount() == 0);
                    int cards = visibleCardCount();
                    String body = browser().findElement(By.tagName("body")).getText();
                    Assert.assertEquals(cards, 0, "no analysis was submitted on " + emptyDay);
                    // A filtered-empty list must show «لا توجد نتائج مطابقة», not the first-use «لا توجد تحليلات حتى الآن» (DEF-ZUL-013).
                    Assert.assertTrue(list.isNoResultsStateVisible(),
                            "expected «" + AnalysesListingMessages.NO_RESULTS_TITLE + "»; page shows «"
                                    + emptyStateExcerpt(body) + "»");
                    list.filters().clearDate();
                    return StepResult.pass("0 cards on " + emptyDay + " with empty state");
                });
    }

    // --- D. Combined criteria, empty/no-results, loading/error ---

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_039
                            + "…"
                            + AnlsLstTestIds.ANL_LST_042
                            + " — AC-39…42 Combined AND filtering")
    public void anlLst039_ac39Through42_combinedSearchAndFilters() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_039);
        AnalysesListCardSnapshot seed = requireListCardsOrSkip().get(0);
        runStep(
                AnlsLstTestIds.ANL_LST_039,
                "Search + status + source AND logic",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    String needle =
                            seed.matchTitle().length() > 4
                                    ? seed.matchTitle().substring(0, 4)
                                    : seed.matchTitle();
                    list.searchByMatchName(needle);
                    list.filters().selectStatus(seed.status());
                    if (!seed.videoSource().isBlank()) {
                        list.filters().selectVideoSource(seed.videoSource());
                    }
                    for (AnalysesListCardSnapshot card : list.readVisibleAnalysisCards()) {
                        Assert.assertTrue(
                                card.rawText().contains(needle) || card.matchTitle().contains(needle));
                        Assert.assertEquals(card.status(), seed.status());
                        if (!seed.videoSource().isBlank()) {
                            Assert.assertEquals(card.videoSource(), seed.videoSource());
                        }
                    }
                    return StepResult.pass("combinedCriteria=true");
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_049 + " — AC-49 Global empty state")
    public void anlLst049_ac49_globalEmptyStateCopy() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_049);
        if (!AnlsLstGate.allowLiveEmptyStateChecks()) {
            throw new SkipException(
                    "Global empty state requires -Danl.lst.allowEmptyStateChecks=true and tenant with zero analyses.");
        }
        runStep(
                AnlsLstTestIds.ANL_LST_049,
                "Empty title + CTA",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    if (!list.isGlobalEmptyStateVisible()) {
                        throw new SkipException("Tenant has analyses; not empty.");
                    }
                    Assert.assertTrue(list.isPrimaryCtaVisible(), AnalysesListingMessages.PRIMARY_CTA);
                    return StepResult.pass("emptyState=true");
                });
    }

    @Test(
            description =
                    AnlsLstTestIds.ANL_LST_051
                            + "…"
                            + AnlsLstTestIds.ANL_LST_053
                            + " — AC-51…53 No-results vs global empty")
    public void anlLst051_ac51Through53_noResultsDistinctFromEmpty() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_051);
        requireListCardsOrSkip();
        String noMatch =
                AnlsLstGate.configuredNoMatchSearchTerm().orElse("ZZZ-NOMATCH-ANL-LST-003");
        runStep(
                AnlsLstTestIds.ANL_LST_051,
                "Impossible search yields no-results copy",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    list.searchByMatchName(noMatch);
                    Assert.assertTrue(list.isNoResultsStateVisible(), AnalysesListingMessages.NO_RESULTS_TITLE);
                    Assert.assertFalse(list.isGlobalEmptyStateVisible(), "Must differ from global empty");
                    return StepResult.pass("noMatch=" + noMatch);
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_054 + " — AC-54 Loading state")
    public void anlLst054_ac54_loadingState() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_054);
        skipUnlessErrorHooks("Loading state requires controllable slow API or dedicated hook.");
    }

    @Test(description = AnlsLstTestIds.ANL_LST_055 + " — AC-55 No empty before load completes")
    public void anlLst055_ac55_emptyNotBeforeLoadCompletes() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_055);
        skipUnlessErrorHooks("Loading vs empty requires timing-controlled environment.");
    }

    @Test(description = AnlsLstTestIds.ANL_LST_056 + " — AC-56 Retrieval error copy")
    public void anlLst056_ac56_listRetrievalErrorCopy() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_056);
        skipUnlessErrorHooks("List error copy requires simulated API failure.");
    }

    @Test(description = AnlsLstTestIds.ANL_LST_057 + " — AC-57 Retry action")
    public void anlLst057_ac57_errorRetryAction() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_057);
        skipUnlessErrorHooks("Retry action requires simulated API failure.");
    }

    @Test(description = AnlsLstTestIds.ANL_LST_058 + " — AC-58 Refresh retrieves latest status")
    public void anlLst058_ac58_refreshShowsLatestStatus() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_058);
        runStep(
                AnlsLstTestIds.ANL_LST_058,
                "Refresh list route",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    int before = list.countVisibleAnalysisCards();
                    list.refresh();
                    int after = list.countVisibleAnalysisCards();
                    Assert.assertTrue(after >= 0);
                    if (before > 0) {
                        Assert.assertTrue(after >= 1 || list.isNoResultsStateVisible());
                    }
                    return StepResult.pass("before=" + before + "; after=" + after);
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_059 + " — AC-59 Read-only integrity")
    public void anlLst059_ac59_browseOperationsDoNotDuplicateRecords() {
        requirePhase(Phase.D, AnlsLstTestIds.ANL_LST_059);
        runStep(
                AnlsLstTestIds.ANL_LST_059,
                "Search/filter/reset/refresh preserve card count",
                () -> {
                    AnalysesListPage list = flow.openFromSidebar();
                    int baseline = list.countVisibleAnalysisCards();
                    if (baseline == 0) {
                        throw new SkipException("No analyses to verify integrity.");
                    }
                    list.filters().selectStatusAll();
                    list.searchByMatchName(" ");
                    list.clearSearch();
                    list.filters().resetFilters();
                    list.refresh();
                    int after = list.countVisibleAnalysisCards();
                    Assert.assertEquals(after, baseline);
                    return StepResult.pass("count=" + baseline);
                });
    }

    @Test(description = AnlsLstTestIds.ANL_LST_001 + " — HOME-001 cross: View More → list")
    public void anlLstHomeCross_openViaHomeRecentViewMore() {
        requirePhase(Phase.A, AnlsLstTestIds.ANL_LST_001);
        runStep(
                AnlsLstTestIds.ANL_LST_001,
                "Home recent → عرض المزيد",
                () -> {
                    try {
                        AnalysesListPage list = flow.openViaHomeRecentViewMore();
                        Assert.assertTrue(list.isPageTitleVisible());
                        return StepResult.pass("homeCrossNav=true");
                    } catch (IllegalStateException ex) {
                        throw new SkipException(ex.getMessage());
                    }
                });
    }

    // --- helpers ---

    /** After clearing a filter the list reloads; give it up to 15 s to render cards again. */
    private void waitForCards() {
        try {
            new WebDriverWait(browser(), Duration.ofSeconds(15))
                    .until(d -> visibleCardCount() > 0);
        } catch (TimeoutException noCards) {
            // the following assertion reports it
        }
    }

    private static String emptyStateExcerpt(String body) {
        int i = body.indexOf("لا توجد");
        return i < 0 ? "(no empty state)" : body.substring(i, Math.min(body.length(), i + 90)).replace('\n', ' ');
    }

    private int visibleCardCount() {
        Object n = ((JavascriptExecutor) browser()).executeScript(
                "return [...document.querySelectorAll('article h3')].filter(e => e.getClientRects().length > 0).length;");
        return n == null ? 0 : ((Number) n).intValue();
    }

    /** Submission day of the newest visible card (cards render «٢٠ أكتوبر ٢٠٢٦ في …»). */
    private static LocalDate latestCardDate(AnalysesListPage list) {
        return list.readVisibleAnalysisCards().stream()
                .map(c -> AnalysesListCardRules.parseSubmissionDate(c.rawText()))
                .flatMap(Optional::stream)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new SkipException("No parseable submission date on visible cards."));
    }

    private static void assertAllCardsSourceIfAny(AnalysesListPage list, String expectedSource) {
        List<AnalysesListCardSnapshot> cards = list.readVisibleAnalysisCards();
        if (cards.isEmpty()) {
            Assert.assertTrue(list.isNoResultsStateVisible(),
                    "«" + expectedSource + "» matches nothing: expected «" + AnalysesListingMessages.NO_RESULTS_TITLE
                            + "»; page shows «" + emptyStateExcerpt(list.bodyText()) + "»");
            return;
        }
        for (AnalysesListCardSnapshot card : cards) {
            if (!card.videoSource().isBlank()) {
                Assert.assertEquals(card.videoSource(), expectedSource);
            }
        }
    }

    private List<AnalysesListCardSnapshot> requireListCardsOrSkip() {
        AnalysesListPage list = flow.openFromSidebar();
        if (list.isGlobalEmptyStateVisible()) {
            throw new SkipException("Analyses list empty in this environment.");
        }
        if (list.isLoadErrorVisible()) {
            throw new SkipException("Analyses list in error state.");
        }
        List<AnalysesListCardSnapshot> cards = list.readVisibleAnalysisCards();
        if (cards.isEmpty()) {
            throw new SkipException("No parseable analysis cards on listing page.");
        }
        return cards;
    }

    private AnalysesListCardSnapshot findCardWithStatus(String status) {
        for (AnalysesListCardSnapshot card : requireListCardsOrSkip()) {
            if (status.equals(card.status())) {
                return card;
            }
        }
        throw new SkipException("No card with status «" + status + "».");
    }

    private static void skipUnlessErrorHooks(String reason) {
        if (!AnlsLstGate.allowErrorStateHooks()) {
            throw new SkipException(reason + " Enable -Danl.lst.allowErrorStateHooks=true when hooks exist.");
        }
        throw new SkipException(reason);
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        AnlsLstGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-ANL-003",
                AnlsLstTestIds.FEATURE,
                "Authenticated analyses listing",
                "Credentials + phase gate satisfied",
                "phase=" + AnlsLstGate.currentPhase());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute analyses listing scenario",
                objective,
                "Per US-FAP-ANL-003 acceptance criteria",
                action,
                browser(),
                testCaseId);
    }
}
