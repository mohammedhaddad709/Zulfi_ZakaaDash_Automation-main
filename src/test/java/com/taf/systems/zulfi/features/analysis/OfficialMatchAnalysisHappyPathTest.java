package com.taf.systems.zulfi.features.analysis;

import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureSubmissionSelector;
import com.taf.systems.zulfi.reporting.EnhancedBugReportWriter;
import com.taf.systems.zulfi.reporting.FailureDiagnostics;
import com.taf.systems.zulfi.reporting.PostSubmitOutcomeHandler;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.flows.analysis.AnalysisVerificationFlow;
import com.taf.systems.zulfi.flows.analysis.CreateAnalysisFlow;
import com.taf.systems.zulfi.flows.analysis.CreateAnalysisFlow.OfficialAnalysisFormSnapshot;
import com.taf.systems.zulfi.flows.analysis.OfficialFixtureSelectionFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.analysis.TeamFieldObservation;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.TeamFieldComparisonDiagnostics;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Epic("Zulfi Portal")
@Feature("Create Analysis")
@Story("Official Match Happy Path")
public class OfficialMatchAnalysisHappyPathTest extends AuthenticatedUiTest {

    private static final String TEST_CASE_ID = "ANL-OFF-HP-001";

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for Create Analysis Happy Path.");
        }
        super.loginBeforeEachTestCase();
    }

    @Test(description = TEST_CASE_ID + " — Official Match Analysis End-to-End Happy Path")
    @Description(
            "Business acceptance for Create Analysis: form completion through persistence when explicitly authorized.")
    public void anlOffHp001_verifyOfficialMatchAnalysisCreation() {
        TestStepReporter.publishTestMetadata(
                TEST_CASE_ID,
                "Official Match Analysis Happy Path",
                "Create Analysis",
                "Complete Create Analysis workflow with single submit when authorized.",
                "Credentials configured; fixture identity configured; creation disabled unless explicitly enabled.",
                "creationEnabled=" + AnalysisCreateGate.isCreationEnabled());

        String runMarker = "Automated QA verification - Official analysis - " + UUID.randomUUID();
        String videoUrl = AnalysisCreateGate.resolveVideoUrl();
        CreateAnalysisFlow createFlow = new CreateAnalysisFlow(browser());
        OfficialFixtureSelectionFlow selectionFlow = new OfficialFixtureSelectionFlow(browser());

        TestStepReporter.runStep(
                "Authenticate",
                "Login with valid credentials",
                "Home Dashboard displayed with ابدأ التحليل.",
                () -> {
                    HomePage home = selectionFlow.verifyAuthenticatedHome();
                    return home.isReady()
                            ? StepResult.pass("homeReady=true; url=" + browser().getCurrentUrl())
                            : StepResult.fail("homeReady=false");
                },
                browser(),
                TEST_CASE_ID);

        CreateAnalysisPage form = createFlow.openCreatePopupFromHome();
        TestStepReporter.runStep(
                "Open Create Analysis",
                "Click «ابدأ التحليل»",
                "«تحليل مباراة جديدة» popup opens.",
                () ->
                        form.isDialogTitleVisible()
                                ? StepResult.pass("dialogVisible=true")
                                : StepResult.fail("dialogVisible=false"),
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Select Match Type",
                "Ensure match type is «رسمية»",
                "Official Match selector becomes available.",
                () -> {
                    form.ensureMatchTypeOfficial();
                    return form.isOfficialMatchSelectorVisible()
                            ? StepResult.pass("officialSelectorVisible=true")
                            : StepResult.fail("officialSelectorVisible=false");
                },
                browser(),
                TEST_CASE_ID);

        OfficialFixtureManifestEntry fixtureEntry = resolveConfiguredFixtureEntry(form);
        TestStepReporter.runStep(
                "Select Official Fixture by Index",
                "Select configured eligible fixture index " + fixtureEntry.fixtureIndex(),
                "Exact intended fixture is selected with identity verified.",
                () -> {
                    form.selectOfficialFixtureByIndex(fixtureEntry.fixtureIndex());
                    boolean labelOk =
                            fixtureEntry.displayLabel().equals(form.getSelectedOfficialFixture());
                    return labelOk
                            ? StepResult.pass("selected=" + form.getSelectedOfficialFixture())
                            : StepResult.fail(
                                    "expected="
                                            + fixtureEntry.displayLabel()
                                            + "; actual="
                                            + form.getSelectedOfficialFixture());
                },
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Verify Match Date",
                "Read autofill date — never type manually",
                "Match date automatically populates as calendar display.",
                () ->
                        MatchDateDisplayRules.looksLikeCalendarDateDisplay(form.getMatchDateValue())
                                ? StepResult.pass("matchDate=" + form.getMatchDateValue())
                                : StepResult.fail("matchDate=" + form.getMatchDateValue()),
                browser(),
                TEST_CASE_ID);

        TeamFieldObservation homeObs = form.observeHomeTeamField();
        TeamFieldObservation awayObs = form.observeAwayTeamField();
        io.qameta.allure.Allure.addAttachment(
                "Team field expected vs actual (UTF-8)",
                "text/plain",
                TeamFieldComparisonDiagnostics.buildFixtureTeamReport(
                        fixtureEntry.fixtureIndex(),
                        fixtureEntry.displayLabel(),
                        form.getMatchDateValue(),
                        form.getSelectedOfficialFixture(),
                        fixtureEntry.homeTeam(),
                        homeObs,
                        fixtureEntry.awayTeam(),
                        awayObs,
                        fixtureEntry.homeTeam().equals(homeObs.resolvedDisplayText()),
                        fixtureEntry.awayTeam().equals(awayObs.resolvedDisplayText())));

        TestStepReporter.runStep(
                "Verify Home and Away Teams",
                "Compare manifest home/away with DOM display values",
                "Correct clubs in correct fields.",
                () ->
                        fixtureEntry.homeTeam().equals(homeObs.resolvedDisplayText())
                                        && fixtureEntry.awayTeam().equals(awayObs.resolvedDisplayText())
                                ? StepResult.pass(
                                        "home="
                                                + homeObs.resolvedDisplayText()
                                                + "; away="
                                                + awayObs.resolvedDisplayText())
                                : StepResult.fail(
                                        TeamFieldComparisonDiagnostics.homeVerificationFailureMessage(
                                                fixtureEntry.homeTeam(), homeObs, form.getSelectedOfficialFixture())),
                browser(),
                TEST_CASE_ID);

        var editability = form.verifyTeamFieldsNonEditable();
        io.qameta.allure.Allure.addAttachment(
                "Team fields editability (UTF-8)", "text/plain", editability.toDiagnosticBlock());
        TestStepReporter.runStep(
                "Verify Team Fields Cannot Be Edited",
                "DOM non-editability signals (+ probe when DOM inconclusive)",
                "Home and Away cannot be edited.",
                () ->
                        editability.bothNonEditable()
                                ? StepResult.pass("homeNonEditable=true; awayNonEditable=true")
                                : StepResult.fail(editability.toDiagnosticBlock()),
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Enter Description",
                "Type unique run marker into description",
                "Description retained in form.",
                () -> {
                    createFlow.fillDescriptionAndVideo(form, runMarker, videoUrl);
                    return form.getDescriptionValue() != null
                                    && form.getDescriptionValue().contains(runMarker)
                            ? StepResult.pass("descriptionLength=" + form.getDescriptionValue().length())
                            : StepResult.fail("description not retained");
                },
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Enter Video URL",
                "Enter approved YouTube URL",
                "Video URL accepted without blocking validation.",
                () ->
                        videoUrl.equals(form.getVideoUrlValue())
                                ? StepResult.pass("videoUrlAccepted=true")
                                : StepResult.fail("videoUrl=" + form.getVideoUrlValue()),
                browser(),
                TEST_CASE_ID);

        OfficialAnalysisFormSnapshot snapshot = createFlow.captureFormSnapshot(form);
        TestStepReporter.runStep(
                "Verify Form Before Submission",
                "Validate official type, fixture, date, teams, description, video, submit state",
                "Form complete; submit enabled; no blocking validation errors.",
                () -> {
                    boolean ok =
                            form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)
                                    && fixtureEntry.displayLabel().equals(snapshot.fixtureLabel())
                                    && MatchDateDisplayRules.looksLikeCalendarDateDisplay(snapshot.matchDate())
                                    && !snapshot.homeTeamPlaceholder().isBlank()
                                    && !snapshot.awayTeamPlaceholder().isBlank()
                                    && form.isSubmitEnabled();
                    return ok
                            ? StepResult.pass("submitEnabled=true; fixture=" + snapshot.fixtureLabel())
                            : StepResult.fail(
                                    "submitEnabled="
                                            + form.isSubmitEnabled()
                                            + "; snapshot="
                                            + snapshot);
                },
                browser(),
                TEST_CASE_ID);
        ScreenshotsManager.takeFullPageScreenshot(browser(), TEST_CASE_ID + "_form_ready");

        if (!AnalysisCreateGate.isCreationEnabled()) {
            TestStepReporter.recordStep(
                    "Submit Analysis Exactly Once",
                    "Click «بدء التحليل»",
                    "Deferred — requires -Danalysis.create.enabled=true",
                    "BLOCKED (creation not authorized)",
                    "DEFERRED");
            TestStepReporter.recordStep(
                    "Verify Submission Outcome",
                    "Success dialog «تم إرسال المباراة للتحليل»",
                    "Deferred until authorized execution",
                    "BLOCKED",
                    "DEFERRED");
            TestStepReporter.recordStep(
                    "Open Created Analysis",
                    "Navigate via success dialog",
                    "Deferred until authorized execution",
                    "BLOCKED",
                    "DEFERRED");
            TestStepReporter.recordStep(
                    "Verify Analyses Listing",
                    "Locate analysis by marker",
                    "Deferred until authorized execution",
                    "BLOCKED",
                    "DEFERRED");
            TestStepReporter.recordStep(
                    "Refresh and Verify Persistence",
                    "Same analysis survives refresh",
                    "Deferred until authorized execution",
                    "BLOCKED",
                    "DEFERRED");
            form.closeCreateAnalysisDialogIfOpen();
            Assert.assertFalse(AnalysisCreateGate.isCreationEnabled(), "No creation in safe mode");
            return;
        }

        AnalysisCreateGate.requireCreationEnabled();
        String configuredFixture =
                AnalysisCreateGate.requireConfiguredOfficialFixtureLabel(snapshot.fixtureLabel());
        Assert.assertEquals(
                snapshot.fixtureLabel(),
                configuredFixture,
                "Configured fixture must match before authorized submit");
        AnalysisSubmissionAttempt submission = AnalysisSubmissionAttempt.authorized(configuredFixture);

        final AnalysisPostSubmitOutcome[] submitOutcome = new AnalysisPostSubmitOutcome[1];
        TestStepReporter.runStep(
                "Submit Analysis Exactly Once",
                "Click «بدء التحليل» once",
                "Success dialog «تم إرسال المباراة للتحليل».",
                () -> {
                    AnalysisPostSubmitOutcome outcome =
                            createFlow.submitOfficialAnalysisExactlyOnce(form, submission);
                    submitOutcome[0] = outcome;
                    if (submission.submitClickCount() != 1) {
                        return StepResult.fail("submitClickCount=" + submission.submitClickCount());
                    }
                    if (outcome != AnalysisPostSubmitOutcome.SUCCESS) {
                        FailureDiagnostics diagnostics =
                                PostSubmitOutcomeHandler.diagnoseCreateAnalysisOutcome(
                                        browser(), form, outcome);
                        if (diagnostics != null) {
                            try {
                                Path bug =
                                        EnhancedBugReportWriter.writeMarkdown(
                                                diagnostics, TEST_CASE_ID, getClass().getName());
                                if (bug != null) {
                                    io.qameta.allure.Allure.addAttachment(
                                            "Bug investigation report",
                                            "text/markdown",
                                            Files.readString(bug));
                                }
                            } catch (java.io.IOException ignored) {
                                // attachment optional
                            }
                        }
                        String toast = form.getVisibleErrorToastBody();
                        return StepResult.fail(
                                "Submit outcome="
                                        + outcome
                                        + "; toast="
                                        + toast
                                        + (diagnostics != null
                                                ? "; classification=" + diagnostics.classification()
                                                : ""));
                    }
                    return StepResult.pass("outcome=SUCCESS");
                },
                browser(),
                TEST_CASE_ID);
        Assert.assertEquals(
                submitOutcome[0],
                AnalysisPostSubmitOutcome.SUCCESS,
                "Submit must succeed; errorToast=" + form.getVisibleErrorToastBody());

        AnalysisVerificationFlow verifyFlow = new AnalysisVerificationFlow(browser());
        TestStepReporter.runStep(
                "Verify Submission Outcome",
                "Assert success message and actions",
                AnalysisSuccessDialog.SUCCESS_MESSAGE + " with navigation actions visible.",
                () -> {
                    AnalysisSuccessDialog dialog = verifyFlow.verifySuccessDialog();
                    return dialog.isSuccessMessageVisible()
                            ? StepResult.pass(AnalysisSuccessDialog.SUCCESS_MESSAGE)
                            : StepResult.fail("successMessageMissing=true");
                },
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Open Created Analysis / Verify Listing",
                "Open analyses list and locate created record",
                "New analysis visible with description marker.",
                () -> {
                    verifyFlow.verifyPersistenceOnAnalysesList(snapshot.fixtureLabel(), runMarker);
                    return StepResult.pass("listedAndPersistent=true");
                },
                browser(),
                TEST_CASE_ID);

        TestStepReporter.recordStep(
                "Refresh and Verify Persistence",
                "Included in listing verification flow",
                "Same analysis remains after refresh.",
                "verified in prior step",
                "PASS");
    }

    private OfficialFixtureManifestEntry resolveConfiguredFixtureEntry(CreateAnalysisPage form) {
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        if (AnalysisCreateGate.optionalExpectedFixtureLabelOverride().isPresent()) {
            String label = AnalysisCreateGate.optionalExpectedFixtureLabelOverride().get();
            if (manifest != null) {
                for (int i = 0; i < manifest.fixtureCount(); i++) {
                    if (label.equals(manifest.entryAt(i).displayLabel())) {
                        return manifest.entryAt(i);
                    }
                }
            }
            throw new SkipException("Explicit fixture label not found in manifest: " + label);
        }
        if (manifest != null && manifest.fixtureCount() > 0) {
            OfficialFixtureManifestEntry entry =
                    OfficialFixtureSubmissionSelector.resolveForHappyPathSubmission(manifest);
            org.testng.Reporter.log(
                    "HP eligible fixture index="
                            + entry.fixtureIndex()
                            + " label="
                            + entry.displayLabel(),
                    true);
            return entry;
        }
        int index =
                AnalysisCreateGate.configuredFixtureIndex()
                        .orElse(AnalysisCreateGate.DEFAULT_HP_SUBMISSION_INDEX);
        return toEntry(form.getOfficialFixtureOptionMetadata(index));
    }

    private static OfficialFixtureManifestEntry toEntry(
            CreateAnalysisPage.OfficialFixtureOptionMetadata meta) {
        return new OfficialFixtureManifestEntry(
                meta.fixtureIndex(),
                meta.displayLabel(),
                meta.enabled(),
                meta.homeTeam(),
                meta.awayTeam(),
                meta.matchId(),
                meta.externalEventId());
    }
}
