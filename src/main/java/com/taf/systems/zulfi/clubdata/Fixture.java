package com.taf.systems.zulfi.clubdata;

import java.util.Objects;

/**
 * One match row as rendered on Match Schedule / club fixtures tab. Scores are null for fixtures not yet played.
 * {@code competition} is the schedule tab it was read from (e.g. «دوري يلو»), empty when unknown.
 */
public record Fixture(
        String competition,
        int round,
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String dateText,
        String timeText,
        String analysisLabel) {

    public boolean played() {
        return homeGoals != null && awayGoals != null;
    }

    public boolean involves(String club) {
        return sameTeam(homeTeam, club) || sameTeam(awayTeam, club);
    }

    public boolean clubIsHome(String club) {
        return sameTeam(homeTeam, club);
    }

    public String opponent(String club) {
        return clubIsHome(club) ? awayTeam : homeTeam;
    }

    public int goalsFor(String club) {
        return clubIsHome(club) ? homeGoals : awayGoals;
    }

    public int goalsAgainst(String club) {
        return clubIsHome(club) ? awayGoals : homeGoals;
    }

    /** W / D / L from {@code club}'s perspective (played fixtures only). */
    public char outcome(String club) {
        int gf = goalsFor(club);
        int ga = goalsAgainst(club);
        return gf > ga ? 'W' : gf == ga ? 'D' : 'L';
    }

    /** Score written club-first, e.g. «2 - 0» for an away 0-2 win — the Home/club results card convention. */
    public String clubFirstScore(String club) {
        return goalsFor(club) + " - " + goalsAgainst(club);
    }

    /** Chronological key within a season (Jul→Jun), from the rendered Arabic date. */
    public int seasonOrderKey() {
        return ArabicDates.seasonOrderKey(dateText);
    }

    public String describe() {
        String score = played() ? homeGoals + "-" + awayGoals : "vs";
        return "R" + round + " " + homeTeam + " " + score + " " + awayTeam + " (" + dateText + ")";
    }

    public static boolean sameTeam(String a, String b) {
        return Objects.equals(canonical(a), canonical(b));
    }

    /** Team names are compared without the optional «نادي» prefix and Arabic letter variants. */
    public static String canonical(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replaceFirst("^نادي\\s+", "")
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ى', 'ي')
                .replace('ة', 'ه')
                .replaceAll("\\s+", " ");
    }
}
