package com.taf.systems.zulfi.reporting;

import com.taf.core.pacing.ExecutionPace;
import java.util.Arrays;
import com.taf.core.media.EvidenceRegistry;
import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.systems.zulfi.config.PortalConfig;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Local defect investigation report (Markdown) built only from what the run actually observed: the executed
 * steps, the failing step's expected vs actual, the explicit failure classification, the concrete test data and
 * the real evidence files (screenshots, browser video). No external ticket is created.
 */
public final class EnhancedBugReportWriter {

    private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(password|passwd|secret|token|credential|cookie).*");
    private static final List<String> RUN_PROPERTY_PREFIXES = List.of(
            "match.schedule.", "analysis.", "anl.", "home.", "mch.", "club", "report.video", "execution.pace",
            "executionType", "browserType", "zulfi.baseUrl");

    private EnhancedBugReportWriter() {}

    public static Path writeMarkdown(FailureDiagnostics diagnostics, String testCaseId, String feature) {
        return writeMarkdown(diagnostics, testCaseId, feature, "<TestClass>#<method>");
    }

    /** @param testReference {@code SimpleClassName#method}, used in the re-run command. */
    public static Path writeMarkdown(
            FailureDiagnostics diagnostics, String testCaseId, String feature, String testReference) {
        try {
            Path dir = OutputPaths.bugReports();
            Files.createDirectories(dir);
            String artifactId = "LOCAL-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
            Path file = dir.resolve(artifactId + "-" + safe(testCaseId) + ".md");
            String body = render(diagnostics, testCaseId, feature, artifactId, file, testReference);
            Files.writeString(file, body);
            Allure.addAttachment("Bug report — " + testCaseId, "text/markdown", body);
            return file;
        } catch (IOException ex) {
            return null;
        }
    }

    static String render(
            FailureDiagnostics d, String testCaseId, String feature, String artifactId, Path reportFile,
            String testReference) {
        TestCaseMetadata meta = TestExecutionContext.metadata()
                .orElse(TestCaseMetadata.minimal(testCaseId, testCaseId, feature, ""));
        List<TestExecutionContext.StepRecord> steps = TestExecutionContext.steps();
        String classificationReason = TestExecutionContext.classificationReason();

        StringBuilder md = new StringBuilder();
        md.append("# [BUG] ").append(testCaseId).append(" — ").append(meta.title()).append('\n');
        md.append("\n> **Summary:** ").append(summary(meta, d)).append("\n\n");

        md.append("| Field | Value |\n|---|---|\n");
        row(md, "Bug ID (local)", artifactId);
        row(md, "Test case", testCaseId + " — " + meta.title());
        row(md, "Feature", meta.feature());
        row(md, "User story", meta.userStoryId());
        row(md, "Acceptance criteria", String.join("; ", meta.acceptanceCriteria()));
        row(md, "Business rules", String.join(", ", meta.businessRules()));
        row(md, "Classification", d.classification() + " — " + meaning(d.classification()));
        row(md, "Priority", meta.priority().isBlank() ? "" : meta.priority() + " (Allure: " + meta.allureSeverity() + ")");
        row(md, "Failed at", d.failedStep());
        row(md, "Page at failure", shortUrl(d.browserUrl()));
        row(md, "Detected (UTC)", DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.now()));

        md.append("\n## Environment\n\n");
        md.append("| Item | Value |\n|---|---|\n");
        row(md, "Portal URL", safePortalUrl());
        row(md, "Browser", PropertyReader.getProperty("browserType"));
        row(md, "Execution", PropertyReader.getProperty("executionType"));
        row(md, "OS", System.getProperty("os.name"));
        row(md, "Execution pace", ExecutionPace.current().name());

        md.append("\n## Preconditions\n\n").append(meta.preconditionsMarkdown()).append('\n');

        Map<String, String> data = TestExecutionContext.testData();
        md.append("\n## Test data used\n\n");
        if (data.isEmpty()) {
            md.append(meta.testDataReference().isBlank() ? "_No test data recorded._\n" : meta.testDataReference() + '\n');
        } else {
            md.append("| Data | Value |\n|---|---|\n");
            data.forEach((key, value) -> row(md, key, value));
        }

