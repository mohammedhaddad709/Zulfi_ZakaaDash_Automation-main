package com.taf.systems.zakaadash.testdata;

import com.taf.systems.zakaadash.utils.TestDataUtil;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Oracle metadata for E2E-PP-FOOTBALL-001 loaded from expected_results.json. */
public record FootballExpectedResults(
        String testCaseId,
        int rawRows,
        int expectedRows,
        int columns,
        int expectedDuplicateRowsRemoved,
        List<String> duplicatedRecordIds,
        List<Map<String, Object>> missingCells,
        int expectedNullCells,
        List<String> businessKey,
        String comparisonKey,
        Map<String, Object> numericTolerance,
        Map<String, Object> totals,
        double weightedRoiPct,
        String rawSha256,
        String expectedSha256) {

    @SuppressWarnings("unchecked")
    public static FootballExpectedResults load(Path jsonFile) {
        Map<String, Object> json = readJson(jsonFile);
        return new FootballExpectedResults(
                String.valueOf(json.get("test_case_id")),
                intValue(json, "raw_rows"),
                intValue(json, "expected_rows"),
                intValue(json, "columns"),
                intValue(json, "expected_duplicate_rows_removed"),
                (List<String>) json.get("duplicated_record_ids"),
                (List<Map<String, Object>>) json.get("missing_cells"),
                intValue(json, "expected_null_cells"),
                (List<String>) json.get("business_key"),
                String.valueOf(json.get("comparison_key")),
                (Map<String, Object>) json.get("numeric_tolerance"),
                (Map<String, Object>) json.get("totals"),
                doubleValue(json, "weighted_roi_pct"),
                String.valueOf(json.get("raw_sha256")),
                String.valueOf(json.get("expected_sha256")));
    }

    public static FootballExpectedResults loadFromClasspath() {
        return load(TestDataUtil.resolveResource(TestDataUtil.FOOTBALL_EXPECTED_JSON));
    }

    public double moneyTolerance() {
        Object value = numericTolerance.get("money_eur");
        return value instanceof Number number ? number.doubleValue() : 0.01;
    }

    public double roiTolerance() {
        Object value = numericTolerance.get("roi_pct");
        return value instanceof Number number ? number.doubleValue() : 0.00001;
    }

    private static Map<String, Object> readJson(Path caseFile) {
        try {
            return new Json().toType(Files.readString(caseFile), Json.MAP_TYPE);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + caseFile, ex);
        }
    }

    private static int intValue(Map<String, Object> json, String key) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("Missing or invalid int field: " + key);
    }

    private static double doubleValue(Map<String, Object> json, String key) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Missing or invalid double field: " + key);
    }
}
