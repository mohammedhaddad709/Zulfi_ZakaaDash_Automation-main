package com.taf.systems.zulfi.reporting;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Non-mutating verification of failure classification rules (no browser). */
public class DefectReportingContractTest {

    @Test
    public void classifiesMissingCredentialsAsTestDataOrConfiguration() {
        Throwable missing =
                new IllegalStateException("Missing or placeholder credential environment variable: ZULFI_USER");
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, missing, "credentialSetup");
        Assert.assertEquals(diagnostics.classification(), DefectClassification.TEST_DATA_ERROR);
    }

    @Test
    public void classifiesDuplicateAnalysisMessageAsTestDataError() {
        Throwable duplicate =
                new RuntimeException("An analysis already exists for this fixture (externalEventId=16652017)");
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(null, duplicate, "submit");
        Assert.assertEquals(diagnostics.classification(), DefectClassification.TEST_DATA_ERROR);
    }

    @Test
    public void bugReportMarkdownIncludesExecutiveSummaryAndTestCaseIdInFilename() throws Exception {
        FailureDiagnostics diagnostics =
                new FailureDiagnostics(
                        "Submit official analysis",
                        "Success dialog",
                        "Duplicate fixture toast",
                        "AnalysisPostSubmitOutcome.DUPLICATE_FIXTURE",
                        "https://portal.example/analysis/create",
                        "java.lang.AssertionError",
                        DefectClassification.PRODUCT_DEFECT);
        Path report =
                EnhancedBugReportWriter.writeMarkdown(diagnostics, "ANL-OFF-HP-001", "Create Analysis");
        Assert.assertNotNull(report);
        String name = report.getFileName().toString();
        Assert.assertTrue(name.contains("ANL-OFF-HP-001"), "Filename must include test case ID: " + name);
        String body = Files.readString(report);
        // Report format v2 (professional bug report): summary, root cause analysis, evidence, re-run command.
        Assert.assertTrue(body.contains("> **Summary:**"));
        Assert.assertTrue(body.contains("## Root cause analysis"));
        Assert.assertTrue(body.contains("## Evidence"));
        Assert.assertTrue(body.contains("## How to re-run this test"));
    }
}
