package com.taf.core.utils.report;

import com.taf.core.utils.OutputPaths;
import io.qameta.allure.Allure;
import com.taf.core.utils.logs.LogsManager;
import java.io.*;
import java.nio.file.*;

public final class AllureAttachmentManager {
    private AllureAttachmentManager() { }
    public static void attachScreenshot(String name, String path) {
        try (InputStream stream = Files.newInputStream(Path.of(path))) {
            Allure.addAttachment(name, "image/png", stream, ".png");
        } catch (Exception ex) {
            LogsManager.warn("Screenshot attachment unavailable: " + ex.getClass().getSimpleName());
        }
    }
    public static void attachLogs() {
        Path path = OutputPaths.logFile();
        if (!Files.isRegularFile(path)) return;
        try {
            // No global logger shutdown: concurrent tests must never disrupt each other's logs.
            // Logs are not attached wholesale because other test credentials/session data may appear.
            Allure.attachment("Log reference", "Redacted execution logs retained locally: " + path);
        } catch (Exception ex) {
            LogsManager.warn("Log reference unavailable");
        }
    }
    public static void attachRecords(String testMethodName) {
        // Video attachments are made from actual returned recording paths, not guessed file names.
    }
    public static void attachVideo(Path path) {
        if (path == null || !Files.isRegularFile(path)) return;
        try (InputStream stream = Files.newInputStream(path)) {
            Allure.addAttachment("Test recording", "video/mp4", stream, ".mp4");
        } catch (Exception ex) {
            LogsManager.warn("Video attachment unavailable: " + ex.getClass().getSimpleName());
        }
    }
}
