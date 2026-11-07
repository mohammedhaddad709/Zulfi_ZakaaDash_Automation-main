package com.taf.systems.zulfi.config;


/** Configurable Default Club for Home results and Schedule deep-link (BR-HOME-RES-009). */
public final class DefaultClubConfig {

    private static final String LABEL_PROPERTY = "portal.defaultClub.label";
    private static final String FRAGMENT_PROPERTY = "portal.defaultClub.fragment";
    private static final String LABEL_ENV = "ZULFI_DEFAULT_CLUB";
    private static final String FRAGMENT_ENV = "ZULFI_DEFAULT_CLUB_FRAGMENT";

    private DefaultClubConfig() {}

    public static String configuredDefaultClubLabel() {
        String fromEnv = System.getenv(LABEL_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(LABEL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromFile = ZulfiConfig.get(LABEL_PROPERTY);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return "نادي الزلفي";
    }

    /** Substring used in cards and club filter assertions (e.g. الزلفي). */
    public static String clubMatchFragment() {
        String fromEnv = System.getenv(FRAGMENT_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(FRAGMENT_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromFile = ZulfiConfig.get(FRAGMENT_PROPERTY);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        String label = configuredDefaultClubLabel();
        if (label.contains("الزلفي")) {
            return "الزلفي";
        }
        return label.length() > 4 ? label.substring(0, Math.min(label.length(), 8)) : label;
    }
}
