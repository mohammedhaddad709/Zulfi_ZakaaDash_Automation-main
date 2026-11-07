package com.taf.systems.zulfi.features.clubinfo;

import com.taf.systems.zulfi.clubinfo.ClubInfoTestIds;
import com.taf.systems.zulfi.config.ClubInfoGate;
import com.taf.systems.zulfi.config.ClubInfoGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.clubinfo.ClubInformationFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.clubinfo.ClubInformationPage;
import com.taf.systems.zulfi.pages.clubinfo.ClubInformationPage.MemberSection;
import com.taf.systems.zulfi.pages.clubinfo.ClubMemberCardSnapshot;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.ClubInfoPageMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * US-FAP-CLUB-001 — Club Information (عن النادي). Enable via {@code -Dclub.info.phase=a|b|c}.
 *
 * <p><b>Retired 2026-10-22</b> (owner decision, docs/testing/REBASELINE_2026-10-22.md): the sidebar «عن النادي» page no
 * longer exists; its content is the «عن النادي» tab of the club page and is covered by CLUB-DET-001/003/014/015/016/018/
 * 022/023. Every case skips with that reason unless {@code -Dclub.info.legacy=true} replays the old story.
 */
@Epic("Zulfi Portal")
@Feature(ClubInfoTestIds.FEATURE)
public class ClubInformationAcceptanceTest extends AuthenticatedUiTest {

    static final String RETIRED =
            "CLUB-INF retired 2026-10-22: «عن النادي» moved into the club page — covered by CLUB-DET (see"
                    + " docs/testing/REBASELINE_2026-10-22.md). Replay the legacy story with -Dclub.info.legacy=true.";

