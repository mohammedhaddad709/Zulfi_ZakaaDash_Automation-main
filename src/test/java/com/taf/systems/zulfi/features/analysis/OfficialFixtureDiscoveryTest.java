package com.taf.systems.zulfi.features.analysis;

import com.taf.core.media.ScreenshotsManager;
import com.taf.systems.zulfi.analysis.fixture.FixtureDiscoveryResult;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestSession;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.analysis.OfficialFixtureSelectionFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

@Epic("Zulfi Portal")
@Feature("Create Analysis")
public class OfficialFixtureDiscoveryTest extends AuthenticatedUiTest {

    private static final String TEST_CASE_ID = "ANL-OFF-DIS-001";
    private static final int SCROLL_PASSES = CreateAnalysisPage.DEFAULT_FIXTURE_SCROLL_PASSES;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for official fixture discovery.");
        }
        super.loginBeforeEachTestCase();
    }

    @Test(description = TEST_CASE_ID + " — Discover Official Fixture Index Manifest (read-only)")
    public void discoverOfficialFixtureIndexManifest() {
        OfficialFixtureManifestSession.beginSuiteRun();
        TestStepReporter.publishTestMetadata(
                TEST_CASE_ID,
                "Discover Official Fixture Index Manifest",
                "Create Analysis",
                "Build run-specific zero-based fixture index manifest without submitting.",
                "Fresh browser; credentials configured; analysis.create.enabled=false.",
                "Scroll passes=" + SCROLL_PASSES);

        Assert.assertFalse(
                AnalysisCreateGate.isCreationEnabled(),
                "Discovery must not run with analysis.create.enabled=true");

        OfficialFixtureSelectionFlow flow = new OfficialFixtureSelectionFlow(browser());
        TestStepReporter.runStep(
                "Login",
                "Authenticate via LoginFlow fixture",
                "Home shows ابدأ التحليل.",
                () -> StepResult.pass("homeReady=" + flow.verifyAuthenticatedHome().isReady()),
                browser(),
                TEST_CASE_ID);

        TestStepReporter.runStep(
                "Open Create Analysis",
                "Click ابدأ التحليل",
                "Dialog «تحليل مباراة جديدة» visible.",
                () -> {
                    CreateAnalysisPage popup = flow.openCreateAnalysisPopup();
                    return popup.isDialogTitleVisible()
                            ? StepResult.pass("dialogVisible=true")
                            : StepResult.fail("dialogVisible=false");
                },
                browser(),
                TEST_CASE_ID);

        CreateAnalysisPage form = new CreateAnalysisPage(browser());
        flow.selectOfficialMatchType(form);
        FixtureDiscoveryResult discovery =
                form.discoverOfficialFixtureOptionsWithProgress(SCROLL_PASSES);
        OfficialFixtureManifest manifest =
                OfficialFixtureManifestStore.buildFromSnapshots(
                        discovery.orderedSnapshots(),
                        discovery.scrollPassesExecuted(),
                        discovery.enumerationComplete());
        OfficialFixtureManifestSession.markDiscoverySucceeded(manifest);

        TestStepReporter.runStep(
                "Persist fixture manifest",
                "Write target/analysis-fixtures/latest-manifest.json",
                "Manifest contains all discovered fixture rows with canonical indexes.",
                () ->
                        manifest.fixtureCount() > 0
                                ? StepResult.pass(
                                        "fixtureCount="
                                                + manifest.fixtureCount()
                                                + "; enabled="
                                                + manifest.enabledCount()
                                                + "; disabled="
                                                + manifest.disabledCount()
                                                + "; enumerationComplete="
                                                + manifest.enumerationComplete()
                                                + "; endOfListLikely="
                                                + discovery.endOfListLikely()
                                                + "; idleScrollPasses="
                                                + discovery.scrollPassesWithNoNewFixtures())
                                : StepResult.fail("No fixtures discovered"),
                browser(),
                TEST_CASE_ID);

        form.dismissOfficialMatchOverlay();
        form.closeCreateAnalysisDialogIfOpen();
        ScreenshotsManager.takeFullPageScreenshot(browser(), TEST_CASE_ID + "_PASS");
        Assert.assertTrue(manifest.fixtureCount() > 0, "At least one official fixture must be discovered");
    }

    @org.testng.annotations.AfterMethod(alwaysRun = true)
    public void recordDiscoveryOutcome(org.testng.ITestResult result) {
        if (result.getStatus() != org.testng.ITestResult.SUCCESS) {
            OfficialFixtureManifestSession.markDiscoveryFailed(
                    "ANL-OFF-DIS-001 failed: "
                            + (result.getThrowable() == null
                                    ? result.getStatus()
                                    : result.getThrowable().getMessage()));
        }
    }
}
