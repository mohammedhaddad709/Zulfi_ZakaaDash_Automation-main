package com.taf.systems.zakaadash.config;

import com.taf.core.utils.dataReader.SystemProfile;

/**
 * ZakaaDash settings from {@code systems/zakaadash.properties}; JVM {@code -D} values win.
 * Browser type and headless/Grid execution come from the shared core keys
 * {@code browserType} and {@code executionType}.
 */
public final class ZakaaDashConfig {
    public static final String SYSTEM = "zakaadash";
    public static final String BASE_URL_KEY = "zakaadash.baseUrl";
    private static final String LEGACY_BASE_URL_KEY = "zakaadash.base.url";

    private final SystemProfile profile = new SystemProfile(SYSTEM);

    ZakaaDashConfig() { }

    /** Base URL without trailing slash; paths such as {@link #zakaadashLoginPath()} start with "/". */
    public String zakaadashBaseUrl() {
        String legacy = System.getProperty(LEGACY_BASE_URL_KEY);
        String normalized = System.getProperty(BASE_URL_KEY) == null && legacy != null && !legacy.isBlank()
                ? profile.normalizeBaseUrl(LEGACY_BASE_URL_KEY, legacy)
                : profile.requireBaseUrl(BASE_URL_KEY);
        return normalized.substring(0, normalized.length() - 1);
    }

    public String zakaadashLoginPath() { return profile.require("zakaadash.login.path"); }

    public String zakaadashDataVersionsPath() { return profile.require("zakaadash.data.versions.path"); }

    /** Framework demo target (the-internet.herokuapp.com) kept from the original ZakaaDash project. */
    public String baseUrl() {
        String url = profile.requireBaseUrl("zakaadash.demo.heroku.baseUrl");
        return url.substring(0, url.length() - 1);
    }

    public int explicitWaitSeconds() { return profile.requireInt("zakaadash.explicit.wait"); }

    public int pageLoadTimeoutSeconds() { return profile.requireInt("zakaadash.page.load.timeout"); }

    public int headlessWindowWidth() { return profile.requireInt("zakaadash.headless.window.width"); }

    public int headlessWindowHeight() { return profile.requireInt("zakaadash.headless.window.height"); }

    public boolean humanDelayEnabled() { return profile.requireBoolean("zakaadash.human.delay.enabled"); }

    public int humanDelayMinMs() { return profile.requireInt("zakaadash.human.delay.min.ms"); }

    public int humanDelayMaxMs() { return profile.requireInt("zakaadash.human.delay.max.ms"); }

    public int humanDelayPreprocessingMs() { return profile.requireInt("zakaadash.human.delay.preprocessing.ms"); }

    public int humanDelayStep4MinMs() { return profile.requireInt("zakaadash.human.delay.step4.min.ms"); }

    public int humanDelayStep4MaxMs() { return profile.requireInt("zakaadash.human.delay.step4.max.ms"); }

    public String environmentName() { return profile.get("zakaadash.environment.name", "dev"); }
}
