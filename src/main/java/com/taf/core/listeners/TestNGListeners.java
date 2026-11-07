package com.taf.core.listeners;

import com.taf.core.drivers.WebDriverProvider;
import com.taf.core.media.BrowserVideoRecorder;
import com.taf.core.media.EvidenceRegistry;
import com.taf.core.media.ScreenRecordManager;
import com.taf.core.media.VideoEvidence;
import com.taf.core.media.ScreenshotsManager;
import com.taf.core.pacing.HumanPacingManager;
import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import com.taf.core.utils.report.AllureAttachmentManager;
import com.taf.core.utils.report.AllureEnvironmentManager;
import com.taf.core.validations.Validation;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.testng.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

/**
 * Shared TAF lifecycle for every system. Assertions finish before the final-status screenshot.
 * System-specific reporting is delegated to {@link SystemReportingHooks}.
 * Emits {@code [TAF-PROGRESS]} lines on stdout that the embedded runner parses for live progress.
 */
public final class TestNGListeners implements IExecutionListener, IInvokedMethodListener, ITestListener, IConfigurationListener {

    private static final List<SystemReportingHooks> HOOKS = loadHooks();

    private static List<SystemReportingHooks> loadHooks() {
        List<SystemReportingHooks> hooks = new ArrayList<>();
        ServiceLoader.load(SystemReportingHooks.class).forEach(hooks::add);
        return List.copyOf(hooks);
    }

    private static List<SystemReportingHooks> systemHooks() {
        String system = PropertyReader.activeSystem();
        return HOOKS.stream().filter(h -> h.ownsSystem(system)).toList();
    }

    private static List<SystemReportingHooks> testHooks(ITestResult result) {
        return HOOKS.stream().filter(h -> h.ownsTest(result)).toList();
    }

    @Override
    public void onExecutionStart() {
        PropertyReader.loadProperties();
        resetCurrentRun(OutputPaths.allureResults());
        resetCurrentRun(OutputPaths.screenshots());
        AllureEnvironmentManager.setEnvironmentVariables();
        systemHooks().forEach(SystemReportingHooks::onExecutionStart);
        LogsManager.info("Fresh TAF execution started: system=" + PropertyReader.activeSystem()
                + ", output=" + OutputPaths.root());
    }

    @Override
    public void onExecutionFinish() {
        systemHooks().forEach(SystemReportingHooks::onExecutionFinish);
        LogsManager.info("TAF execution completed");
    }

