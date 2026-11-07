package com.taf.systems.zulfi.reporting;

import com.taf.core.media.ScreenshotsManager;
import io.qameta.allure.Allure;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.Assert;
import org.testng.SkipException;

/**
 * Step-level Expected/Actual reporting for Allure (one step per logical action). Every step is also recorded in
 * {@link TestExecutionContext#steps()} so the bug report can list the exact steps that ran, and every failure
 * carries an explicit {@link DefectClassification}.
 */
public final class TestStepReporter {

    private TestStepReporter() {}

    /**
     * Outcome of one step. {@code classification} is only meaningful for failures; when null a failed
     * verification is treated as a {@link DefectClassification#PRODUCT_DEFECT} candidate.
     */
    public record StepResult(String actual, boolean passed, DefectClassification classification) {
        public static StepResult pass(String actual) {
            return new StepResult(actual, true, null);
        }

        /** The product did not behave as the step expects (default failure meaning). */
        public static StepResult fail(String actual) {
            return new StepResult(actual, false, DefectClassification.PRODUCT_DEFECT);
        }

        public static StepResult fail(String actual, DefectClassification classification) {
            return new StepResult(actual, false, classification);
        }
    }

    @FunctionalInterface
    public interface StepAction {
        StepResult run() throws Exception;
    }

    /**
     * Legacy per-test metadata for tests without a full {@link TestCaseReport}. When the annotation already
     * published the specification, only the test name/labels are refreshed (no duplicate attachments).
     */
    public static void publishTestMetadata(
            String testCaseId,
            String title,
            String feature,
            String objective,
            String preconditions,
            String testDataReference) {
        Allure.label("testCaseId", testCaseId);
        Allure.label("testTitle", title);
        Allure.feature(feature);
        Allure.getLifecycle().updateTestCase(testCase -> testCase.setName(testCaseId + " — " + title));
        if (TestExecutionContext.specificationPublished()) {
            return;
        }
        String body =
                """
                | Field | Value |
                |---|---|
                | Test Case ID | %s |
                | Title | %s |
                | Feature | %s |
                | Objective | %s |
                | Preconditions | %s |
                | Test Data | %s |
                """
                        .formatted(testCaseId, title, feature, objective, preconditions, testDataReference);
        Allure.addAttachment("Test case specification", "text/markdown", body);
        TestExecutionContext.markSpecificationPublished();
    }

    /** Records a concrete value used by the test (shown as an Allure parameter and in the bug report). */
    public static void recordTestData(String key, String value) {
        TestExecutionContext.recordTestData(key, value);
        Allure.parameter(key, value == null ? "" : value);
    }

    public static void runStep(String stepTitle, String action, String expected, StepAction stepAction) {
        runStep(stepTitle, action, expected, stepAction, null, null);
    }

    public static void runStep(
            String stepTitle,
            String action,
            String expected,
            StepAction stepAction,
            WebDriver driver,
            String evidenceLabel) {
        int step = allocateStepNumber(stepTitle);
        String allureStepName = "STEP " + String.format("%02d", step) + " — " + stepTitle;
        Allure.step(
                allureStepName,
                () -> {
                    long stepStart = System.nanoTime();
                    StepResult result;
                    try {
                        result = stepAction.run();
                    } catch (SkipException skip) {
                        finish(step, stepTitle, action, expected, "Not executed: " + skip.getMessage(), "SKIP",
                                elapsedMillis(stepStart), null);
                        throw skip;
                    } catch (Throwable ex) {
                        Path shot = capture(driver, evidenceLabel, step, "FAIL");
                        String actual = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                        finish(step, stepTitle, action, expected, actual, "FAIL", elapsedMillis(stepStart), shot);
                        DefectClassification classification = classify(ex);
                        TestExecutionContext.classifyFailure(classification, reasonFor(classification, ex));
                        if (ex instanceof RuntimeException runtime) throw runtime;
                        if (ex instanceof Error error) throw error;
                        throw new RuntimeException(ex);
                    }
                    String status = result.passed() ? "PASS" : "FAIL";
                    Path shot = capture(driver, evidenceLabel, step, status);
                    finish(step, stepTitle, action, expected, result.actual(), status, elapsedMillis(stepStart), shot);
                    if (!result.passed()) {
                        DefectClassification classification = result.classification() == null
                                ? DefectClassification.PRODUCT_DEFECT : result.classification();
                        TestExecutionContext.classifyFailure(classification, reasonFor(classification, null));
                    }
                    Assert.assertTrue(
                            result.passed(),
                            result.passed()
                                    ? stepTitle + " passed"
                                    : "Step failed: " + stepTitle + " — expected: " + expected
                                            + " — actual: " + result.actual());
                });
    }

