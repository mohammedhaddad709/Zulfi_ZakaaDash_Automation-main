package com.taf.systems.zulfi.analysis.preflight;

import java.util.Locale;

/** Historical exclusions from verified prior runs — not re-submitted during preflight. */
public final class KnownFixtureExclusions {

    public static final String AL_OKHDOOD_X_AL_BUKAIRIYAH = "الأخدود × نادي البكيرية";
    public static final String AL_OKHDOOD_EXTERNAL_EVENT_ID = "16651975";
    public static final String AL_OKHDOOD_MATCH_ID = "6";

    public static final String AL_TAAI_X_AL_BUKAIRIYAH = "نادي الطائي × نادي البكيرية";
    public static final String AL_TAAI_EXTERNAL_EVENT_ID = "16652017";
    public static final String AL_TAAI_MATCH_ID = "9";

    /** @deprecated use {@link #AL_TAAI_MATCH_ID} */
    public static final String MATCH_ID = AL_TAAI_MATCH_ID;

    /** @deprecated use {@link #AL_TAAI_EXTERNAL_EVENT_ID} */
    public static final String EXTERNAL_EVENT_ID = AL_TAAI_EXTERNAL_EVENT_ID;

    private KnownFixtureExclusions() {}

    public static boolean isKnownDuplicateFixture(String fixtureLabel) {
        if (fixtureLabel == null || fixtureLabel.isBlank()) {
            return false;
        }
        String normalized = normalize(fixtureLabel);
        if (normalize(AL_OKHDOOD_X_AL_BUKAIRIYAH).equals(normalized)) {
            return true;
        }
        if (normalize(AL_TAAI_X_AL_BUKAIRIYAH).equals(normalized)) {
            return true;
        }
        return (normalized.contains("الطائي") && normalized.contains("البكيرية"));
    }

    public static String knownDuplicateReason(String fixtureLabel) {
        if (fixtureLabel != null && normalize(fixtureLabel).equals(normalize(AL_OKHDOOD_X_AL_BUKAIRIYAH))) {
            return "Analysis already exists for this fixture (externalEventId="
                    + AL_OKHDOOD_EXTERNAL_EVENT_ID
                    + ", matchId="
                    + AL_OKHDOOD_MATCH_ID
                    + ").";
        }
        if (fixtureLabel != null && normalize(fixtureLabel).equals(normalize(AL_TAAI_X_AL_BUKAIRIYAH))) {
            return "Analysis already exists for this fixture (externalEventId="
                    + AL_TAAI_EXTERNAL_EVENT_ID
                    + ", matchId="
                    + AL_TAAI_MATCH_ID
                    + ").";
        }
        return "Fixture blocked for submission because a prior analysis exists (verified automation run).";
    }

    public static String knownDuplicateReason() {
        return knownDuplicateReason(AL_TAAI_X_AL_BUKAIRIYAH);
    }

    public static boolean isKnownDuplicateExternalEventId(String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            return false;
        }
        String id = externalEventId.trim();
        return AL_OKHDOOD_EXTERNAL_EVENT_ID.equals(id) || AL_TAAI_EXTERNAL_EVENT_ID.equals(id);
    }

    public static boolean isKnownDuplicateMatchId(String matchIdNumeric) {
        if (matchIdNumeric == null || matchIdNumeric.isBlank()) {
            return false;
        }
        String id = matchIdNumeric.trim();
        return AL_OKHDOOD_MATCH_ID.equals(id) || AL_TAAI_MATCH_ID.equals(id);
    }

    static String normalize(String label) {
        return label.replaceAll("\\s+", " ").trim();
    }
}
