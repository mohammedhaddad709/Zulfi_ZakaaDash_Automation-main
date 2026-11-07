package com.taf.systems.zakaadash.base;

import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.driver.DriverManager;
import com.taf.systems.zakaadash.reporting.TestCaseMetadata;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.ScreenshotUtil;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;

/** ZakaaDash system base test: one fresh shared-core browser per test method, failure evidence on failure. */
public abstract class ZakaaDashBaseTest {

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        DriverManager.startDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE && DriverManager.hasDriver()) {
                recordFailureEvidence(result);
            } else if (result.getStatus() == ITestResult.SUCCESS && DriverManager.hasDriver()) {
                ScreenshotUtil.captureAndAttach(DriverManager.getDriver(),
                        TestCaseMetadata.from(result).testCaseId() + "_pass");
            }
        } catch (Throwable reportingError) {
            Reporter.log("[ZakaaDash] Failure evidence capture error (original test failure preserved): "
                    + reportingError.getMessage(), true);
        } finally {
            DriverManager.quitDriver();
        }
    }

    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    private void recordFailureEvidence(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();
        TestCaseMetadata metadata = TestCaseMetadata.from(result);

        BusinessStep.writeAttribute(result, BusinessStep.ENVIRONMENT, environmentSummary());

        String url = "N/A";
        String pageTitle = "N/A";
        try {
            url = driver.getCurrentUrl();
            pageTitle = driver.getTitle();
            BusinessStep.writeAttribute(result, BusinessStep.FAILURE_URL, url);
            BusinessStep.writeAttribute(result, BusinessStep.PAGE_TITLE, pageTitle);
        } catch (WebDriverException ex) {
            Reporter.log("[ZakaaDash] Browser state unavailable during failure capture: " + ex.getMessage(), true);
        }

        try {
            Path screenshot = ScreenshotUtil.captureAndAttach(driver, metadata.testCaseId() + "_failure");
            BusinessStep.writeAttribute(result, BusinessStep.SCREENSHOT_PATH, screenshot.toAbsolutePath().toString());
        } catch (Throwable screenshotError) {
            BusinessStep.writeAttribute(result, BusinessStep.SCREENSHOT_PATH, "Unavailable: " + screenshotError.getMessage());
            Reporter.log("[ZakaaDash] Screenshot capture failed (original test failure preserved): "
                    + screenshotError.getMessage(), true);
        }

        Throwable error = result.getThrowable();
        String context = """
                Test Case ID: %s
                Test Type: %s
                Failed step: %s
                Last completed step: %s
                Current page: %s
                URL: %s
                Page title: %s
                Screenshot: %s
                Exception: %s
                """.formatted(
                metadata.testCaseId(),
                metadata.testType(),
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_STEP),
                BusinessStep.readAttribute(result, BusinessStep.LAST_COMPLETED_STEP),
                BusinessStep.readAttribute(result, BusinessStep.CURRENT_PAGE),
                url,
                pageTitle,
                BusinessStep.readAttribute(result, BusinessStep.SCREENSHOT_PATH),
                error != null ? error.getClass().getName() + ": " + error.getMessage() : "none");

        try {
            Allure.addAttachment("Failure context", "text/plain", context, ".txt");
        } catch (Throwable allureError) {
            Reporter.log("[ZakaaDash] Allure attachment failed (original test failure preserved): "
                    + allureError.getMessage(), true);
        }
        Reporter.log(context, true);
    }

    private static String environmentSummary() {
        var config = ConfigManager.config();
        return "browser=" + PropertyReader.getProperty("browserType")
                + ", execution=" + PropertyReader.getProperty("executionType")
                + ", environment=" + config.environmentName()
                + ", zakaadash.base.url=" + config.zakaadashBaseUrl();
    }
}
