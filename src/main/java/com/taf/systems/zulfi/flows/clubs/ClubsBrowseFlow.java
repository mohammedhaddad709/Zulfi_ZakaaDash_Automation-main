package com.taf.systems.zulfi.flows.clubs;

import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.clubs.ClubCardSnapshot;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** US-FAP-CLUBS navigation orchestration. */
public final class ClubsBrowseFlow {

    private final WebDriver driver;

    public ClubsBrowseFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open authenticated Home then sidebar «الأندية»")
    public ClubsListPage openClubsViaSidebar() {
        new HomePage(driver).awaitAuthenticatedShell();
        ClubsListPage page = new ClubsListPage(driver);
        if (page.isOnClubsRoute()) {
            return page.waitUntilLoaded();
        }
        return page.openViaSidebarNavigation();
    }

    @Step("Open clubs listing route directly")
    public ClubsListPage openClubsListing() {
        new HomePage(driver).awaitAuthenticatedShell();
        return new ClubsListPage(driver).openDirectly();
    }

    @Step("Open first visible club card and wait for detail route")
    public String openFirstClubDetailById() {
        ClubsListPage listing = openClubsViaSidebar();
        if (listing.isEmptyStateVisible() || listing.isLoadErrorVisible()) {
            throw new IllegalStateException("Cannot open club detail — listing empty or in error state.");
        }
        if (listing.countVisibleClubCards() == 0) {
            throw new IllegalStateException("No parseable club cards on listing page.");
        }
        ClubCardSnapshot first = listing.readVisibleClubCards().getFirst();
        if (first.clubId().isBlank()) {
            throw new IllegalStateException("First club card has no resolvable club id; href=" + first.href());
        }
        String beforeUrl = driver.getCurrentUrl();
        listing.clickClubCardByIndex(0);
        new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> !d.getCurrentUrl().equals(beforeUrl));
        if (driver.getCurrentUrl().equals(beforeUrl)) {
            throw new IllegalStateException("Club card click did not change URL; id=" + first.clubId());
        }
        return first.clubId();
    }
}
