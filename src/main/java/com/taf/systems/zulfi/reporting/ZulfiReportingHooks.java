package com.taf.systems.zulfi.reporting;

import com.taf.core.drivers.WebDriverProvider;
import com.taf.core.listeners.SystemReportingHooks;
import com.taf.core.pacing.PacingDiagnostics;
import com.taf.core.utils.OutputPaths;
import com.taf.systems.zulfi.config.ZulfiConfig;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

/** Zulfi Portal enterprise reporting, unchanged in behaviour, attached to the shared TAF lifecycle. */
public final class ZulfiReportingHooks implements SystemReportingHooks {

    private static final String PACKAGE_PREFIX = "com.taf.systems.zulfi.";
    private static final ThreadLocal<PacingDiagnostics> PACING_DIAGNOSTICS = new ThreadLocal<>();

    @Override
    public boolean ownsSystem(String activeSystem) {
        return ZulfiConfig.SYSTEM.equalsIgnoreCase(activeSystem);
    }

    @Override
    public boolean ownsTest(ITestResult result) {
        return result.getTestClass() != null && result.getTestClass().getName().startsWith(PACKAGE_PREFIX);
    }

    @Override
    public void onExecutionStart() {
        ExecutionSummaryCollector.onExecutionStart();
    }

    @Override
    public void onExecutionFinish() {
        ExecutionSummaryCollector.onExecutionFinish();
        AutomationFailureGrouper.writeSummaryIfNeeded();
        ExecutionSummaryWriter.write(
                OutputPaths.allureResults(),
                ExecutionSummaryCollector.snapshot(
                        "(see " + OutputPaths.reportsRoot().resolve("latest-run.properties") + " after publish)",
                        OutputPaths.screenshots().toString()));
        RunReportPublisher.publishSafely();
    }

    @Override
    public void onTestOutcome(ITestResult result) {
        ExecutionSummaryCollector.onTestOutcome(result);
    }

    @Override
    public void beforeTest(ITestResult result, WebDriver driver) {
        TestCaseMetadataRegistrar.register(result);
        if (driver != null) {
            TestExecutionContext.registerDriver(driver);
        }
        PACING_DIAGNOSTICS.set(PacingDiagnostics.start());
    }

    @Override
    public void afterAssertions(ITestResult result, WebDriver driver) {
        PacingDiagnostics pacing = PACING_DIAGNOSTICS.get();
        if (pacing != null) {
            pacing.finishAndAttach();
            PACING_DIAGNOSTICS.remove();
        }
        if (driver != null) {
            TestExecutionContext.registerDriver(driver);
        }
    }

    @Override
    public String evidenceStem(ITestResult result) {
        return TestExecutionContext.testCaseId();
    }

    @Override
    public void onTestFailure(ITestResult result, WebDriver driver) {
        AllureFailureDiagnostics.attachForTestFailure(result, driver);
    }

    @Override
    public void onTestThrowable(ITestResult result, WebDriver driver) {
        AutomationFailureGrouper.recordFailure(resolveTestCaseId(result), result.getThrowable());
        if (!(result.getInstance() instanceof WebDriverProvider)) {
            return;
        }
        FailureDiagnostics diagnostics = FailureDiagnostics.fromThrowable(driver, result.getThrowable(), result.getName());
        // The writer attaches the report to Allure once; the file stays in <output.dir>/bug-reports.
        EnhancedBugReportWriter.writeMarkdown(
                diagnostics,
                resolveTestCaseId(result),
                resolveFeature(result),
                result.getTestClass().getRealClass().getSimpleName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void afterTest(ITestResult result) {
        TestExecutionContext.clear();
    }

    @Override
    public void onConfigurationFailure(ITestResult result, WebDriver driver) {
        TestExecutionContext.markSetupFailure();
        AllureFailureDiagnostics.attachForTestFailure(result, null);
    }

    private static String resolveFeature(ITestResult result) {
        TestExecutionContext.Context ctx = TestExecutionContext.current();
        if (ctx != null && ctx.feature() != null && !ctx.feature().isBlank()) {
            return ctx.feature();
        }
        return featureName(result);
    }

    private static String featureName(ITestResult result) {
        return result.getTestClass() == null ? "Unknown" : result.getTestClass().getName();
    }

    private static String resolveTestCaseId(ITestResult result) {
        TestExecutionContext.Context ctx = TestExecutionContext.current();
        if (ctx != null && ctx.testCaseId() != null && !ctx.testCaseId().isBlank()) {
            return ctx.testCaseId();
        }
        TestCaseMetadata meta =
                TestCaseMetadata.fromDescription(result.getMethod().getDescription(), result.getName(), featureName(result));
        return meta.id();
    }
}
