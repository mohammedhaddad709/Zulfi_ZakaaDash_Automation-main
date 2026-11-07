package com.taf.systems.zulfi.analysis.fixture;

import com.taf.core.utils.OutputPaths;
import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class OfficialFixtureManifestStore {

    /** Run-scoped location (honours -Doutput.dir, so runner runs never share a manifest). */
    public static Path latestManifest() {
        return OutputPaths.root().resolve("analysis-fixtures").resolve("latest-manifest.json");
    }

    private OfficialFixtureManifestStore() {}

    public static OfficialFixtureManifest buildFromSnapshots(
            List<OfficialFixtureOptionSnapshot> orderedSnapshots,
            int scrollPassesUsed,
            boolean enumerationComplete) {
        List<IndexedFixtureEntry> catalog = OfficialFixtureIndexCatalog.fromOrderedSnapshots(orderedSnapshots);
        List<OfficialFixtureManifestEntry> entries = new ArrayList<>();
        int enabled = 0;
        int disabled = 0;
        for (IndexedFixtureEntry row : catalog) {
            if (row.enabled()) {
                enabled++;
            } else {
                disabled++;
            }
            OfficialFixtureOptionSnapshot snap = row.snapshot();
            entries.add(
                    new OfficialFixtureManifestEntry(
                            row.fixtureIndex(),
                            snap.displayLabel(),
                            snap.enabled(),
                            snap.homeTeam(),
                            snap.awayTeam(),
                            snap.matchId(),
                            snap.externalEventId()));
        }
        String runId =
                OfficialFixtureManifestSession.suiteRunId() != null
                        ? OfficialFixtureManifestSession.suiteRunId()
                        : "unbound-" + Instant.now().toEpochMilli();
        return new OfficialFixtureManifest(
                runId,
                Instant.now().toString(),
                true,
                enumerationComplete,
                scrollPassesUsed,
                catalog.size(),
                enabled,
                disabled,
                List.copyOf(entries));
    }

    public static void deleteManifestQuietly() {
        try {
            Files.deleteIfExists(latestManifest());
        } catch (IOException ignored) {
            // best effort
        }
    }

    public static void persist(OfficialFixtureManifest manifest) {
        try {
            Files.createDirectories(latestManifest().getParent());
            Files.writeString(latestManifest(), toJson(manifest), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot write fixture manifest: " + latestManifest(), ex);
        }
    }

    public static OfficialFixtureManifest load() {
        if (!Files.isRegularFile(latestManifest())) {
            return null;
        }
        try {
            return fromJson(Files.readString(latestManifest(), StandardCharsets.UTF_8));
        } catch (IOException | ParseException ex) {
            throw new IllegalStateException("Cannot read fixture manifest: " + latestManifest(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static String toJson(OfficialFixtureManifest manifest) {
        JSONObject root = new JSONObject();
        root.put("suiteRunId", manifest.suiteRunId());
        root.put("discoveredAtIso", manifest.discoveredAtIso());
        root.put("discoverySucceeded", manifest.discoverySucceeded());
        root.put("enumerationComplete", manifest.enumerationComplete());
        root.put("scrollPassesUsed", manifest.scrollPassesUsed());
        root.put("fixtureCount", manifest.fixtureCount());
        root.put("enabledCount", manifest.enabledCount());
        root.put("disabledCount", manifest.disabledCount());
        JSONArray fixtures = new JSONArray();
        for (OfficialFixtureManifestEntry entry : manifest.fixtures()) {
            JSONObject row = new JSONObject();
            row.put("fixtureIndex", entry.fixtureIndex());
            row.put("displayLabel", entry.displayLabel());
            row.put("enabled", entry.enabled());
            row.put("homeTeam", entry.homeTeam());
            row.put("awayTeam", entry.awayTeam());
            row.put("matchId", entry.matchId());
            row.put("externalEventId", entry.externalEventId());
            fixtures.add(row);
        }
        root.put("fixtures", fixtures);
        return root.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static OfficialFixtureManifest fromJson(String json) throws ParseException {
        JSONObject root = (JSONObject) new JSONParser().parse(json);
        JSONArray fixturesArray = (JSONArray) root.get("fixtures");
        List<OfficialFixtureManifestEntry> fixtures = new ArrayList<>();
        for (Object item : fixturesArray) {
            JSONObject row = (JSONObject) item;
            fixtures.add(
                    new OfficialFixtureManifestEntry(
                            intValue(row.get("fixtureIndex")),
                            stringValue(row.get("displayLabel")),
                            booleanValue(row.get("enabled")),
                            stringValue(row.get("homeTeam")),
                            stringValue(row.get("awayTeam")),
                            stringValue(row.get("matchId")),
                            stringValue(row.get("externalEventId"))));
        }
        return new OfficialFixtureManifest(
                stringValue(root.get("suiteRunId")),
                stringValue(root.get("discoveredAtIso")),
                root.get("discoverySucceeded") == null || booleanValue(root.get("discoverySucceeded")),
                booleanValue(root.get("enumerationComplete")),
                intValue(root.get("scrollPassesUsed")),
                intValue(root.get("fixtureCount")),
                intValue(root.get("enabledCount")),
                intValue(root.get("disabledCount")),
                List.copyOf(fixtures));
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
