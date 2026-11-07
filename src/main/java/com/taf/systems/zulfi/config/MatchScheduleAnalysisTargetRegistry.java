package com.taf.systems.zulfi.config;

import java.util.Optional;

/** Per-thread schedule fixture pin (safe when TestNG runs classes in parallel). */
public final class MatchScheduleAnalysisTargetRegistry {

    private static final ThreadLocal<String> EXTERNAL_EVENT_ID = new ThreadLocal<>();

    private MatchScheduleAnalysisTargetRegistry() {}

    public static void setExternalEventId(String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            EXTERNAL_EVENT_ID.remove();
            return;
        }
        EXTERNAL_EVENT_ID.set(externalEventId.trim());
    }

    public static Optional<String> externalEventId() {
        String value = EXTERNAL_EVENT_ID.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static void clear() {
        EXTERNAL_EVENT_ID.remove();
    }
}
