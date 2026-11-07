package com.taf.systems.zulfi.analysis.preflight;


import java.util.Locale;
/** Parses fixture display labels into home/away when structure allows. */
final class FixtureLabelParser {

    private FixtureLabelParser() {}

    static String[] splitTeams(String label) {
        if (label == null || label.isBlank()) {
            return new String[] {"Not available", "Not available"};
        }
        if (label.contains("×")) {
            String[] parts = label.split("×", 2);
            if (parts.length == 2) {
                return new String[] {parts[0].trim(), parts[1].trim()};
            }
        }
        String lower = label.toLowerCase(Locale.ROOT);
        for (String sep : new String[] {" vs ", " v ", " ضد "}) {
            int idx = lower.indexOf(sep.trim());
            if (idx > 0) {
                return new String[] {
                    label.substring(0, idx).trim(), label.substring(idx + sep.length()).trim()
                };
            }
        }
        return new String[] {"Not available", "Not available"};
    }
}
