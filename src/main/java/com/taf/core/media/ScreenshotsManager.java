package com.taf.core.media;

import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.report.AllureAttachmentManager;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

/** Browser-viewport evidence; never reports a false full-page capability. */
public final class ScreenshotsManager {
    private ScreenshotsManager() { }
    /** Captures the viewport, attaches it to Allure, registers it as evidence; returns the file or null. */
    public static Path takeFullPageScreenshot(WebDriver driver, String screenshotName) {
        if (!(driver instanceof TakesScreenshot shot)) {
            LogsManager.warn("Screenshot unavailable: browser session not ready");
            return null;
        }
        try {
            Path destination = OutputPaths.screenshots();
            Files.createDirectories(destination);
            String safe = screenshotName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path image = destination.resolve(safe + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".png");
            Files.write(image, shot.getScreenshotAs(OutputType.BYTES));
            AllureAttachmentManager.attachScreenshot(screenshotName, image.toAbsolutePath().toString());
            EvidenceRegistry.record(EvidenceRegistry.Kind.SCREENSHOT, screenshotName, image);
            return image;
        } catch (Exception e) {
            LogsManager.warn("Screenshot capture unavailable: " + e.getClass().getSimpleName());
            return null;
        }
    }
    public static void takeElementScreenshot(WebDriver driver, By selector) {
        try {
            Path dir = OutputPaths.screenshots();
            Files.createDirectories(dir);
            Path output = dir.resolve("element-" + UUID.randomUUID() + ".png");
            Files.write(output, driver.findElement(selector).getScreenshotAs(OutputType.BYTES));
            AllureAttachmentManager.attachScreenshot("Element", output.toString());
        } catch (Exception e) {
            LogsManager.warn("Element screenshot unavailable: " + e.getClass().getSimpleName());
        }
    }
}
