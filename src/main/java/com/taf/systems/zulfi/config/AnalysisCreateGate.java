package com.taf.systems.zulfi.config;

import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import org.testng.SkipException;

import java.util.Optional;
import java.util.OptionalInt;

/** Safety gate for tests that create real portal analyses. */
public final class AnalysisCreateGate {
    private static final String PROPERTY = "analysis.create.enabled";
    private static final String FRIENDLY_CREATE_PROPERTY = "analysis.friendly.create.enabled";
    private static final String FRIENDLY_MATCH_NAME_ENV = "ZULFI_FRIENDLY_MATCH_NAME";
    private static final String FRIENDLY_MATCH_NAME_PROPERTY = "analysis.friendly.matchName";
    private static final String FIXTURE_ENV = "ZULFI_OFFICIAL_FIXTURE";
    private static final String FIXTURE_LABEL_PROPERTY = "analysis.fixture.label";
    private static final String FIXTURE_INDEX_PROPERTY = "analysis.fixture.index";
    private static final String FIXTURE_INDEX_ENV = "ANALYSIS_FIXTURE_INDEX";
    /** Preferred HP index when not overridden (skips known duplicate indexes 0–1 when manifest resolves). */
    public static final int DEFAULT_HP_SUBMISSION_INDEX = 2;

    /** @deprecated use {@link #DEFAULT_HP_SUBMISSION_INDEX} */
    public static final int DEFAULT_AUTHORIZED_FIXTURE_INDEX = DEFAULT_HP_SUBMISSION_INDEX;

    private AnalysisCreateGate() {}

    public static boolean isCreationEnabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "false"));
    }

    public static void requireCreationEnabled() {
        if (!isCreationEnabled()) {
            throw new SkipException(
                    "Analysis creation tests are disabled. Run with -D" + PROPERTY + "=true to authorize one real record.");
        }
    }

    public static boolean isFriendlyCreationEnabled() {
        return Boolean.parseBoolean(System.getProperty(FRIENDLY_CREATE_PROPERTY, "false"));
    }

    /** Friendly submit requires master creation gate plus explicit friendly authorization. */
    public static void requireFriendlyCreationEnabled() {
        requireCreationEnabled();
        if (!isFriendlyCreationEnabled()) {
            throw new SkipException(
                    "Friendly analysis creation disabled. Run with -D"
                            + PROPERTY
                            + "=true and -D"
                            + FRIENDLY_CREATE_PROPERTY
                            + "=true.");
        }
    }

    /** Unique-friendly default for authorized runs (override via env/property). */
    public static String resolveFriendlyMatchNameForRun() {
        String fromEnv = System.getenv(FRIENDLY_MATCH_NAME_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(FRIENDLY_MATCH_NAME_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return "الزلفي × ضيف أوتوميشن " + System.currentTimeMillis();
    }

    public static String resolveVideoUrl() {
        String fromEnv = System.getenv("ANALYSIS_VIDEO_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return "https://youtu.be/Oquu8zAJyi0?si=AsBw315O9gvIUNIT";
    }

    /** Optional zero-based official fixture index for authorized creation (requires manifest or live resolution). */
    public static OptionalInt configuredFixtureIndex() {
        String raw = System.getProperty(FIXTURE_INDEX_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(FIXTURE_INDEX_ENV);
        }
        if (raw == null || raw.isBlank()) {
            raw = ZulfiConfig.get(FIXTURE_INDEX_PROPERTY);
        }
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            throw new SkipException("Invalid " + FIXTURE_INDEX_PROPERTY + ": " + raw);
        }
    }

    public static Optional<String> optionalExpectedFixtureLabelOverride() {
        return optionalExplicitFixtureLabel();
    }

    /** Required exact official fixture label for create flows (no arbitrary first-match selection). */
    public static String requireConfiguredOfficialFixtureLabel() {
        return requireConfiguredOfficialFixtureLabel(null);
    }

    /**
     * Resolves authorized fixture label: explicit env/property override, else manifest by index, else
     * verified label from the form after selection.
     */
    public static String requireConfiguredOfficialFixtureLabel(String verifiedSelectedLabel) {
        Optional<String> explicit = optionalExplicitFixtureLabel();
        if (explicit.isPresent()) {
            String label = explicit.get();
            assertMatchesVerified(label, verifiedSelectedLabel);
            assertEligibleForSubmission(label);
            return label;
        }
        int index = configuredFixtureIndex().orElse(DEFAULT_AUTHORIZED_FIXTURE_INDEX);
        Optional<String> fromManifest = labelFromManifestIndex(index);
        if (fromManifest.isPresent()) {
            String label = fromManifest.get();
            assertMatchesVerified(label, verifiedSelectedLabel);
            assertEligibleForSubmission(label);
            return label;
        }
        if (verifiedSelectedLabel != null && !verifiedSelectedLabel.isBlank()) {
            assertEligibleForSubmission(verifiedSelectedLabel.trim());
            return verifiedSelectedLabel.trim();
        }
        throw new SkipException(
                "Configured official fixture required. Set "
                        + FIXTURE_ENV
                        + ", -D"
                        + FIXTURE_LABEL_PROPERTY
                        + ", or -D"
                        + FIXTURE_INDEX_PROPERTY
                        + " with manifest at "
                        + OfficialFixtureManifestStore.latestManifest()
                        + ", or run discovery before HP.");
    }

    private static Optional<String> optionalExplicitFixtureLabel() {
        String fromEnv = System.getenv(FIXTURE_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Optional.of(fromEnv.trim());
        }
        String fromProperty = System.getProperty(FIXTURE_LABEL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Optional.of(fromProperty.trim());
        }
        fromProperty = System.getProperty(FIXTURE_ENV);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Optional.of(fromProperty.trim());
        }
        return Optional.empty();
    }

    private static Optional<String> labelFromManifestIndex(int index) {
        OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
        if (manifest == null || index < 0 || index >= manifest.fixtureCount()) {
            return Optional.empty();
        }
        OfficialFixtureManifestEntry entry = manifest.entryAt(index);
        if (!entry.enabled()) {
            throw new SkipException("Fixture index " + index + " is disabled: " + entry.displayLabel());
        }
        return Optional.of(entry.displayLabel());
    }

    private static void assertMatchesVerified(String expected, String verifiedSelectedLabel) {
        if (verifiedSelectedLabel == null || verifiedSelectedLabel.isBlank()) {
            return;
        }
        if (!expected.equals(verifiedSelectedLabel.trim())) {
            throw new IllegalStateException(
                    "Fixture identity mismatch before submit. Configured="
                            + expected
                            + "; selected="
                            + verifiedSelectedLabel);
        }
    }

    private static void assertEligibleForSubmission(String label) {
        if (KnownFixtureExclusions.isKnownDuplicateFixture(label)) {
            throw new SkipException(
                    "Configured fixture is a known duplicate for submission: "
                            + label
                            + ". "
                            + KnownFixtureExclusions.knownDuplicateReason(label)
                            + " Use another index (e.g. "
                            + DEFAULT_HP_SUBMISSION_INDEX
                            + ") or run discovery.");
        }
    }

    /** Optional fragment for preflight/read-only inspection only. */
    public static String optionalFixtureSearchFragment() {
        String preferred = System.getenv("ZULFI_TEST_MATCH");
        return preferred == null ? "" : preferred.trim();
    }

    public static int submitWaitSeconds() {
        return Math.max(5, parseIntProperty("analysis.create.submitWaitSeconds", 15));
    }

    private static int parseIntProperty(String key, int defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
