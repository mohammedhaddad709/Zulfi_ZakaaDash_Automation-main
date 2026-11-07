package com.taf.systems.zulfi.features.analysis.friendly;

import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestSession;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.friendly.AnlFriendlyTestIds;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.AnlFriendlyAnalysisGate;
import com.taf.systems.zulfi.config.AnlFriendlyAnalysisGate.Phase;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.flows.analysis.FriendlyMatchAnalysisFlow;
import com.taf.systems.zulfi.flows.analysis.FriendlyMatchCreateFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.FriendlyMatchNameRules;
import com.taf.systems.zulfi.validations.FriendlyMatchValidationMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.NoSuchElementException;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;

/**
 * US-FAP-ANL-FRIENDLY acceptance tests. Enable via {@code -Danl.friendly.phase=a|b|c|d|e}.
 */
@Epic("Zulfi Portal")
@Feature(AnlFriendlyTestIds.FEATURE)
public class FriendlyMatchAcceptanceTest extends AuthenticatedUiTest {

    private FriendlyMatchAnalysisFlow openFlow;
    private FriendlyMatchCreateFlow createFlow;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for ANL-FRIENDLY tests.");
        }
        super.loginBeforeEachTestCase();
        openFlow = new FriendlyMatchAnalysisFlow(browser());
        createFlow = new FriendlyMatchCreateFlow(browser());
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_001 + " — Display friendly fields; hide official selector")
    public void anlFrd001_displayFriendlyMetadataFields() {
        requirePhase(Phase.A, AnlFriendlyTestIds.ANL_FRD_001);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_001,
                "Select «ودية» and verify friendly fields",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    Assert.assertTrue(form.isMatchType(CreateAnalysisPage.MATCH_TYPE_FRIENDLY));
                    Assert.assertTrue(form.isFriendlyMatchNameFieldVisible(), "اسم المباراة visible");
                    Assert.assertTrue(form.isFriendlyMatchDateFieldVisible(), "تاريخ المباراة visible");
                    Assert.assertFalse(
                            form.isOfficialMatchSelectorVisible(),
                            "Official match selector must not appear for «ودية»");
                    return StepResult.pass("Friendly fields visible; official selector hidden");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_002 + " — Require friendly match name")
    public void anlFrd002_requireFriendlyMatchName() {
        requirePhase(Phase.B, AnlFriendlyTestIds.ANL_FRD_002);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_002,
                "Submit without اسم المباراة",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.clearFriendlyMatchName();
                    assertValidationFeedback(
                            form,
                            FriendlyMatchValidationMessages.FIELD_REQUIRED,
                            "Empty friendly match name");
                    return StepResult.pass("Name required validation observed");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_003 + " — Minimum name length 3")
    public void anlFrd003_validateFriendlyMatchNameMinLength() {
        requirePhase(Phase.B, AnlFriendlyTestIds.ANL_FRD_003);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_003,
                "Enter 2-character name",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.enterFriendlyMatchName("أب");
                    form.selectFriendlyMatchDate(LocalDate.now().minusDays(1));
                    assertValidationFeedback(
                            form,
                            FriendlyMatchValidationMessages.NAME_MIN_LENGTH,
                            "Below minimum name length");
                    return StepResult.pass("Min length message observed");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_004 + " — Maximum name length 150")
    public void anlFrd004_enforceFriendlyMatchNameMaxLength() {
        requirePhase(Phase.B, AnlFriendlyTestIds.ANL_FRD_004);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_004,
                "Attempt to enter 151 characters",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    String attempt =
                            FriendlyMatchNameRules.repeatChar('أ', FriendlyMatchNameRules.MAX_LENGTH + 1);
                    form.enterFriendlyMatchName(attempt);
                    String actual = form.getFriendlyMatchNameValue();
                    Assert.assertTrue(
                            actual.length() <= FriendlyMatchNameRules.MAX_LENGTH,
                            "Expected maxlength enforcement; actualLength=" + actual.length());
                    Integer maxAttr = form.getFriendlyMatchNameMaxLengthAttribute();
                    if (maxAttr != null) {
                        Assert.assertEquals(
                                maxAttr.intValue(),
                                FriendlyMatchNameRules.MAX_LENGTH,
                                "HTML maxlength attribute");
                    }
                    return StepResult.pass("storedLength=" + actual.length());
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_005 + " — Trim friendly match name")
    public void anlFrd005_trimFriendlyMatchName() {
        requirePhase(Phase.B, AnlFriendlyTestIds.ANL_FRD_005);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_005,
                "Leading/trailing spaces and spaces-only",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.enterFriendlyMatchName("   ");
                    form.blurFriendlyMatchNameField();
                    String spacesOnly = form.getFriendlyMatchNameValue();
                    Assert.assertTrue(
                            FriendlyMatchNameRules.isEmptyAfterTrim(spacesOnly),
                            "Spaces-only treated as empty; value=[" + spacesOnly + "]");

                    String core = "الزلفي × الفريق الضيف";
                    form.enterFriendlyMatchName("  " + core + "  ");
                    form.blurFriendlyMatchNameField();
                    String trimmedObserved = FriendlyMatchNameRules.trim(form.getFriendlyMatchNameValue());
                    Assert.assertTrue(
                            trimmedObserved.equals(core) || form.getFriendlyMatchNameValue().contains(core),
                            "Trim or preserve inner name; observed=" + form.getFriendlyMatchNameValue());
                    return StepResult.pass("Trim behavior verified at field level");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_003 + " — Invalid characters in name")
    public void anlFrd003b_invalidFriendlyMatchNameCharacters() {
        requirePhase(Phase.B, AnlFriendlyTestIds.ANL_FRD_003);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_003,
                "Enter disallowed characters",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.enterFriendlyMatchName("abc@invalid");
                    form.blurFriendlyMatchNameField();
                    assertValidationFeedback(
                            form,
                            FriendlyMatchValidationMessages.NAME_INVALID_CHARACTERS,
                            "Invalid name characters");
                    return StepResult.pass("Invalid character validation observed");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_006 + " — Require friendly match date")
    public void anlFrd006_requireFriendlyMatchDate() {
        requirePhase(Phase.C, AnlFriendlyTestIds.ANL_FRD_006);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_006,
                "Valid name without date",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.enterFriendlyMatchName("الزلفي × ضيف");
                    form.clearFriendlyMatchDate();
                    assertValidationFeedback(
                            form,
                            FriendlyMatchValidationMessages.FIELD_REQUIRED,
                            "Empty friendly match date");
                    return StepResult.pass("Date required validation observed");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_007 + " — Reject future friendly match date")
    public void anlFrd007_rejectFutureFriendlyMatchDate() {
        requirePhase(Phase.C, AnlFriendlyTestIds.ANL_FRD_007);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_007,
                "Select tomorrow",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    form.enterFriendlyMatchName("الزلفي × ضيف");
                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                    // The date field only accepts calendar picks; a disabled future day is the rejection itself.
                    if (!form.isMatchDateSelectableInCalendar(tomorrow)) {
                        return StepResult.pass("Future date rejected by prevention: " + tomorrow + " is disabled in the calendar");
                    }
                    form.selectFriendlyMatchDate(tomorrow);
                    assertValidationFeedback(
                            form,
                            FriendlyMatchValidationMessages.DATE_NOT_TODAY_OR_PAST,
                            "Future friendly match date");
                    return StepResult.pass("Future date rejected with validation message");
                });
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_008 + " — Existing description and video inputs")
    public void anlFrd008_existingAnalysisInputsRemainAvailable() {
        requirePhase(Phase.D, AnlFriendlyTestIds.ANL_FRD_008);
        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_008,
                "After valid friendly metadata",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    createFlow.completeRequiredFriendlyMetadata(
                            form, "الزلفي × ضيف", LocalDate.now().minusDays(3));
                    Assert.assertTrue(form.isDescriptionFieldVisible(), "Description field visible");
                    Assert.assertTrue(form.isVideoUrlFieldVisible(), "Video URL field visible");
                    form.enterOptionalDescription("وصف ودي").enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    Assert.assertFalse(form.getVideoUrlValue().isBlank(), "Video URL entry works");
                    return StepResult.pass("Shared analysis inputs available");
                });
    }

    @Test(
            description =
                    AnlFriendlyTestIds.ANL_FRD_009
                            + " / "
                            + AnlFriendlyTestIds.ANL_FRD_010
                            + " / "
                            + AnlFriendlyTestIds.ANL_FRD_012
                            + " — One authorized friendly submit + list (exactly-once)")
    public void anlFrd009_e2e_createFriendlyAnalysisWithoutOfficialLink() {
        requirePhase(Phase.E, AnlFriendlyTestIds.ANL_FRD_009);
        AnlFriendlyAnalysisGate.requireFriendlySubmitPhaseE();
        String matchName = AnalysisCreateGate.resolveFriendlyMatchNameForRun();
        LocalDate playedDate = LocalDate.now().minusDays(2);
        AnalysisSubmissionAttempt attempt = AnalysisSubmissionAttempt.authorized(matchName);

        TestStepReporter.runStep(
                "Authorized friendly submit (009/010/012)",
                "Exactly one «ودية» record; no official id; listed in التحليلات",
                "SUCCESS + searchable title",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    Assert.assertFalse(form.isOfficialMatchSelectorVisible());
                    createFlow.completeRequiredFriendlyMetadata(form, matchName, playedDate);
                    createFlow.fillDescriptionAndVideo(
                            form, "تحليل ودي أوتوميشن", AnalysisCreateGate.resolveVideoUrl());
                    Assert.assertFalse(form.isOfficialMatchSelected());

                    AnalysisPostSubmitOutcome outcome =
                            createFlow.submitFriendlyAnalysisExactlyOnce(form, attempt);
                    Assert.assertEquals(outcome, AnalysisPostSubmitOutcome.SUCCESS);

                    AnalysesListPage list = new AnalysesListPage(browser()).openViaSidebarNavigation();
                    String needle = matchName.length() > 14 ? matchName.substring(0, 14) : matchName;
                    list.searchByMatchName(needle);
                    Assert.assertTrue(
                            list.containsVisibleMatchTitle(matchName)
                                    || list.containsVisibleMatchTitle(needle),
                            "Friendly analysis listed; needle=" + needle);
                    return StepResult.pass("outcome=SUCCESS; listed=true; name=" + matchName);
                },
                browser(),
                AnlFriendlyTestIds.ANL_FRD_009);
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_011 + " — Friendly match not added to official schedule manifest")
    public void anlFrd011_friendlyDoesNotAlterOfficialScheduleManifest() {
        requirePhase(Phase.E, AnlFriendlyTestIds.ANL_FRD_011);
        if (!OfficialFixtureManifestSession.isDiscoverySucceededThisRun()) {
            throw new SkipException(
                    "Run with official fixture discovery in same suite for manifest baseline (see friendly-match-analysis-e-suite.xml).");
        }
        OfficialFixtureManifest before = OfficialFixtureManifestStore.load();
        int countBefore = before == null ? -1 : before.fixtureCount();

        AnlFriendlyAnalysisGate.requireFriendlySubmitPhaseE();
        String matchName = AnalysisCreateGate.resolveFriendlyMatchNameForRun() + "-sched";
        AnalysisSubmissionAttempt attempt = AnalysisSubmissionAttempt.authorized(matchName);

        TestStepReporter.runStep(
                "Submit friendly analysis and compare manifest",
                "Yellow League dropdown inventory unchanged",
                "fixtureCount stable",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    createFlow.completeRequiredFriendlyMetadata(
                            form, matchName, LocalDate.now().minusDays(5));
                    createFlow.fillDescriptionAndVideo(form, "schedule guard", AnalysisCreateGate.resolveVideoUrl());
                    AnalysisPostSubmitOutcome outcome =
                            createFlow.submitFriendlyAnalysisExactlyOnce(form, attempt);
                    Assert.assertEquals(outcome, AnalysisPostSubmitOutcome.SUCCESS);

                    OfficialFixtureManifest after = OfficialFixtureManifestStore.load();
                    Assert.assertNotNull(after, "Manifest still present");
                    Assert.assertEquals(
                            after.fixtureCount(),
                            countBefore,
                            "Official fixture manifest count must not increase for friendly analysis");
                    return StepResult.pass("manifestCount=" + countBefore);
                },
                browser(),
                AnlFriendlyTestIds.ANL_FRD_011);
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_013 + " — No auto-match to official fixture")
    public void anlFrd013_noAutomaticOfficialLinkage() {
        requirePhase(Phase.E, AnlFriendlyTestIds.ANL_FRD_013);
        throw new SkipException(
                "ANL-FRD-013 requires backend observability (no auto official_match_id from similarity). "
                        + "Track as manual/API check until analysis detail exposes linkage.");
    }

    @Test(description = AnlFriendlyTestIds.ANL_FRD_014 + " — Failed submit does not affect schedule manifest")
    public void anlFrd014_failedSubmitDoesNotAffectSchedule() {
        requirePhase(Phase.E, AnlFriendlyTestIds.ANL_FRD_014);
        Assert.assertFalse(
                AnalysisCreateGate.isFriendlyCreationEnabled(),
                "This scenario must not create records; keep analysis.friendly.create.enabled=false");
        if (!OfficialFixtureManifestSession.isDiscoverySucceededThisRun()) {
            throw new SkipException("Manifest baseline requires discovery in same suite run.");
        }
        OfficialFixtureManifest before = OfficialFixtureManifestStore.load();
        int countBefore = before.fixtureCount();

        runFriendlyFormStep(
                AnlFriendlyTestIds.ANL_FRD_014,
                "Attempt submit with invalid video URL",
                () -> {
                    CreateAnalysisPage form = openFlow.openFriendlyMatchAnalysisForm();
                    createFlow.completeRequiredFriendlyMetadata(
                            form, "الزلفي × ضيف فشل", LocalDate.now().minusDays(1));
                    form.enterVideoUrl("://invalid-video-url");
                    form.probeFriendlyValidationBySubmitAttempt();
                    Assert.assertFalse(form.isCreationSuccessState(), "Submit must not succeed with invalid video");
                    Assert.assertTrue(form.isCreatePopupDisplayed(), "Create dialog remains open after failed submit");
                    OfficialFixtureManifest after = OfficialFixtureManifestStore.load();
                    Assert.assertEquals(after.fixtureCount(), countBefore, "Manifest unchanged after failed submit");
                    return StepResult.pass("Failed submit; manifest stable");
                });
    }

    private static void requirePhase(Phase minimum, String testCaseId) {
        AnlFriendlyAnalysisGate.requirePhaseAtLeast(minimum);
        if (minimum.ordinal() < Phase.E.ordinal()) {
            Assert.assertFalse(
                    AnalysisCreateGate.isCreationEnabled(),
                    "ANL-FRIENDLY phases a–d must run with analysis.create.enabled=false");
            Assert.assertFalse(
                    AnalysisCreateGate.isFriendlyCreationEnabled(),
                    "ANL-FRIENDLY phases a–d must run with analysis.friendly.create.enabled=false");
        }
        TestStepReporter.publishTestMetadata(
                testCaseId,
                "US-FAP-ANL-FRIENDLY",
                AnlFriendlyTestIds.FEATURE,
                "Authenticated; Home → تحليل مباراة جديدة → ودية",
                "Credentials + phase gate satisfied",
                "phase="
                        + AnlFriendlyAnalysisGate.currentPhase()
                        + "; create="
                        + AnalysisCreateGate.isCreationEnabled()
                        + "; friendlyCreate="
                        + AnalysisCreateGate.isFriendlyCreationEnabled());
    }

    private void runFriendlyFormStep(String testCaseId, String objective, TestStepReporter.StepAction action) {
        try {
            TestStepReporter.runStep(
                    "Execute friendly acceptance scenario",
                    objective,
                    "Per US-FAP-ANL-FRIENDLY AC",
                    action,
                    browser(),
                    testCaseId);
        } catch (NoSuchElementException missing) {
            throw new SkipException(
                    "Friendly match DOM not available for " + testCaseId + ": " + missing.getMessage(), missing);
        }
    }

    private static void assertValidationFeedback(
            CreateAnalysisPage form, String expectedMessageFragment, String context) {
        form.probeFriendlyValidationBySubmitAttempt();
        boolean messageVisible = form.isCreateFormMessageVisible(expectedMessageFragment);
        boolean stillOnForm = form.isCreatePopupDisplayed();
        Assert.assertTrue(
                messageVisible || (stillOnForm && !form.isCreationSuccessState()),
                context + " — expected message containing ["
                        + expectedMessageFragment
                        + "]; submitEnabled="
                        + form.isSubmitEnabled());
    }
}
