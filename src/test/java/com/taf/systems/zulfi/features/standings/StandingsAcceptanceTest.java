package com.taf.systems.zulfi.features.standings;

import com.taf.systems.zulfi.clubdata.ClubFeatureTestIds;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.clubdata.StandingsRow;
import com.taf.systems.zulfi.config.FeaturePhaseGate;
import com.taf.systems.zulfi.config.FeaturePhaseGate.Phase;
import com.taf.systems.zulfi.features.clubdata.ClubDataUiTest;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage.LeagueClubCard;
import com.taf.systems.zulfi.pages.home.HomeClubWidgets;
import com.taf.systems.zulfi.pages.standings.StandingsPage;
import com.taf.systems.zulfi.pages.standings.StandingsTable;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * US-FAP-STANDINGS — league table on Home («ترتيب دوري الدرجة الأولى» snippet) and the full {@code /standings} page.
 * Enable via {@code -Dstandings.phase=a|b|c}. Read-only.
 */
@Epic("Zulfi Portal")
@Feature(ClubFeatureTestIds.FEATURE_STANDINGS)
public class StandingsAcceptanceTest extends ClubDataUiTest {

    @Override
    protected FeaturePhaseGate gate() {
        return FeaturePhaseGate.STANDINGS;
    }

    @Override
    protected String story() {
        return ClubFeatureTestIds.STORY_STANDINGS;
    }

    @Override
    protected String feature() {
        return ClubFeatureTestIds.FEATURE_STANDINGS;
    }

    @Test(description = ClubFeatureTestIds.STD_001 + " — Home standings snippet: columns and highlighted Default Club")
    public void std001_homeSnippetColumnsAndOwnClubHighlight() {
        String id = ClubFeatureTestIds.STD_001;
        requirePhase(Phase.A, id, "Home standings snippet columns and own-club highlight");
        HomeClubWidgets home = flow().openHome();
        step(id, "Read snippet headers", "Columns " + StandingsTable.EXPECTED_HEADERS, () -> {
            List<String> headers = home.standingsTable().headers();
            return compare("headers", StandingsTable.EXPECTED_HEADERS, headers);
        });
        step(id, "Default Club row is highlighted", "Exactly one highlighted row = «" + ClubDataFlow.defaultClub() + "»", () -> {
            List<StandingsRow> rows = home.standingsTable().rows();
            List<String> highlighted = rows.stream().filter(StandingsRow::highlighted).map(StandingsRow::team).toList();
            boolean ok = highlighted.size() == 1 && Fixture.sameTeam(highlighted.getFirst(), ClubDataFlow.defaultClub());
            return ok ? StepResult.pass("rows=" + rows.size() + "; highlighted=" + highlighted)
                    : StepResult.fail("highlighted rows=" + highlighted + " of " + rows.size());
        });
    }

    @Test(description = ClubFeatureTestIds.STD_002 + " — «عرض الجدول الدوري كامل» opens the full table")
    public void std002_fullTableLinkOpensStandingsPage() {
        String id = ClubFeatureTestIds.STD_002;
        requirePhase(Phase.A, id, "«عرض الجدول الدوري كامل» opens full standings");
        int clubs = flow().clubCards().size();
        HomeClubWidgets home = flow().openHome();
        step(id, "Click «عرض الجدول الدوري كامل»", "Navigates to /standings with heading «ترتيب الدوري…»", () -> {
            home.clickFullStandings();
            StandingsPage page = new StandingsPage(browser()).waitUntilLoaded();
            return page.isOnRoute() && !page.heading().isBlank()
                    ? StepResult.pass("url=" + browser().getCurrentUrl() + "; heading=" + page.heading())
                    : StepResult.fail("url=" + browser().getCurrentUrl() + "; heading='" + page.heading() + "'");
        });
        step(id, "Full table lists every league club", "Row count = clubs on «الأندية» (" + clubs + ")", () ->
                compare("standings rows", clubs, new StandingsPage(browser()).table().rows().size()));
    }

