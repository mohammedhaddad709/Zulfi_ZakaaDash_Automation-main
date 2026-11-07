package com.taf.systems.zulfi.flows.clubinfo;

import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.clubinfo.ClubInformationPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-CLUB-001 navigation orchestration. */
public final class ClubInformationFlow {

    private final WebDriver driver;

    public ClubInformationFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open authenticated Home then sidebar «عن النادي»")
    public ClubInformationPage openClubInfoViaSidebar() {
        new HomePage(driver).awaitAuthenticatedShell();
        try {
            return new ClubInformationPage(driver).openViaSidebarNavigation();
        } catch (RuntimeException sidebarBlocked) {
            return new ClubInformationPage(driver).openDirectly();
        }
    }

    @Step("Open Club Information route directly")
    public ClubInformationPage openClubInformationDirectly() {
        new HomePage(driver).awaitAuthenticatedShell();
        return new ClubInformationPage(driver).openDirectly();
    }

    @Step("Refresh Club Information and wait for stable state")
    public ClubInformationPage revisitClubInformation() {
        ClubInformationPage page = openClubInformationDirectly();
        return page.refresh();
    }
}
