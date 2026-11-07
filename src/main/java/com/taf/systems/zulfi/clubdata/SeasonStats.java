package com.taf.systems.zulfi.clubdata;

import java.util.ArrayList;
import java.util.List;

/** A club's league totals as shown on standings / club overview, or as recomputed from fixtures. */
public record SeasonStats(
        int played, int wins, int draws, int losses, int goalsFor, int goalsAgainst, int goalDiff, int points) {

    public static SeasonStats fromFixtures(String club, List<Fixture> fixtures) {
        int p = 0, w = 0, d = 0, l = 0, gf = 0, ga = 0;
        for (Fixture f : fixtures) {
            if (!f.played() || !f.involves(club)) {
                continue;
            }
            p++;
            gf += f.goalsFor(club);
            ga += f.goalsAgainst(club);
            switch (f.outcome(club)) {
                case 'W' -> w++;
                case 'D' -> d++;
                default -> l++;
            }
        }
        return new SeasonStats(p, w, d, l, gf, ga, gf - ga, w * 3 + d);
    }

    /** Arithmetic rules every standings row must satisfy; empty when consistent. */
    public List<String> internalProblems() {
        List<String> problems = new ArrayList<>();
        if (wins + draws + losses != played) {
            problems.add("W+D+L=" + (wins + draws + losses) + " ≠ played=" + played);
        }
        if (wins * 3 + draws != points) {
            problems.add("3×W+D=" + (wins * 3 + draws) + " ≠ points=" + points);
        }
        if (goalsFor - goalsAgainst != goalDiff) {
            problems.add("GF−GA=" + (goalsFor - goalsAgainst) + " ≠ goalDiff=" + goalDiff);
        }
        return problems;
    }

    /** Field-by-field differences against {@code other} (labels in Arabic as shown in the UI). */
    public List<String> differencesFrom(SeasonStats other) {
        List<String> diffs = new ArrayList<>();
        compare(diffs, "لعب", played, other.played);
        compare(diffs, "فوز", wins, other.wins);
        compare(diffs, "تعادل", draws, other.draws);
        compare(diffs, "خسارة", losses, other.losses);
        compare(diffs, "له", goalsFor, other.goalsFor);
        compare(diffs, "عليه", goalsAgainst, other.goalsAgainst);
        compare(diffs, "الفارق", goalDiff, other.goalDiff);
        compare(diffs, "النقاط", points, other.points);
        return diffs;
    }

    public String describe() {
        return "P" + played + " W" + wins + " D" + draws + " L" + losses + " GF" + goalsFor + " GA" + goalsAgainst
                + " GD" + goalDiff + " Pts" + points;
    }

    private static void compare(List<String> diffs, String label, int mine, int theirs) {
        if (mine != theirs) {
            diffs.add(label + ": " + mine + " ≠ " + theirs);
        }
    }
}
