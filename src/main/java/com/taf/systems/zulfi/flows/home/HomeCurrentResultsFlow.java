package com.taf.systems.zulfi.flows.home;

import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.home.CurrentTeamResultsSection;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-HOME-CURRENT-RESULTS navigation orchestration. */
public final class HomeCurrentResultsFlow {

    private final WebDriver driver;

    public HomeCurrentResultsFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open authenticated Home and wait for current results section")
    public CurrentTeamResultsSection openHomeWithCurrentResultsSection() {
        HomePage home = new HomePage(driver).awaitReady();
        return home.currentTeamResultsSection().waitUntilSectionVisible();
    }

    @Step("Home → عرض المزيد → Match Schedule")
    public MatchSchedulePage navigateToScheduleViaViewMore() {
        CurrentTeamResultsSection section = openHomeWithCurrentResultsSection();
        if (!section.isViewMoreVisible()) {
            throw new IllegalStateException("«عرض المزيد» not visible in نتائج الفريق الحالية");
        }
        section.clickViewMore();
        return new MatchSchedulePage(driver).waitUntilLoaded();
    }
}
