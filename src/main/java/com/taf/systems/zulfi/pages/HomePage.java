package com.taf.systems.zulfi.pages;

import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.home.CurrentTeamResultsSection;
import com.taf.systems.zulfi.pages.home.HomeOverviewPage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Locale;

/** Used only to recognize a completed login and provide future navigation. */
public final class HomePage {
    private static final By START_ANALYSIS = By.cssSelector("button[aria-label='ابدأ التحليل']");
    private final WebDriver driver;
    private final ElementActions actions;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
    }
    public HomePage awaitReady() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(12))
                    .until(ExpectedConditions.visibilityOfElementLocated(START_ANALYSIS));
            PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
            return this;
        } catch (TimeoutException homeCtaMissing) {
            return awaitAuthenticatedShell();
        }
    }

    /** Logged-in shell when Home CTA is absent (e.g. already on /clubs or /matches). */
    public HomePage awaitAuthenticatedShell() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(
                            d -> {
                                if (!d.findElements(START_ANALYSIS).isEmpty()
                                        && d.findElement(START_ANALYSIS).isDisplayed()) {
                                    return true;
                                }
                                String url = d.getCurrentUrl().toLowerCase(Locale.ROOT);
                                if (url.contains("/auth/login")) {
                                    return false;
                                }
                                return url.contains("zakaadash")
                                        && (url.contains("/clubs")
                                                || url.contains("/matches")
                                                || url.contains("/analysis")
                                                || url.contains("/club")
                                                || url.contains("/home")
                                                || url.endsWith("-portal-vue/")
                                                || url.endsWith("-portal-vue"));
                            });
            return this;
        } catch (TimeoutException ex) {
            throw new IllegalStateException(
                    "Authenticated portal shell not detected. Actual URL=" + driver.getCurrentUrl(),
                    ex);
        }
    }
    public boolean isReady() { return !driver.findElements(START_ANALYSIS).isEmpty() && driver.findElement(START_ANALYSIS).isDisplayed(); }
    public String url() { return PortalConfig.homeUrl(); }

    public HomePage clickStartAnalysis() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        CreateAnalysisPage existing = new CreateAnalysisPage(driver);
        if (existing.isCreatePopupDisplayed()) {
            return this;
        }
        WebElement button = new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(ExpectedConditions.elementToBeClickable(START_ANALYSIS));
        if (!button.isEnabled()) {
            throw new IllegalStateException("Home CTA «ابدأ التحليل» is not enabled");
        }
        actions.click(START_ANALYSIS);
        return this;
    }

    public CurrentTeamResultsSection currentTeamResultsSection() {
        return new CurrentTeamResultsSection(driver);
    }

    /** US-FAP-HOME-001 — Hero, summary, upcoming match, recent analyses (distinct from current results). */
    public HomeOverviewPage overview() {
        return new HomeOverviewPage(driver);
    }
}
