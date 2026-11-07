package com.taf.core.utils.dataReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Resolves a credential by name (e.g. {@code ZULFI_PASSWORD}) without ever storing it in tracked files.
 *
 * <p>Precedence: JVM {@code -D<NAME>} property, then process environment variable, then the git-ignored local file
 * {@code config/credentials.local.properties} (override the path with {@code -Dcredentials.file}). The file is read
 * from the filesystem, never from the classpath, so it is not packaged into build snapshots or test resources.
 * Values are never logged.
 */
public final class LocalCredentials {

    public static final String FILE_PROPERTY = "credentials.file";
    public static final String DEFAULT_FILE = "config/credentials.local.properties";

    private LocalCredentials() { }

    /**
     * Value or {@code null} when no source provides one (blank counts as absent). An {@code enc:v1:} value from any
     * source is decrypted with {@code RUNNER_SECRET_KEY} ({@link SecretVault}).
     */
    public static String get(String name) {
        String fromProperty = System.getProperty(name);
        if (isUsable(fromProperty)) return SecretVault.reveal(fromProperty);
        String fromEnvironment = System.getenv(name);
        if (isUsable(fromEnvironment)) return SecretVault.reveal(fromEnvironment);
        String fromFile = load().getProperty(name);
        return isUsable(fromFile) ? SecretVault.reveal(fromFile.strip()) : null;
    }

    public static Path file() {
        String configured = System.getProperty(FILE_PROPERTY);
        return Path.of(configured == null || configured.isBlank() ? DEFAULT_FILE : configured).toAbsolutePath().normalize();
    }

    private static Properties load() {
        Properties properties = new Properties();
        Path file = file();
        if (!Files.isRegularFile(file)) return properties;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read local credentials file " + file + ": " + ex.getMessage(), ex);
        }
        return properties;
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }
}
