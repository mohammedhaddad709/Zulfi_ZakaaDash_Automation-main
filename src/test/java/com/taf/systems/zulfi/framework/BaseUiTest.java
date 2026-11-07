package com.taf.systems.zulfi.framework;

import com.taf.core.drivers.GUIDriver;
import com.taf.core.drivers.WebDriverProvider;
import com.taf.core.pacing.HumanPacingManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

/** One clean browser per test method, no shared login state across cases. */
public abstract class BaseUiTest implements WebDriverProvider {
    private GUIDriver session;

    /** When true, {@link #createFreshBrowser()} runs once per class; {@link #closeBrowser()} runs in {@link AfterClass}. */
    protected boolean reuseBrowserAcrossMethods() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public final void createFreshBrowser() {
        if (!reuseBrowserAcrossMethods() || session == null) {
            if (session != null) {
                session.quitDriver();
            }
            session = new GUIDriver();
            HumanPacingManager.pauseForTransition();
        }
    }

    @AfterMethod(alwaysRun = true)
    public final void closeBrowser() {
        if (!reuseBrowserAcrossMethods() && session != null) {
            session.quitDriver();
            session = null;
        }
    }

    @AfterClass(alwaysRun = true)
    public final void closeSharedBrowserAfterClass() {
        if (reuseBrowserAcrossMethods() && session != null) {
            session.quitDriver();
            session = null;
        }
    }
    protected final WebDriver browser() {
        WebDriver current = getWebDriver();
        if (current == null) throw new IllegalStateException("WebDriver not initialized");
        return current;
    }
    @Override public final WebDriver getWebDriver() { return session == null ? null : session.get(); }
}
