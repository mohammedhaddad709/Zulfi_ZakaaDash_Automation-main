package com.taf.core.utils.dataReader;

import com.taf.core.utils.logs.LogsManager;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads {@code test-data/<system>/<name>.json} from the test classpath.
 *
 * <p>Per-run overrides: {@code -Ddata.<system>.<name>.<path>} (e.g.
 * {@code -Ddata.zakaadash.login-data.admin.role=viewer}) wins over the file value. Never put
 * passwords in these files; credentials come from each system's environment-based provider.
 */
public class JsonReader {
    private static final String TEST_DATA_ROOT = "test-data/";
    private final String overridePrefix;
    private final String json;

    /** Legacy form: {@code name} is relative to {@code test-data/} (may include a system folder). */
    public JsonReader(String jsonFileName) {
        this.overridePrefix = "data." + jsonFileName.replace('/', '.') + ".";
        this.json = read(TEST_DATA_ROOT + jsonFileName + ".json");
    }

    public JsonReader(String system, String jsonFileName) {
        this.overridePrefix = "data." + system + "." + jsonFileName + ".";
        this.json = read(TEST_DATA_ROOT + system + "/" + jsonFileName + ".json");
    }

    // valid.username
    public String getJsonData(String jsonPath) {
        String override = System.getProperty(overridePrefix + jsonPath);
        if (override != null) return override;
        try {
            Object value = JsonPath.read(json, jsonPath);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            LogsManager.error("Error reading json path:", jsonPath, e.getMessage());
            return "";
        }
    }

    private static String read(String resource) {
        try (InputStream stream = JsonReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                LogsManager.error("Test data resource not found on classpath:", resource);
                return "{}";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogsManager.error("Error reading json file:", resource, e.getMessage());
            return "{}";
        }
    }
}
