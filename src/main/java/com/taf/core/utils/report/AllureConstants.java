package com.taf.core.utils.report;
import com.taf.core.utils.OutputPaths;
import java.nio.file.Path;

/** Local filesystem paths (correct on Windows and Linux). */
public final class AllureConstants {
    private AllureConstants() { }
    public static final Path USER_DIR = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    public static final Path USER_HOME = Path.of(System.getProperty("user.home")).toAbsolutePath();
    public static final Path RESULTS_FOLDER = OutputPaths.allureResults();
    public static final Path REPORT_PATH = OutputPaths.reportsRoot().resolve("latest/allure-html");
    public static final Path FULL_REPORT_PATH = REPORT_PATH;
    public static final Path HISTORY_FOLDER = OutputPaths.reportsRoot().resolve("history");
    public static final Path RESULTS_HISTORY_FOLDER = RESULTS_FOLDER.resolve("history");
    public static final String INDEX_HTML = "index.html";
    public static final String REPORT_PREFIX = "AllureReport_";
    public static final String REPORT_EXTENSION = ".html";
    public static final String ALLURE_ZIP_BASE_URL = "https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/";
    public static final Path EXTRACTION_DIR = USER_HOME.resolve(".m2/repository/allure");
}
