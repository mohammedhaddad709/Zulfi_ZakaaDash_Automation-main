package com.taf.core.drivers;

import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;


public class SafariFactory extends AbstractDriver {
    public SafariOptions getOptions() {
        SafariOptions options = new SafariOptions();
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(pageLoadStrategy);
        return options;
    }

    @Override
    public WebDriver createDriver() {
        if (PropertyReader.getProperty("executionType").equalsIgnoreCase("Local") ||
                PropertyReader.getProperty("executionType").equalsIgnoreCase("LocalHeadless") )
        {
            return new SafariDriver(getOptions());
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

