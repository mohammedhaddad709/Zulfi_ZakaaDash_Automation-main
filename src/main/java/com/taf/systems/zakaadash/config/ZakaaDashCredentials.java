package com.taf.systems.zakaadash.config;

import com.taf.core.utils.dataReader.LocalCredentials;

import java.util.Objects;

public final class ZakaaDashCredentials {

    private static final String EMAIL_PROPERTY = "ZAKAADASH_EMAIL";
    private static final String PASSWORD_PROPERTY = "ZAKAADASH_PASSWORD";

    private final String email;
    private final String password;

    private ZakaaDashCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static ZakaaDashCredentials load() {
        String email = resolve(EMAIL_PROPERTY);
        String password = resolve(PASSWORD_PROPERTY);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "ZakaaDash email is not configured. Set JVM property, environment variable or config/credentials.local.properties: "
                            + EMAIL_PROPERTY);
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ZakaaDash password is not configured. Set JVM property, environment variable or config/credentials.local.properties: "
                            + PASSWORD_PROPERTY);
        }
        return new ZakaaDashCredentials(email.trim(), password);
    }

    /** JVM property, environment variable, then the git-ignored config/credentials.local.properties. */
    private static String resolve(String key) {
        return LocalCredentials.get(key);
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "ZakaaDashCredentials{email='***configured***', password='***'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZakaaDashCredentials that)) {
            return false;
        }
        return Objects.equals(email, that.email) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }
}
