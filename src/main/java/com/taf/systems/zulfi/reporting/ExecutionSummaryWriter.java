package com.taf.systems.zulfi.reporting;

import com.taf.core.utils.logs.LogsManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persists suite execution summary alongside Allure raw results. */
public final class ExecutionSummaryWriter {

    private ExecutionSummaryWriter() {}

    public static void write(Path allureResults, ExecutionSummaryCollector.ExecutionSummary summary) {
        if (!Files.isDirectory(allureResults)) {
            return;
        }
        try {
            Path summaryFile = allureResults.resolve("execution-summary.md");
            String markdown = summary.toMarkdown();
            Files.writeString(summaryFile, markdown);
            appendEnvironmentProperties(allureResults, summary);
            LogsManager.info("Execution summary written: " + summaryFile.toAbsolutePath());
        } catch (IOException ex) {
            LogsManager.warn("Execution summary write skipped: " + ex.getMessage());
        }
    }

    private static void appendEnvironmentProperties(
            Path allureResults, ExecutionSummaryCollector.ExecutionSummary summary) throws IOException {
        Path env = allureResults.resolve("environment.properties");
        StringBuilder builder = new StringBuilder();
        if (Files.isRegularFile(env)) {
            builder.append(Files.readString(env));
            if (!builder.toString().endsWith("\n")) {
                builder.append('\n');
            }
        }
        builder.append("Project=").append(summary.project()).append('\n');
        builder.append("ExecutionRunId=").append(summary.runId()).append('\n');
        builder.append("JavaVersion=").append(summary.javaVersion()).append('\n');
        builder.append("ExecutionPace=").append(summary.executionPace()).append('\n');
        builder.append("TotalTests=").append(summary.totalTests()).append('\n');
        builder.append("PassedTests=").append(summary.passed()).append('\n');
        builder.append("FailedTests=").append(summary.failed()).append('\n');
        builder.append("BrokenTests=").append(summary.broken()).append('\n');
        builder.append("SkippedTests=").append(summary.skipped()).append('\n');
        builder.append("OverallOutcome=").append(summary.overallOutcome()).append('\n');
        builder.append("ScreenshotEvidenceLocation=").append(summary.screenshotEvidenceLocation()).append('\n');
        Files.writeString(env, builder.toString());
    }
}
