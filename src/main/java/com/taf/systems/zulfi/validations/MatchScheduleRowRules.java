package com.taf.systems.zulfi.validations;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing helpers for Match Schedule rows (US-FAP-MCH). */
public final class MatchScheduleRowRules {

    // «4 - 0» only: a ':' separator made every kickoff time («12:30») parse as a score, so unplayed rows looked played.
    private static final Pattern SCORE_PATTERN =
            Pattern.compile("(?<![\\d/.:\\-])(\\d{1,2})\\s*[-–]\\s*(\\d{1,2})(?![\\d/.:\\-])");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
    private static final Pattern MATCH_ID_PATTERN =
            Pattern.compile("#\\s*\\d+|مباراة\\s*#\\s*\\d+", Pattern.UNICODE_CASE);
    private static final Pattern DATE_TOKEN = MatchDateDisplayRules.CALENDAR_DATE_DISPLAY;

    private MatchScheduleRowRules() {}

    public static boolean hasRecordedScore(String rowText) {
        return rowText != null && SCORE_PATTERN.matcher(rowText).find();
    }

    public static int[] parseFirstScore(String rowText) {
        if (rowText == null) {
            return new int[0];
        }
        Matcher matcher = SCORE_PATTERN.matcher(rowText);
        if (!matcher.find()) {
            return new int[0];
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    public static boolean hasKickoffTime(String rowText) {
        return rowText != null && TIME_PATTERN.matcher(rowText).find();
    }

    public static String extractDateToken(String rowText) {
        if (rowText == null) {
            return "";
        }
        Matcher matcher = DATE_TOKEN.matcher(rowText);
        if (matcher.find()) {
            String token = matcher.group();
            return MatchDateDisplayRules.looksLikeCalendarDateDisplay(token) ? token : "";
        }
        return "";
    }

    public static boolean isUnplayedRow(String rowText) {
        return rowText != null
                && !rowText.isBlank()
                && hasKickoffTime(rowText)
                && !hasRecordedScore(rowText);
    }

    public static boolean isPlayedRow(String rowText) {
        return hasRecordedScore(rowText);
    }

    public static String findKnownAnalysisStatus(String rowText) {
        return HomeResultCardRules.findKnownAnalysisStatus(rowText);
    }

    public static boolean mentionsMatchId(String rowText) {
        return rowText != null && MATCH_ID_PATTERN.matcher(rowText).find();
    }

    /** First numeric official match id token from schedule row copy (#123 or مباراة #123). */
    public static Optional<String> extractFirstMatchIdNumeric(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = MATCH_ID_PATTERN.matcher(rowText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String token = matcher.group();
        Matcher digits = Pattern.compile("(\\d+)").matcher(token);
        if (digits.find()) {
            return Optional.of(digits.group(1));
        }
        return Optional.empty();
    }

    /** Club names should appear before match #ID in row text when both are present. */
    public static boolean clubNamesPrecedeMatchId(String rowText, String clubFragment) {
        if (rowText == null || clubFragment == null || clubFragment.isBlank()) {
            return true;
        }
        int clubIdx = rowText.indexOf(clubFragment);
        Matcher idMatcher = MATCH_ID_PATTERN.matcher(rowText);
        if (!idMatcher.find()) {
            return true;
        }
        int idIdx = idMatcher.start();
        if (clubIdx < 0) {
            return true;
        }
        return clubIdx < idIdx;
    }

    public static boolean appearsFriendlyMatch(String rowText) {
        return rowText != null && rowText.contains(MatchSchedulePageMessages.FRIENDLY_MATCH_MARKER);
    }

    public static boolean rowMentionsClub(String rowText, String clubFragment) {
        return HomeResultCardRules.mentionsClub(rowText, clubFragment);
    }
}
