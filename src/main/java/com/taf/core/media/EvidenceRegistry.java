package com.taf.core.media;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Evidence files actually produced by the current test method (screenshots, video), in capture order.
 * Reporting reads this list instead of guessing file names, so every bug report links real files.
 */
public final class EvidenceRegistry {

    public enum Kind { SCREENSHOT, VIDEO }

    public record Evidence(Kind kind, String label, Path path, Instant capturedAt) { }

    private static final ThreadLocal<List<Evidence>> CURRENT = ThreadLocal.withInitial(ArrayList::new);

    private EvidenceRegistry() { }

    /** Called by the shared listener when a test method starts. */
    public static void reset() {
        CURRENT.get().clear();
    }

    public static void record(Kind kind, String label, Path path) {
        if (path != null) {
            CURRENT.get().add(new Evidence(kind, label, path.toAbsolutePath().normalize(), Instant.now()));
        }
    }

    public static List<Evidence> current() {
        return List.copyOf(CURRENT.get());
    }

    public static List<Evidence> current(Kind kind) {
        return CURRENT.get().stream().filter(e -> e.kind() == kind).toList();
    }
}
