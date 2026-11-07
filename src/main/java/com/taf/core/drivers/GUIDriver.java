package com.taf.core.drivers;

import com.taf.core.utils.actions.*;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.validations.Validation;
import com.taf.core.validations.Verification;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ThreadGuard;

/** New instance per test case; never share a browser between test methods. */
public final class GUIDriver {
    private final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    public GUIDriver() {
        this(null);
    }

    /** @param pageLoadStrategy system-specific strategy; {@code null} keeps the factory default (EAGER). */
    public GUIDriver(PageLoadStrategy pageLoadStrategy) {
        String configured = PropertyReader.getProperty("browserType");
        Browser browser;
        try {
            browser = Browser.valueOf(configured.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unsupported browserType '" + configured
                    + "'. Supported: CHROME, EDGE, FIREFOX, SAFARI", ex);
        }
        AbstractDriver factory = browser.getDriverFactory().withPageLoadStrategy(pageLoadStrategy);
        driverThreadLocal.set(ThreadGuard.protect(factory.createDriver()));
    }
    public WebDriver get() { return driverThreadLocal.get(); }
    public ElementActions element() { return new ElementActions(get()); }
    public BrowserActions browser() { return new BrowserActions(get()); }
    public FrameActions frame() { return new FrameActions(get()); }
    public AlertActions alert() { return new AlertActions(get()); }
    public Validation validation() { return new Validation(get()); }
    public Verification verification() { return new Verification(get()); }
    public void quitDriver() {
        WebDriver current = driverThreadLocal.get();
        try { if (current != null) current.quit(); }
        finally { driverThreadLocal.remove(); }
    }
}
