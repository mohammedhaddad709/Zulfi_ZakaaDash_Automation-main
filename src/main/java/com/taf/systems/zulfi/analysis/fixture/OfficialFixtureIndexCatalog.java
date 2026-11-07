package com.taf.systems.zulfi.analysis.fixture;

import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import java.util.ArrayList;
import java.util.List;

/** Builds canonical zero-based fixture indexes from ordered dropdown snapshots. */
public final class OfficialFixtureIndexCatalog {

    private OfficialFixtureIndexCatalog() {}

    public static List<IndexedFixtureEntry> fromOrderedSnapshots(List<OfficialFixtureOptionSnapshot> ordered) {
        List<IndexedFixtureEntry> fixtures = new ArrayList<>();
        int index = 0;
        for (OfficialFixtureOptionSnapshot snapshot : ordered) {
            if (!"FIXTURE".equals(snapshot.rowKind())) {
                continue;
            }
            fixtures.add(new IndexedFixtureEntry(index++, snapshot));
        }
        return List.copyOf(fixtures);
    }

    public static IndexedFixtureEntry requireEntry(List<IndexedFixtureEntry> catalog, int fixtureIndex) {
        if (fixtureIndex < 0 || fixtureIndex >= catalog.size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid fixture index "
                            + fixtureIndex
                            + "; valid range 0.."
                            + (catalog.isEmpty() ? "none" : (catalog.size() - 1)));
        }
        return catalog.get(fixtureIndex);
    }
}
