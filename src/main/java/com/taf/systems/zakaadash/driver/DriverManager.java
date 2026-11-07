package com.taf.systems.zakaadash.driver;

import com.taf.core.drivers.GUIDriver;
import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.core.utils.dataReader.PropertyReader;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * ZakaaDash-only browser holder. Browsers are created by the shared core {@link GUIDriver}
 * (browserType / executionType: Local, LocalHeadless, Remote Grid); this holder is never shared with Zulfi.
 */
public final class DriverManager {

    private static final ThreadLocal<GUIDriver> SESSION = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void startDriver() {
        if (SESSION.get() != null) {
            throw new IllegalStateException("A WebDriver is already running on thread " + Thread.currentThread().getName());
        }
        // ZakaaDash pages were written against full page loads (Selenium default NORMAL), not TAF's EAGER.
        GUIDriver session = new GUIDriver(PageLoadStrategy.NORMAL);
        SESSION.set(session);
        WebDriver driver = session.get();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigManager.config().pageLoadTimeoutSeconds()));
        driver.manage().window().maximize();
        if (isHeadless()) {
            var config = ConfigManager.config();
            driver.manage().window().setSize(new Dimension(config.headlessWindowWidth(), config.headlessWindowHeight()));
        }
    }

    private static boolean isHeadless() {
        String executionType = PropertyReader.getProperty("executionType");
        return "LocalHeadless".equalsIgnoreCase(executionType) || "Remote".equalsIgnoreCase(executionType);
    }

    public static WebDriver getDriver() {
        GUIDriver session = SESSION.get();
        if (session == null) {
            throw new IllegalStateException("No WebDriver started on thread " + Thread.currentThread().getName());
        }
        return session.get();
    }

    public static boolean hasDriver() {
        return SESSION.get() != null;
    }

    public static void quitDriver() {
        GUIDriver session = SESSION.get();
        try {
            if (session != null) {
                session.quitDriver();
            }
        } finally {
            SESSION.remove();
        }
    }
}
