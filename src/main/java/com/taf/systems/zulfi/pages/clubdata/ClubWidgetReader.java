package com.taf.systems.zulfi.pages.clubdata;

import com.taf.systems.zulfi.clubdata.ClubWidgets.LeaderboardEntry;
import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.ui.PortalDom;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the match/result widgets the portal renders identically on Home, the club page and Match Schedule.
 * Each read is a single script call over a section root; nothing is clicked.
 */
public final class ClubWidgetReader {

    private static final Pattern SCORE = Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)$");
    private static final Pattern ROUND_NUMBER = Pattern.compile("(?:الجولة|جولة|Round)\\s*(\\d+)");

    /** Fixture rows: an {@code article} whose {@code .grid} has [home | score-or-time + date | away] columns. */
    private static final String READ_FIXTURES =
            "const out = [];"
                    + "for (const a of arguments[0].querySelectorAll('article')) {"
                    // inactive tab panels stay in the DOM (display:none) — read only what the user sees
                    + "  if (a.getClientRects().length === 0) continue;"
                    + "  const grid = a.querySelector('.grid'); if (!grid) continue;"
                    + "  const cols = [...grid.children]; if (cols.length < 3) continue;"
                    + "  const name = c => { const s = c.querySelector('span.truncate');"
                    + "    return s ? s.textContent.trim() : (c.querySelector('img') ? c.querySelector('img').alt : ''); };"
                    + "  const spans = [...cols[1].querySelectorAll('span')].map(s => s.textContent.trim());"
                    + "  const t = cols[1].querySelector('time');"
                    + "  const sec = a.closest('section[id^=\"round-\"]');"
                    + "  const roundSpan = [...a.querySelectorAll('span')].map(s => s.textContent.trim())"
                    + "    .find(x => /الجولة|جولة|Round/.test(x)) || '';"
                    + "  const status = [...a.children].filter(c => c !== grid && !c.contains(grid) && !c.querySelector('time'))"
                    + "    .map(c => c.innerText.trim()).filter(x => x && !/^(الجولة|جولة|Round)/.test(x)).join(' | ');"
                    + "  out.push({ home: name(cols[0]), away: name(cols[2]),"
                    + "    score: spans.find(x => /^\\d+\\s*-\\s*\\d+$/.test(x)) || '',"
                    + "    time: spans.find(x => /\\d{1,2}:\\d{2}/.test(x)) || '',"
                    + "    date: t ? t.textContent.trim() : '',"
                    + "    round: sec ? sec.id.replace('round-', '') : roundSpan,"
                    + "    status: status });"
                    + "}"
                    + "return out;";

    private static final String READ_RESULT_CARDS =
            "return [...arguments[0].querySelectorAll('div.group')]"
                    + ".filter(g => g.querySelector('img') && g.querySelector('.p-tag'))"
                    + ".map(g => ({ opponent: g.querySelector('img').alt, score: g.querySelector('.p-tag').textContent.trim(),"
                    + "  tag: g.querySelector('.p-tag').className }));";

    private static final String READ_LEADERBOARD =
            "return [...arguments[0].querySelectorAll('img')].map(img => {"
                    + "  const row = img.closest('div.flex-between') || img.parentElement;"
                    + "  const link = row.querySelector('a');"
                    + "  const pos = row.querySelector('p');"
                    + "  const spans = [...row.querySelectorAll(':scope > span')];"
                    + "  return { name: link ? link.textContent.trim() : img.alt, href: link ? link.getAttribute('href') : '',"
                    + "    position: pos ? pos.textContent.trim() : '', value: spans.length ? spans[spans.length - 1].textContent.trim() : '',"
                    + "    src: img.getAttribute('src') || '' };"
                    + "});";

    private static final String READ_KPIS =
            "const out = [];"
                    + "for (const a of arguments[0].querySelectorAll('article')) {"
                    + "  const ps = [...a.querySelectorAll('p')].map(p => p.textContent.trim()).filter(x => x);"
                    + "  if (ps.length >= 2) out.push({ label: ps[0], values: ps.slice(1) });"
                    + "}"
                    + "return out;";

    private ClubWidgetReader() {}

    public static List<Fixture> fixtures(WebDriver driver, WebElement root, String competition) {
        List<Fixture> fixtures = new ArrayList<>();
        for (Map<String, Object> r : PortalDom.rows(driver, READ_FIXTURES, root)) {
            Matcher score = SCORE.matcher(PortalDom.str(r, "score"));
            boolean played = score.matches();
            fixtures.add(new Fixture(
                    competition,
                    roundNumber(PortalDom.str(r, "round")),
                    PortalDom.str(r, "home"),
                    PortalDom.str(r, "away"),
                    played ? Integer.valueOf(score.group(1)) : null,
                    played ? Integer.valueOf(score.group(2)) : null,
                    PortalDom.str(r, "date"),
                    PortalDom.str(r, "time"),
                    PortalDom.str(r, "status")));
        }
        return fixtures;
    }

    /** Upcoming cards share the fixture markup (time instead of score). */
    public static List<UpcomingCard> upcoming(WebDriver driver, WebElement root) {
        return fixtures(driver, root, "").stream()
                .map(f -> new UpcomingCard(f.round(), f.homeTeam(), f.awayTeam(), f.dateText(), f.timeText()))
                .toList();
    }

    public static List<ResultCard> resultCards(WebDriver driver, WebElement root) {
        List<ResultCard> cards = new ArrayList<>();
        for (Map<String, Object> r : PortalDom.rows(driver, READ_RESULT_CARDS, root)) {
            String score = PortalDom.str(r, "score");
            Matcher m = SCORE.matcher(score);
            boolean ok = m.matches();
            cards.add(new ResultCard(
                    PortalDom.str(r, "opponent"),
                    score,
                    ok ? Integer.parseInt(m.group(1)) : -1,
                    ok ? Integer.parseInt(m.group(2)) : -1,
                    PortalDom.str(r, "tag")));
        }
        return cards;
    }

    public static List<LeaderboardEntry> leaderboard(WebDriver driver, WebElement section) {
        return PortalDom.rows(driver, READ_LEADERBOARD, section).stream()
                .map(r -> new LeaderboardEntry(
                        PortalDom.str(r, "name"),
                        PortalDom.str(r, "position"),
                        PortalDom.str(r, "value"),
                        PortalDom.str(r, "href"),
                        PortalDom.str(r, "src")))
                .toList();
    }

    /** KPI / stat cards: label → rendered values (one or two {@code <p>} after the label). */
    public static Map<String, List<String>> kpis(WebDriver driver, WebElement root) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map<String, Object> r : PortalDom.rows(driver, READ_KPIS, root)) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) r.get("values");
            out.put(PortalDom.str(r, "label"),
                    values == null ? List.of() : values.stream().map(v -> PortalDom.normalize(String.valueOf(v))).toList());
        }
        return out;
    }

    /** «الجولة 6» → 6; cup labels such as «Round of 32» → 0. */
    public static int roundNumber(String label) {
        String normalized = PortalDom.normalize(label);
        if (normalized.matches("\\d+")) {
            return Integer.parseInt(normalized);
        }
        Matcher m = ROUND_NUMBER.matcher(normalized);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** W (green) / D (neutral) / L (red) implied by a result tag or form badge colour class; '?' when unknown. */
    public static char outcomeFromColor(String cssClass) {
        if (cssClass == null) {
            return '?';
        }
        if (cssClass.matches(".*\\bbg-green-\\d+.*")) {
            return 'W';
        }
        if (cssClass.matches(".*\\bbg-red-\\d+.*")) {
            return 'L';
        }
        if (cssClass.matches(".*\\bbg-neutral-\\d+.*") || cssClass.matches(".*\\bbg-gray-\\d+.*")) {
            return 'D';
        }
        return '?';
    }
}
