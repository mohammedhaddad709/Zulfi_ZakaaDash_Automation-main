package com.taf.systems.zulfi.flows.clubdata;

import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.config.DefaultClubConfig;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.clubdetails.ClubDetailsPage;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage;
import com.taf.systems.zulfi.pages.clubs.ClubsListPage.LeagueClubCard;
import com.taf.systems.zulfi.pages.home.HomeClubWidgets;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleFixturesView;
import com.taf.systems.zulfi.pages.standings.StandingsPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.Comparator;
import java.util.List;

/**
 * Navigation + data collection for the club-data stories. The Default Club comes from {@link DefaultClubConfig}
 * (never hardcoded in tests); its ID is resolved from the clubs grid by name.
 */
public final class ClubDataFlow {

    private final WebDriver driver;

    public ClubDataFlow(WebDriver driver) {
        this.driver = driver;
    }

    public static String defaultClub() {
        return DefaultClubConfig.configuredDefaultClubLabel();
    }

    @Step("Open authenticated Home club widgets")
    public HomeClubWidgets openHome() {
        if (!driver.getCurrentUrl().equals(PortalConfig.homeUrl())) {
            driver.navigate().to(PortalConfig.homeUrl());
        }
        new HomePage(driver).awaitReady();
        return new HomeClubWidgets(driver).waitUntilLoaded();
    }

    @Step("Open «الأندية» grid")
    public List<LeagueClubCard> clubCards() {
        return new ClubsListPage(driver).openDirectly().readLeagueClubCards();
    }

    @Step("Resolve Default Club card on «الأندية»")
    public LeagueClubCard defaultClubCard() {
        String club = defaultClub();
        return clubCards().stream()
                .filter(c -> Fixture.sameTeam(c.name(), club))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Default Club «" + club + "» not found on «الأندية»"));
    }

    @Step("Open Default Club details")
    public ClubDetailsPage openDefaultClubDetails() {
        String href = defaultClubCard().detailsHref();
        String id = ClubsListPage.extractClubIdFromHref(href);
        return new ClubDetailsPage(driver).open(id);
    }

    @Step("Open «ترتيب الدوري»")
    public StandingsPage openStandings() {
        return new StandingsPage(driver).openDirectly();
    }

    @Step("Open Match Schedule")
    public ScheduleFixturesView openSchedule() {
        return new ScheduleFixturesView(driver).open();
    }

    /** Every fixture of the active competition tab. */
    @Step("Read Match Schedule fixtures for «{competition}»")
    public List<Fixture> scheduleFixtures(String competition) {
        ScheduleFixturesView schedule = new ScheduleFixturesView(driver);
        if (!schedule.isOnRoute()) {
            schedule.open();
        }
        return schedule.selectCompetition(competition).fixtures();
    }

    /** Default Club fixtures of one competition in chronological order (oldest first). */
    public static List<Fixture> clubFixtures(List<Fixture> all, String club) {
        return all.stream()
                .filter(f -> f.involves(club))
                .sorted(Comparator.comparingInt(Fixture::seasonOrderKey).thenComparingInt(Fixture::round))
                .toList();
    }

    public static List<Fixture> played(List<Fixture> fixtures) {
        return fixtures.stream().filter(Fixture::played).toList();
    }

    public static List<Fixture> upcoming(List<Fixture> fixtures) {
        return fixtures.stream().filter(f -> !f.played()).toList();
    }

    /** The last {@code n} played fixtures, newest first (Home/club results card order). */
    public static List<Fixture> lastPlayedNewestFirst(List<Fixture> chronological, int n) {
        List<Fixture> played = played(chronological);
        List<Fixture> tail = played.subList(Math.max(0, played.size() - n), played.size());
        return tail.reversed();
    }
}
