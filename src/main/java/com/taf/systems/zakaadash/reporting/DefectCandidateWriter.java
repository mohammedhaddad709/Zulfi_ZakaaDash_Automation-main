package com.taf.systems.zakaadash.reporting;

import com.taf.systems.zakaadash.utils.BusinessStep;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class DefectCandidateWriter {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DefectCandidateWriter() {
    }

    static void writeAll(Path defectDirectory, List<ITestResult> results, Path allureResultsDirectory) {
        try {
            Files.createDirectories(defectDirectory);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create defect candidate directory: " + defectDirectory, ex);
        }

        results.stream()
                .filter(result -> result.getStatus() == ITestResult.FAILURE)
                .forEach(result -> writeOne(defectDirectory, result, allureResultsDirectory));
    }

    private static void writeOne(Path defectDirectory, ITestResult result, Path allureResultsDirectory) {
        TestCaseMetadata metadata = TestCaseMetadata.from(result);
        Throwable error = result.getThrowable();
        String classification = classify(result, error);

        String fileName = sanitize(metadata.testCaseId()) + "_"
                + FILE_TIME.format(LocalDateTime.now()) + "_"
                + result.getEndMillis() + ".md";
        Path file = defectDirectory.resolve(fileName);
        try {
            Files.writeString(file, buildMarkdown(metadata, result, error, classification, allureResultsDirectory),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not write defect candidate: " + file, ex);
        }
    }

    private static String buildMarkdown(
            TestCaseMetadata metadata,
            ITestResult result,
            Throwable error,
            String classification,
            Path allureResultsDirectory) {
        String description = result.getMethod().getDescription();
        if (description == null || description.isBlank()) {
            description = metadata.title();
        }

        String report = """
                # Defect Candidate

                ## Test Case ID
                %s

                ## Feature
                %s

                ## Classification
                %s

                ## Failed Business Step
                %s (page: %s)

                ## Preconditions
                %s

                ## Steps to Reproduce
                1. %s
                2. Complete business steps through: %s
                3. Fail at: %s on page %s

                ## Expected Result
                %s

                ## Actual Result
                %s

                ## Browser / Environment
                %s

                ## Current URL
                %s

                ## Exception / Assertion
                %s

                ## Screenshot
                %s

                ## Allure Evidence
                %s
                """.formatted(
                metadata.testCaseId(),
                metadata.feature(),
                classification,
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_PAGE),
                preconditions(result),
                description,
                BusinessStep.readAttribute(result, BusinessStep.LAST_COMPLETED_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_PAGE),
                metadata.title(),
                actualResult(error, result),
                BusinessStep.readAttribute(result, BusinessStep.ENVIRONMENT),
                BusinessStep.readAttribute(result, BusinessStep.FAILURE_URL),
                exceptionText(error),
                BusinessStep.readAttribute(result, BusinessStep.SCREENSHOT_PATH),
                allureResultsDirectory.toAbsolutePath());
        return report + preprocessingSection(result);
    }

    private static String preprocessingSection(ITestResult result) {
        if ("N/A".equals(BusinessStep.readAttribute(result, BusinessStep.EXPECTED_RESULT))) {
            return "";
        }
        double durationSeconds = (result.getEndMillis() - result.getStartMillis()) / 1000.0;
        return """

                ## Preprocessing Context
                - Preprocessing Step (last reached): %s
                - Category / Tab: %s
                - Dataset: %s
                - Source Name: %s
                - Applied Configuration: %s
                - Expected (case data): %s
                - Duration (s): %.1f
                """.formatted(
                BusinessStep.readAttribute(result, BusinessStep.PREPROCESSING_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CATEGORY),
                BusinessStep.readAttribute(result, BusinessStep.DATASET),
                BusinessStep.readAttribute(result, BusinessStep.SOURCE_NAME),
                BusinessStep.readAttribute(result, BusinessStep.CONFIGURATION),
                BusinessStep.readAttribute(result, BusinessStep.EXPECTED_RESULT),
                durationSeconds);
    }

    private static String preconditions(ITestResult result) {
        if ("com.taf.systems.zakaadash.tests.config.ZakaaDashCredentialsTest".equals(result.getTestClass().getName())) {
            return """
                    - ZAKAADASH_EMAIL and ZAKAADASH_PASSWORD supplied via JVM property or environment variable
                    - No browser session required""";
        }
        return """
                - WebDriver session started for this test method
                - ZakaaDash credentials configured when the test requires authentication
                - Application reachable at configured zakaadash.base.url""";
    }

    private static String actualResult(Throwable error, ITestResult result) {
        String pageTitle = BusinessStep.readAttribute(result, BusinessStep.PAGE_TITLE);
        String url = BusinessStep.readAttribute(result, BusinessStep.FAILURE_URL);
        StringBuilder actual = new StringBuilder();
        if (!"N/A".equals(pageTitle)) {
            actual.append("Page title: ").append(pageTitle).append(". ");
        }
        if (!"N/A".equals(url)) {
            actual.append("URL: ").append(url).append(". ");
        }
        if (error != null) {
            actual.append(error.getClass().getSimpleName()).append(": ").append(error.getMessage());
        }
        return actual.isEmpty() ? "Test failed without exception details" : actual.toString().trim();
    }

    private static String exceptionText(Throwable error) {
        if (error == null) {
            return "N/A";
        }
        return error.getClass().getName() + ": " + error.getMessage();
    }

    static String classify(ITestResult result, Throwable error) {
        String pageTitle = BusinessStep.readAttribute(result, BusinessStep.PAGE_TITLE);
        String url = BusinessStep.readAttribute(result, BusinessStep.FAILURE_URL);
        String message = error != null && error.getMessage() != null ? error.getMessage() : "";

        if (isGatewayFailure(pageTitle) || message.matches("(?s).*\\b50[234]\\b.*")
                || (!"N/A".equals(url) && url.contains("zakaadash")
                && message.contains("NoSuchElementException") && message.contains("#email"))) {
            return "Automation Infrastructure Failure";
        }
        if (message.contains("not configured") || message.contains("ZakaaDash email") || message.contains("ZakaaDash password")) {
            return "Automation Infrastructure Failure";
        }
        if (error instanceof AssertionError) {
            return "Potential Product Defect";
        }
        if (error instanceof org.openqa.selenium.TimeoutException
                || message.contains("Unable to locate element")
                || message.contains("Authentication failed")) {
            return "Automation or Product UI Investigation";
        }
        return "Automation or Product UI Investigation";
    }

    private static boolean isGatewayFailure(String pageTitle) {
        String title = pageTitle.toLowerCase();
        return title.contains("bad gateway") || title.contains("service unavailable") || title.contains("gateway time-out")
                || title.contains("gateway timeout");
    }

    private static String sanitize(String testCaseId) {
        return testCaseId.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
