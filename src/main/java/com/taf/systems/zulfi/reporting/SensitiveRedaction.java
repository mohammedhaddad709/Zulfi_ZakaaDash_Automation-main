package com.taf.systems.zulfi.reporting;

import java.util.regex.Pattern;

/** Redacts secrets from report text without altering unrelated evidence. */
public final class SensitiveRedaction {

    private static final Pattern PASSWORD_LIKE =
            Pattern.compile("(?i)(password|passwd|token|secret|authorization|cookie|api[_-]?key)\\s*[=:]\\s*\\S+");

    private SensitiveRedaction() {}

    public static String redact(String input) {
        if (input == null) {
            return "";
        }
        String redacted = PASSWORD_LIKE.matcher(input).replaceAll("$1=[REDACTED]");
        if (input.contains("ZULFI_PASSWORD") || input.contains("ZULFI_USER")) {
            redacted = redacted.replaceAll("(?i)ZULFI_PASSWORD\\s*=\\s*\\S+", "ZULFI_PASSWORD=[REDACTED]");
        }
        return redacted;
    }
}
