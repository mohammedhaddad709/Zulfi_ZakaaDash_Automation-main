package com.taf.systems.zulfi.features.analysis;

import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestSession;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.OfficialFixtureSelectionFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.analysis.TeamFieldObservation;
import com.taf.systems.zulfi.reporting.TestExecutionContext;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.TeamFieldComparisonDiagnostics;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Epic("Zulfi Portal")
@Feature("Create Analysis")
public class OfficialFixtureSelectionTest extends AuthenticatedUiTest implements ITest {

    private final ThreadLocal<String> dynamicTestName = new ThreadLocal<>();

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for fixture selection tests.");
        }
        super.loginBeforeEachTestCase();
    }

    @BeforeMethod(alwaysRun = true)
    public void bindDynamicTestName(java.lang.reflect.Method method, Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            dynamicTestName.set(method.getName());
            return;
        }
        int index = (Integer) parameters[0];
        if (index < 0) {
            dynamicTestName.set(method.getName() + " — fixture discovery pending or unavailable");
            return;
        }
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        String fixture = manifest == null ? "unknown" : manifest.entryAt(index).displayLabel();
        String testCaseId = formatTestCaseId(index);
        dynamicTestName.set(testCaseId + " — Verify Official Fixture at Index " + index + " — [" + fixture + "]");
        TestExecutionContext.Context ctx = TestExecutionContext.current();
        if (ctx != null && manifest != null) {
            OfficialFixtureManifestEntry entry = manifest.entryAt(index);
            ctx.bindFixtureContext(
                    index, entry.displayLabel(), entry.matchId(), entry.externalEventId());
        }
    }

    @Override
    public String getTestName() {
        return dynamicTestName.get();
    }

    @DataProvider(name = "officialFixtureIndexes")
    public Object[][] officialFixtureIndexes() {
        if (!OfficialFixtureManifestSession.isDiscoverySucceededThisRun()) {
            return new Object[][] {{-1}};
        }
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        if (manifest == null
                || !manifest.discoverySucceeded()
                || manifest.fixtureCount() == 0) {
            return new Object[][] {{-1}};
        }
        String onlyIndex = System.getProperty("analysis.fixture.selection.onlyIndex");
        if (onlyIndex != null && !onlyIndex.isBlank()) {
            int index = Integer.parseInt(onlyIndex.trim());
            if (index < 0 || index >= manifest.fixtureCount()) {
                return new Object[][] {{-1}};
            }
            return new Object[][] {{index}};
        }
        String indexList = System.getProperty("analysis.fixture.selection.indexes");
        if (indexList != null && !indexList.isBlank()) {
            String[] parts = indexList.split(",");
            List<Object[]> rows = new ArrayList<>();
            for (String part : parts) {
                int index = Integer.parseInt(part.trim());
                if (index >= 0 && index < manifest.fixtureCount()) {
                    rows.add(new Object[] {index});
                }
            }
            if (rows.isEmpty()) {
                return new Object[][] {{-1}};
            }
            return rows.toArray(new Object[0][]);
        }
        if (!"true".equalsIgnoreCase(System.getProperty("analysis.fixture.selection.indexMode"))) {
            return new Object[][] {{-1}};
        }
        Object[][] rows = new Object[manifest.fixtureCount()][1];
        for (int i = 0; i < manifest.fixtureCount(); i++) {
            rows[i][0] = i;
        }
        return rows;
    }

    @Test(description = "ANL-OFF-SEL-SMOKE — Official fixture user-flow (label, metadata, no submit)")
    public void verifyOfficialFixtureSelectionUserFlowSmoke() {
        OfficialFixtureManifestSession.requireManifestForSelection();
        Assert.assertFalse(
                AnalysisCreateGate.isCreationEnabled(),
                "Selection smoke must not run with analysis.create.enabled=true");

        String testCaseId = "ANL-OFF-SEL-SMOKE";
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "Official fixture selection user-flow smoke",
                "Create Analysis",
                "Pick enabled fixture by label, assert autofill, optional description/link, close without submit.",
                "Manifest discovery completed; creation disabled.",
                "indexMode=false; read-only");

        OfficialFixtureSelectionFlow flow = new OfficialFixtureSelectionFlow(browser());
        TestStepReporter.runStep(
                "Login",
                "Authenticate and reach Home",
                "Home CTA visible.",
                () -> StepResult.pass("homeReady=" + flow.verifyAuthenticatedHome().isReady()),
                browser(),
                testCaseId);

        CreateAnalysisPage form = flow.openCreateAnalysisPopup();
        flow.selectFirstEnabledOfficialFixtureByLabel(form);

        String selectedLabel = form.getSelectedOfficialFixture();
        String matchDate = form.getMatchDateValue();
        TestStepReporter.runStep(
                "Verify autofill after label selection",
                "Date and team placeholders populated; teams non-editable",
                "Official fixture metadata hydrated.",
                () -> {
                    boolean ok =
                            form.isOfficialMatchSelected()
                                    && MatchDateDisplayRules.looksLikeCalendarDateDisplay(matchDate)
                                    && form.hasPopulatedTeamPlaceholders()
                                    && form.verifyTeamFieldsNonEditable().bothNonEditable();
                    return ok
                            ? StepResult.pass(
                                    "fixture="
                                            + selectedLabel
                                            + "; date="
                                            + matchDate
                                            + "; knownDuplicateExcluded="
                                            + !KnownFixtureExclusions.isKnownDuplicateFixture(selectedLabel))
                            : StepResult.fail("autofillIncomplete; fixture=" + selectedLabel);
                },
                browser(),
                testCaseId);

        flow.fillOptionalMetadataReadOnly(form);
        TestStepReporter.runStep(
                "Optional description and video link",
                "Fill fields when visible (no submit)",
                "Optional metadata accepted in form state.",
                () -> StepResult.pass("descriptionAndLinkAttempted=true"),
                browser(),
                testCaseId);

        form.dismissOfficialMatchOverlay();
        form.closeCreateAnalysisDialogIfOpen();
        TestStepReporter.runStep(
                "Close without submit",
                "Dismiss create dialog",
                "Dialog closed.",
                () ->
                        !form.isCreatePopupDisplayed()
                                ? StepResult.pass("createDialogOpen=false")
                                : StepResult.fail("createDialogOpen=true"),
                browser(),
                testCaseId);

        Allure.addAttachment(
                "Fixture selection observables",
                "text/plain",
                "fixture="
                        + selectedLabel
                        + "\nmatchDate="
                        + matchDate
                        + "\nsubmitNotClicked=true");
        ScreenshotsManager.takeFullPageScreenshot(browser(), testCaseId + "_PASS");
    }

    @Test(
            dataProvider = "officialFixtureIndexes",
            description = "ANL-OFF-SEL — Verify Official Fixture Selection By Index")
    public void verifyOfficialFixtureSelectionByIndex(int fixtureIndex) {
        if (fixtureIndex < 0) {
            throw new SkipException(
                    OfficialFixtureManifestSession.discoveryFailureReason() != null
                            ? OfficialFixtureManifestSession.discoveryFailureReason()
                            : "Discovery did not complete successfully in this suite run.");
        }
        OfficialFixtureManifestSession.requireManifestForSelection();
        Assert.assertFalse(
                AnalysisCreateGate.isCreationEnabled(),
                "Selection tests must not run with analysis.create.enabled=true");

        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        if (fixtureIndex < 0 || fixtureIndex >= manifest.fixtureCount()) {
            throw new SkipException("Invalid fixture index " + fixtureIndex + " for current manifest.");
        }

        OfficialFixtureManifestEntry expected = manifest.entryAt(fixtureIndex);
        String testCaseId = formatTestCaseId(fixtureIndex);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "Verify Official Fixture at Index " + fixtureIndex,
                "Create Analysis",
                "Verify indexed official fixture selection and autofill without submit.",
                "Fresh browser; manifest discovery completed; creation disabled.",
                "index="
                        + fixtureIndex
                        + "; fixture="
                        + expected.displayLabel()
                        + "; enabled="
                        + expected.enabled());

        OfficialFixtureSelectionFlow flow = new OfficialFixtureSelectionFlow(browser());

        TestStepReporter.runStep(
                "Login",
                "Authenticate and reach Home",
                "Home CTA visible.",
                () -> StepResult.pass("homeReady=" + flow.verifyAuthenticatedHome().isReady()),
                browser(),
                testCaseId);

        CreateAnalysisPage form = flow.openCreateAnalysisPopup();
        TestStepReporter.runStep(
                "Open Create Analysis",
                "Open «تحليل مباراة جديدة» from Home",
                "Create dialog visible.",
                () ->
                        form.isDialogTitleVisible()
                                ? StepResult.pass("dialogVisible=true")
                                : StepResult.fail("dialogVisible=false"),
                browser(),
                testCaseId);

        flow.selectOfficialMatchType(form);
        TestStepReporter.runStep(
                "Select Official Match Type",
                "Ensure match type is رسمية",
                "Official fixture selector visible.",
                () ->
                        form.isOfficialMatchSelectorVisible()
                                ? StepResult.pass("officialSelectorVisible=true")
                                : StepResult.fail("officialSelectorVisible=false"),
                browser(),
                testCaseId);

        form.openOfficialFixtureDropdown();
        TestStepReporter.runStep(
                "Open Official Fixture Dropdown",
                "Open PrimeVue official fixture overlay",
                "Fixture options rendered.",
                () ->
                        form.getOfficialFixtureOptionCount() > 0
                                ? StepResult.pass("optionCount=" + form.getOfficialFixtureOptionCount())
                                : StepResult.fail("No fixture options visible"),
                browser(),
                testCaseId);

        var drift = flow.verifyIndexMatchesManifest(form, expected);
        TestStepReporter.runStep(
                "Locate Fixture by Index",
                "Resolve index "
                        + fixtureIndex
                        + " and compare with manifest «"
                        + expected.displayLabel()
                        + "»",
                "Live fixture identity matches manifest for requested index.",
                () ->
                        drift.stable()
                                ? StepResult.pass("indexStable=true; label=" + expected.displayLabel())
                                : StepResult.fail(drift.message()),
                browser(),
                testCaseId);
        Assert.assertTrue(drift.stable(), drift.message());

        if (!expected.enabled()) {
            TestStepReporter.runStep(
                    "Verify Disabled Fixture State",
                    "Confirm fixture index " + fixtureIndex + " is not selectable",
                    "Option is disabled; no selection performed.",
                    () ->
                            !form.isOfficialFixtureOptionEnabled(fixtureIndex)
                                    ? StepResult.pass("enabled=false")
                                    : StepResult.fail("Expected disabled fixture"),
                    browser(),
                    testCaseId);
            form.dismissOfficialMatchOverlay();
            form.closeCreateAnalysisDialogIfOpen();
            ScreenshotsManager.takeFullPageScreenshot(browser(), testCaseId + "_DISABLED_PASS");
            return;
        }

        form.selectOfficialFixtureByIndex(fixtureIndex);
        String selectedLabel = form.getSelectedOfficialFixture();
        String matchDate = form.getMatchDateValue();
        TestStepReporter.runStep(
                "Select Indexed Fixture",
                "Click enabled fixture at index " + fixtureIndex,
                "Fixture selected and overlay closed.",
                () ->
                        form.isOfficialMatchSelected()
                                ? StepResult.pass("selectedLabel=" + selectedLabel)
                                : StepResult.fail("Fixture not selected"),
                null,
                testCaseId);

        TestStepReporter.runStep(
                "Verify Selected Fixture Label",
                "Compare combobox label with manifest",
                "Selected label equals manifest label.",
                () ->
                        expected.displayLabel().equals(selectedLabel)
                                ? StepResult.pass("labelMatch=true")
                                : StepResult.fail(
                                        "expected="
                                                + expected.displayLabel()
                                                + "; actual="
                                                + selectedLabel),
                browser(),
                testCaseId);

        TestStepReporter.runStep(
                "Verify Automatically Populated Date",
                "Read official match date from create form datepicker",
                "Match date looks like calendar date.",
                () ->
                        MatchDateDisplayRules.looksLikeCalendarDateDisplay(matchDate)
                                ? StepResult.pass("matchDate=" + matchDate)
                                : StepResult.fail("matchDate=" + matchDate),
                browser(),
                testCaseId);

        TeamFieldObservation homeObs = form.observeHomeTeamField();
        Allure.addAttachment("Home team DOM observation", "text/plain", homeObs.toDiagnosticBlock());
        boolean homeMatches = expected.homeTeam().equals(homeObs.resolvedDisplayText());
        TeamFieldObservation awayObs = form.observeAwayTeamField();
        Allure.addAttachment("Away team DOM observation", "text/plain", awayObs.toDiagnosticBlock());
        boolean awayMatches = expected.awayTeam().equals(awayObs.resolvedDisplayText());
        String teamComparisonReport =
                TeamFieldComparisonDiagnostics.buildFixtureTeamReport(
                        fixtureIndex,
                        expected.displayLabel(),
                        matchDate,
                        selectedLabel,
                        expected.homeTeam(),
                        homeObs,
                        expected.awayTeam(),
                        awayObs,
                        homeMatches,
                        awayMatches);
        Allure.addAttachment(
                "Team field expected vs actual (UTF-8)",
                "text/plain",
                new String(TeamFieldComparisonDiagnostics.utf8Bytes(teamComparisonReport)));

        TestStepReporter.runStep(
                "Verify Home Team Display",
                "Read #analysis-home-team club name from DOM (value before empty prompt placeholder)",
                "Home field shows expected club name from manifest metadata.",
                () ->
                        homeMatches
                                ? StepResult.pass(
                                        "home="
                                                + homeObs.resolvedDisplayText()
                                                + "; source="
                                                + homeObs.resolutionSource())
                                : StepResult.fail(
                                        TeamFieldComparisonDiagnostics.homeVerificationFailureMessage(
                                                expected.homeTeam(),
                                                homeObs,
                                                selectedLabel)),
                browser(),
                testCaseId);

        TestStepReporter.runStep(
                "Verify Away Team Display",
                "Read #analysis-away-team club name from DOM (value before empty prompt placeholder)",
                "Away field shows expected club name from manifest metadata.",
                () ->
                        awayMatches
                                ? StepResult.pass(
                                        "away="
                                                + awayObs.resolvedDisplayText()
                                                + "; source="
                                                + awayObs.resolutionSource())
                                : StepResult.fail(
                                        TeamFieldComparisonDiagnostics.awayVerificationFailureMessage(
                                                expected.awayTeam(),
                                                awayObs,
                                                selectedLabel)),
                browser(),
                testCaseId);

        var editability = form.verifyTeamFieldsNonEditable();
        Allure.addAttachment(
                "Team fields editability (UTF-8)",
                "text/plain",
                editability.toDiagnosticBlock());
        TestStepReporter.runStep(
                "Verify Team Fields Are Non-Editable",
                "Inspect DOM non-editability signals and optional safe edit probe",
                "Home and away fields cannot be edited by the user.",
                () ->
                        editability.bothNonEditable()
                                ? StepResult.pass(
                                        "homeNonEditable="
                                                + editability.homeNonEditable()
                                                + "; awayNonEditable="
                                                + editability.awayNonEditable())
                                : StepResult.fail(
                                        """
                                        Team Fields Non-Editability Verification Failed
                                        Home non-editable: %s
                                        Away non-editable: %s
                                        Selected Fixture: %s
                                        See attachment «Team fields editability (UTF-8)» for DOM/probe evidence.
                                        """
                                                .formatted(
                                                        editability.homeNonEditable(),
                                                        editability.awayNonEditable(),
                                                        selectedLabel)),
                browser(),
                testCaseId);

        form.dismissOfficialMatchOverlay();
        form.closeCreateAnalysisDialogIfOpen();
        TestStepReporter.runStep(
                "Capture Result and Close Form",
                "Close create dialog without submit",
                "Dialog closed; no submission occurred.",
                () ->
                        !form.isCreatePopupDisplayed()
                                ? StepResult.pass("createDialogOpen=false")
                                : StepResult.fail("createDialogOpen=true"),
                browser(),
                testCaseId);

        Allure.addAttachment(
                "Fixture selection observables",
                "text/plain",
                "index="
                        + fixtureIndex
                        + "\nfixture="
                        + expected.displayLabel()
                        + "\nenabled="
                        + expected.enabled()
                        + "\nmatchDate="
                        + matchDate
                        + "\nhome="
                        + homeObs.resolvedDisplayText()
                        + " (source="
                        + homeObs.resolutionSource()
                        + ")"
                        + "\naway="
                        + awayObs.resolvedDisplayText()
                        + " (source="
                        + awayObs.resolutionSource()
                        + ")"
                        + "\nsubmitNotClicked=true");

        ScreenshotsManager.takeFullPageScreenshot(browser(), testCaseId + "_PASS");
        Assert.assertFalse(form.isCreatePopupDisplayed(), "Create dialog must be closed without submit");
        Assert.assertFalse(AnalysisCreateGate.isCreationEnabled(), "Creation gate must remain disabled");
    }

    private static String formatTestCaseId(int index) {
        return String.format("ANL-OFF-SEL-%03d", index);
    }
}
