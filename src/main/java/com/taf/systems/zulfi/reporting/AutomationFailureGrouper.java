package com.taf.systems.zulfi.reporting;

import com.taf.core.utils.OutputPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Groups repeated automation failures to avoid duplicate product-defect noise. */
public final class AutomationFailureGrouper {

    private static final Map<String, List<String>> FINGERPRINT_TO_TESTS = new ConcurrentHashMap<>();

    private AutomationFailureGrouper() {}

    public static void recordFailure(String testCaseId, Throwable error) {
        if (testCaseId == null || testCaseId.isBlank() || error == null) {
            return;
        }
        String fingerprint = fingerprint(error);
        FINGERPRINT_TO_TESTS
                .computeIfAbsent(fingerprint, key -> new CopyOnWriteArrayList<>())
                .add(testCaseId);
    }

    public static void writeSummaryIfNeeded() {
        if (FINGERPRINT_TO_TESTS.isEmpty()) {
            return;
        }
        try {
            Path out = OutputPaths.root().resolve("automation-failure-groups.md");
            Files.createDirectories(out.getParent());
            StringBuilder md = new StringBuilder();
            md.append("# Automation failure grouping\n\n");
            md.append("Generated: ").append(Instant.now()).append("\n\n");
            md.append("| Root-cause fingerprint | Affected test count | Sample test IDs |\n");
            md.append("|------------------------|--------------------:|-----------------|\n");
            for (Map.Entry<String, List<String>> entry : sortedByCount().entrySet()) {
                List<String> tests = entry.getValue();
                String sample =
                        tests.size() <= 5
                                ? String.join(", ", tests)
                                : String.join(", ", tests.subList(0, 5)) + ", …";
                md.append("| ")
                        .append(entry.getKey().replace("|", "/"))
                        .append(" | ")
                        .append(tests.size())
                        .append(" | ")
                        .append(sample)
                        .append(" |\n");
            }
            md.append("\n> One investigation per fingerprint is sufficient when classification is AUTOMATION_DEFECT.\n");
            Files.writeString(out, md.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static LinkedHashMap<String, List<String>> sortedByCount() {
        List<Map.Entry<String, List<String>>> entries = new ArrayList<>(FINGERPRINT_TO_TESTS.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        LinkedHashMap<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    static String fingerprint(Throwable error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.contains("teamFieldsReadOnly=false")
                || message.contains("Team Fields Are Non-Editable")) {
            return "AUTOMATION:teamFieldsNonEditable_check_false_positive";
        }
        if (message.contains("Home Team Verification Failed")) {
            return "AUTOMATION:home_team_display_mismatch";
        }
        if (message.contains("Away Team Verification Failed")) {
            return "AUTOMATION:away_team_display_mismatch";
        }
        if (message.length() > 120) {
            return error.getClass().getSimpleName() + ":" + message.substring(0, 120);
        }
        return error.getClass().getSimpleName() + ":" + message;
    }
}
