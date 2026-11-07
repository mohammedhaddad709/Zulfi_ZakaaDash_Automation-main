package com.taf.systems.zulfi.clubdata;

import java.util.List;

/**
 * One standings table row. {@code form} is the «آخر 5 مباريات» letters left-to-right in DOM order (oldest → newest),
 * {@code formColors} the matching badge classes, {@code zone} the row border colour class (promotion / relegation).
 */
public record StandingsRow(
        int position,
        String team,
        SeasonStats stats,
        List<String> form,
        List<String> formColors,
        boolean highlighted,
        String zone,
        String logoSrc) {

    public String describe() {
        return "#" + position + " " + team + " " + stats.describe() + " form=" + String.join("", form);
    }
}
