package com.taf.systems.zulfi.reporting;

import org.openqa.selenium.WebDriver;

import java.util.Optional;

public record FailureDiagnostics(
        String failedStep,
        String expectedBehavior,
        String actualBehavior,
        String failureReason,
        String browserUrl,
        String exceptionType,
        DefectClassification classification) {

    public static FailureDiagnostics fromThrowable(WebDriver driver, Throwable error, String failedStep) {
        String url = driver == null ? "n/a" : safeUrl(driver);
        // Explicit classification recorded by the failing step wins; the keyword heuristic is a legacy fallback.
        DefectClassification classification =
                TestExecutionContext.explicitClassification().orElseGet(() -> classify(error));
        Optional<TestExecutionContext.StepRecord> failing = TestExecutionContext.failedStep();
        String step = failing.map(s -> "STEP " + String.format("%02d", s.number()) + " — " + s.title())
                .orElseGet(() -> resolveFailedStep(failedStep));
        String expected = failing.map(TestExecutionContext.StepRecord::expected)
                .filter(s -> s != null && !s.isBlank())
                .orElseGet(FailureDiagnostics::resolveExpected);
        String actual = failing.map(TestExecutionContext.StepRecord::actual)
                .filter(s -> s != null && !s.isBlank())
                .orElseGet(() -> error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        return new FailureDiagnostics(
                step,
                expected,
                actual,
                error.getClass().getSimpleName(),
                url,
                error.getClass().getName(),
                classification);
    }

    private static String resolveFailedStep(String failedStep) {
        String active = TestExecutionContext.lastStepTitle();
        if (active != null && !active.isBlank() && !"Unnamed step".equals(active)) {
            return active;
        }
        return failedStep;
    }

    private static String resolveExpected() {
        return TestExecutionContext.metadata()
                .map(TestCaseMetadata::expectedFinalResult)
                .filter(s -> !s.isBlank())
                .orElse("See test case Expected Result and step Expected attachments.");
    }

    private static DefectClassification classify(Throwable error) {
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        if (message.contains("fixture order/identity drift")
                || message.contains("index identity drift")) {
            return DefectClassification.ENVIRONMENT_ERROR;
        }
        if (message.contains("missing or placeholder credential")
                || message.contains("zulfi_official_fixture")
                || message.contains("analysis already exists")
                || message.contains("externaleventid")) {
            return DefectClassification.TEST_DATA_ERROR;
        }
        if (message.contains("internet_disconnected") || message.contains("timeout")) {
            return DefectClassification.ENVIRONMENT_ERROR;
        }
        if (message.contains("nosuchwindow") || message.contains("webdriver")) {
            return DefectClassification.AUTOMATION_DEFECT;
        }
        if (message.contains("teamfieldsreadonly=false")
                || message.contains("team fields non-editability verification failed")
                || message.contains("home team verification failed")
                || message.contains("away team verification failed")) {
            return DefectClassification.AUTOMATION_DEFECT;
        }
        if (message.contains("form not populated from schedule")
                || message.contains("start analysis action not visible")
                || message.contains("schedule-bound")
                || message.contains("success message")
                || message.contains("duplicate fixture")
                || message.contains("listing must")
                || message.contains("same analysis record must remain")) {
            return DefectClassification.PRODUCT_DEFECT;
        }
        return DefectClassification.INCONCLUSIVE;
    }

    private static String safeUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception ex) {
            return "unavailable";
        }
    }
}
