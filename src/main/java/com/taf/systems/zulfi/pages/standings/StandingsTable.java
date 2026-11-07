package com.taf.systems.zulfi.pages.standings;

import com.taf.systems.zulfi.clubdata.SeasonStats;
import com.taf.systems.zulfi.clubdata.StandingsRow;
import com.taf.systems.zulfi.ui.PortalDom;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** PrimeVue {@code .standings-datatable} reader, shared by the Home snippet and the full standings page. */
public final class StandingsTable {

    public static final List<String> EXPECTED_HEADERS =
            List.of("#", "النادي", "لعب", "فاز", "تعادل", "خسر", "+/-", "الفارق", "النقاط", "آخر 5 مباريات");

    private static final Pattern ZONE = Pattern.compile("border-(green|red|orange|yellow|blue|amber|lime)-\\d+");

    private static final String READ_ROWS =
            "return [...arguments[0].querySelectorAll('tbody tr[role=row]')].map(tr => {"
                    + " const td = [...tr.querySelectorAll(':scope > td')];"
                    + " const t = i => td[i] ? td[i].innerText.trim() : '';"
                    + " const formCell = td[9];"
                    + " const badges = formCell ? [...formCell.querySelectorAll(':scope > div > span')] : [];"
                    + " const club = td[1];"
                    + " return { pos: t(0), team: club ? (club.querySelector('span.truncate') || club).textContent.trim() : '',"
                    + "  p: t(2), w: t(3), d: t(4), l: t(5), goals: t(6), gd: t(7), pts: t(8),"
                    + "  form: badges.map(s => s.textContent.trim()), formColors: badges.map(s => s.className),"
                    + "  cls: tr.className, logo: club && club.querySelector('img') ? club.querySelector('img').src : '' };"
                    + "});";

    private final WebDriver driver;
    private final WebElement root;

    public StandingsTable(WebDriver driver, WebElement root) {
        this.driver = driver;
        this.root = root;
    }

    public WebElement root() {
        return root;
    }

    public List<String> headers() {
        return PortalDom.rows(driver,
                        "return [...arguments[0].querySelectorAll('thead th')].map(th => ({h: th.innerText.trim()}));",
                        root)
                .stream()
                .map(r -> PortalDom.str(r, "h"))
                .toList();
    }

    public List<StandingsRow> rows() {
        List<StandingsRow> rows = new ArrayList<>();
        for (Map<String, Object> r : PortalDom.rows(driver, READ_ROWS, root)) {
            String[] goals = PortalDom.str(r, "goals").split(":");
            int gf = goals.length == 2 ? PortalDom.parseIntOr(goals[0], -1) : -1;
            int ga = goals.length == 2 ? PortalDom.parseIntOr(goals[1], -1) : -1;
            SeasonStats stats = new SeasonStats(
                    PortalDom.parseIntOr(PortalDom.str(r, "p"), -1),
                    PortalDom.parseIntOr(PortalDom.str(r, "w"), -1),
                    PortalDom.parseIntOr(PortalDom.str(r, "d"), -1),
                    PortalDom.parseIntOr(PortalDom.str(r, "l"), -1),
                    gf,
                    ga,
                    PortalDom.parseIntOr(PortalDom.str(r, "gd"), Integer.MIN_VALUE),
                    PortalDom.parseIntOr(PortalDom.str(r, "pts"), -1));
            String cls = PortalDom.str(r, "cls");
            Matcher zone = ZONE.matcher(cls);
            rows.add(new StandingsRow(
                    PortalDom.parseIntOr(PortalDom.str(r, "pos"), -1),
                    PortalDom.str(r, "team"),
                    stats,
                    stringList(r.get("form")),
                    stringList(r.get("formColors")),
                    cls.contains("bg-secondary"),
                    zone.find() ? zone.group(1) : "",
                    PortalDom.str(r, "logo")));
        }
        return rows;
    }

    public List<String> brokenLogos() {
        return PortalDom.brokenImages(driver, root);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        return ((List<Object>) raw).stream().map(o -> PortalDom.normalize(String.valueOf(o))).toList();
    }
}
