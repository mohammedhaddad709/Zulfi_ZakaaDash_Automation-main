package com.taf.systems.zulfi.reporting;

import io.qameta.allure.Allure;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

/** Business-readable failure explanation while preserving raw exceptions. */
public final class AllureFailureDiagnostics {

    private AllureFailureDiagnostics() {}

    public static void attachForTestFailure(ITestResult result, WebDriver driver) {
        Throwable throwable = result.getThrowable();
        if (throwable == null) {
            return;
        }
        FailureCategory category = classify(throwable, TestExecutionContext.setupFailure());
        String url = BrowserObservations.currentUrl(driver);
        String failedStep = TestExecutionContext.lastStepTitle();
        String expected =
                TestExecutionContext.metadata()
                        .map(TestCaseMetadata::expectedFinalResult)
                        .filter(s -> !s.isBlank())
                        .orElse("See step Expected Result attachments and test objective.");
        String actual = BrowserObservations.observationFromThrowable(driver, throwable);
        String summary = buildExecutiveSummary(failedStep, category, throwable);

        try {
            Allure.label("failureCategory", category.name());
        } catch (Exception ignored) {
            // Allure context may be unavailable for configuration-only failures.
        }

        attachFixtureContextIfPresent();

        Allure.addAttachment(
                "Failure diagnostics",
                "text/markdown",
                """
                ## Executive summary
                %s

                | Field | Value |
                |---|---|
                | Failed step | %s |
                | Expected behavior | %s |
                | Actual observed behavior | %s |
                | Classification | %s |
                | Browser URL at failure | %s |
                | Recommended investigation | %s |
                | Exception type | %s |

                **Stack trace (preserved):**
                ```
                %s
                ```
                """
                        .formatted(
                                summary,
                                failedStep,
                                expected,
                                actual,
                                category.name(),
                                url,
                                investigationHint(category),
                                throwable.getClass().getName(),
                                stackTrace(throwable)));
    }

    public static FailureCategory classify(Throwable throwable, boolean setupFailure) {
        if (throwable == null) {
            return FailureCategory.INCONCLUSIVE;
        }
        String message = throwable.getMessage() == null ? "" : throwable.getMessage().toLowerCase();
        String type = throwable.getClass().getName().toLowerCase();

        if (setupFailure
                || message.contains("environment variable")
                || message.contains("credential")
                || message.contains("placeholder credential")
                || message.contains("missing or placeholder")) {
            return FailureCategory.CONFIGURATION_ERROR;
        }
        if (type.contains("timeout") || message.contains("timeout")) {
            return FailureCategory.TIMEOUT;
        }
        if (message.contains("connection refused")
                || message.contains("unknown host")
                || message.contains("network")
                || message.contains("session not created")) {
            return FailureCategory.ENVIRONMENT_ERROR;
        }
        if (message.contains("test data")
                || message.contains("invalid data")
                || message.contains("already exists")
                || message.contains("externaleventid")
                || message.contains("duplicate fixture")
                || message.contains("analysis already exists")) {
            return FailureCategory.TEST_DATA_ERROR;
        }
        if (throwable instanceof AssertionError assertionError && assertionError.getCause() != null) {
            return classify(assertionError.getCause(), setupFailure);
        }
        if (throwable instanceof AssertionError) {
            return FailureCategory.PRODUCT_DEFECT;
        }
        if (type.contains("elementclickintercepted")
                || type.contains("nosuchelement")
                || type.contains("staleelement")) {
            return FailureCategory.AUTOMATION_DEFECT;
        }
        return FailureCategory.INCONCLUSIVE;
    }

    private static void attachFixtureContextIfPresent() {
        TestExecutionContext.Context ctx = TestExecutionContext.current();
        if (ctx == null || ctx.fixtureLabel() == null) {
            return;
        }
        String body =
                """
                | Field | Value |
                |---|---|
                | Fixture index | %s |
                | Fixture label | %s |
                | Match ID | %s |
                | External event ID | %s |
                """
                        .formatted(
                                ctx.fixtureIndex() == null ? "n/a" : ctx.fixtureIndex(),
                                ctx.fixtureLabel(),
                                ctx.matchId() == null ? "n/a" : ctx.matchId(),
                                ctx.externalEventId() == null ? "n/a" : ctx.externalEventId());
        Allure.addAttachment("Fixture context", "text/markdown", body);
    }

    private static String buildExecutiveSummary(String failedStep, FailureCategory category, Throwable throwable) {
        String message = SensitiveRedaction.redact(throwable.getMessage());
        if (category == FailureCategory.CONFIGURATION_ERROR) {
            return "The automated run could not continue because required configuration or credentials were missing or invalid ("
                    + message
                    + "). The user flow stopped during step «"
                    + failedStep
                    + "» before the portal behavior under test could be verified.";
        }
        if (category == FailureCategory.PRODUCT_DEFECT) {
            return "While exercising the portal at step «"
                    + failedStep
                    + "», the application behavior did not match the documented expectation: "
                    + message
                    + ". This impacts users attempting the same action in the UI.";
        }
        return "Validation or execution failed at step «"
                + failedStep
                + "»: "
                + message
                + ". Review the screenshot, URL, and classification below to determine user impact.";
    }

    private static String investigationHint(FailureCategory category) {
        return switch (category) {
            case CONFIGURATION_ERROR -> "Verify environment variables, secrets, and portal configuration before re-running.";
            case ENVIRONMENT_ERROR -> "Verify network connectivity, browser availability, and target environment health.";
            case TIMEOUT -> "Review application responsiveness, wait strategy, and execution pace settings.";
            case TEST_DATA_ERROR -> "Review approved test data, fixture eligibility, and data preparation steps.";
            case AUTOMATION_DEFECT -> "Review locator stability, wait conditions, and automation step implementation.";
            case PRODUCT_DEFECT -> "Review application behavior against approved requirements with BA/Dev.";
            case INCONCLUSIVE -> "Review failure screenshot, URL, and stack trace; classify after manual triage.";
        };
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return SensitiveRedaction.redact(writer.toString());
    }
}
