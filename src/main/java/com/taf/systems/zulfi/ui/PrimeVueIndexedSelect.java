package com.taf.systems.zulfi.ui;

import com.taf.systems.zulfi.analysis.fixture.IndexedFixtureEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureIndexCatalog;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.validations.OfficialFixtureLabelRules;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * PrimeVue p-select helper: ordered fixture discovery and index resolution (not native HTML select).
 */
public final class PrimeVueIndexedSelect {

    private PrimeVueIndexedSelect() {}

    public static List<OfficialFixtureOptionSnapshot> mergeOrderedSnapshots(
            List<OfficialFixtureOptionSnapshot> existing,
            List<OfficialFixtureOptionSnapshot> batch) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<OfficialFixtureOptionSnapshot> ordered = new ArrayList<>(existing);
        for (OfficialFixtureOptionSnapshot snapshot : ordered) {
            seen.add(snapshotKey(snapshot));
        }
        for (OfficialFixtureOptionSnapshot snapshot : batch) {
            if (seen.add(snapshotKey(snapshot))) {
                ordered.add(snapshot);
            }
        }
        return ordered;
    }

    public static List<IndexedFixtureEntry> fixtureCatalog(List<OfficialFixtureOptionSnapshot> ordered) {
        return OfficialFixtureIndexCatalog.fromOrderedSnapshots(ordered);
    }

    public static boolean sameFixtureIdentity(
            OfficialFixtureOptionSnapshot live, OfficialFixtureManifestIdentity expected) {
        if (live == null || expected == null) {
            return false;
        }
        if (!normalize(live.displayLabel()).equals(normalize(expected.displayLabel()))) {
            return false;
        }
        if (expected.matchId() != null
                && !expected.matchId().isBlank()
                && !"Not available".equalsIgnoreCase(expected.matchId())) {
            String liveMatch = live.matchId();
            if (liveMatch != null
                    && !liveMatch.isBlank()
                    && !"Not available".equalsIgnoreCase(liveMatch)
                    && !normalize(liveMatch).equals(normalize(expected.matchId()))) {
                return false;
            }
        }
        return true;
    }

    public record OfficialFixtureManifestIdentity(String displayLabel, String matchId, String externalEventId) {}

    public static String snapshotKey(OfficialFixtureOptionSnapshot snapshot) {
        return snapshot.rowKind() + "|" + normalize(snapshot.displayLabel());
    }

    public static boolean isFixtureRow(OfficialFixtureOptionSnapshot snapshot) {
        return snapshot != null
                && "FIXTURE".equals(snapshot.rowKind())
                && OfficialFixtureLabelRules.isEligibleFixtureRow(snapshot.displayLabel());
    }

    public static void forEachFixtureIndex(List<IndexedFixtureEntry> catalog, IntConsumer consumer) {
        for (IndexedFixtureEntry entry : catalog) {
            consumer.accept(entry.fixtureIndex());
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
