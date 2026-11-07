package com.taf.systems.zakaadash.reporting;

import com.taf.systems.zakaadash.utils.BusinessStep;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.taf.core.utils.OutputPaths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QaSummaryListener implements IReporter {

    private static final String HEADER =
            "| Test Case ID | Title | Feature | Priority | Execution Type | Test Type | Dataset | Source Name"
                    + " | Preprocessing Step | Category | Status | Duration (s) | Failure Step |";
    private static final String SEPARATOR = "|---|---|---|---|---|---|---|---|---|---|---|---:|---|";

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        List<ITestResult> results = collectResults(suites);
        results.sort(Comparator.comparingLong(ITestResult::getStartMillis));

        List<String> lines = new ArrayList<>();
        lines.add("# QA Execution Summary");
        lines.add("");
        lines.add("Total: " + results.size()
                + " | Passed: " + count(results, ITestResult.SUCCESS)
                + " | Failed: " + count(results, ITestResult.FAILURE)
                + " | Skipped: " + count(results, ITestResult.SKIP));
        lines.add("");
        lines.add(HEADER);
        lines.add(SEPARATOR);
        results.forEach(result -> lines.add(toRow(result)));

        if (results.isEmpty()) {
            return;
        }
        Path qaSummaryRoot = OutputPaths.root().resolve("qa-summary");
        Path summaryFile = qaSummaryRoot.resolve("qa-summary.md");
        Path allureResults = OutputPaths.allureResults();
        try {
            Files.createDirectories(qaSummaryRoot);
            Files.write(summaryFile, lines, StandardCharsets.UTF_8);
            DefectCandidateWriter.writeAll(qaSummaryRoot.resolve("defect-candidates"), results, allureResults);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not write QA summary to " + summaryFile, ex);
        }
    }

    private static List<ITestResult> collectResults(List<ISuite> suites) {
        List<ITestResult> results = new ArrayList<>();
        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                context.getPassedTests().getAllResults().stream().filter(ZakaaDashTests::owns).forEach(results::add);
                context.getFailedTests().getAllResults().stream().filter(ZakaaDashTests::owns).forEach(results::add);
                context.getSkippedTests().getAllResults().stream().filter(ZakaaDashTests::owns).forEach(results::add);
            }
        }
        return results;
    }

    private static long count(List<ITestResult> results, int status) {
        return results.stream().filter(result -> result.getStatus() == status).count();
    }

    private static String toRow(ITestResult result) {
        TestCaseMetadata metadata = TestCaseMetadata.from(result);
        String dataset = BusinessStep.readAttribute(result, BusinessStep.DATASET);
        double durationSeconds = (result.getEndMillis() - result.getStartMillis()) / 1000.0;
        return "| " + String.join(" | ",
                metadata.testCaseId(),
                metadata.title(),
                metadata.feature(),
                metadata.priority(),
                metadata.executionTypes(),
                metadata.testType(),
                dataset,
                BusinessStep.readAttribute(result, BusinessStep.SOURCE_NAME),
                BusinessStep.readAttribute(result, BusinessStep.PREPROCESSING_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CATEGORY),
                toStatus(result.getStatus()),
                String.format("%.1f", durationSeconds),
                failureStep(result)) + " |";
    }

    private static String toStatus(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.FAILURE -> "FAIL";
            case ITestResult.SKIP -> "SKIP";
            default -> "UNKNOWN";
        };
    }

    private static String failureStep(ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE) {
            return "";
        }
        return "failed: " + BusinessStep.readAttribute(result, BusinessStep.CURRENT_STEP)
                + " / last completed: " + BusinessStep.readAttribute(result, BusinessStep.LAST_COMPLETED_STEP)
                + " / page: " + BusinessStep.readAttribute(result, BusinessStep.CURRENT_PAGE);
    }
}
