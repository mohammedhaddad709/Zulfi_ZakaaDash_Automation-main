package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.home.HomeOverviewPage;
import com.taf.systems.zulfi.pages.home.HomeRecentAnalysesSection;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-ANL-003 — open Analyses list and apply read-only browse/search/filter flows. */
public final class AnalysesListingFlow {

    private final WebDriver driver;

    public AnalysesListingFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Sidebar → التحليلات")
    public AnalysesListPage openFromSidebar() {
        return new AnalysesListPage(driver).openViaSidebarNavigation();
    }

    @Step("Direct route → Analyses list")
    public AnalysesListPage openDirectly() {
        return new AnalysesListPage(driver).openDirectly();
    }

    @Step("Home → آخر التحليلات → عرض المزيد → التحليلات")
    public AnalysesListPage openViaHomeRecentViewMore() {
        HomeRecentAnalysesSection recent =
                new HomeOverviewPage(driver).awaitReady().recentAnalyses().waitUntilSectionVisible();
        if (!recent.isViewMoreVisible()) {
            throw new IllegalStateException("«عرض المزيد» not visible under «آخر التحليلات»");
        }
        recent.clickViewMore();
        return new AnalysesListPage(driver).waitUntilLoaded();
    }

    @Step("Close create-analysis dialog if open")
    public void closeCreateDialogIfOpen() {
        new CreateAnalysisPage(driver).closeCreateAnalysisDialogIfOpen();
    }
}
