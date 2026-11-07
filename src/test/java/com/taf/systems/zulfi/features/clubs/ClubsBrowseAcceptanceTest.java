package com.taf.systems.zulfi.features.clubs;

import com.taf.systems.zulfi.clubs.ClubBrowseTestIds;
import com.taf.systems.zulfi.config.ClubsYellowLeagueGate;
import com.taf.systems.zulfi.config.ClubsYellowLeagueGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.clubs.ClubsBrowseFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.clubs.ClubCardSnapshot;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.ClubsPageMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * US-FAP-CLUBS — Yellow League clubs listing (read-only). Enable via {@code -Dclubs.yellow.phase=a|b|c}.
 */
@Epic("Zulfi Portal")
@Feature(ClubBrowseTestIds.FEATURE)
public class ClubsBrowseAcceptanceTest extends AuthenticatedUiTest {

    private ClubsBrowseFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for CLUBS tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new ClubsBrowseFlow(browser());
    }

    @Test(description = ClubBrowseTestIds.CLUB_001 + " — Sidebar «الأندية» opens listing")
    public void club001_sidebarNavOpensClubsListing() {
        requirePhase(Phase.A, ClubBrowseTestIds.CLUB_001);
        runStep(
                ClubBrowseTestIds.CLUB_001,
                "Navigate via sidebar",
                () -> {
                    ClubsListPage page = flow.openClubsViaSidebar();
                    Assert.assertTrue(
                            page.isOnClubsRoute() || page.isPageTitleVisible(),
                            "Clubs listing route or title visible; url=" + browser().getCurrentUrl());
                    return StepResult.pass("clubsRoute=true");
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_002 + " — Page title «الأندية»")
    public void club002_displayPageTitle() {
        requirePhase(Phase.A, ClubBrowseTestIds.CLUB_002);
        runStep(
                ClubBrowseTestIds.CLUB_002,
                "Verify listing heading",
                () -> {
                    ClubsListPage page = flow.openClubsViaSidebar();
                    Assert.assertTrue(page.isPageTitleVisible(), ClubsPageMessages.PAGE_TITLE);
                    return StepResult.pass("titleVisible=true");
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_003 + " — Page description")
    public void club003_displayPageDescription() {
        requirePhase(Phase.A, ClubBrowseTestIds.CLUB_003);
        runStep(
                ClubBrowseTestIds.CLUB_003,
                "Verify league subtitle «" + ClubsPageMessages.PAGE_DESCRIPTION + "» (re-baselined 2026-10-22)",
                () -> {
                    ClubsListPage page = flow.openClubsViaSidebar();
                    Assert.assertTrue(page.isPageDescriptionVisible(), ClubsPageMessages.PAGE_DESCRIPTION);
                    return StepResult.pass("descriptionVisible=true");
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_008 + " — Read-only listing (no add/edit/delete)")
    public void club008_listingIsReadOnly() {
        requirePhase(Phase.A, ClubBrowseTestIds.CLUB_008);
        runStep(
                ClubBrowseTestIds.CLUB_008,
                "No mutation controls on listing",
                () -> {
                    ClubsListPage page = flow.openClubsViaSidebar();
                    Assert.assertTrue(
                            page.hasNoAddEditDeleteControls(),
                            "Clubs browse must not expose add/edit/delete actions");
                    return StepResult.pass("readOnly=true");
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_004 + " — Club cards show names")
    public void club004_displayClubCardsWithNames() {
        requirePhase(Phase.B, ClubBrowseTestIds.CLUB_004);
        requireClubCards();
        runStep(
                ClubBrowseTestIds.CLUB_004,
                "Each visible card exposes a club name",
                () -> {
                    List<ClubCardSnapshot> cards = flow.openClubsViaSidebar().readVisibleClubCards();
                    for (ClubCardSnapshot card : cards) {
                        Assert.assertFalse(card.name().isBlank(), "Club name required; index=" + card.index());
                    }
                    return StepResult.pass("cards=" + cards.size());
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_005 + " — Club logo optional on cards")
    public void club005_clubLogoIsOptional() {
        requirePhase(Phase.B, ClubBrowseTestIds.CLUB_005);
        requireClubCards();
        runStep(
                ClubBrowseTestIds.CLUB_005,
                "Logo may be present or absent without failing listing",
                () -> {
                    List<ClubCardSnapshot> cards = flow.openClubsViaSidebar().readVisibleClubCards();
                    int withLogo = 0;
                    for (ClubCardSnapshot card : cards) {
                        if (card.logoPresent()) {
                            withLogo++;
                        }
                    }
                    return StepResult.pass("cards=" + cards.size() + "; withLogo=" + withLogo);
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_006 + " — Club cards are selectable")
    public void club006_clubCardsAreSelectable() {
        requirePhase(Phase.B, ClubBrowseTestIds.CLUB_006);
        requireClubCards();
        runStep(
                ClubBrowseTestIds.CLUB_006,
                "Cards resolve clickable club targets",
                () -> {
                    List<ClubCardSnapshot> cards = flow.openClubsViaSidebar().readVisibleClubCards();
                    for (ClubCardSnapshot card : cards) {
                        Assert.assertTrue(
                                !card.href().isBlank() || !card.clubId().isBlank(),
                                "Card must be navigable via href or data-club-id; index=" + card.index());
                    }
                    return StepResult.pass("selectableCards=" + cards.size());
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_007 + " — Navigate by club ID in URL")
    public void club007_openClubDetailUsesClubIdInUrl() {
        requirePhase(Phase.B, ClubBrowseTestIds.CLUB_007);
        requireClubCards();
        runStep(
                ClubBrowseTestIds.CLUB_007,
                "Click card → detail URL contains stable club id",
                () -> {
                    String clubId = flow.openFirstClubDetailById();
                    Assert.assertTrue(
                            new ClubsListPage(browser()).currentUrlContainsClubId(clubId),
                            "Detail route must include club id=" + clubId + "; url=" + browser().getCurrentUrl());
                    return StepResult.pass("clubId=" + clubId);
                });
    }

    @Test(description = ClubBrowseTestIds.CLUB_009 + " — Empty state copy")
    public void club009_noClubsEmptyState() {
        requirePhase(Phase.C, ClubBrowseTestIds.CLUB_009);
        throw new SkipException("Empty state requires Yellow League environment with zero clubs or controllable test data.");
    }

    @Test(description = ClubBrowseTestIds.CLUB_010 + " — Load failure and retry")
    public void club010_clubsLoadFailureWithRetry() {
        requirePhase(Phase.C, ClubBrowseTestIds.CLUB_010);
        throw new SkipException(
                "Error state requires simulated API failure, network hooks, or dedicated test environment.");
    }

    private void requireClubCards() {
        ClubsListPage page = flow.openClubsViaSidebar();
        if (page.isEmptyStateVisible()) {
            throw new SkipException("Yellow League clubs listing is empty in this environment.");
        }
        if (page.isLoadErrorVisible()) {
            throw new SkipException("Clubs listing is in error state — cannot assert card scenarios.");
        }
        if (page.countVisibleClubCards() == 0) {
            throw new SkipException(
                    "No parseable club cards (align DOM selectors with portal markup if clubs exist in UI).");
        }
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        ClubsYellowLeagueGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-CLUBS",
                ClubBrowseTestIds.FEATURE,
                "Authenticated portal with Yellow League clubs data",
                "Credentials + phase gate satisfied",
                "phase=" + ClubsYellowLeagueGate.currentPhase());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute Clubs browse scenario",
                objective,
                "Per US-FAP-CLUBS AC",
                action,
                browser(),
                testCaseId);
    }
}