    @Test(description = ClubFeatureTestIds.STD_003 + " — Every row is arithmetically consistent")
    public void std003_rowsInternallyConsistent() {
        String id = ClubFeatureTestIds.STD_003;
        requirePhase(Phase.B, id, "Standings rows: P=W+D+L, Pts=3W+D, GD=GF−GA");
        List<StandingsRow> rows = flow().openStandings().table().rows();
        step(id, "Validate each row", "No arithmetic mismatch in any of " + rows.size() + " rows", () -> {
            List<String> problems = new ArrayList<>();
            for (StandingsRow row : rows) {
                row.stats().internalProblems().forEach(p -> problems.add(row.team() + ": " + p));
            }
            return verdict(rows.size() + " rows consistent", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_004 + " — Positions are sequential and ordered by points")
    public void std004_orderedByPoints() {
        String id = ClubFeatureTestIds.STD_004;
        requirePhase(Phase.B, id, "Positions 1..N, points non-increasing");
        List<StandingsRow> rows = flow().openStandings().table().rows();
        step(id, "Check order", "position[i]=i+1 and points[i] ≥ points[i+1]", () -> {
            List<String> problems = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).position() != i + 1) {
                    problems.add("row " + (i + 1) + " shows position " + rows.get(i).position());
                }
                if (i > 0 && rows.get(i).stats().points() > rows.get(i - 1).stats().points()) {
                    problems.add(rows.get(i).team() + " (" + rows.get(i).stats().points() + " pts) ranked below "
                            + rows.get(i - 1).team() + " (" + rows.get(i - 1).stats().points() + " pts)");
                }
            }
            return verdict("order valid for " + rows.size() + " rows", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_005 + " — «آخر 5 مباريات» badges are valid and colour-coded")
    public void std005_formBadges() {
        String id = ClubFeatureTestIds.STD_005;
        requirePhase(Phase.B, id, "Form column: ≤5 W/D/L badges, colours match, within totals");
        List<StandingsRow> rows = flow().openStandings().table().rows();
        step(id, "Validate form badges", "W=green, D=neutral, L=red; count ≤ min(5, played); per-letter ≤ totals", () -> {
            List<String> problems = new ArrayList<>();
            for (StandingsRow row : rows) {
                List<String> form = row.form();
                if (form.size() > 5 || form.size() > row.stats().played()) {
                    problems.add(row.team() + ": " + form.size() + " badges for " + row.stats().played() + " played");
                }
                for (int i = 0; i < form.size(); i++) {
                    String letter = form.get(i);
                    if (!List.of("W", "D", "L").contains(letter)) {
                        problems.add(row.team() + ": badge '" + letter + "'");
                    } else if (i < row.formColors().size()
                            && ClubWidgetReader.outcomeFromColor(row.formColors().get(i)) != letter.charAt(0)) {
                        problems.add(row.team() + ": badge " + letter + " has colour " + row.formColors().get(i));
                    }
                }
                Map<String, Long> counts = form.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                if (counts.getOrDefault("W", 0L) > row.stats().wins() || counts.getOrDefault("D", 0L) > row.stats().draws()
                        || counts.getOrDefault("L", 0L) > row.stats().losses()) {
                    problems.add(row.team() + ": form " + String.join("", form) + " exceeds W/D/L totals "
                            + row.stats().wins() + "/" + row.stats().draws() + "/" + row.stats().losses());
                }
            }
            return verdict("form valid for " + rows.size() + " rows", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_006 + " — Home snippet matches the full table")
    public void std006_homeSnippetMatchesFullTable() {
        String id = ClubFeatureTestIds.STD_006;
        requirePhase(Phase.B, id, "Home snippet rows equal /standings rows");
        List<StandingsRow> snippet = flow().openHome().standingsTable().rows();
        List<StandingsRow> full = flow().openStandings().table().rows();
        step(id, "Compare row by row", "Each snippet row (" + snippet.size() + ") equals the same position on /standings", () -> {
            List<String> problems = new ArrayList<>();
            for (StandingsRow s : snippet) {
                StandingsRow f = full.stream().filter(r -> r.position() == s.position()).findFirst().orElse(null);
                if (f == null) {
                    problems.add("position " + s.position() + " missing on /standings");
                } else if (!Fixture.sameTeam(s.team(), f.team()) || !s.stats().equals(f.stats()) || !s.form().equals(f.form())) {
                    problems.add("Home " + s.describe() + " ≠ Standings " + f.describe());
                }
            }
            return verdict(snippet.size() + " snippet rows match", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_007 + " — Zone legend and zone colours are coherent")
    public void std007_zonesAndLegend() {
        String id = ClubFeatureTestIds.STD_007;
        requirePhase(Phase.B, id, "Zone legend present; zones contiguous; top = promotion, bottom = relegation");
        StandingsPage page = flow().openStandings();
        step(id, "Legend labels", "Legend shows " + StandingsPage.ZONE_LEGEND, () -> {
            List<String> missing = StandingsPage.ZONE_LEGEND.stream().filter(l -> !page.bodyText().contains(l)).toList();
            return verdict("legend complete", missing.stream().map(m -> "legend missing «" + m + "»").toList());
        });
        step(id, "Zone colours per row", "Each zone colour forms one contiguous block; #1 green, last red", () -> {
            List<StandingsRow> rows = page.table().rows();
            List<String> problems = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            String previous = null;
            for (StandingsRow row : rows) {
                String zone = row.zone();
                if (!zone.isEmpty() && !zone.equals(previous) && seen.contains(zone)) {
                    problems.add("zone '" + zone + "' appears again at #" + row.position());
                }
                if (!zone.isEmpty() && !seen.contains(zone)) {
                    seen.add(zone);
                }
                previous = zone;
            }
            if (!rows.isEmpty() && !"green".equals(rows.getFirst().zone())) {
                problems.add("#1 zone is '" + rows.getFirst().zone() + "' (expected promotion/green)");
            }
            if (!rows.isEmpty() && !"red".equals(rows.getLast().zone())) {
                problems.add("last place zone is '" + rows.getLast().zone() + "' (expected relegation/red)");
            }
            return verdict("zones " + seen, problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_008 + " — Every club logo in the table loads")
    public void std008_logosLoad() {
        String id = ClubFeatureTestIds.STD_008;
        requirePhase(Phase.B, id, "No broken club logo on /standings");
        StandingsTable table = flow().openStandings().table();
        step(id, "Check logos", "Every rendered logo has naturalWidth > 0", () ->
                verdict(table.rows().size() + " logos loaded", table.brokenLogos().stream().map(b -> "broken logo " + b).toList()));
    }

    @Test(description = ClubFeatureTestIds.STD_009 + " — «الأندية» rank badges match standings positions")
    public void std009_clubsGridRankMatchesStandings() {
        String id = ClubFeatureTestIds.STD_009;
        requirePhase(Phase.B, id, "«#N» on «الأندية» equals standings position for every club");
        List<LeagueClubCard> cards = flow().clubCards();
        List<StandingsRow> rows = flow().openStandings().table().rows();
        step(id, "Compare every club", "Card #N = standings position for " + cards.size() + " clubs", () -> {
            List<String> problems = new ArrayList<>();
            for (LeagueClubCard card : cards) {
                StandingsRow row = rows.stream().filter(r -> Fixture.sameTeam(r.team(), card.name())).findFirst().orElse(null);
                if (row == null) {
                    problems.add(card.name() + " not on standings");
                } else if (row.position() != card.rank()) {
                    problems.add(card.name() + ": card #" + card.rank() + " vs standings #" + row.position());
                }
            }
            return verdict(cards.size() + " ranks match", problems);
        });
    }

    @Test(description = ClubFeatureTestIds.STD_010 + " — League name spelled consistently («الدرجة الأولى»)")
    public void std010_leagueNameConsistentAcrossPages() {
        String id = ClubFeatureTestIds.STD_010;
        requirePhase(Phase.C, id, "League name copy consistent on Home snippet, /standings and club badge");
        String homeTitle = flow().openHome().standingsTitle();
        String standingsHeading = flow().openStandings().heading();
        step(id, "Spelling of «الأولى»", "Home snippet and /standings both write «الدرجة الأولى» (hamza + alif maqsura)", () -> {
            List<String> problems = new ArrayList<>();
            if (!homeTitle.contains("الدرجة الأولى")) {
                problems.add("Home snippet title «" + homeTitle + "»");
            }
            if (!standingsHeading.contains("الدرجة الأولى")) {
                problems.add("/standings heading «" + standingsHeading + "»");
            }
            return verdict("home=«" + homeTitle + "»; standings=«" + standingsHeading + "»", problems);
        });
    }
}