        md.append("\n## Steps to reproduce\n\n");
        if (steps.isEmpty()) {
            md.append("_The failure happened before the first reported step (setup or navigation). See the exception below._\n");
        } else {
            for (TestExecutionContext.StepRecord step : steps) {
                String marker = switch (step.status()) {
                    case "PASS" -> "✅";
                    case "FAIL" -> "❌";
                    default -> "⏭️";
                };
                md.append(step.number()).append(". ").append(marker).append(" **").append(oneLine(step.title())).append("** — ")
                        .append(oneLine(step.action())).append("FAIL".equals(step.status()) ? "  ← **fails here**" : "");
                md.append("  \n   _Expected:_ ").append(oneLine(step.expected()));
                if (!"PASS".equals(step.status()) && step.actual() != null && !step.actual().isBlank()) {
                    md.append("  \n   _Actual:_ ").append(oneLine(step.actual()));
                }
                md.append('\n');
            }
        }

        List<TestExecutionContext.StepRecord> failedChecks = TestExecutionContext.failedSteps();
        if (failedChecks.size() > 1) {
            md.append("\n## All failed checks (").append(failedChecks.size()).append(")\n\n");
            md.append("The test kept running after the first failure so every finding is reported in one run.\n\n");
            md.append("| # | Check | Expected | Actual |\n|---|---|---|---|\n");
            for (TestExecutionContext.StepRecord failed : failedChecks) {
                md.append("| ").append(failed.number()).append(" | ").append(oneLine(failed.title()).replace("|", "\\|")).append(" | ")
                        .append(oneLine(failed.expected()).replace("|", "\\|")).append(" | ")
                        .append(oneLine(failed.actual()).replace("|", "\\|")).append(" |\n");
            }
        }
        md.append("\n## Expected result (primary failure)\n\n").append(nonBlank(d.expectedBehavior())).append('\n');
        md.append("\n## Actual result (primary failure)\n\n").append(nonBlank(d.actualBehavior())).append('\n');

        md.append("\n## Root cause analysis\n\n");
        md.append("- **Classification:** ").append(d.classification()).append(" — ").append(meaning(d.classification())).append('\n');
        if (!classificationReason.isBlank()) {
            md.append("- **Why this classification:** ").append(classificationReason).append('\n');
        }
        md.append("- **Where it failed:** ").append(d.failedStep()).append(" on `").append(shortUrl(d.browserUrl())).append("`\n");
        md.append("- **Technical detail:** `").append(d.exceptionType()).append("`\n");
        md.append("\n```\n").append(firstLines(d.actualBehavior(), 12)).append("\n```\n");

        md.append("\n## Evidence\n\n").append(evidence(reportFile, steps));

