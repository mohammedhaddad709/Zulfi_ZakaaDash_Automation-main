package com.taf.systems.zakaadash.utils;

import io.qameta.allure.Allure;
import com.taf.core.utils.OutputPaths;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

        private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    public static Path captureAndAttach(WebDriver driver, String name) {
        byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Path screenshotDirectory = OutputPaths.screenshots();
        Path file = screenshotDirectory.resolve(name + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png");
        try {
            Files.createDirectories(screenshotDirectory);
            Files.write(file, png);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not save screenshot to " + file, ex);
        }
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
        return file.toAbsolutePath();
    }
}
