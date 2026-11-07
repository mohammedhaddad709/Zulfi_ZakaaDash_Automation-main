package com.taf.systems.zulfi.config;

import org.testng.SkipException;

import java.util.Locale;

/**
 * Shared parsing and enforcement for every phased feature gate. A gate keeps its own {@code Phase} enum
 * ({@code NONE} first, then {@code A}, {@code B}, …) and its public API; only the logic lives here.
 *
 * <p>Semantics: property unset, blank or unknown → {@code NONE} → every phased test skips (fail-safe).
 */
public final class PhaseGates {

    private PhaseGates() {}

    /** {@code -D<property>=b} → {@code Phase.B}; anything else → the enum's {@code NONE}. */
    public static <E extends Enum<E>> E parse(String property, Class<E> phases) {
        E none = Enum.valueOf(phases, "NONE");
        String raw = System.getProperty(property, "none");
        if (raw == null || raw.isBlank()) {
            return none;
        }
        String letter = raw.trim().toUpperCase(Locale.ROOT);
        for (E phase : phases.getEnumConstants()) {
            if (phase != none && phase.name().equals(letter)) {
                return phase;
            }
        }
        return none;
    }

    /** Skips the calling test unless {@code current} is at least {@code minimum}. */
    public static <E extends Enum<E>> void require(String property, E current, E minimum, String feature, String strategyDoc) {
        boolean disabled = "NONE".equals(current.name());
        if (disabled || current.ordinal() < minimum.ordinal()) {
            throw new SkipException(
                    feature + " tests require -D" + property + "=" + minimum.name().toLowerCase(Locale.ROOT)
                            + " (current=" + (disabled ? "none" : current.name().toLowerCase(Locale.ROOT)) + "). See "
                            + strategyDoc);
        }
    }
}
