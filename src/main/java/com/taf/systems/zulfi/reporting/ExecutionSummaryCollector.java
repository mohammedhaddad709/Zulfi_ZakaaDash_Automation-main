package com.taf.systems.zulfi.reporting;

import com.taf.core.pacing.ExecutionPace;
import com.taf.core.utils.dataReader.PropertyReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.ITestResult;

/** Aggregates suite-level counters and timing for the execution summary attachment. */
public final class ExecutionSummaryCollector {

    private static final AtomicInteger passed = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();
    private static final AtomicInteger skipped = new AtomicInteger();
    private static final AtomicInteger broken = new AtomicInteger();
    private static final AtomicReference<Instant> started = new AtomicReference<>();
    private static final AtomicReference<Instant> finished = new AtomicReference<>();
    private static final AtomicReference<String> runId = new AtomicReference<>();

    private ExecutionSummaryCollector() {}

    public static void onExecutionStart() {
        passed.set(0);
        failed.set(0);
        skipped.set(0);
        broken.set(0);
        Instant now = Instant.now();
        started.set(now);
        finished.set(null);
        runId.set(
                DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS")
                        .withZone(ZoneOffset.UTC)
                        .format(now));
    }

    public static void onTestOutcome(ITestResult result) {
        switch (result.getStatus()) {
            case ITestResult.SUCCESS -> passed.incrementAndGet();
            case ITestResult.FAILURE -> {
                if (isBroken(result.getThrowable())) {
                    broken.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            }
            case ITestResult.SKIP -> skipped.incrementAndGet();
            default -> broken.incrementAndGet();
        }
    }

    public static void onExecutionFinish() {
        finished.set(Instant.now());
    }

    public static String runId() {
        return runId.get();
    }

    public static ExecutionSummary snapshot(String allureHtmlPath, String screenshotPath) {
        Instant start = started.get() == null ? Instant.now() : started.get();
        Instant end = finished.get() == null ? Instant.now() : finished.get();
        int total = passed.get() + failed.get() + skipped.get() + broken.get();
        String outcome =
                failed.get() > 0 || broken.get() > 0
                        ? "FAILED"
                        : skipped.get() == total && total > 0
                                ? "SKIPPED"
                                : total == 0
                                        ? "NO_TESTS"
                                        : "PASSED";
        return new ExecutionSummary(
                runId.get(),
                start,
                end,
                "football-auto-taf-foundation",
                PropertyReader.getProperty("environment"),
                PropertyReader.getProperty("browserType"),
                System.getProperty("browser.version", "See browser capabilities"),
                System.getProperty("os.name"),
                System.getProperty("java.version"),
                ExecutionPace.current().name().toLowerCase(),
                total,
                passed.get(),
                failed.get(),
                broken.get(),
                skipped.get(),
                outcome,
                allureHtmlPath,
                screenshotPath);
    }

    private static boolean isBroken(Throwable throwable) {
        if (throwable == null) {
            return true;
        }
        if (throwable instanceof AssertionError) {
            return false;
        }
        String message = throwable.getMessage() == null ? "" : throwable.getMessage().toLowerCase();
        return throwable instanceof IllegalStateException
                || message.contains("environment variable")
                || message.contains("credential")
                || message.contains("webdriver")
                || message.contains("session not created");
    }

    public record ExecutionSummary(
            String runId,
            Instant start,
            Instant end,
            String project,
            String environment,
            String browser,
            String browserVersion,
            String os,
            String javaVersion,
            String executionPace,
            int totalTests,
            int passed,
            int failed,
            int broken,
            int skipped,
            String overallOutcome,
            String allureReportLocation,
            String screenshotEvidenceLocation) {

        public String toMarkdown() {
            long durationMs = end.toEpochMilli() - start.toEpochMilli();
            return """
                    # Test Execution Summary

                    | Field | Value |
                    |---|---|
                    | Execution Run ID | %s |
                    | Execution Start (UTC) | %s |
                    | Execution End (UTC) | %s |
                    | Total Duration | %d ms |
                    | Project | %s |
                    | Environment | %s |
                    | Browser | %s |
                    | Browser Version | %s |
                    | OS | %s |
                    | Java Version | %s |
                    | Execution Pace | %s |
                    | Total Tests | %d |
                    | Passed | %d |
                    | Failed | %d |
                    | Broken | %d |
                    | Skipped | %d |
                    | Overall Outcome | %s |
                    | Allure Report Location | %s |
                    | Screenshot Evidence Location | %s |
                    """
                    .formatted(
                            runId,
                            start.atOffset(ZoneOffset.UTC),
                            end.atOffset(ZoneOffset.UTC),
                            durationMs,
                            project,
                            environment,
                            browser,
                            browserVersion,
                            os,
                            javaVersion,
                            executionPace,
                            totalTests,
                            passed,
                            failed,
                            broken,
                            skipped,
                            overallOutcome,
                            allureHtmlPathOrDefault(),
                            screenshotEvidenceLocation);
        }

        private String allureHtmlPathOrDefault() {
            return allureReportLocation == null || allureReportLocation.isBlank()
                    ? "(generated after publish step)"
                    : allureReportLocation;
        }
    }
}
