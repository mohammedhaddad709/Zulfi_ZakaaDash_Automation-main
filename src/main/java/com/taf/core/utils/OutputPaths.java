package com.taf.core.utils;

import java.nio.file.Path;

/**
 * Single source of truth for every artifact location written during a run.
 *
 * <p>{@code -Doutput.dir} sets the run root (default {@code target}); the embedded runner passes
 * {@code runner/runs/<id>/output} so concurrent runs never share screenshots, logs, downloads or
 * Allure results. {@code -Dallure.results.directory} and {@code -Dreports.dir} can be set
 * independently; when absent they derive from the run root / project defaults.
 */
public final class OutputPaths {
    private OutputPaths() { }

    public static Path root() {
        return Path.of(System.getProperty("output.dir", "target")).toAbsolutePath().normalize();
    }

    public static Path allureResults() {
        String explicit = System.getProperty("allure.results.directory");
        return explicit == null || explicit.isBlank()
                ? root().resolve("allure-results")
                : Path.of(explicit).toAbsolutePath().normalize();
    }

    public static Path screenshots() { return root().resolve("screenshots"); }
    public static Path recordings() { return root().resolve("recordings"); }
    public static Path downloads() { return root().resolve("downloads"); }
    public static Path logs() { return root().resolve("Logs"); }
    public static Path logFile() { return logs().resolve("logs.log"); }
    public static Path bugReports() { return root().resolve("bug-reports"); }

    /** Published run archives (Zulfi {@code test-execution-reports}); the runner points this inside the run folder. */
    public static Path reportsRoot() {
        return Path.of(System.getProperty("reports.dir", "test-execution-reports")).toAbsolutePath().normalize();
    }
}
