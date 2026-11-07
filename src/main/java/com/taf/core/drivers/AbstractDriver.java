package com.taf.core.drivers;

import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;

public abstract class AbstractDriver {
    protected final String remoteHost = PropertyReader.getProperty("remoteHost");
    protected final String remotePort = PropertyReader.getProperty("remotePort");
    // Original sample CRX deliberately removed; no browser extension required.
    protected final String downloadsPath = OutputPaths.downloads().toString();
    /** Systems may choose a strategy; the TAF default stays EAGER. */
    protected PageLoadStrategy pageLoadStrategy = PageLoadStrategy.EAGER;

    public abstract WebDriver createDriver();

    AbstractDriver withPageLoadStrategy(PageLoadStrategy strategy) {
        if (strategy != null) this.pageLoadStrategy = strategy;
        return this;
    }

    /** Selenium Grid session; uploads stream local files to the node. */
    protected WebDriver createRemote(Capabilities options) {
        String url = "http://" + remoteHost + ":" + remotePort + "/wd/hub";
        try {
            RemoteWebDriver driver = new RemoteWebDriver(new URI(url).toURL(), options);
            driver.setFileDetector(new LocalFileDetector());
            return driver;
        } catch (Exception e) {
            LogsManager.error("Error creating RemoteWebDriver at " + url + ": " + e.getMessage());
            throw new IllegalStateException(
                    "Failed to create RemoteWebDriver at " + url + " (check -DremoteHost / -DremotePort)", e);
        }
    }
}
