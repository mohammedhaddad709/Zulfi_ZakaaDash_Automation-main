package com.taf.core.utils.dataReader;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Shared TAF configuration: base resources plus the selected system profile
 * ({@code systems/<active.system>.properties}). JVM {@code -D} properties always override file defaults.
 * System-specific code should read its own profile through {@link SystemProfile} instead.
 */
public final class PropertyReader {
    private static final Properties DEFAULTS = new Properties();
    private static volatile boolean loaded;
    private PropertyReader() { }

    public static synchronized Properties loadProperties() {
        if (!loaded) {
            for (String resource : List.of("webapp.properties", "environment.properties",
                    "waits.properties", "video.properties", "seleniumGrid.properties")) {
                load(resource, DEFAULTS);
            }
            load("systems/" + activeSystem() + ".properties", DEFAULTS);
            loaded = true;
        }
        Properties result = new Properties();
        result.putAll(DEFAULTS);
        result.putAll(System.getProperties());
        return result;
    }

    public static String getProperty(String key) {
        loadProperties();
        String cli = System.getProperty(key);
        if (cli != null) return cli;
        return DEFAULTS.getProperty(key, "");
    }

    /** Selected system for this JVM: {@code -Dactive.system}, else {@code environment.properties}. */
    public static String activeSystem() {
        String cli = System.getProperty("active.system");
        if (cli != null && !cli.isBlank()) return cli.trim();
        Properties environment = new Properties();
        load("environment.properties", environment);
        return environment.getProperty("active.system", "zulfi").trim();
    }

    static void load(String resource, Properties target) {
        try (InputStream stream = PropertyReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream != null) target.load(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load " + resource, ex);
        }
    }
}
