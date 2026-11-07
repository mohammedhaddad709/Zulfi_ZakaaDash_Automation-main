package com.taf.core.drivers;

import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;

import java.util.HashMap;
import java.util.Map;

public class FirefoxFactory extends AbstractDriver {
    private FirefoxOptions getOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.setPageLoadStrategy(pageLoadStrategy);
        options.addArguments("--start-maximized");
        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = downloadsPath;
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.default_directory",downloadPath);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);
        
        options.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        FirefoxProfile profile = new FirefoxProfile();
        // No default extension injection in the portal tests.
        options.setProfile(profile);
        switch (PropertyReader.getProperty("executionType"))
        {
            case "LocalHeadless" -> options.addArguments("--headless=new");
            case  "Remote" ->
            {
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-extensions");
                options.addArguments("--headless=new");
            }
        }
        return options;
    }

    @Override
    public WebDriver createDriver() {
        if (PropertyReader.getProperty("executionType").equalsIgnoreCase("Local") ||
                PropertyReader.getProperty("executionType").equalsIgnoreCase("LocalHeadless") )
        {
            return new FirefoxDriver(getOptions());
        }
        else if (PropertyReader.getProperty("executionType").equalsIgnoreCase("Remote")) {
            return createRemote(getOptions());

        }
        else {
            LogsManager.error("Invalid execution type: " + PropertyReader.getProperty("executionType"));
            throw new IllegalArgumentException("Invalid execution type: " + PropertyReader.getProperty("executionType"));
        }
    }
}
