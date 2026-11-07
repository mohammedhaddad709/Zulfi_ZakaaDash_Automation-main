package com.taf.systems.zulfi.analysis.fixture;

import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import org.testng.SkipException;

/** Picks an official fixture index eligible for authorized Happy Path submission. */
public final class OfficialFixtureSubmissionSelector {

    private OfficialFixtureSubmissionSelector() {}

    public static OfficialFixtureManifestEntry resolveForHappyPathSubmission(OfficialFixtureManifest manifest) {
        int preferred = AnalysisCreateGate.configuredFixtureIndex().orElse(AnalysisCreateGate.DEFAULT_HP_SUBMISSION_INDEX);
        if (manifest != null) {
            if (isEligible(manifest, preferred)) {
                return manifest.entryAt(preferred);
            }
            for (int i = 0; i < manifest.fixtureCount(); i++) {
                if (isEligible(manifest, i)) {
                    return manifest.entryAt(i);
                }
            }
            throw new SkipException(
                    "No enabled non-duplicate official fixture found in manifest for Happy Path submission.");
        }
        if (KnownFixtureExclusions.isKnownDuplicateFixture(labelHintForIndex(preferred))) {
            throw new SkipException(
                    "Preferred fixture index "
                            + preferred
                            + " is a known duplicate for submission. Run discovery or set -Danalysis.fixture.index.");
        }
        throw new SkipException(
                "Manifest missing; run ANL-OFF-DIS-001 before authorized HP or set "
                        + "ZULFI_OFFICIAL_FIXTURE with an eligible fixture.");
    }

    public static int resolveSubmissionIndex(OfficialFixtureManifest manifest) {
        return resolveForHappyPathSubmission(manifest).fixtureIndex();
    }

    private static boolean isEligible(OfficialFixtureManifest manifest, int index) {
        if (index < 0 || index >= manifest.fixtureCount()) {
            return false;
        }
        OfficialFixtureManifestEntry entry = manifest.entryAt(index);
        return entry.enabled() && !KnownFixtureExclusions.isKnownDuplicateFixture(entry.displayLabel());
    }

    private static String labelHintForIndex(int index) {
        if (index == 0) {
            return KnownFixtureExclusions.AL_OKHDOOD_X_AL_BUKAIRIYAH;
        }
        if (index == 1) {
            return KnownFixtureExclusions.AL_TAAI_X_AL_BUKAIRIYAH;
        }
        return "";
    }
}
