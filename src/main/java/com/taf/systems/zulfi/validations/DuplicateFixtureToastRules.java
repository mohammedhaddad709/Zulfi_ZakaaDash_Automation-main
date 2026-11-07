package com.taf.systems.zulfi.validations;

/** Classifies duplicate-fixture toast copy for defect reporting. */
public final class DuplicateFixtureToastRules {

    private DuplicateFixtureToastRules() {}

    public static boolean isDuplicateFixtureMessage(String toastBody) {
        if (toastBody == null || toastBody.isBlank()) {
            return false;
        }
        String lower = toastBody.toLowerCase();
        return lower.contains("already exists")
                || lower.contains("duplicate")
                || toastBody.contains("موجود")
                || toastBody.contains("مسبق");
    }

    /**
     * English technical toast (externalEventId/matchId) on an Arabic UI — product i18n/UX defect.
     */
    public static boolean isUnlocalizedTechnicalDuplicateToast(String toastBody) {
        if (toastBody == null || toastBody.isBlank()) {
            return false;
        }
        boolean english =
                toastBody.contains("An analysis already exists")
                        || toastBody.contains("externalEventId")
                        || toastBody.contains("matchId");
        boolean lacksArabicUserMessage =
                !toastBody.contains("تحليل") && !toastBody.contains("مباراة") && !toastBody.contains("موجود");
        return english && lacksArabicUserMessage;
    }
}