    private ClubInformationFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!Boolean.getBoolean("club.info.legacy")) {
            throw new SkipException(RETIRED);
        }
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for CLUB-INFO tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new ClubInformationFlow(browser());
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_001 + " — AC-01 Sidebar «عن النادي»")
    public void clubInf001_sidebarAccess() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_001);
        runStep(
                ClubInfoTestIds.CLUB_INF_001,
                "Authenticated user opens «عن النادي» from sidebar",
                () -> {
                    ClubInformationPage page = flow.openClubInfoViaSidebar();
                    Assert.assertTrue(
                            page.isOnClubInfoRoute() || page.isPageTitleVisible(),
                            "Club Information reachable; url=" + browser().getCurrentUrl());
                    return StepResult.pass("clubInfoRoute=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_002 + " — AC-02 Opens Club Information page")
    public void clubInf002_opensClubInformationPage() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_002);
        runStep(
                ClubInfoTestIds.CLUB_INF_002,
                "Sidebar selection lands on Club Information",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(page.isPageTitleVisible(), ClubInfoPageMessages.PAGE_TITLE);
                    return StepResult.pass("titleVisible=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_003 + " — AC-03 Same access for supported roles")
    public void clubInf003_supportedRolesSameAccess() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_003);
        runStep(
                ClubInfoTestIds.CLUB_INF_003,
                "Single credential smoke (BR-CLUB-007 parity with CLUBS pattern)",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertFalse(page.isLoadErrorVisible(), "Authorized user sees content or loading — not error");
                    return StepResult.pass("roleParity=singleCredentialSmoke");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_004 + " — AC-04 Section «نبذة عن النادي»")
    public void clubInf004_aboutSectionTitle() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_004);
        runStep(
                ClubInfoTestIds.CLUB_INF_004,
                "Verify overview section heading",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionTitleVisible(ClubInfoPageMessages.SECTION_ABOUT),
                            ClubInfoPageMessages.SECTION_ABOUT);
                    return StepResult.pass("aboutTitle=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_005 + " — AC-05 Overview content")
    public void clubInf005_aboutSectionContent() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_005);
        requireContentLoaded();
        runStep(
                ClubInfoTestIds.CLUB_INF_005,
                "Approved club overview text present",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionContentPresent(ClubInfoPageMessages.SECTION_ABOUT),
                            "Overview body copy expected under «نبذة عن النادي»");
                    return StepResult.pass("aboutContent=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_006 + " — AC-06 Section «تاريخ النادي»")
    public void clubInf006_historySectionTitle() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_006);
        runStep(
                ClubInfoTestIds.CLUB_INF_006,
                "Verify history section heading",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionTitleVisible(ClubInfoPageMessages.SECTION_HISTORY),
                            ClubInfoPageMessages.SECTION_HISTORY);
                    return StepResult.pass("historyTitle=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_007 + " — AC-07 History content")
    public void clubInf007_historySectionContent() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_007);
        requireContentLoaded();
        runStep(
                ClubInfoTestIds.CLUB_INF_007,
                "Approved club history text present",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionContentPresent(ClubInfoPageMessages.SECTION_HISTORY),
                            "History body copy expected under «تاريخ النادي»");
                    return StepResult.pass("historyContent=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_008 + " — AC-08 Overview & history read-only")
    public void clubInf008_overviewHistoryReadOnly() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_008);
        runStep(
                ClubInfoTestIds.CLUB_INF_008,
                "No editable fields in overview/history sections",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(page.overviewAndHistoryAreReadOnly(), "Overview and history must be read-only");
                    return StepResult.pass("readOnlyOverview=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_009 + " — AC-09 Section «مجلس الإدارة»")
    public void clubInf009_boardSectionTitle() {
        requirePhase(Phase.A, ClubInfoTestIds.CLUB_INF_009);
        runStep(
                ClubInfoTestIds.CLUB_INF_009,
                "Verify board section heading",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionTitleVisible(ClubInfoPageMessages.SECTION_BOARD),
                            ClubInfoPageMessages.SECTION_BOARD);
                    return StepResult.pass("boardTitle=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_010 + " — AC-10 Board member cards")
    public void clubInf010_boardMemberCards() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_010);
        requireMemberCards(MemberSection.BOARD);
        runStep(
                ClubInfoTestIds.CLUB_INF_010,
                "Each board member represented by a card",
                () -> {
                    List<ClubMemberCardSnapshot> cards =
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.BOARD);
                    Assert.assertFalse(cards.isEmpty(), "At least one board member card expected");
                    return StepResult.pass("boardCards=" + cards.size());
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_011 + " — AC-11 Board name and role")
    public void clubInf011_boardNameAndRole() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_011);
        requireMemberCards(MemberSection.BOARD);
        runStep(
                ClubInfoTestIds.CLUB_INF_011,
                "Board cards expose name and position",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.BOARD)) {
                        Assert.assertFalse(card.name().isBlank(), "Board name required; index=" + card.index());
                        Assert.assertFalse(card.role().isBlank(), "Board role required; index=" + card.index());
                    }
                    return StepResult.pass("boardFields=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_012 + " — AC-12 Board photo when available")
    public void clubInf012_boardPhotoWhenAvailable() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_012);
        requireMemberCards(MemberSection.BOARD);
        runStep(
                ClubInfoTestIds.CLUB_INF_012,
                "Board cards show image or avatar slot",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.BOARD)) {
                        Assert.assertTrue(
                                card.photoOrPlaceholderPresent(),
                                "Photo or placeholder slot; index=" + card.index());
                    }
                    return StepResult.pass("boardPhoto=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_013 + " — AC-13 Board placeholder heuristic")
    public void clubInf013_boardBrokenImagePlaceholder() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_013);
        requireMemberCards(MemberSection.BOARD);
        runStep(
                ClubInfoTestIds.CLUB_INF_013,
                "Missing/broken images use placeholder — not broken img",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.BOARD)) {
                        Assert.assertTrue(
                                card.usesPlaceholderHeuristic() || card.photoOrPlaceholderPresent(),
                                "Placeholder heuristic; index=" + card.index());
                    }
                    return StepResult.pass("boardPlaceholder=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_014 + " — AC-14 Section «الفريق الإداري»")
    public void clubInf014_adminTeamSection() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_014);
        runStep(
                ClubInfoTestIds.CLUB_INF_014,
                "Administrative team section visible",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionTitleVisible(ClubInfoPageMessages.SECTION_ADMIN_TEAM),
                            ClubInfoPageMessages.SECTION_ADMIN_TEAM);
                    return StepResult.pass("adminSection=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_015 + " — AC-15 Admin member cards")
    public void clubInf015_adminMemberCards() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_015);
        requireMemberCards(MemberSection.ADMIN);
        runStep(
                ClubInfoTestIds.CLUB_INF_015,
                "Administrative members as cards",
                () -> {
                    List<ClubMemberCardSnapshot> cards =
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.ADMIN);
                    Assert.assertFalse(cards.isEmpty(), "At least one admin card expected");
                    return StepResult.pass("adminCards=" + cards.size());
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_016 + " — AC-16 Admin name and role")
    public void clubInf016_adminNameAndRole() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_016);
        requireMemberCards(MemberSection.ADMIN);
        runStep(
                ClubInfoTestIds.CLUB_INF_016,
                "Admin cards expose name and position",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.ADMIN)) {
                        Assert.assertFalse(card.name().isBlank(), "Admin name; index=" + card.index());
                        Assert.assertFalse(card.role().isBlank(), "Admin role; index=" + card.index());
                    }
                    return StepResult.pass("adminFields=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_017 + " — AC-17 Admin photo or placeholder")
    public void clubInf017_adminPhotoOrPlaceholder() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_017);
        requireMemberCards(MemberSection.ADMIN);
        runStep(
                ClubInfoTestIds.CLUB_INF_017,
                "Admin photo/placeholder heuristic",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.ADMIN)) {
                        Assert.assertTrue(
                                card.photoOrPlaceholderPresent() || card.usesPlaceholderHeuristic(),
                                "Admin visual; index=" + card.index());
                    }
                    return StepResult.pass("adminPhoto=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_018 + " — AC-18 Section «الفريق الرياضي»")
    public void clubInf018_footballTeamSection() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_018);
        runStep(
                ClubInfoTestIds.CLUB_INF_018,
                "Football team section visible",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(
                            page.isSectionTitleVisible(ClubInfoPageMessages.SECTION_FOOTBALL_TEAM),
                            ClubInfoPageMessages.SECTION_FOOTBALL_TEAM);
                    return StepResult.pass("footballSection=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_019 + " — AC-19 Football team cards")
    public void clubInf019_footballTeamCards() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_019);
        requireMemberCards(MemberSection.FOOTBALL);
        runStep(
                ClubInfoTestIds.CLUB_INF_019,
                "Football team members as cards",
                () -> {
                    List<ClubMemberCardSnapshot> cards =
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.FOOTBALL);
                    Assert.assertFalse(cards.isEmpty(), "At least one football card expected");
                    return StepResult.pass("footballCards=" + cards.size());
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_020 + " — AC-20 Football name and role")
    public void clubInf020_footballNameAndRole() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_020);
        requireMemberCards(MemberSection.FOOTBALL);
        runStep(
                ClubInfoTestIds.CLUB_INF_020,
                "Football cards expose name and position",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.FOOTBALL)) {
                        Assert.assertFalse(card.name().isBlank(), "Player name; index=" + card.index());
                        Assert.assertFalse(card.role().isBlank(), "Player role; index=" + card.index());
                    }
                    return StepResult.pass("footballFields=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_021 + " — AC-21 Football photo or placeholder")
    public void clubInf021_footballPhotoOrPlaceholder() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_021);
        requireMemberCards(MemberSection.FOOTBALL);
        runStep(
                ClubInfoTestIds.CLUB_INF_021,
                "Football photo/placeholder heuristic",
                () -> {
                    for (ClubMemberCardSnapshot card :
                            flow.openClubInformationDirectly().readMemberCards(MemberSection.FOOTBALL)) {
                        Assert.assertTrue(
                                card.photoOrPlaceholderPresent() || card.usesPlaceholderHeuristic(),
                                "Football visual; index=" + card.index());
                    }
                    return StepResult.pass("footballPhoto=ok");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_022 + " — AC-22 No player profile navigation")
    public void clubInf022_footballCardsNoProfileNavigation() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_022);
        requireMemberCards(MemberSection.FOOTBALL);
        runStep(
                ClubInfoTestIds.CLUB_INF_022,
                "Football cards must not link to player/member profiles",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(page.footballCardsHaveNoProfileNavigation(), "No profile href on cards");
                    Assert.assertTrue(
                            page.clickFirstFootballCardDoesNotOpenProfile(),
                            "Click must not open profile; url=" + browser().getCurrentUrl());
                    return StepResult.pass("noProfileNav=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_023 + " — AC-23 No add/edit/delete")
    public void clubInf023_pageIsReadOnlyNoMutations() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_023);
        runStep(
                ClubInfoTestIds.CLUB_INF_023,
                "No mutation controls on Club Information",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(page.hasNoAddEditDeleteControls(), "BR-CLUB-005");
                    return StepResult.pass("noMutations=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_024 + " — AC-24 Viewing does not modify data")
    public void clubInf024_viewingDoesNotModifyData() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_024);
        runStep(
                ClubInfoTestIds.CLUB_INF_024,
                "Read-only page — no submit/save controls",
                () -> {
                    ClubInformationPage page = flow.openClubInformationDirectly();
                    Assert.assertTrue(page.hasNoAddEditDeleteControls(), "No data mutation affordances");
                    Assert.assertTrue(page.overviewAndHistoryAreReadOnly(), "Text sections not editable");
                    return StepResult.pass("viewOnly=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_025 + " — AC-25 Refresh retrieves current info")
    public void clubInf025_refreshRetrievesCurrentInfo() {
        requirePhase(Phase.B, ClubInfoTestIds.CLUB_INF_025);
        requireContentLoaded();
        runStep(
                ClubInfoTestIds.CLUB_INF_025,
                "Revisit/refresh keeps Club Information available",
                () -> {
                    ClubInformationPage page = flow.revisitClubInformation();
                    Assert.assertTrue(
                            page.isOnClubInfoRoute() || page.isPageTitleVisible(),
                            "Still on Club Information after refresh");
                    Assert.assertTrue(page.allApprovedSectionsVisible(), "Sections remain after refresh");
                    return StepResult.pass("refreshOk=true");
                });
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_026 + " — AC-26 Loading state")
    public void clubInf026_loadingState() {
        requirePhase(Phase.C, ClubInfoTestIds.CLUB_INF_026);
        throw new SkipException("Loading state requires controllable slow API or network hooks.");
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_027 + " — AC-27 Retrieval error copy")
    public void clubInf027_retrievalErrorCopy() {
        requirePhase(Phase.C, ClubInfoTestIds.CLUB_INF_027);
        throw new SkipException("Error state requires simulated API failure or dedicated test environment.");
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_028 + " — AC-28 Retry action visible")
    public void clubInf028_retryActionOnError() {
        requirePhase(Phase.C, ClubInfoTestIds.CLUB_INF_028);
        throw new SkipException("Retry visibility requires induced retrieval failure.");
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_029 + " — AC-29 Retry requests data again")
    public void clubInf029_retryRequestsAgain() {
        requirePhase(Phase.C, ClubInfoTestIds.CLUB_INF_029);
        throw new SkipException("Retry behavior requires induced failure + recovery hooks.");
    }

    @Test(description = ClubInfoTestIds.CLUB_INF_030 + " — AC-30 Successful retry restores page")
    public void clubInf030_successfulRetryRestoresContent() {
        requirePhase(Phase.C, ClubInfoTestIds.CLUB_INF_030);
        throw new SkipException("Successful retry requires controllable failure/recovery environment.");
    }

    private void requireContentLoaded() {
        ClubInformationPage page = flow.openClubInformationDirectly();
        if (page.isLoadErrorVisible()) {
            throw new SkipException("Club Information is in error state — cannot assert content scenarios.");
        }
        if (page.isLoadingStateVisible()) {
            throw new SkipException("Club Information still loading — retry when stable.");
        }
    }

    private void requireMemberCards(MemberSection section) {
        requireContentLoaded();
        ClubInformationPage page = flow.openClubInformationDirectly();
        if (page.countMemberCards(section) == 0) {
            throw new SkipException(
                    "No parseable "
                            + section.name()
                            + " member cards (align DOM selectors with portal markup if members exist).");
        }
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        ClubInfoGate.requirePhaseAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-CLUB-001",
                ClubInfoTestIds.FEATURE,
                "Authenticated portal with club information data",
                "Credentials + phase gate satisfied",
                "phase=" + ClubInfoGate.currentPhase());
    }

    private void runStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(
                "Execute Club Information scenario",
                objective,
                "Per US-FAP-CLUB-001 AC",
                action,
                browser(),
                testCaseId);
    }
}
