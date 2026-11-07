package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleRowSnapshot;

import java.util.List;

/** Read-only Yellow League schedule grid state for US-FAP-MCH assertions. */
public record MatchScheduleGridSnapshot(
        List<MatchScheduleRowSnapshot> rows,
        String selectedClubFilterLabel,
        boolean emptyFilterVisible,
        boolean loadErrorVisible) {

    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }

    public boolean hasParseableRows() {
        return rowCount() > 0;
    }
}
