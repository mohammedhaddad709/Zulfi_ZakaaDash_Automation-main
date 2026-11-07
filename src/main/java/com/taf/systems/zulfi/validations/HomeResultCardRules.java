package com.taf.systems.zulfi.validations;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing helpers for Home current-result cards. */
public final class HomeResultCardRules {

    // Hyphen only: «03:05 م» kickoff times must not read as scores (see MatchScheduleRowRules).
    private static final Pattern SCORE_PATTERN =
            Pattern.compile("(?<![\\d/.:\\-])(\\d{1,2})\\s*[-–]\\s*(\\d{1,2})(?![\\d/.:\\-])");
    private static final Set<String> KNOWN_STATUSES =
            Set.of(
                    HomeCurrentResultsMessages.STATUS_NOT_ANALYZED,
                    HomeCurrentResultsMessages.STATUS_IN_PROGRESS,
                    HomeCurrentResultsMessages.STATUS_COMPLETED);

    private HomeResultCardRules() {}

    public static boolean hasRecordedScore(String cardText) {
        return cardText != null && SCORE_PATTERN.matcher(cardText).find();
    }

    public static int[] parseFirstScore(String cardText) {
        if (cardText == null) {
            return new int[0];
        }
        Matcher matcher = SCORE_PATTERN.matcher(cardText);
        if (!matcher.find()) {
            return new int[0];
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    public static boolean mentionsClub(String cardText, String clubFragment) {
        if (cardText == null || clubFragment == null || clubFragment.isBlank()) {
            return false;
        }
        return cardText.contains(clubFragment);
    }

    public static String findKnownAnalysisStatus(String cardText) {
        if (cardText == null) {
            return "";
        }
        for (String status : KNOWN_STATUSES) {
            if (cardText.contains(status)) {
                return status;
            }
        }
        return "";
    }

    public static boolean hasKnownAnalysisStatus(String cardText) {
        return !findKnownAnalysisStatus(cardText).isBlank();
    }
}
