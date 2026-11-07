package com.taf.systems.zulfi.analysis.fixture;

import java.util.UUID;

/** Same-JVM discovery → selection coordination; prevents stale manifest reuse. */
public final class OfficialFixtureManifestSession {

    private static volatile String suiteRunId;
    private static volatile boolean discoverySucceeded;
    private static volatile String discoveryFailureReason;

    private OfficialFixtureManifestSession() {}

    public static void beginSuiteRun() {
        suiteRunId = UUID.randomUUID().toString();
        discoverySucceeded = false;
        discoveryFailureReason = null;
        OfficialFixtureManifestStore.deleteManifestQuietly();
    }

    public static String suiteRunId() {
        return suiteRunId;
    }

    public static void markDiscoverySucceeded(OfficialFixtureManifest manifest) {
        discoverySucceeded = true;
        discoveryFailureReason = null;
        OfficialFixtureManifestStore.persist(manifest);
    }

    public static void markDiscoveryFailed(String reason) {
        discoverySucceeded = false;
        discoveryFailureReason = reason == null ? "Discovery failed" : reason;
        OfficialFixtureManifestStore.deleteManifestQuietly();
    }

    public static boolean isDiscoverySucceededThisRun() {
        return discoverySucceeded;
    }

    public static String discoveryFailureReason() {
        return discoveryFailureReason;
    }

    public static void requireManifestForSelection() {
        if (!discoverySucceeded) {
            throw new org.testng.SkipException(
                    discoveryFailureReason != null
                            ? discoveryFailureReason
                            : "Discovery did not succeed in this suite run; selection blocked.");
        }
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        if (manifest == null || manifest.fixtureCount() == 0) {
            throw new org.testng.SkipException(
                    "No valid fixture manifest for this run; discovery may have been incomplete.");
        }
        if (suiteRunId != null && manifest.suiteRunId() != null && !suiteRunId.equals(manifest.suiteRunId())) {
            throw new org.testng.SkipException(
                    "Manifest suiteRunId does not match current run; stale manifest rejected.");
        }
    }
}
