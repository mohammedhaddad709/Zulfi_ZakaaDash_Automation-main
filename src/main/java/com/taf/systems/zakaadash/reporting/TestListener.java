package com.taf.systems.zakaadash.reporting;

import com.taf.systems.zakaadash.utils.BusinessStep;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        if (!ZakaaDashTests.owns(result)) {
            return;
        }
        TestCaseMetadata metadata = TestCaseMetadata.from(result);
        log("START " + metadata.testCaseId() + " - " + metadata.title());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (!ZakaaDashTests.owns(result)) {
            return;
        }
        log("PASS  " + TestCaseMetadata.from(result).testCaseId());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!ZakaaDashTests.owns(result)) {
            return;
        }
        TestCaseMetadata metadata = TestCaseMetadata.from(result);
        Throwable error = result.getThrowable();
        log("FAIL  " + metadata.testCaseId()
                + " | failed step: " + attribute(result, BusinessStep.CURRENT_STEP)
                + " | last completed step: " + attribute(result, BusinessStep.LAST_COMPLETED_STEP)
                + " | page: " + attribute(result, BusinessStep.CURRENT_PAGE)
                + " | " + (error != null ? error.getClass().getSimpleName() + ": " + error.getMessage() : "no exception"));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (!ZakaaDashTests.owns(result)) {
            return;
        }
        log("SKIP  " + TestCaseMetadata.from(result).testCaseId());
    }

    private static String attribute(ITestResult result, String name) {
        return BusinessStep.readAttribute(result, name);
    }

    private static void log(String message) {
        Reporter.log("[ZakaaDash] " + message, true);
    }
}
