package com.taf.systems.zulfi.clubdata;

import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.SquadPlayer;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.ui.PortalDom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Widget-versus-fixtures comparisons shared by the cross-page E2E (ZDC) and the club page tab checks (CLUB-DET).
 * Each method returns human-readable problems; empty means consistent. {@code source} names where the expected
 * fixtures were read (e.g. «schedule», «fixtures tab») so failures say what disagreed with what.
 */
public final class ClubDataComparisons {

    private ClubDataComparisons() {}

    /** Result cards (newest first) against the expected played fixtures (newest first): opponent + club-first score. */
    public static List<String> resultCards(List<ResultCard> cards, List<Fixture> expected, String club, String source) {
        List<String> problems = new ArrayList<>();
        if (cards.size() != expected.size()) {
            problems.add(cards.size() + " cards but " + source + " has " + expected.size() + " played matches to show");
        }
        for (int i = 0; i < Math.min(cards.size(), expected.size()); i++) {
            ResultCard c = cards.get(i);
            Fixture f = expected.get(i);
            if (!Fixture.sameTeam(c.opponent(), f.opponent(club)) || !c.score().equals(f.clubFirstScore(club))) {
                problems.add("card " + (i + 1) + ": «" + c.describe() + "» vs " + source + " «" + f.opponent(club) + " "
                        + f.clubFirstScore(club) + "» (" + f.describe() + ")");
            }
        }
        return problems;
    }

    /** Upcoming cards against the expected unplayed fixtures, in order: teams, date, kick-off and round. */
    public static List<String> upcoming(List<UpcomingCard> cards, List<Fixture> expected, String source) {
        List<String> problems = new ArrayList<>();
        if (cards.size() != expected.size() || cards.stream().anyMatch(Objects::isNull)) {
            problems.add(cards.size() + " cards vs " + expected.size() + " in " + source);
            return problems;
        }
        for (int i = 0; i < cards.size(); i++) {
            UpcomingCard c = cards.get(i);
            Fixture f = expected.get(i);
            boolean same = Fixture.sameTeam(c.homeTeam(), f.homeTeam()) && Fixture.sameTeam(c.awayTeam(), f.awayTeam())
                    && ArabicDates.sameDisplayedDate(c.dateText(), f.dateText())
                    && PortalDom.normalize(c.timeText()).equals(f.timeText())
                    && (c.round() == 0 || f.round() == 0 || c.round() == f.round());
            if (!same) {
                problems.add("«" + c.describe() + "» vs " + source + " «" + f.describe() + " " + f.timeText() + "»");
            }
        }
        return problems;
    }

    public static boolean sameMatch(Fixture a, Fixture b) {
        return Fixture.sameTeam(a.homeTeam(), b.homeTeam()) && Fixture.sameTeam(a.awayTeam(), b.awayTeam())
                && ArabicDates.sameDisplayedDate(a.dateText(), b.dateText());
    }

    /**
     * Leaderboard players that are not on the squad list. Names are compared after Arabic normalisation; a short
     * display name matches the full name when one contains the other.
     */
    public static List<String> notInSquad(Collection<LeaderboardEntry> leaders, Collection<SquadPlayer> squad) {
        List<String> squadNames = squad.stream().map(p -> Fixture.canonical(p.name())).filter(n -> !n.isBlank()).toList();
        return leaders.stream()
                .map(LeaderboardEntry::name)
                .filter(name -> {
                    String leader = Fixture.canonical(name);
                    return squadNames.stream().noneMatch(s -> s.contains(leader) || leader.contains(s));
                })
                .distinct()
                .toList();
    }

    /** Ages above the team's age limit (U21 / U18); blank ages («—») are not judged. */
    public static List<String> overAgeLimit(Collection<SquadPlayer> squad, int limit) {
        return squad.stream()
                .filter(p -> PortalDom.parseIntOr(p.age(), -1) > limit)
                .map(p -> p.name() + " (" + p.age() + ")")
                .toList();
    }
}
