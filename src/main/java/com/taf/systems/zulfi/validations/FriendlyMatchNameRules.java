package com.taf.systems.zulfi.validations;

import java.util.regex.Pattern;

/** Business rules for friendly match name (aligned with approved Match Name behavior). */
public final class FriendlyMatchNameRules {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 150;

    private static final Pattern ALLOWED_CHARACTERS =
            Pattern.compile("^[\\p{L}\\p{N}\\s\\-_/():.,&]+$");

    private FriendlyMatchNameRules() {}

    public static String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static boolean isEmptyAfterTrim(String raw) {
        return trim(raw).isEmpty();
    }

    public static boolean meetsMinimumLength(String raw) {
        return trim(raw).length() >= MIN_LENGTH;
    }

    public static boolean withinMaximumLength(String raw) {
        return trim(raw).length() <= MAX_LENGTH;
    }

    public static boolean containsOnlyAllowedCharacters(String raw) {
        String trimmed = trim(raw);
        if (trimmed.isEmpty()) {
            return true;
        }
        return ALLOWED_CHARACTERS.matcher(trimmed).matches();
    }

    public static String repeatChar(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }
}
