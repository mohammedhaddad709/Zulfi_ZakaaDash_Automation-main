package com.taf.systems.zulfi.validations;


import java.util.Locale;
/** PrimeVue official-match dropdown: fixture rows vs round headers (الجولة N). */
public final class OfficialFixtureLabelRules {

    public static final String OFFICIAL_MATCH_PLACEHOLDER = "اختر مباراة رسمية";

    private OfficialFixtureLabelRules() {}

    public static boolean isEligibleFixtureRow(String text) {
        if (text == null || text.isEmpty() || OFFICIAL_MATCH_PLACEHOLDER.equals(text)) {
            return false;
        }
        if (text.startsWith("الجولة")) {
            return false;
        }
        if (text.contains("×") || text.contains(" ضد ")) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains(" vs ") || lower.contains(" v ");
    }
}