    @Override
    public void onTestStart(ITestResult result) {
        progress("START", result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testHooks(result).forEach(h -> h.onTestOutcome(result));
        progress("PASS", result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        testHooks(result).forEach(h -> h.onTestOutcome(result));
        progress("FAIL", result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        testHooks(result).forEach(h -> h.onTestOutcome(result));
        if (result.getThrowable() != null) {
            AllureAttachmentManager.attachLogs();
            Allure.addAttachment(
                    "Skip reason",
                    "text/plain",
                    "Screenshot unavailable — test was skipped: " + result.getThrowable().getMessage());
        }
        progress("SKIP", result);
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        EvidenceRegistry.reset();
        WebDriver driver = driverOf(result);
        testHooks(result).forEach(h -> h.beforeTest(result, driver));
        if (result.getInstance() instanceof WebDriverProvider && videoMode(result) != VideoEvidence.Mode.OFF) {
            if (isScreenRecorderEngine()) {
                ScreenRecordManager.startRecording();
            } else if (driver != null) {
                BrowserVideoRecorder.start(driver, result.getName());
            }
        }
    }

    /** Explicit -Dreport.video wins; otherwise the test class's {@link VideoEvidence}; otherwise the file default. */
    static VideoEvidence.Mode videoMode(ITestResult result) {
        String explicit = System.getProperty("report.video");
        String raw = explicit;
        if (raw == null || raw.isBlank()) {
            Class<?> testClass = result.getTestClass() == null ? null : result.getTestClass().getRealClass();
            VideoEvidence annotation = testClass == null ? null : testClass.getAnnotation(VideoEvidence.class);
            if (annotation != null) return annotation.value();
            raw = PropertyReader.getProperty("report.video");
        }
        try {
            return VideoEvidence.Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException unknown) {
            return VideoEvidence.Mode.OFF;
        }
    }

    /** {@code -Dreport.video.engine=screen} keeps the legacy desktop recorder (visible local runs only). */
    private static boolean isScreenRecorderEngine() {
        return "screen".equalsIgnoreCase(PropertyReader.getProperty("report.video.engine"));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        Validation.assertAll(result); // MUST precede screenshot/status handling.
        List<SystemReportingHooks> hooks = testHooks(result);
        WebDriver driver = driverOf(result);
        hooks.forEach(h -> h.afterAssertions(result, driver));
        if (driver != null) {
            HumanPacingManager.pauseForFinalObservation();
            String status =
                    switch (result.getStatus()) {
                        case ITestResult.SUCCESS -> "PASS";
                        case ITestResult.FAILURE -> "FAIL";
                        case ITestResult.SKIP -> "SKIP";
                        default -> "UNKNOWN";
                    };
            String stem = evidenceStem(hooks, result) + "_" + sanitize(result.getName()) + "_" + status;
            ScreenshotsManager.takeFullPageScreenshot(driver, stem);
        }

        if (result.getStatus() == ITestResult.FAILURE) {
            hooks.forEach(h -> h.onTestFailure(result, driver));
        }

        VideoEvidence.Mode mode = videoMode(result);
        boolean keepVideo = mode == VideoEvidence.Mode.ALWAYS
                || (mode == VideoEvidence.Mode.ON_FAILURE && result.getStatus() == ITestResult.FAILURE);
        Path video = ScreenRecordManager.stopRecording(result.getName());
        if (video != null && keepVideo) {
            EvidenceRegistry.record(EvidenceRegistry.Kind.VIDEO, result.getName(), video);
        }
        if (BrowserVideoRecorder.isRecording()) {
            if (keepVideo) {
                video = BrowserVideoRecorder.stopAndSave(evidenceStem(hooks, result) + "_" + sanitize(result.getName()));
            } else {
                BrowserVideoRecorder.stopAndDiscard();
            }
        }
        if (keepVideo) {
            AllureAttachmentManager.attachVideo(video);
        }
        if (result.getThrowable() != null) {
            AllureAttachmentManager.attachLogs();
            hooks.forEach(h -> h.onTestThrowable(result, driver));
        }
        hooks.forEach(h -> h.afterTest(result));
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        LogsManager.warn("Test setup/teardown failed: " + result.getName());
        WebDriver driver = driverOf(result);
        List<SystemReportingHooks> hooks = testHooks(result);
        if (driver != null) {
            ScreenshotsManager.takeFullPageScreenshot(driver, evidenceStem(hooks, result) + "_SETUP_FAILURE");
        }
        hooks.forEach(h -> h.onConfigurationFailure(result, driver));
    }

    private static WebDriver driverOf(ITestResult result) {
        return result.getInstance() instanceof WebDriverProvider provider ? provider.getWebDriver() : null;
    }

    private static String evidenceStem(List<SystemReportingHooks> hooks, ITestResult result) {
        for (SystemReportingHooks hook : hooks) {
            String stem = hook.evidenceStem(result);
            if (stem != null && !stem.isBlank()) return stem;
        }
        return sanitize(result.getMethod().getMethodName());
    }

    private static void progress(String status, ITestResult result) {
        String className = result.getTestClass() == null ? "unknown" : result.getTestClass().getName();
        System.out.println("[TAF-PROGRESS] " + status + " " + className + "#" + result.getMethod().getMethodName());
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void resetCurrentRun(Path directory) {
        try {
            if (Files.exists(directory)) {
                try (var paths = Files.walk(directory)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
            }
            Files.createDirectories(directory);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare clean test artifacts at " + directory, ex);
        }
    }
}