        md.append("\n## How to re-run this test\n\n```\n").append(rerunCommand(testCaseId, testReference)).append("\n```\n");
        md.append("\n> Local investigation artifact generated by the automation run. No Azure DevOps / Jira ticket was created.\n");
        return md.toString();
    }

    private static String summary(TestCaseMetadata meta, FailureDiagnostics d) {
        String technicalToast = d.failureReason() == null ? "" : d.failureReason().toLowerCase();
        if (d.classification() == DefectClassification.PRODUCT_DEFECT && technicalToast.contains("technical toast")) {
            return "Instead of a clear Arabic message about the duplicate analysis, the portal showed a technical English "
                    + "error (externalEventId/matchId) to the user.";
        }
        return "While verifying \"" + meta.title() + "\", the step \"" + d.failedStep() + "\" expected: "
                + oneLine(d.expectedBehavior()) + " — but observed: " + oneLine(d.actualBehavior()) + ".";
    }

    static String meaning(DefectClassification classification) {
        return switch (classification) {
            case PRODUCT_DEFECT -> "the portal behaves differently from the user story (raise to development after review)";
            case AUTOMATION_DEFECT -> "the test code failed; fix automation before triaging the product";
            case TEST_DATA_ERROR -> "required test data was missing or not in the expected state";
            case CONFIGURATION_ERROR -> "run configuration (flags, credentials, target) is missing or invalid";
            case ENVIRONMENT_ERROR -> "browser, network or environment instability; rerun first";
            case INCONCLUSIVE -> "needs manual triage using the evidence below (product change, slowness or locator drift)";
        };
    }

    private static String evidence(Path reportFile, List<TestExecutionContext.StepRecord> steps) {
        StringBuilder md = new StringBuilder();
        List<EvidenceRegistry.Evidence> videos = EvidenceRegistry.current(EvidenceRegistry.Kind.VIDEO);
        if (videos.isEmpty()) {
            md.append("- **Video:** not recorded for this run (enable with `-Dreport.video=ON_FAILURE`, Chromium only).\n");
        } else {
            for (EvidenceRegistry.Evidence video : videos) {
                md.append("- **Video:** [").append(video.path().getFileName()).append("](").append(link(reportFile, video.path())).append(")\n");
            }
        }
        List<EvidenceRegistry.Evidence> shots = EvidenceRegistry.current(EvidenceRegistry.Kind.SCREENSHOT);
        steps.stream().filter(s -> "FAIL".equals(s.status()) && s.screenshot() != null).reduce((a, b) -> b)
                .ifPresent(failing -> md.append("- **Screenshot at the failing step:**\n\n  ![failing step](")
                        .append(link(reportFile, failing.screenshot())).append(")\n"));
        if (shots.isEmpty()) {
            md.append("- **Screenshots:** none captured (browser not available at failure time).\n");
        } else {
            md.append("- **All screenshots (capture order):**\n");
            for (EvidenceRegistry.Evidence shot : shots) {
                md.append("  - ").append(shot.label()).append(": [").append(shot.path().getFileName()).append("](")
                        .append(link(reportFile, shot.path())).append(")\n");
            }
        }
        md.append("- **Allure:** open the run's Allure report and filter by the test case ID; every step has an "
                + "expected-vs-actual table and its screenshot.\n");
        return md.toString();
    }

    private static String rerunCommand(String testCaseId, String testReference) {
        List<String> props = new ArrayList<>();
        System.getProperties().forEach((key, value) -> {
            String k = String.valueOf(key);
            if (RUN_PROPERTY_PREFIXES.stream().anyMatch(k::startsWith) && !SECRET_KEY.matcher(k).matches()) {
                props.add("\"-D" + k + "=" + value + "\"");
            }
        });
        props.sort(String::compareTo);
        return "mvn test \"-Dtest=" + testReference + "\" \"-Dactive.system=zulfi\" " + String.join(" ", props)
                + "\n# test case: " + testCaseId + " (credentials come from env / config/credentials.local.properties)";
    }

    private static String link(Path reportFile, Path target) {
        try {
            return reportFile.toAbsolutePath().getParent().relativize(target.toAbsolutePath()).toString().replace('\\', '/');
        } catch (IllegalArgumentException differentRoot) {
            return target.toAbsolutePath().toString().replace('\\', '/');
        }
    }

    private static String safePortalUrl() {
        try {
            return PortalConfig.baseUrl();
        } catch (RuntimeException ex) {
            return "(not configured: " + ex.getMessage() + ")";
        }
    }

    private static void row(StringBuilder md, String field, String value) {
        if (value != null && !value.isBlank()) {
            md.append("| ").append(field).append(" | ").append(oneLine(value).replace("|", "\\|")).append(" |\n");
        }
    }

    /** Keeps the report readable: data: URLs and very long URLs are shortened. */
    private static String shortUrl(String url) {
        if (url == null) return "";
        if (url.startsWith("data:")) return "data: URL (local test page)";
        return url.length() > 160 ? url.substring(0, 157) + "..." : url;
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s*\\R\\s*", " ").trim();
    }

    private static String firstLines(String value, int max) {
        if (value == null) return "";
        String[] lines = value.split("\\R");
        return String.join("\n", Arrays.copyOf(lines, Math.min(max, lines.length)));
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? "_Not captured._" : value.trim();
    }

    private static String safe(String testCaseId) {
        return testCaseId == null || testCaseId.isBlank() ? "UNKNOWN" : testCaseId.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
