package com.taf.systems.zulfi.config;

import com.taf.core.utils.dataReader.SystemProfile;

/**
 * Zulfi Portal settings from {@code systems/zulfi.properties}; JVM {@code -D} values win.
 * Independent of {@code active.system}, so Zulfi code never reads another system's defaults.
 */
public final class ZulfiConfig {
    public static final String SYSTEM = "zulfi";
    public static final String BASE_URL_KEY = "zulfi.baseUrl";
    private static final SystemProfile PROFILE = new SystemProfile(SYSTEM);

    private ZulfiConfig() { }

    public static String get(String key) {
        return PROFILE.get(key);
    }

    /**
     * Precedence: {@code -Dzulfi.baseUrl}, legacy {@code -DbaseUrlWeb}, env {@code ZULFI_BASE_URL}, profile default.
     */
    public static String baseUrl() {
        String value = System.getProperty(BASE_URL_KEY);
        if (isBlank(value)) value = System.getProperty("baseUrlWeb");
        if (isBlank(value)) value = System.getenv("ZULFI_BASE_URL");
        return isBlank(value) ? PROFILE.requireBaseUrl(BASE_URL_KEY) : PROFILE.normalizeBaseUrl(BASE_URL_KEY, value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
