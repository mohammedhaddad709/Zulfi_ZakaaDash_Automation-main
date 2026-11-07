package com.taf.systems.zulfi.reporting;

import com.taf.core.media.EvidenceRegistry;
import org.openqa.selenium.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Contract of the professional bug report: built only from real steps, real data and real evidence files. */
public class BugReportContentContractTest {

    private Path evidenceDir;

    @BeforeMethod
    public void startContext() throws Exception {
        evidenceDir = Files.createTempDirectory("bug-report-evidence");
        EvidenceRegistry.reset();
        TestCaseMetadata metadata = new TestCaseMetadata(
                "MCH-ANL-002", "«تحليل المباراة» opens the create view", "Match Schedule — Official Analysis",
                "US-FAP-MCH-ANALYSIS (33009)", "", "Opens the create view for the clicked match.", "Confirm routing",
                "P1", "Functional", List.of("User is authenticated", "An eligible match exists"),
                "First eligible row", "Create view for the clicked match; Match Type «رسمية».",
                List.of("AC — Open Official Analysis Pop-up"), List.of("BR-MCH-ANL-002"));
        TestExecutionContext.begin(metadata);
        TestExecutionContext.recordTestData("externalEventId", "16651987");
        TestExecutionContext.recordTestData("match", "الطائي × الجندل");
    }

    @AfterMethod(alwaysRun = true)
    public void clearContext() {
        TestExecutionContext.clear();
        EvidenceRegistry.reset();
    }

    private static TestExecutionContext.StepRecord step(int n, String title, String action, String expected, String actual, String status, Path shot) {
        return new TestExecutionContext.StepRecord(n, title, action, expected, actual, status, 120, shot);
    }

    @Test(description = "CORE-RPT-001 — Bug report lists the real steps, expected vs actual and the failing step")
    public void reportShowsRealStepsAndFailingStep() throws Exception {
        Path shot = Files.writeString(evidenceDir.resolve("fail.png"), "png");
        EvidenceRegistry.record(EvidenceRegistry.Kind.SCREENSHOT, "MCH-ANL-002_STEP02_FAIL", shot);
        TestExecutionContext.recordStep(step(1, "Locate an eligible match", "Open the schedule", "A match is found", "externalEventId=16651987", "PASS", null));
        TestExecutionContext.recordStep(step(2, "Verify Match Type", "Read the Match Type control", "Match Type = «رسمية» and disabled",
                "Match Type=«رسمية» but the control is still enabled", "FAIL", shot));
        TestExecutionContext.classifyFailure(DefectClassification.PRODUCT_DEFECT, "Behaviour contradicts the story.");

        Throwable error = new AssertionError("Step failed: Verify Match Type");
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, error, "mchAnl002");
        Path report = EnhancedBugReportWriter.writeMarkdown(diagnostics, "MCH-ANL-002", "Match Schedule — Official Analysis",
                "MatchScheduleAnalysisAcceptanceTest#mchAnl002_open");
        String body = Files.readString(report);

