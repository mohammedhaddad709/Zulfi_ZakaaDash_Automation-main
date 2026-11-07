package com.taf.systems.zulfi.clubdata;

/** Small value types read from Home and club-details widgets. */
public final class ClubWidgets {

    private ClubWidgets() {}

    /**
     * «نتائج الفريق للموسم الحالي» card: opponent (logo alt), score written club-first, and the tag colour class.
     */
    public record ResultCard(String opponent, String score, int clubGoals, int opponentGoals, String tagClass) {
        public char outcome() {
            return clubGoals > opponentGoals ? 'W' : clubGoals == opponentGoals ? 'D' : 'L';
        }

        public String describe() {
            return opponent + " " + score;
        }
    }

    /** Upcoming fixture card (Home list or club «المباراة القادمة»). */
    public record UpcomingCard(int round, String homeTeam, String awayTeam, String dateText, String timeText) {
        public String describe() {
            return "R" + round + " " + homeTeam + " vs " + awayTeam + " " + dateText + " " + timeText;
        }
    }

    /** Leaderboard row (أعلى تصنيف / أفضل الهدافين / أفضل صانعي الأهداف). */
    public record LeaderboardEntry(String name, String position, String value, String profileHref, String imageSrc) {
        public double numericValue() {
            try {
                return Double.parseDouble(value.replace(',', '.'));
            } catch (NumberFormatException ex) {
                return Double.NaN;
            }
        }
    }

    /** Squad table row (قائمة الفريق). Empty strings where the UI shows «—». */
    public record SquadPlayer(
            String name,
            String position,
            String appearances,
            String minutes,
            String marketValue,
            String nationality,
            String age,
            String shirtNumber) {}
}
