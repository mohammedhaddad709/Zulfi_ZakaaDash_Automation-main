package com.taf.systems.zulfi.features.analysis;

import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.analysis.preflight.EligibilityStatus;
import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureEligibilityEvaluator;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureEligibilityRecord;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixturePreflightReportWriter;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.OfficialFixturePreflightFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("Zulfi Portal")
@Feature("Create Analysis Preflight")
public class OfficialFixturePreflightTest extends AuthenticatedUiTest {

    private static final String TEST_CASE_ID = "PREFLIGHT-OFF-001";
    private static final String TITLE = "Official Match Eligibility Preflight (Read-Only)";
    private static final int SCROLL_PASSES = 8;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException(
                    "CONFIGURATION: ZULFI_USER and ZULFI_PASSWORD required for authenticated preflight");
        }
        super.loginBeforeEachTestCase();
    }

    @Test(description = TEST_CASE_ID + " — " + TITLE)
    @Description("Authenticated read-only inspection of official fixtures and analyses listing. No submit.")
    public void preflight001_officialFixtureEligibilityReadOnly() throws Exception {
        TestStepReporter.publishTestMetadata(
                TEST_CASE_ID,
                TITLE,
                "Create Analysis Preflight",
                "Identify potentially eligible official fixtures without creating analyses.",
                "Fresh browser; credentials configured; analysis.create.enabled=false.",
                "Portal UI evidence only");

        Assert.assertFalse(
                AnalysisCreateGate.isCreationEnabled(),
                "Preflight must run with analysis creation disabled");

        OfficialFixturePreflightFlow flow = new OfficialFixturePreflightFlow(browser());

        TestStepReporter.runStep(
                "Login and Verify Home",
                "Use AuthenticatedUiTest login then confirm Home CTA",
                "Home dashboard ready with «ابدأ التحليل» visible.",
                () ->
                        flow.verifyHomeReady().isReady()
                                ? StepResult.pass("Home ready; startAnalysisVisible=true")
                                : StepResult.fail("Home CTA not visible"),
                browser(),
                TEST_CASE_ID);

        CreateAnalysisPage form =
                TestStepReporter.observe(
                        () -> {
                            CreateAnalysisPage popup = flow.openCreateAnalysisPopupFromHome();
                            TestStepReporter.runStep(
                                    "Open Create Analysis Popup",
                                    "Click «ابدأ التحليل» and wait for «تحليل مباراة جديدة»",
                                    "Create Analysis dialog and form are displayed.",
                                    () -> {
                                        ScreenshotsManager.takeFullPageScreenshot(
                                                browser(), TEST_CASE_ID + "_02_create_popup");
                                        return popup.isDialogTitleVisible()
                                                ? StepResult.pass(
                                                        "Dialog title visible="
                                                                + CreateAnalysisPage.DIALOG_TITLE_FRAGMENT)
                                                : StepResult.fail("Dialog title not visible");
                                    },
                                    browser(),
                                    TEST_CASE_ID);
                            return popup;
                        });

        TestStepReporter.runStep(
                "Select Official Match Type",
                "Ensure match type «رسمية» is selected",
                "Official match selector is visible.",
                () -> {
                    flow.selectOfficialMatchType(form);
                    return form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)
                                    && form.isOfficialMatchSelectorVisible()
                            ? StepResult.pass("matchType=رسمية; officialSelectorVisible=true")
                            : StepResult.fail("Official match type/selector not ready");
                },
                browser(),
                TEST_CASE_ID);

        List<OfficialFixtureOptionSnapshot> observed = new ArrayList<>();
        TestStepReporter.runStep(
                "Inspect Official Fixture Options",
                "Open official-match dropdown and scan options with scroll",
                "Fixture rows collected with enabled/disabled state.",
                () -> {
                    observed.addAll(flow.inspectOfficialFixtures(form, SCROLL_PASSES));
                    ScreenshotsManager.takeFullPageScreenshot(
                            browser(), TEST_CASE_ID + "_04_fixture_dropdown");
                    long fixtures =
                            observed.stream().filter(o -> "FIXTURE".equals(o.rowKind())).count();
                    return fixtures > 0
                            ? StepResult.pass(
                                    "Observed " + observed.size() + " rows; fixtureRows=" + fixtures)
                            : StepResult.fail("No fixture rows observed");
                },
                browser(),
                TEST_CASE_ID);

        form.dismissOfficialMatchOverlay();

        OfficialFixtureOptionSnapshot sample =
                observed.stream()
                        .filter(o -> "FIXTURE".equals(o.rowKind()) && o.enabled())
                        .filter(o -> !KnownFixtureExclusions.isKnownDuplicateFixture(o.displayLabel()))
                        .findFirst()
                        .orElse(null);

        if (sample != null) {
            CreateAnalysisPage inspectForm = form;
            flow.selectOfficialMatchType(inspectForm);
            String label = sample.displayLabel();
            TestStepReporter.runStep(
                    "Inspect Candidate Match Metadata",
                    "Select one enabled non-known-duplicate fixture read-only",
                    "Fixture remains selected; date and team placeholders populated; teams not editable.",
                    () -> {
                        inspectForm.selectEnabledFixtureForReadOnlyInspection(label);
                        ScreenshotsManager.takeFullPageScreenshot(
                                browser(), TEST_CASE_ID + "_05_candidate_metadata");
                        boolean ok =
                                inspectForm.isOfficialMatchSelected()
                                        && inspectForm.hasPopulatedTeamPlaceholders()
                                        && inspectForm.verifyTeamFieldsNonEditable().bothNonEditable();
                        String actual =
                                "fixture="
                                        + inspectForm.getOfficialMatchSelectedLabel()
                                        + "; date="
                                        + inspectForm.getMatchDateValue()
                                        + "; homePh="
                                        + inspectForm.getHomeTeamPlaceholder()
                                        + "; awayPh="
                                        + inspectForm.getAwayTeamPlaceholder()
                                        + "; readonly="
                                        + inspectForm.verifyTeamFieldsNonEditable().bothNonEditable();
                        return ok ? StepResult.pass(actual) : StepResult.fail(actual);
                    },
                    browser(),
                    TEST_CASE_ID);
            inspectForm.dismissOfficialMatchOverlay();
            inspectForm.closeCreateAnalysisDialogIfOpen();
        } else {
            TestStepReporter.recordStep(
                    "Inspect Candidate Match Metadata",
                    "Select enabled candidate",
                    "Metadata captured for selectable fixture.",
                    "No enabled non-excluded fixture found for selection inspection",
                    "SKIP");
        }

        AnalysesListPage listPage = flow.openAnalysesListFromUi(form);
        TestStepReporter.runStep(
                "Review Existing Analyses",
                "Navigate to Analyses listing via UI",
                "Analyses listing loads; search can be used for evidence.",
                () -> {
                    ScreenshotsManager.takeFullPageScreenshot(
                            browser(), TEST_CASE_ID + "_06_analyses_list");
                    int titles = listPage.listVisibleMatchTitles().size();
                    return StepResult.pass("Analyses list loaded; visibleTitles=" + titles);
                },
                browser(),
                TEST_CASE_ID);

        List<OfficialFixtureEligibilityRecord> evaluated = new ArrayList<>();
        for (OfficialFixtureOptionSnapshot option : observed) {
            if (!"FIXTURE".equals(option.rowKind())) {
                continue;
            }
            String evidence = listPage.searchEvidenceSummary(option.displayLabel());
            evaluated.add(
                    OfficialFixtureEligibilityEvaluator.evaluate(
                                    List.of(option),
                                    new OfficialFixtureEligibilityEvaluator.InspectionContext(
                                            evidence, "Not available"))
                            .getFirst());
        }

        if (evaluated.stream().noneMatch(r -> KnownFixtureExclusions.isKnownDuplicateFixture(r.fixture()))) {
            evaluated.add(
                    new OfficialFixtureEligibilityRecord(
                            KnownFixtureExclusions.AL_TAAI_X_AL_BUKAIRIYAH,
                            "Not available",
                            "نادي الطائي",
                            "نادي البكيرية",
                            "Not available",
                            KnownFixtureExclusions.knownDuplicateReason(),
                            KnownFixtureExclusions.MATCH_ID,
                            EligibilityStatus.INELIGIBLE,
                            "Known duplicate fixture from historical verified failure",
                            "Historical run policy",
                            "Do not use for E2E until business confirms eligibility change"));
        }

        if (evaluated.isEmpty()) {
            evaluated.add(
                    new OfficialFixtureEligibilityRecord(
                            "Not available",
                            "Not available",
                            "Not available",
                            "Not available",
                            "Not available",
                            "Not available",
                            "Not available",
                            EligibilityStatus.UNKNOWN,
                            "No fixture rows captured",
                            "UI dropdown",
                            "Repeat preflight when portal data available"));
        }

        List<OfficialFixtureEligibilityRecord> finalEvaluated = List.copyOf(evaluated);
        TestStepReporter.runStep(
                "Evaluate Fixture Eligibility",
                "Apply eligibility rules including known duplicate exclusion",
                "Each fixture classified with evidence-based status.",
                () -> {
                    long ineligible =
                            finalEvaluated.stream()
                                    .filter(r -> r.eligibilityStatus() == EligibilityStatus.INELIGIBLE)
                                    .count();
                    long potential =
                            finalEvaluated.stream()
                                    .filter(
                                            r ->
                                                    r.eligibilityStatus()
                                                            == EligibilityStatus.POTENTIALLY_ELIGIBLE)
                                    .count();
                    return StepResult.pass(
                            "ineligible=" + ineligible + "; potentiallyEligible=" + potential);
                },
                browser(),
                TEST_CASE_ID);

        String recommended =
                finalEvaluated.stream()
                        .filter(r -> r.eligibilityStatus() == EligibilityStatus.POTENTIALLY_ELIGIBLE)
                        .map(OfficialFixtureEligibilityRecord::fixture)
                        .findFirst()
                        .orElse("");

        String limitations =
                "Dropdown scan used "
                        + SCROLL_PASSES
                        + " scroll passes; virtualized lists may hide rows. "
                        + "Analyses listing reflects current page/search only — absence does not prove backend eligibility. "
                        + "Match/external IDs only when exposed on option DOM attributes.";

        Path reportPath = Path.of("docs/recovery/PHASE12_OFFICIAL_FIXTURE_PREFLIGHT_REPORT.md");
        TestStepReporter.runStep(
                "Generate Preflight Report",
                "Write markdown inventory to docs/recovery",
                "PHASE12_OFFICIAL_FIXTURE_PREFLIGHT_REPORT.md created with observed data.",
                () -> {
                    OfficialFixturePreflightReportWriter.writeMarkdown(
                            reportPath, observed, finalEvaluated, limitations, recommended);
                    return StepResult.pass("Report written: " + reportPath.toAbsolutePath());
                },
                null,
                TEST_CASE_ID);

        ScreenshotsManager.takeFullPageScreenshot(browser(), TEST_CASE_ID + "_PASS");
        Assert.assertFalse(AnalysisCreateGate.isCreationEnabled(), "Creation must remain disabled");
    }
}