        Assert.assertTrue(body.startsWith("# [BUG] MCH-ANL-002 — «تحليل المباراة» opens the create view"), body.substring(0, 80));
        Assert.assertTrue(body.contains("1. ✅ **Locate an eligible match** — Open the schedule"), "passed steps show title and action");
        Assert.assertTrue(body.contains("2. ❌ **Verify Match Type** — Read the Match Type control  ← **fails here**"), "failing step is marked");
        Assert.assertTrue(body.contains("_Actual:_ Match Type=«رسمية» but the control is still enabled"), "failing step shows its actual inline");
        Assert.assertTrue(body.contains("_Expected:_ Match Type = «رسمية» and disabled"));
        Assert.assertTrue(body.contains("Match Type=«رسمية» but the control is still enabled"), "actual result of the failing step");
        Assert.assertTrue(body.contains("| Acceptance criteria | AC — Open Official Analysis Pop-up |"));
        Assert.assertTrue(body.contains("| Business rules | BR-MCH-ANL-002 |"));
        Assert.assertTrue(body.contains("| externalEventId | 16651987 |"), "test data used");
        Assert.assertTrue(body.contains("PRODUCT_DEFECT — the portal behaves differently from the user story"));
        Assert.assertTrue(body.contains("Why this classification:** Behaviour contradicts the story."));
        Assert.assertTrue(body.contains("-Dtest=MatchScheduleAnalysisAcceptanceTest#mchAnl002_open"), "re-run command names the real test");
    }

    @Test(description = "CORE-RPT-002 — Evidence section links only files that exist and says when there is no video")
    public void evidenceSectionLinksRealFiles() throws Exception {
        Path shot = Files.writeString(evidenceDir.resolve("step.png"), "png");
        Path video = Files.writeString(evidenceDir.resolve("run.mp4"), "mp4");
        EvidenceRegistry.record(EvidenceRegistry.Kind.SCREENSHOT, "step", shot);
        EvidenceRegistry.record(EvidenceRegistry.Kind.VIDEO, "run", video);
        TestExecutionContext.recordStep(step(1, "Check", "Do it", "OK", "Not OK", "FAIL", shot));

        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, new AssertionError("x"), "t");
        String withVideo = Files.readString(EnhancedBugReportWriter.writeMarkdown(diagnostics, "MCH-ANL-002", "F"));
        Assert.assertTrue(withVideo.contains("**Video:** [run.mp4]("), withVideo);
        Assert.assertTrue(withVideo.contains("![failing step]("), "failing-step screenshot is embedded");
        Assert.assertTrue(withVideo.contains("step.png"));

        EvidenceRegistry.reset();
        String without = Files.readString(EnhancedBugReportWriter.writeMarkdown(diagnostics, "MCH-ANL-002", "F"));
        Assert.assertTrue(without.contains("**Video:** not recorded for this run"), "honest about missing video");
    }

    @Test(description = "CORE-RPT-003 — Explicit step classification wins over keyword heuristics")
    public void explicitClassificationWinsOverHeuristics() {
        TestExecutionContext.recordStep(step(1, "Verify", "Do", "OK", "A timeout happened", "FAIL", null));
        TestExecutionContext.classifyFailure(DefectClassification.PRODUCT_DEFECT, "explicit");
        // the message contains "timeout" which the legacy heuristic maps to ENVIRONMENT_ERROR
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, new RuntimeException("timeout while verifying"), "t");
        Assert.assertEquals(diagnostics.classification(), DefectClassification.PRODUCT_DEFECT);
        Assert.assertTrue(diagnostics.failedStep().startsWith("STEP 01 — Verify"), diagnostics.failedStep());
        Assert.assertEquals(diagnostics.actualBehavior(), "A timeout happened", "the step's actual result is used verbatim");
    }

    @Test(description = "CORE-RPT-004 — Exceptions are classified by what they mean, not by message text")
    public void stepExceptionsAreClassifiedBySemantics() {
        Assert.assertEquals(TestStepReporter.classify(new AssertionError("x")), DefectClassification.PRODUCT_DEFECT);
        Assert.assertEquals(TestStepReporter.classify(new NoSuchElementException("gone")), DefectClassification.INCONCLUSIVE);
        Assert.assertEquals(TestStepReporter.classify(new EvidenceUnavailableException("no id")), DefectClassification.INCONCLUSIVE);
        Assert.assertEquals(TestStepReporter.classify(new IllegalStateException("Missing credential")), DefectClassification.CONFIGURATION_ERROR);
        Assert.assertEquals(TestStepReporter.classify(new NullPointerException()), DefectClassification.AUTOMATION_DEFECT);
    }

    @Test(description = "CORE-RPT-006 — Several failed checks are all listed; the first one is the primary failure")
    public void reportListsEveryFailedCheck() throws Exception {
        TestExecutionContext.recordStep(step(1, "Locate", "Open", "found", "found", "PASS", null));
        TestExecutionContext.recordStep(step(2, "Verify locked", "Read lock", "locked", "not locked", "FAIL", null));
        TestExecutionContext.recordStep(step(3, "Verify result", "Read result", "1-1 shown", "result missing", "FAIL", null));
        TestExecutionContext.classifyFailure(DefectClassification.PRODUCT_DEFECT, "first failure");

        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, new AssertionError("2 check(s) failed"), "t");
        Assert.assertTrue(diagnostics.failedStep().startsWith("STEP 02"), "primary failure is the first failed check: " + diagnostics.failedStep());
        String body = Files.readString(EnhancedBugReportWriter.writeMarkdown(diagnostics, "MCH-ANL-004", "F"));
        Assert.assertTrue(body.contains("## All failed checks (2)"), body);
        Assert.assertTrue(body.contains("| 2 | Verify locked | locked | not locked |"));
        Assert.assertTrue(body.contains("| 3 | Verify result | 1-1 shown | result missing |"));
        Assert.assertTrue(body.contains("(primary failure)"));
    }

    @Test(description = "CORE-RPT-005 — Failure before any step still produces a readable report")
    public void reportWithoutStepsExplainsWhy() throws Exception {
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, new IllegalStateException("login page did not load"), "setup");
        String body = Files.readString(EnhancedBugReportWriter.writeMarkdown(diagnostics, "MCH-ANL-001", "F"));
        Assert.assertTrue(body.contains("The failure happened before the first reported step"), body);
        Assert.assertTrue(body.contains("## Test data used"));
    }
}
