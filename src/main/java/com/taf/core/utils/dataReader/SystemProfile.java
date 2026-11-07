package com.taf.core.utils.dataReader;

import java.net.URI;
import java.util.Properties;

/**
 * Namespaced configuration for one system, read directly from {@code systems/<system>.properties}
 * so it works regardless of {@code active.system}. Precedence: JVM {@code -D} property, then file default.
 */
public final class SystemProfile {
    private final String system;
    private final Properties defaults = new Properties();

    public SystemProfile(String system) {
        this.system = system;
        PropertyReader.load("systems/" + system + ".properties", defaults);
    }

    public String system() { return system; }

    /** Value or empty string when neither a JVM property nor a file default exists. */
    public String get(String key) {
        String cli = System.getProperty(key);
        if (cli != null) return cli;
        return defaults.getProperty(key, "");
    }

    public String get(String key, String fallback) {
        String value = get(key);
        return value.isBlank() ? fallback : value;
    }

    public String require(String key) {
        String value = get(key).trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required setting '" + key + "' for system '" + system
                    + "'. Set -D" + key + "=... or add it to systems/" + system + ".properties");
        }
        return value;
    }

    public int requireInt(String key) {
        String value = require(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Setting '" + key + "' for system '" + system
                    + "' must be an integer but was '" + value + "'", ex);
        }
    }

    public boolean requireBoolean(String key) {
        return Boolean.parseBoolean(require(key));
    }

    /** Absolute http(s) URL with exactly one trailing slash, so callers can append relative paths. */
    public String requireBaseUrl(String key) {
        return normalizeBaseUrl(key, require(key));
    }

    /** Validates a URL obtained for {@code key} from another source (e.g. a legacy alias or env var). */
    public String normalizeBaseUrl(String key, String value) {
        value = value.trim();
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Setting '" + key + "' for system '" + system + "' is not a valid URL: " + value, ex);
        }
        if (uri.getScheme() == null || uri.getHost() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalStateException("Setting '" + key + "' for system '" + system
                    + "' must be an absolute http(s) URL but was: " + value);
        }
        return value.replaceAll("/+$", "") + "/";
    }

    /** Joins a base URL and a path without doubling or dropping slashes. */
    public static String join(String baseUrl, String path) {
        String base = baseUrl.replaceAll("/+$", "");
        if (path == null || path.isBlank()) return base + "/";
        return base + "/" + path.replaceAll("^/+", "");
    }
}
