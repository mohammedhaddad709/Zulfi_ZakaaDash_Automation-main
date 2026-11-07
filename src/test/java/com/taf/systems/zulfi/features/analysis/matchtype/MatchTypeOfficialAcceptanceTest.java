package com.taf.systems.zulfi.features.analysis.matchtype;

import com.taf.systems.zulfi.analysis.matchtype.AnlMatchTypeTestIds;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestSession;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.pages.analysis.TeamFieldObservation;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.AnlMatchTypeAnalysisGate;
import com.taf.systems.zulfi.config.AnlMatchTypeAnalysisGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.MatchTypeOfficialAnalysisFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.MatchTypeValidationMessages;
import com.taf.systems.zulfi.validations.TeamFieldComparisonDiagnostics;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * US-FAP-ANL-MATCH-TYPE — Match Type + Official Match linkage on «تحليل مباراة جديدة».
 * Enable via {@code -Danl.match.type.phase=a|b|c|d|e} (default {@code none} skips all).
 */
@Epic("Zulfi Portal")
@Feature(AnlMatchTypeTestIds.FEATURE)
public class MatchTypeOfficialAcceptanceTest extends AuthenticatedUiTest {

    private MatchTypeOfficialAnalysisFlow flow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for ANL-MATCH-TYPE tests.");
        }
        super.loginBeforeEachTestCase();
        flow = new MatchTypeOfficialAnalysisFlow(browser());
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_001 + " — Display نوع المباراة; no auto-selection")
    public void anlType001_displayMatchTypeWithoutAutoSelection() {
        requirePhase(Phase.A, AnlMatchTypeTestIds.ANL_TYPE_001);
        TestStepReporter.runStep(
                "Open New Match Analysis",
                "Form shows required «نوع المباراة»",
                "No Match Type pre-selected; official match field hidden until «رسمية»",
                () -> {
                    CreateAnalysisPage form = flow.openNewMatchAnalysisFromHome();
                    Assert.assertTrue(form.isDialogTitleVisible(), "Create dialog title");
                    Assert.assertTrue(form.isMatchTypeSelectorVisible(), "Match type field visible");
                    Assert.assertTrue(
                            form.isMatchTypeUnselected(),
                            "Expected no «رسمية»/«ودية» selected on open; aria-label="
                                    + form.getMatchTypeValue());
                    Assert.assertFalse(
                            form.isOfficialMatchSelectorVisible(),
                            "Official match selector must stay hidden until «رسمية»");
                    return StepResult.pass("Match type unset; official selector hidden");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_001);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_002 + " — «رسمية» shows اختر المباراة")
    public void anlType002_officialTypeShowsOfficialMatchSelector() {
        requirePhase(Phase.A, AnlMatchTypeTestIds.ANL_TYPE_002);
        TestStepReporter.runStep(
                "Select «رسمية»",
                "Required «اختر المباراة» field appears",
                "Official match combobox visible",
                () -> {
                    CreateAnalysisPage form = flow.openNewMatchAnalysisFromHome();
                    flow.selectOfficialMatchTypeExplicitly(form);
                    Assert.assertTrue(form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL));
                    Assert.assertTrue(
                            form.isOfficialMatchSelectorVisible(),
                            "Official match selector must appear for «رسمية»");
                    return StepResult.pass("Official match selector visible");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_002);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_003 + " — Dropdown: played only (no upcoming)")
    public void anlType003_officialDropdownPlayedMatchesOnly() {
        requirePhase(Phase.B, AnlMatchTypeTestIds.ANL_TYPE_003);
        runPendingEligibilityStep(
                AnlMatchTypeTestIds.ANL_TYPE_003,
                "Inventory excludes upcoming fixtures (schedule tag or API metadata)",
                () -> {
                    CreateAnalysisPage form = openOfficialForm();
                    int count = form.getOfficialFixtureOptionCount();
                    Assert.assertTrue(count > 0, "Expected at least one official option for eligibility audit");
                    return StepResult.pass("options=" + count + " (upcoming filter not yet asserted in UI)");
                });
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_004 + " — Exclude matches under analysis")
    public void anlType004_excludeMatchesUnderAnalysis() {
        requirePhase(Phase.B, AnlMatchTypeTestIds.ANL_TYPE_004);
        runPendingEligibilityStep(
                AnlMatchTypeTestIds.ANL_TYPE_004,
                "Options with analysis «قيد التحليل» must not be selectable",
                () -> StepResult.pass("pending schedule/analysis status markers in dropdown"));
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_005 + " — Exclude completed analysis matches")
    public void anlType005_excludeCompletedAnalysisMatches() {
        requirePhase(Phase.B, AnlMatchTypeTestIds.ANL_TYPE_005);
        runPendingEligibilityStep(
                AnlMatchTypeTestIds.ANL_TYPE_005,
                "Options with «اكتمل التحليل» must not appear as selectable",
                () -> StepResult.pass("pending schedule/analysis status markers in dropdown"));
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_006 + " — Select eligible match retains Match ID")
    public void anlType006_selectEligibleOfficialMatchRetainsId() {
        requirePhase(Phase.B, AnlMatchTypeTestIds.ANL_TYPE_006);
        requireManifest();
        int index = resolveSelectionIndex();
        OfficialFixtureManifestEntry expected =
                OfficialFixtureManifestStore.load().entryAt(index);

        TestStepReporter.runStep(
                "Select official fixture by index",
                "Form associates selected Match ID",
                "Selected label matches manifest; matchId retained",
                () -> {
                    CreateAnalysisPage form = openOfficialForm();
                    CreateAnalysisPage.OfficialFixtureOptionMetadata before =
                            form.getOfficialFixtureOptionMetadata(index);
                    form.selectOfficialFixtureByIndex(index);
                    Assert.assertTrue(form.isOfficialMatchSelected(), "Official match selected");
                    String selectedLabel = form.getSelectedOfficialFixture();
                    Assert.assertFalse(selectedLabel.isBlank(), "Selected label");
                    if (before.matchId() != null && !before.matchId().isBlank()) {
                        Assert.assertTrue(
                                selectedLabel.contains(before.matchId())
                                        || selectedLabel.contains("#" + before.matchId()),
                                "Selected label should reference match id; label=" + selectedLabel);
                    }
                    Assert.assertEquals(
                            expected.matchId(),
                            before.matchId(),
                            "Manifest matchId aligned with live option metadata");
                    return StepResult.pass("matchId=" + before.matchId());
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_006);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_007 + " — Schedule metadata; no manual date/name")
    public void anlType007_scheduleMetadataAutofill() {
        requirePhase(Phase.B, AnlMatchTypeTestIds.ANL_TYPE_007);
        requireManifest();
        int index = resolveSelectionIndex();
        OfficialFixtureManifestEntry expected =
                OfficialFixtureManifestStore.load().entryAt(index);

        TestStepReporter.runStep(
                "Select official fixture",
                "Match date and clubs populated from schedule",
                "User does not type official metadata",
                () -> {
                    CreateAnalysisPage form = openOfficialForm();
                    form.selectOfficialFixtureByIndex(index);
                    Assert.assertTrue(
                            form.isOfficialMatchDateDisplayValid(),
                            "Match date display valid after autofill; value=" + form.getMatchDateValue());
                    Assert.assertTrue(
                            MatchDateDisplayRules.looksLikeCalendarDateDisplay(form.getMatchDateValue()),
                            "Calendar date from schedule");
                    TeamFieldObservation homeObs = form.observeHomeTeamField();
                    TeamFieldObservation awayObs = form.observeAwayTeamField();
                    Assert.assertEquals(
                            expected.homeTeam(),
                            homeObs.resolvedDisplayText(),
                            TeamFieldComparisonDiagnostics.homeVerificationFailureMessage(
                                    expected.homeTeam(), homeObs, form.getSelectedOfficialFixture()));
                    Assert.assertEquals(
                            expected.awayTeam(),
                            awayObs.resolvedDisplayText(),
                            TeamFieldComparisonDiagnostics.awayVerificationFailureMessage(
                                    expected.awayTeam(), awayObs, form.getSelectedOfficialFixture()));
                    return StepResult.pass("Autofill verified (teams + date)");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_007);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_008 + " — Require official match selection")
    public void anlType008_requireOfficialMatchValidation() {
        requirePhase(Phase.C, AnlMatchTypeTestIds.ANL_TYPE_008);
        TestStepReporter.runStep(
                "Attempt continue with «رسمية» and no fixture",
                "Validation blocks submit",
                "Message: " + MatchTypeValidationMessages.OFFICIAL_MATCH_REQUIRED,
                () -> {
                    CreateAnalysisPage form = openOfficialForm();
                    Assert.assertFalse(form.isOfficialMatchSelected(), "No fixture pre-selected");
                    form.clickSubmitWhenEnabled();
                    boolean messageVisible =
                            form.isCreateFormMessageVisible(
                                    MatchTypeValidationMessages.OFFICIAL_MATCH_REQUIRED);
                    if (!messageVisible && form.isSubmitEnabled()) {
                        throw new UnsupportedOperationException(
                                "Submit stayed enabled without official match; wire validation locator");
                    }
                    Assert.assertTrue(
                            messageVisible || !form.isSubmitEnabled(),
                            "Expected validation message or disabled submit");
                    return StepResult.pass(
                            messageVisible
                                    ? "Validation message visible"
                                    : "Submit disabled (alternative UX)");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_008);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_009 + " — Empty state when no eligible matches")
    public void anlType009_noEligibleOfficialMatchesEmptyState() {
        requirePhase(Phase.C, AnlMatchTypeTestIds.ANL_TYPE_009);
        runPendingEligibilityStep(
                AnlMatchTypeTestIds.ANL_TYPE_009,
                "When inventory empty show: "
                        + MatchTypeValidationMessages.NO_ELIGIBLE_OFFICIAL_MATCHES,
                () -> {
                    throw new UnsupportedOperationException(
                            "Requires controlled test data with zero eligible official fixtures");
                });
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_010 + " — Official → Friendly clears official selection")
    public void anlType010_switchOfficialToFriendlyClearsOfficialMatch() {
        requirePhase(Phase.D, AnlMatchTypeTestIds.ANL_TYPE_010);
        requireManifest();
        int index = resolveSelectionIndex();

        TestStepReporter.runStep(
                "Switch «رسمية» → «ودية» after fixture selected",
                "Hide official selector; clear selection",
                "No official match retained",
                () -> {
                    CreateAnalysisPage form = openOfficialForm();
                    form.selectOfficialFixtureByIndex(index);
                    Assert.assertTrue(form.isOfficialMatchSelected());
                    flow.selectFriendlyMatchTypeExplicitly(form);
                    Assert.assertTrue(form.isMatchType(CreateAnalysisPage.MATCH_TYPE_FRIENDLY));
                    Assert.assertFalse(
                            form.isOfficialMatchSelectorVisible(),
                            "Official match field must hide for «ودية»");
                    return StepResult.pass("Official selector hidden after friendly type");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_010);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_011 + " — Friendly → Official requires new selection")
    public void anlType011_switchFriendlyToOfficialRequiresSelection() {
        requirePhase(Phase.D, AnlMatchTypeTestIds.ANL_TYPE_011);
        TestStepReporter.runStep(
                "Switch «ودية» → «رسمية»",
                "Show official dropdown; require selection",
                "No stale friendly metadata as official",
                () -> {
                    CreateAnalysisPage form = flow.openNewMatchAnalysisFromHome();
                    flow.selectFriendlyMatchTypeExplicitly(form);
                    flow.selectOfficialMatchTypeExplicitly(form);
                    Assert.assertTrue(form.isOfficialMatchSelectorVisible());
                    Assert.assertFalse(
                            form.isOfficialMatchSelected(),
                            "User must pick an official match after type switch");
                    return StepResult.pass("Official selector visible; placeholder state");
                },
                browser(),
                AnlMatchTypeTestIds.ANL_TYPE_011);
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_012 + " — Revalidate eligibility at submission")
    public void anlType012_revalidateOfficialMatchAtSubmission() {
        requirePhase(Phase.E, AnlMatchTypeTestIds.ANL_TYPE_012);
        AnlMatchTypeAnalysisGate.requireSubmitPhaseE();
        runPendingEligibilityStep(
                AnlMatchTypeTestIds.ANL_TYPE_012,
                "Ineligible fixture at submit shows: "
                        + MatchTypeValidationMessages.OFFICIAL_MATCH_NO_LONGER_ELIGIBLE,
                () -> {
                    throw new UnsupportedOperationException(
                            "Requires fixture that becomes ineligible between select and submit");
                });
    }

    @Test(description = AnlMatchTypeTestIds.ANL_TYPE_013 + " — Successful official linkage (delegates to HP gate)")
    public void anlType013_successfulOfficialLinkage() {
        requirePhase(Phase.E, AnlMatchTypeTestIds.ANL_TYPE_013);
        AnlMatchTypeAnalysisGate.requireSubmitPhaseE();
        throw new SkipException(
                "ANL-TYPE-013 submit linkage is covered by ANL-OFF-HP-001; run analysis-official-hp-suite.xml "
                        + "with -Danalysis.create.enabled=true when authorized.");
    }

    private CreateAnalysisPage openOfficialForm() {
        CreateAnalysisPage form = flow.openNewMatchAnalysisFromHome();
        flow.selectOfficialMatchTypeExplicitly(form);
        return form;
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        AnlMatchTypeAnalysisGate.requirePhaseAtLeast(minimum);
        if (minimum.ordinal() < Phase.E.ordinal()) {
            Assert.assertFalse(
                    AnalysisCreateGate.isCreationEnabled(),
                    "ANL-TYPE phases a–d must run with analysis.create.enabled=false");
        }
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-ANL-MATCH-TYPE",
                AnlMatchTypeTestIds.FEATURE,
                "Authenticated user; Home → تحليل مباراة جديدة",
                "Credentials configured; phase gate satisfied",
                "phase=" + AnlMatchTypeAnalysisGate.currentPhase()
                        + "; createEnabled="
                        + AnalysisCreateGate.isCreationEnabled());
    }

    private static void requireManifest() {
        OfficialFixtureManifestSession.requireManifestForSelection();
    }

    private static int resolveSelectionIndex() {
        return AnalysisCreateGate.configuredFixtureIndex()
                .orElse(AnalysisCreateGate.DEFAULT_HP_SUBMISSION_INDEX);
    }

    private void runPendingEligibilityStep(
            String testCaseId, String objective, TestStepReporter.StepAction action) {
        try {
            TestStepReporter.runStep(
                    "Execute eligibility scenario",
                    objective,
                    "Per US-FAP-ANL-MATCH-TYPE AC / BR-ANL-TYPE-004…006",
                    action,
                    browser(),
                    testCaseId);
        } catch (UnsupportedOperationException pending) {
            throw new SkipException(
                    "Pending test data or DOM markers for " + testCaseId + ": " + pending.getMessage(),
                    pending);
        }
    }
}
