package com.taf.systems.zakaadash.config;

public final class ConfigManager {

    private static final ZakaaDashConfig CONFIG = new ZakaaDashConfig();

    private ConfigManager() {
    }

    public static ZakaaDashConfig config() {
        return CONFIG;
    }
}
