package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.pages.HomePage;
import org.openqa.selenium.WebDriver;

/** Facade for US-FAP-HOME-001 sections on the authenticated Home page. */
public final class HomeOverviewPage {

    private final WebDriver driver;

    public HomeOverviewPage(WebDriver driver) {
        this.driver = driver;
    }

    public HomeOverviewPage awaitReady() {
        new HomePage(driver).awaitReady();
        return this;
    }

    public HomeHeroSection hero() {
        return new HomeHeroSection(driver);
    }

    public HomeAnalysisSummarySection analysisSummary() {
        return new HomeAnalysisSummarySection(driver);
    }

    public HomeUpcomingMatchSection upcomingMatch() {
        return new HomeUpcomingMatchSection(driver);
    }

    public HomeRecentAnalysesSection recentAnalyses() {
        return new HomeRecentAnalysesSection(driver);
    }
}
