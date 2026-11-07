package com.taf.systems.zulfi.flows.home;

import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.home.HomeOverviewPage;
import com.taf.systems.zulfi.pages.home.HomeRecentAnalysesSection;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-HOME-001 navigation orchestration (read-only — no analysis submit). */
public final class HomeOverviewFlow {

    private final WebDriver driver;

    public HomeOverviewFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open authenticated Home overview")
    public HomeOverviewPage openHomeOverview() {
        return new HomeOverviewPage(driver).awaitReady();
    }

    @Step("Home Hero → ابدأ التحليل → create dialog")
    public CreateAnalysisPage openCreateAnalysisFromHero() {
        HomeOverviewPage overview = openHomeOverview();
        overview.hero().waitUntilVisible().clickStartAnalysis();
        return new CreateAnalysisPage(driver).waitForCreatePopupReady();
    }

    @Step("Home Hero → استعراض التحليلات → التحليلات list")
    public AnalysesListPage openAnalysesListFromHero() {
        HomeOverviewPage overview = openHomeOverview();
        overview.hero().waitUntilVisible().clickBrowseAnalyses();
        return new AnalysesListPage(driver).waitUntilLoaded();
    }

    @Step("Refresh Home and wait for readiness")
    public HomeOverviewPage refreshHomeOverview() {
        driver.navigate().refresh();
        return openHomeOverview();
    }

    @Step("Home آخر التحليلات → عرض المزيد → التحليلات")
    public AnalysesListPage navigateToAnalysesViaRecentViewMore() {
        HomeRecentAnalysesSection recent = openHomeOverview().recentAnalyses().waitUntilSectionVisible();
        if (!recent.isViewMoreVisible()) {
            throw new IllegalStateException("«عرض المزيد» not visible under «آخر التحليلات»");
        }
        recent.clickViewMore();
        return new AnalysesListPage(driver).waitUntilLoaded();
    }

    @Step("Close create-analysis dialog if open")
    public HomeOverviewPage closeCreateDialogIfOpen() {
        new CreateAnalysisPage(driver).closeCreateAnalysisDialogIfOpen();
        return openHomeOverview();
    }

    public HomePage homePage() {
        return new HomePage(driver);
    }
}
