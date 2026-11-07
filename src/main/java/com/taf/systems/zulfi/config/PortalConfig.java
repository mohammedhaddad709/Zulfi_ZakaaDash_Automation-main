package com.taf.systems.zulfi.config;

import java.net.URI;

public final class PortalConfig {
    private PortalConfig() { }
    public static String baseUrl() {
        String value = ZulfiConfig.baseUrl();
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("Portal URL must use HTTPS");
        return value;
    }
    public static String loginUrl() { return baseUrl() + "auth/login"; }
    public static String homeUrl() { return baseUrl(); }
}
