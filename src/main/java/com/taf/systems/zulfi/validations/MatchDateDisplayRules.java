package com.taf.systems.zulfi.validations;

import java.util.regex.Pattern;

public final class MatchDateDisplayRules {

    /** Numeric dates, or the portal's Arabic day + month form («19 سبتمبر», «الأحد، 3 يناير»). */
    static final Pattern CALENDAR_DATE_DISPLAY =
            Pattern.compile("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2}"
                    + "|\\d{1,2}\\s+(?:يناير|فبراير|مارس|أبريل|ابريل|مايو|يونيو|يوليو|أغسطس|اغسطس|سبتمبر|أكتوبر|اكتوبر|نوفمبر|ديسمبر)");
    private static final Pattern TIMEZONE_OFFSET_IN_DATE_FIELD = Pattern.compile("[+-]\\d{4}");

    private MatchDateDisplayRules() {}

    public static boolean looksLikeCalendarDateDisplay(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        if (isTimezoneLeakInMatchDateField(trimmed)) {
            return false;
        }
        return CALENDAR_DATE_DISPLAY.matcher(trimmed).find();
    }

    public static boolean isTimezoneLeakInMatchDateField(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (TIMEZONE_OFFSET_IN_DATE_FIELD.matcher(trimmed).find()
                && (lower.contains("time") || lower.contains("gmt") || trimmed.contains("("))) {
            return true;
        }
        return lower.contains("eastern european")
                || lower.contains("summer time")
                || lower.contains("standard time")
                || lower.contains("timezone");
    }
}
