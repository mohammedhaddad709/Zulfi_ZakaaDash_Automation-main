package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.flows.home.HomeCurrentResultsFlow;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleRowSnapshot;
import com.taf.systems.zulfi.validations.MatchSchedulePageMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.List;

/** US-FAP-MCH — open Match Schedule, club filter, and read Yellow League grid (read-only). */
public final class MatchScheduleBrowseFlow {

    private final WebDriver driver;
    private final HomeCurrentResultsFlow homeFlow;

    public MatchScheduleBrowseFlow(WebDriver driver) {
        this.driver = driver;
        this.homeFlow = new HomeCurrentResultsFlow(driver);
    }

    @Step("Sidebar → جدول المباريات")
    public MatchSchedulePage openFromSidebar() {
        awaitAuthenticatedShell();
        return schedulePage().openFromSidebar();
    }

    @Step("Direct route → Match Schedule (/matches)")
    public MatchSchedulePage openDirectly() {
        awaitAuthenticatedShell();
        return schedulePage().openDirectly();
    }

    @Step("Home → عرض المزيد → Match Schedule (preselected club)")
    public MatchSchedulePage openViaHomeViewMore() {
        awaitAuthenticatedShell();
        return homeFlow.navigateToScheduleViaViewMore();
    }

    @Step("Capture schedule grid snapshot")
    public MatchScheduleGridSnapshot captureGrid(MatchSchedulePage schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule required");
        }
        List<MatchScheduleRowSnapshot> rows = schedule.readMatchRows();
        return new MatchScheduleGridSnapshot(
                rows,
                schedule.readSelectedClubFilterLabel(),
                schedule.isEmptyFilterStateVisible(),
                schedule.isLoadErrorVisible());
    }

    @Step("Open sidebar schedule and capture grid")
    public MatchScheduleGridSnapshot openFromSidebarAndCaptureGrid() {
        return captureGrid(openFromSidebar());
    }

    @Step("Filter النادي → {clubFragment}")
    public MatchSchedulePage filterByClub(MatchSchedulePage schedule, String clubFragment) {
        requireSchedule(schedule);
        schedule.selectClubByFragment(clubFragment);
        return schedule;
    }

    @Step("Filter → جميع الأندية")
    public MatchSchedulePage showAllClubs(MatchSchedulePage schedule) {
        requireSchedule(schedule);
        schedule.selectAllClubs();
        return schedule;
    }

    @Step("Reset filter then apply club «{clubFragment}»")
    public MatchScheduleGridSnapshot filterFromAllClubs(MatchSchedulePage schedule, String clubFragment) {
        showAllClubs(schedule);
        filterByClub(schedule, clubFragment);
        return captureGrid(schedule);
    }

    @Step("Apply empty-filter club scenario «{emptyClubFragment}»")
    public MatchScheduleGridSnapshot applyEmptyFilterClub(MatchSchedulePage schedule, String emptyClubFragment) {
        filterByClub(schedule, emptyClubFragment);
        return captureGrid(schedule);
    }

    public boolean isOnScheduleRoute(MatchSchedulePage schedule) {
        return schedule != null
                && (schedule.isOnMatchScheduleRoute() || schedule.isDisplayed());
    }

    public boolean isPageChromeVisible(MatchSchedulePage schedule) {
        return schedule != null
                && schedule.isPageTitleVisible()
                && schedule.isPageDescriptionVisible();
    }

    public boolean clubFilterShows(MatchSchedulePage schedule, String clubFragment) {
        return schedule != null && schedule.clubFilterSelectionContains(clubFragment);
    }

    public boolean clubFilterShowsAllClubs(MatchSchedulePage schedule) {
        if (schedule == null) {
            return false;
        }
        String label = schedule.readSelectedClubFilterLabel();
        // Re-baselined 2026-10-22: no «جميع الأندية» option — a cleared select showing its placeholder means all clubs.
        return schedule.clubFilterSelectionContains(MatchSchedulePageMessages.FILTER_ALL_CLUBS)
                || (label != null && (label.contains("جميع") || label.equals(MatchSchedulePageMessages.CLUB_FILTER_LABEL)));
    }

    public boolean hasFriendlyRowsHeuristic(MatchSchedulePage schedule) {
        return schedule != null && schedule.hasFriendlyMatchRowsHeuristic();
    }

    private void awaitAuthenticatedShell() {
        new HomePage(driver).awaitAuthenticatedShell();
    }

    private MatchSchedulePage schedulePage() {
        return new MatchSchedulePage(driver);
    }

    private static void requireSchedule(MatchSchedulePage schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule required");
        }
    }
}
