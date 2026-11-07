package com.taf.systems.zulfi.reporting;

import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Non-invasive report publisher: a broken viewer never alters test pass/fail exit status. */
public final class RunReportPublisher {
    private RunReportPublisher() { }
    public static void publishSafely() {
        try { publish(); }
        catch (Exception ex) { LogsManager.warn("Allure publishing warning: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
    }
    private static void publish() throws IOException, InterruptedException {
        Path results = OutputPaths.allureResults();
        if (!Files.isDirectory(results)) return;
        try (var files = Files.list(results)) {
            if (files.noneMatch(p -> p.getFileName().toString().endsWith("-result.json"))) {
                LogsManager.info("No test cases executed: Allure HTML is intentionally not generated.");
                return;
            }
        }
        String runId =
                ExecutionSummaryCollector.runId() != null
                        ? ExecutionSummaryCollector.runId()
                        : DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS")
                                .withZone(ZoneOffset.UTC)
                                .format(Instant.now());
        Path run = OutputPaths.reportsRoot().resolve("runs").resolve(runId);
        Path latest = OutputPaths.reportsRoot().resolve("latest");
        Path history = OutputPaths.reportsRoot().resolve("history").resolve(runId);
        Files.createDirectories(run);
        Files.writeString(run.resolve("RUN_ID.txt"), runId);
        copyTree(results, run.resolve("allure-results"));
        Path captures = OutputPaths.screenshots();
        if (Files.isDirectory(captures)) copyTree(captures, run.resolve("screenshots"));

        Path html = run.resolve("allure-html");
        Files.createDirectories(html);
        List<String> cmd = new ArrayList<>(AllureCliResolver.resolveCommand().orElseGet(() -> List.of("allure")));
        cmd.addAll(
                List.of(
                        "generate",
                        results.toAbsolutePath().toString(),
                        "-o",
                        html.toAbsolutePath().toString(),
                        "--clean",
                        "--single-file"));
        boolean generated = false;
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(run.resolve("allure-generation.log").toFile()).start();
            if (!process.waitFor(90, TimeUnit.SECONDS)) { process.destroyForcibly(); LogsManager.warn("Allure generation timeout"); }
            else generated = process.exitValue() == 0 && Files.isRegularFile(html.resolve("index.html"));
        } catch (IOException e) { LogsManager.warn("Allure CLI not available; raw results preserved; install Allure 2 CLI or set ALLURE_CLI"); }
        if (!generated) Files.writeString(run.resolve("REPORT_NOT_GENERATED.txt"),
                "Raw Allure results were collected. A compatible Allure CLI with --single-file support is required to build HTML.\n");
        copyTree(run, history);
        if (Files.exists(latest)) deleteTree(latest);
        copyTree(run, latest);
        writeLatestRunPointer(runId, generated, run);
        archiveBugReports(run);
        if (generated && PropertyReader.getProperty("report.open").equalsIgnoreCase("true")
                && !isCi() && Desktop.isDesktopSupported() && !isHeadless()) {
            try { Desktop.getDesktop().browse(latest.resolve("allure-html/index.html").toAbsolutePath().toUri()); }
            catch (Exception e) { LogsManager.warn("Browser open skipped: " + e.getClass().getSimpleName()); }
        }
        LogsManager.info("Run archive=" + history.toAbsolutePath());
    }
    private static boolean isCi() { return System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null; }
    private static boolean isHeadless() { return java.awt.GraphicsEnvironment.isHeadless() || PropertyReader.getProperty("executionType").toLowerCase().contains("headless"); }
    private static void deleteTree(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private static void writeLatestRunPointer(String runId, boolean generated, Path run) throws IOException {
        Path props = OutputPaths.reportsRoot().resolve("latest-run.properties");
        Files.createDirectories(props.getParent());
        String body =
                "runId="
                        + runId
                        + "\n"
                        + "runDirectory="
                        + run.toAbsolutePath()
                        + "\n"
                        + "allureHtmlGenerated="
                        + generated
                        + "\n"
                        + "allureHtmlPath="
                        + run.resolve("allure-html/index.html").toAbsolutePath()
                        + "\n";
        Files.writeString(props, body);
    }

    private static void archiveBugReports(Path run) throws IOException {
        Path bugs = OutputPaths.bugReports();
        if (!Files.isDirectory(bugs)) {
            return;
        }
        Path dest = run.resolve("bugs");
        Files.createDirectories(dest);
        try (var files = Files.list(bugs)) {
            for (Path file : files.toList()) {
                if (Files.isRegularFile(file)) {
                    Files.copy(file, dest.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (var files = Files.walk(from)) {
            for (Path source : files.toList()) {
                Path destination = to.resolve(from.relativize(source));
                if (Files.isDirectory(source)) Files.createDirectories(destination);
                else { Files.createDirectories(destination.getParent()); Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING); }
            }
        }
    }
}
