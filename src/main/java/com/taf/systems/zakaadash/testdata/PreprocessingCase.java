package com.taf.systems.zakaadash.testdata;

import com.taf.systems.zakaadash.utils.TestDataUtil;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * One controlled Preprocessing scenario loaded from {@code <case folder>/case.json}.
 * {@code settings} and {@code expected} stay untyped until a step gets detailed coverage;
 * then introduce a small typed record for that step only.
 */
public record PreprocessingCase(
        String testCaseId,
        String title,
        Path datasetFile,
        String sourceNamePrefix,
        String category,
        Map<String, Object> settings,
        Map<String, Object> expected) {

    private static final String CASE_FILE = "case.json";
    private static final String SOURCE_NAME_PREFIX_ROOT = "AUTO_PP_";

    public static PreprocessingCase load(String classpathCaseFolder) {
        Path folder = TestDataUtil.resolveResource(classpathCaseFolder);
        Path caseFile = folder.resolve(CASE_FILE);
        Map<String, Object> json = readJson(caseFile);

        Path datasetFile = folder.resolve(requiredText(json, "dataset", caseFile));
        if (!Files.isRegularFile(datasetFile)) {
            throw new IllegalStateException("Dataset declared in " + caseFile + " does not exist: " + datasetFile);
        }
        String sourceNamePrefix = requiredText(json, "sourceNamePrefix", caseFile);
        if (!sourceNamePrefix.startsWith(SOURCE_NAME_PREFIX_ROOT)) {
            throw new IllegalStateException("sourceNamePrefix in " + caseFile + " must start with "
                    + SOURCE_NAME_PREFIX_ROOT + " so generated sources are identifiable: " + sourceNamePrefix);
        }
        return new PreprocessingCase(
                requiredText(json, "testCaseId", caseFile),
                requiredText(json, "title", caseFile),
                datasetFile,
                sourceNamePrefix,
                String.valueOf(json.getOrDefault("category", "")),
                objectOrEmpty(json, "settings"),
                objectOrEmpty(json, "expected"));
    }

    public String datasetLabel() {
        return testCaseId + "/" + datasetFile.getFileName();
    }

    public String requiredExpectedText(String key) {
        Object value = expected.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("case.json of " + testCaseId + " has no expected." + key);
        }
        return value.toString();
    }

    private static Map<String, Object> readJson(Path caseFile) {
        try {
            return new Json().toType(Files.readString(caseFile), Json.MAP_TYPE);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + caseFile, ex);
        }
    }

    private static String requiredText(Map<String, Object> json, String key, Path caseFile) {
        Object value = json.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException(caseFile + " is missing required field: " + key);
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectOrEmpty(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
