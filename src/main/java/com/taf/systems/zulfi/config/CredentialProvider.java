package com.taf.systems.zulfi.config;

import com.taf.core.utils.dataReader.LocalCredentials;

/**
 * Credentials come from -D, the process environment, or the git-ignored {@code config/credentials.local.properties}
 * (see {@link LocalCredentials}). They are never read from test-data JSON and never committed.
 */
public final class CredentialProvider {
    private CredentialProvider() { }
    public static boolean isConfigured() {
        return isPresent("ZULFI_USER") && isPresent("ZULFI_PASSWORD");
    }

    public static Credentials requireCredentials() {
        String username = require("ZULFI_USER");
        String password = require("ZULFI_PASSWORD");
        return new Credentials(username, password);
    }

    private static boolean isPresent(String name) {
        return usable(LocalCredentials.get(name));
    }
    private static String require(String name) {
        String value = LocalCredentials.get(name);
        if (!usable(value))
            throw new IllegalStateException("Missing or placeholder credential: " + name
                    + " (set the environment variable, -D" + name + ", or " + LocalCredentials.DEFAULT_FILE + ")");
        return value;
    }
    private static boolean usable(String value) {
        return value != null && !value.isBlank() && !value.contains("YOUR_") && !value.contains("<");
    }
    public static final class Credentials {
        private final String username;
        private final String password;
        private Credentials(String username, String password) { this.username = username; this.password = password; }
        public String username() { return username; }
        public String password() { return password; }
        @Override public String toString() { return "Credentials[REDACTED]"; }
    }
}
