package com.taf.systems.zulfi.reporting;

import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TestCaseMetadata(
        String id,
        String title,
        String feature,
        String userStoryId,
        String requirementId,
        String description,
        String testObjective,
        String priority,
        String testType,
        List<String> preconditions,
        String testDataReference,
        String expectedFinalResult,
        List<String> acceptanceCriteria,
        List<String> businessRules) {

    private static final Pattern ID_TITLE =
            Pattern.compile("^([A-Z0-9]+(?:-[A-Z0-9]+)*)\\s*[—\\-]\\s*(.+)$");

    public static TestCaseMetadata fromAnnotation(TestCaseReport report) {
        return new TestCaseMetadata(
                report.id(),
                report.title(),
                report.feature(),
                blankToEmpty(report.userStoryId()),
                blankToEmpty(report.requirementId()),
                report.description(),
                report.testObjective(),
                blankToEmpty(report.priority()),
                report.testType(),
                List.of(report.preconditions()),
                blankToEmpty(report.testDataReference()),
                blankToEmpty(report.expectedFinalResult()),
                List.of(report.acceptanceCriteria()),
                List.of(report.businessRules()));
    }

    /** Parses TestNG {@code @Test(description)} values such as {@code LOGIN-HP-001 — Title here}. */
    public static TestCaseMetadata fromDescription(String description, String methodNameFallback, String feature) {
        if (description != null && !description.isBlank()) {
            Matcher matcher = ID_TITLE.matcher(description.trim());
            if (matcher.matches()) {
                return minimal(
                        matcher.group(1).trim(),
                        matcher.group(2).trim(),
                        feature,
                        description.trim());
            }
            return minimal(methodNameFallback, description.trim(), feature, description.trim());
        }
        return minimal(methodNameFallback, methodNameFallback, feature, "");
    }

    public static TestCaseMetadata minimal(String id, String title, String feature, String description) {
        return new TestCaseMetadata(
                id,
                title,
                feature == null || feature.isBlank() ? "Unknown" : feature,
                "",
                "",
                description == null ? "" : description,
                "",
                "",
                "Functional",
                List.of(),
                "",
                "",
                List.of(),
                List.of());
    }

    /** Allure severity label for the documented priority (P1/P2/P3 or legacy words). */
    public String allureSeverity() {
        String p = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        if (p.startsWith("P1") || p.contains("CRITICAL") || p.contains("HIGH")) return "critical";
        if (p.startsWith("P2") || p.contains("NORMAL") || p.contains("MEDIUM")) return "normal";
        if (p.startsWith("P3") || p.contains("MINOR") || p.contains("LOW")) return "minor";
        return "";
    }

    /** True when the metadata came from a full {@link TestCaseReport} specification (not a parsed description). */
    public boolean isFullSpecification() {
        return !testObjective.isBlank() || !acceptanceCriteria.isEmpty() || !expectedFinalResult.isBlank();
    }

    /** Single markdown specification block used by Allure and the bug report. */
    public String specificationMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("| Field | Value |\n|---|---|\n");
        row(md, "Test Case ID", id);
        row(md, "Title", title);
        row(md, "Feature", feature);
        row(md, "User story", userStoryId);
        row(md, "Acceptance criteria", String.join("; ", acceptanceCriteria));
        row(md, "Business rules", String.join(", ", businessRules));
        row(md, "Priority", priority);
        row(md, "Test type", testType);
        row(md, "Objective", testObjective);
        row(md, "Test data", testDataReference);
        md.append("\n**Description**\n\n")
                .append(description == null || description.isBlank() ? "_None._" : description).append('\n');
        md.append("\n**Preconditions**\n\n").append(preconditionsMarkdown()).append('\n');
        md.append("\n**Expected final result**\n\n")
                .append(expectedFinalResult.isBlank() ? "_See step expectations._" : expectedFinalResult).append('\n');
        return md.toString();
    }

    private static void row(StringBuilder md, String field, String value) {
        if (value != null && !value.isBlank()) {
            md.append("| ").append(field).append(" | ").append(value.replace("|", "\\|")).append(" |\n");
        }
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    public String preconditionsMarkdown() {
        if (preconditions.isEmpty()) {
            return "_None documented._";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < preconditions.size(); i++) {
            builder.append(i + 1).append(". ").append(preconditions.get(i)).append('\n');
        }
        return builder.toString().trim();
    }

    @Override
    public String toString() {
        return id + " — " + title;
    }
}
