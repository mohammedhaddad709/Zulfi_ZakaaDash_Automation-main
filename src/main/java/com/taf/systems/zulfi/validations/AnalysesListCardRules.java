package com.taf.systems.zulfi.validations;

import com.taf.systems.zulfi.clubdata.ArabicDates;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Heuristics for analysis list cards (submission date ordering, video source). */
public final class AnalysesListCardRules {

    private static final Pattern CALENDAR_FRAGMENT =
            Pattern.compile("(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2})");
    private static final List<DateTimeFormatter> DATE_FORMATS =
            List.of(
                    DateTimeFormatter.ofPattern("d/M/uuuu"),
                    DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                    DateTimeFormatter.ofPattern("uuuu-M-d"),
                    DateTimeFormatter.ofPattern("uuuu/MM/dd"));

    private AnalysesListCardRules() {}

    public static Optional<String> detectVideoSource(String cardText) {
        if (cardText == null || cardText.isBlank()) {
            return Optional.empty();
        }
        String lower = cardText.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube")) {
            return Optional.of(AnalysesListingMessages.VIDEO_SOURCE_YOUTUBE);
        }
        if (lower.contains("google drive") || lower.contains("drive")) {
            return Optional.of(AnalysesListingMessages.VIDEO_SOURCE_GOOGLE_DRIVE);
        }
        return Optional.empty();
    }

    public static Optional<LocalDate> parseSubmissionDate(String cardText) {
        if (cardText == null || cardText.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CALENDAR_FRAGMENT.matcher(cardText);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (MatchDateDisplayRules.isTimezoneLeakInMatchDateField(token)) {
                continue;
            }
            Optional<LocalDate> parsed = tryParseDate(token);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        // Current cards: «٢٠ أكتوبر ٢٠٢٦ في ١٠:٠٤ ص»
        return ArabicDates.parseDayMonthYear(cardText);
    }

    /** True when every parseable adjacent pair is non-increasing (newest first). */
    public static boolean looksNewestFirst(List<String> cardTexts) {
        if (cardTexts == null || cardTexts.size() < 2) {
            return true;
        }
        List<LocalDate> dates = new ArrayList<>();
        for (String text : cardTexts) {
            parseSubmissionDate(text).ifPresent(dates::add);
        }
        if (dates.size() < 2) {
            return true;
        }
        for (int i = 0; i < dates.size() - 1; i++) {
            if (dates.get(i).isBefore(dates.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    private static Optional<LocalDate> tryParseDate(String token) {
        String normalized = token.trim().replace('.', '/').replace('-', '/');
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(normalized, formatter));
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return Optional.empty();
    }
}