    public static void recordStep(String title, String action, String expected, String actual, String status) {
        int step = allocateStepNumber(title);
        String name = "STEP " + String.format("%02d", step) + " — " + title;
        if ("BLOCKED".equals(status) || "SKIP".equals(status)) {
            // Shown as skipped in Allure (never as a green pass): the outcome could not be verified or was not executed.
            String uuid = UUID.randomUUID().toString();
            Allure.getLifecycle().startStep(uuid, new io.qameta.allure.model.StepResult().setName(name));
            try {
                finish(step, title, action, expected, actual, status, 0, null);
            } finally {
                Allure.getLifecycle().updateStep(uuid, result -> result.setStatus(io.qameta.allure.model.Status.SKIPPED));
                Allure.getLifecycle().stopStep(uuid);
            }
            return;
        }
        Allure.step(name, () -> finish(step, title, action, expected, actual, status, 0, null));
    }

    public static <T> T observe(Supplier<T> supplier) {
        return supplier.get();
    }

    /** Failures inside a step: assertion = product expectation not met; locator/wait problems need triage. */
    static DefectClassification classify(Throwable ex) {
        if (ex instanceof EvidenceUnavailableException) return DefectClassification.INCONCLUSIVE;
        if (ex instanceof AssertionError) return DefectClassification.PRODUCT_DEFECT;
        if (ex instanceof NoSuchElementException
                || ex instanceof TimeoutException
                || ex instanceof StaleElementReferenceException) {
            return DefectClassification.INCONCLUSIVE;
        }
        if (ex instanceof WebDriverException) return DefectClassification.ENVIRONMENT_ERROR;
        if (ex instanceof IllegalStateException state
                && state.getMessage() != null
                && state.getMessage().toLowerCase().contains("credential")) {
            return DefectClassification.CONFIGURATION_ERROR;
        }
        return DefectClassification.AUTOMATION_DEFECT;
    }

    static String reasonFor(DefectClassification classification, Throwable ex) {
        return switch (classification) {
            case PRODUCT_DEFECT -> "The portal's observable behaviour contradicts the step's expected result "
                    + "(derived from the user story). Confirm with the evidence before raising the product bug.";
            case INCONCLUSIVE -> ex instanceof EvidenceUnavailableException
                    ? "BLOCKED: the expected outcome cannot be observed through the UI; needs API/DB evidence or a UI change."
                    : "An expected element did not appear in time. Either the product no longer shows it (possible product "
                            + "defect), the environment was slow, or the locator drifted (automation). Compare the "
                            + "failure screenshot/video with the expected result to decide.";
            case ENVIRONMENT_ERROR -> "The browser session or network failed during the step; rerun before triaging.";
            case CONFIGURATION_ERROR -> "Run configuration is missing or invalid (credentials, flags or target data).";
            case TEST_DATA_ERROR -> "The data required by the preconditions was not available in the environment.";
            case AUTOMATION_DEFECT -> "Unexpected exception inside the automation code; fix the test before triaging the product.";
        };
    }

    private static Path capture(WebDriver driver, String evidenceLabel, int step, String status) {
        if (driver == null || evidenceLabel == null) {
            return null;
        }
        return ScreenshotsManager.takeFullPageScreenshot(
                driver, evidenceLabel + "_STEP" + String.format("%02d", step) + "_" + status);
    }

    private static void finish(
            int step, String title, String action, String expected, String actual, String status,
            long durationMs, Path screenshot) {
        TestExecutionContext.recordStep(new TestExecutionContext.StepRecord(
                step, title, action, expected, actual, status, durationMs, screenshot));
        String markdown =
                """
                | | |
                |---|---|
                | Step | %02d — %s |
                | Action | %s |
                | Expected | %s |
                | Actual | %s |
                | Verdict | **%s** |
                | Duration | %d ms |
                """
                        .formatted(step, cell(title), cell(action), cell(expected), cell(actual), status, durationMs);
        Allure.addAttachment("Step " + String.format("%02d", step) + " — expected vs actual", "text/markdown", markdown);
    }

    private static String cell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", "<br>");
    }

    private static long elapsedMillis(long nanoStart) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoStart);
    }

    private static int allocateStepNumber(String stepTitle) {
        TestExecutionContext.Context ctx = TestExecutionContext.current();
        if (ctx == null) {
            return 0;
        }
        TestExecutionContext.recordActiveStep(stepTitle);
        return ctx.nextStepNumber();
    }
}
