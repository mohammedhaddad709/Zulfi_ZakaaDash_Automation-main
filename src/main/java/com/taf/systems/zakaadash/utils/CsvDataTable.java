package com.taf.systems.zakaadash.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Minimal RFC4180-style CSV reader for football oracle verification (no external CSV library). */
public final class CsvDataTable {

    private final List<String> headers;
    private final List<Map<String, String>> rows;

    private CsvDataTable(List<String> headers, List<Map<String, String>> rows) {
        this.headers = List.copyOf(headers);
        this.rows = List.copyOf(rows);
    }

    public static CsvDataTable read(Path csvFile) throws IOException {
        String raw = Files.readString(csvFile, StandardCharsets.UTF_8);
        if (raw.startsWith("\uFEFF")) {
            raw = raw.substring(1);
        }
        List<String> lines = raw.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            throw new IOException("CSV is empty: " + csvFile);
        }
        List<String> headers = parseLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> values = parseLine(lines.get(index));
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                row.put(headers.get(column), column < values.size() ? values.get(column) : "");
            }
            rows.add(row);
        }
        return new CsvDataTable(headers, rows);
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public List<String> headers() {
        return headers;
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return headers.size();
    }

    public List<Map<String, String>> rows() {
        return rows;
    }

    public Map<String, Map<String, String>> indexBy(String keyColumn) {
        Map<String, Map<String, String>> indexed = new TreeMap<>();
        for (Map<String, String> row : rows) {
            String key = row.get(keyColumn);
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Row missing key column " + keyColumn);
            }
            if (indexed.put(key, row) != null) {
                throw new IllegalStateException("Duplicate key in column " + keyColumn + ": " + key);
            }
        }
        return indexed;
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (inQuotes) {
                if (ch == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    public static boolean numericEquals(String left, String right, double tolerance) {
        Double leftNumber = parseNumber(left);
        Double rightNumber = parseNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Math.abs(leftNumber - rightNumber) <= tolerance;
        }
        return normalizeText(left).equals(normalizeText(right));
    }

    public static Double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static boolean isBlankCell(String value) {
        return value == null || value.isBlank();
    }

    public double sumColumn(String column) {
        double sum = 0;
        for (Map<String, String> row : rows) {
            Double number = parseNumber(row.get(column));
            if (number != null) {
                sum += number;
            }
        }
        return sum;
    }

    public double weightedRoiPct() {
        double revenue = sumColumn("total_revenue_eur");
        double cost = sumColumn("total_cost_eur");
        if (cost == 0) {
            return 0;
        }
        return 100.0 * (revenue - cost) / cost;
    }

    public String summaryForReport() {
        return String.format(Locale.ROOT, "rows=%d, columns=%d", rowCount(), columnCount());
    }
}
