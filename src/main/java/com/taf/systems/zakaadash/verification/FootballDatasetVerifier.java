package com.taf.systems.zakaadash.verification;

import com.taf.systems.zakaadash.testdata.FootballExpectedResults;
import com.taf.systems.zakaadash.utils.CsvDataTable;
import io.qameta.allure.Allure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class FootballDatasetVerifier {

    private FootballDatasetVerifier() {
    }

    public static VerificationReport verifyRawFixture(Path rawCsv, FootballExpectedResults expected) throws IOException {
        String hash = CsvDataTable.sha256(rawCsv);
        CsvDataTable raw = CsvDataTable.read(rawCsv);
        List<String> issues = new ArrayList<>();
        if (!expected.rawSha256().equalsIgnoreCase(hash)) {
            issues.add("raw SHA-256 mismatch: expected " + expected.rawSha256() + " actual " + hash);
        }
        if (raw.rowCount() != expected.rawRows()) {
            issues.add("raw row count: expected " + expected.rawRows() + " actual " + raw.rowCount());
        }
        if (raw.columnCount() != expected.columns()) {
            issues.add("raw column count: expected " + expected.columns() + " actual " + raw.columnCount());
        }
        return new VerificationReport("raw-fixture", issues.isEmpty(), issues, raw.summaryForReport());
    }

    public static VerificationReport compareProcessedToGolden(
            CsvDataTable processed,
            CsvDataTable golden,
            FootballExpectedResults expected) {
        List<String> issues = new ArrayList<>();
        if (processed.columnCount() != expected.columns() || golden.columnCount() != expected.columns()) {
            issues.add("column count expected " + expected.columns()
                    + " processed=" + processed.columnCount() + " golden=" + golden.columnCount());
        }
        if (!processed.headers().equals(golden.headers())) {
            issues.add("column name mismatch processed=" + processed.headers() + " golden=" + golden.headers());
        }
        if (processed.rowCount() != expected.expectedRows()) {
            issues.add("processed row count expected " + expected.expectedRows() + " actual " + processed.rowCount());
        }
        Map<String, Map<String, String>> processedById = processed.indexBy(expected.comparisonKey());
        Map<String, Map<String, String>> goldenById = golden.indexBy(expected.comparisonKey());
        if (processedById.size() != expected.expectedRows()) {
            issues.add("unique " + expected.comparisonKey() + " count=" + processedById.size());
        }
        Set<String> missingIds = goldenById.keySet().stream()
                .filter(id -> !processedById.containsKey(id))
                .collect(Collectors.toSet());
        Set<String> extraIds = processedById.keySet().stream()
                .filter(id -> !goldenById.containsKey(id))
                .collect(Collectors.toSet());
        if (!missingIds.isEmpty()) {
            issues.add("missing record_ids in processed output: " + missingIds);
        }
        if (!extraIds.isEmpty()) {
            issues.add("extra record_ids in processed output: " + extraIds);
        }
        for (String recordId : goldenById.keySet()) {
            Map<String, String> expectedRow = goldenById.get(recordId);
            Map<String, String> actualRow = processedById.get(recordId);
            if (actualRow == null) {
                continue;
            }
            for (String header : golden.headers()) {
                String expectedValue = expectedRow.get(header);
                String actualValue = actualRow.get(header);
                if (CsvDataTable.isBlankCell(actualValue) && !CsvDataTable.isBlankCell(expectedValue)) {
                    issues.add("blank cell record_id=" + recordId + " column=" + header);
                    continue;
                }
                if ("roi_pct".equals(header)) {
                    if (!CsvDataTable.numericEquals(actualValue, expectedValue, expected.roiTolerance())) {
                        issues.add("roi_pct mismatch record_id=" + recordId
                                + " expected=" + expectedValue + " actual=" + actualValue);
                    }
                } else if (header.endsWith("_eur") || header.contains("attendance") || header.contains("matches")
                        || header.contains("goals") || header.contains("points") || header.contains("wins")
                        || header.contains("draws") || header.contains("losses")) {
                    if (!CsvDataTable.numericEquals(actualValue, expectedValue, expected.moneyTolerance())) {
                        issues.add("numeric mismatch record_id=" + recordId + " column=" + header
                                + " expected=" + expectedValue + " actual=" + actualValue);
                    }
                } else if (!CsvDataTable.normalizeText(actualValue).equals(CsvDataTable.normalizeText(expectedValue))) {
                    issues.add("text mismatch record_id=" + recordId + " column=" + header
                            + " expected=" + expectedValue + " actual=" + actualValue);
                }
            }
        }
        verifyFilledCells(processed, expected, issues);
        verifyBusinessKeyUnique(processed, expected, issues);
        verifyFinancialTotals(processed, expected, issues);
        attachComparisonSummary(processed, golden, issues);
        return new VerificationReport("processed-vs-golden", issues.isEmpty(), issues, processed.summaryForReport());
    }

    private static void verifyFilledCells(CsvDataTable processed, FootballExpectedResults expected, List<String> issues) {
        Map<String, Map<String, String>> byId = processed.indexBy(expected.comparisonKey());
        for (Map<String, Object> missing : expected.missingCells()) {
            String recordId = String.valueOf(missing.get("record_id"));
            String column = String.valueOf(missing.get("column"));
            Object expectedValue = missing.get("expected_value");
            Map<String, String> row = byId.get(recordId);
            if (row == null) {
                issues.add("filled-cell check missing record_id=" + recordId);
                continue;
            }
            String actual = row.get(column);
            if (!CsvDataTable.numericEquals(actual, String.valueOf(expectedValue), expected.moneyTolerance())) {
                issues.add("filled cell record_id=" + recordId + " column=" + column
                        + " expected=" + expectedValue + " actual=" + actual);
            }
        }
    }

    private static void verifyBusinessKeyUnique(CsvDataTable processed, FootballExpectedResults expected, List<String> issues) {
        List<String> key = expected.businessKey();
        Map<String, Long> counts = processed.rows().stream()
                .map(row -> key.stream().map(row::get).collect(Collectors.joining("|")))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        List<String> duplicates = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicates.isEmpty()) {
            issues.add("duplicate business keys: " + duplicates);
        }
    }

    private static void verifyFinancialTotals(CsvDataTable processed, FootballExpectedResults expected, List<String> issues) {
        double revenue = processed.sumColumn("total_revenue_eur");
        double cost = processed.sumColumn("total_cost_eur");
        double net = processed.sumColumn("net_return_eur");
        double expectedRevenue = ((Number) expected.totals().get("total_revenue_eur")).doubleValue();
        double expectedCost = ((Number) expected.totals().get("total_cost_eur")).doubleValue();
        double expectedNet = ((Number) expected.totals().get("net_return_eur")).doubleValue();
        double tol = expected.moneyTolerance();
        if (Math.abs(revenue - expectedRevenue) > tol) {
            issues.add("total_revenue_eur sum expected " + expectedRevenue + " actual " + revenue);
        }
        if (Math.abs(cost - expectedCost) > tol) {
            issues.add("total_cost_eur sum expected " + expectedCost + " actual " + cost);
        }
        if (Math.abs(net - expectedNet) > tol) {
            issues.add("net_return_eur sum expected " + expectedNet + " actual " + net);
        }
        double weighted = processed.weightedRoiPct();
        if (Math.abs(weighted - expected.weightedRoiPct()) > expected.roiTolerance()) {
            issues.add("weighted ROI expected " + expected.weightedRoiPct() + " actual " + weighted);
        }
        for (Map<String, String> row : processed.rows()) {
            Double rowRevenue = CsvDataTable.parseNumber(row.get("total_revenue_eur"));
            Double rowCost = CsvDataTable.parseNumber(row.get("total_cost_eur"));
            Double rowNet = CsvDataTable.parseNumber(row.get("net_return_eur"));
            Double rowRoi = CsvDataTable.parseNumber(row.get("roi_pct"));
            if (rowCost == null || rowCost == 0 || rowRevenue == null || rowNet == null || rowRoi == null) {
                continue;
            }
            double recomputedNet = rowRevenue - rowCost;
            if (Math.abs(recomputedNet - rowNet) > tol) {
                issues.add("net_return_eur recompute record_id=" + row.get("record_id")
                        + " expected=" + recomputedNet + " actual=" + rowNet);
            }
            double recomputedRoi = 100.0 * recomputedNet / rowCost;
            if (Math.abs(recomputedRoi - rowRoi) > expected.roiTolerance()) {
                issues.add("roi_pct recompute record_id=" + row.get("record_id")
                        + " expected=" + recomputedRoi + " actual=" + rowRoi);
            }
        }
    }

    private static void attachComparisonSummary(CsvDataTable processed, CsvDataTable golden, List<String> issues) {
        StringBuilder report = new StringBuilder();
        report.append("Processed: ").append(processed.summaryForReport()).append('\n');
        report.append("Golden: ").append(golden.summaryForReport()).append('\n');
        report.append("Issues (").append(issues.size()).append("):\n");
        for (String issue : issues.stream().limit(50).toList()) {
            report.append("- ").append(issue).append('\n');
        }
        Allure.addAttachment("Football dataset verification", "text/plain", report.toString(), ".txt");
    }

    public record VerificationReport(String phase, boolean passed, List<String> issues, String summary) {

        public String diagnosticText() {
            return phase + " passed=" + passed + " " + summary + " issues=" + issues;
        }
    }
}
