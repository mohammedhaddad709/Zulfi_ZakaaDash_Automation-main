package com.taf.systems.zakaadash.utils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TestDataUtil {

    public static final String BASELINE_VALID_CSV = "test-data/zakaadash/uploads/baseline/zd_baseline_valid.csv";
    public static final String UNSUPPORTED_TYPE_TXT = "test-data/zakaadash/uploads/invalid-files/zd_unsupported_type.txt";
    public static final String PREPROCESSING_BASELINE_CASE = "test-data/zakaadash/preprocessing/entry/baseline";
    public static final String PREPROCESSING_E2E_CASE = "test-data/zakaadash/preprocessing/e2e/PP-E2E-001";
    public static final String FOOTBALL_RAW_UPLOAD_CSV = "test-data/zakaadash/uploads/football/zd_football_roi_raw.csv";
    public static final String FOOTBALL_EXPECTED_JSON = "test-data/zakaadash/expected/football/expected_results.json";
    public static final String FOOTBALL_GOLDEN_CSV = "test-data/zakaadash/expected/football/zd_football_roi_expected_clean.csv";

    private static final String TEST_DATA_ROOT = "test-data";
    private static final String SYSTEM_FOLDER = "zakaadash";
    private static final DateTimeFormatter NAME_SUFFIX = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private TestDataUtil() {
    }

    /** Unique, human-readable name for data created on the shared DEV environment, e.g. AUTO_PP_X_260929111502. */
    public static String uniqueName(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(NAME_SUFFIX);
    }

    /** Path below test-data/zakaadash (e.g. preprocessing/entry/PP-ENTRY-001/input.csv) for reports. */
    public static String datasetLabel(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        for (int i = 0; i < normalized.getNameCount() - 2; i++) {
            if (TEST_DATA_ROOT.equals(normalized.getName(i).toString())
                    && SYSTEM_FOLDER.equals(normalized.getName(i + 1).toString())) {
                return normalized.subpath(i + 2, normalized.getNameCount()).toString().replace('\\', '/');
            }
        }
        return normalized.getFileName().toString();
    }

    public static Path resolveResource(String classpathLocation) {
        URL resource = TestDataUtil.class.getClassLoader().getResource(classpathLocation);
        if (resource == null) {
            throw new IllegalStateException("Test data resource not found on classpath: " + classpathLocation);
        }
        try {
            return Paths.get(resource.toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Invalid test data resource location: " + resource, ex);
        }
    }
}
