package com.taf.systems.zulfi.pages.home;

import com.taf.systems.zulfi.clubdata.ClubWidgets.ResultCard;
import com.taf.systems.zulfi.clubdata.ClubWidgets.UpcomingCard;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.pages.standings.StandingsTable;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.systems.zulfi.validations.HomeCurrentResultsMessages;
import com.taf.systems.zulfi.validations.HomeOverviewMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Home widgets that carry club data: KPI cards (goals, top scorer), «نتائج الفريق للموسم الحالي»,
 * «المباريات القادمة», «آخر التحليلات» and the league standings snippet — plus each section's «عرض المزيد».
 */
public final class HomeClubWidgets {

    public static final String KPI_TEAM_GOALS = "عدد الأهداف";
    public static final String STANDINGS_FULL_TABLE_LINK = "عرض الجدول الدوري كامل";
    public static final String VIEW_MORE = HomeCurrentResultsMessages.VIEW_MORE;

    private static final By KPI_GRID = By.xpath(
            "//article/p[normalize-space(.)='" + HomeOverviewMessages.KPI_TOTAL_ANALYSES + "']/ancestor::section[1]");
    private static final By STANDINGS_SECTION = By.xpath(
            "//a[contains(@href,'/standings')]/ancestor::section[.//*[contains(@class,'standings-datatable')]][1]");

    private final WebDriver driver;

    public HomeClubWidgets(WebDriver driver) {
        this.driver = driver;
    }

    public HomeClubWidgets waitUntilLoaded() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        PortalDom.waitForSection(driver, HomeCurrentResultsMessages.SECTION_TITLE);
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .ignoring(RuntimeException.class)
                .until(d -> !resultCards().isEmpty() && d.findElement(KPI_GRID).isDisplayed());
        return this;
    }

    // --- KPI cards ---

    public Map<String, List<String>> kpis() {
        return ClubWidgetReader.kpis(driver, driver.findElement(KPI_GRID));
    }

    public int teamGoals() {
        List<String> values = kpis().getOrDefault(KPI_TEAM_GOALS, List.of());
        return values.isEmpty() ? -1 : PortalDom.parseIntOr(values.getFirst(), -1);
    }

    /** [name, goals] as rendered in «هداف الفريق». */
    public List<String> topScorer() {
        return kpis().getOrDefault(HomeOverviewMessages.KPI_TOP_SCORER, List.of());
    }

    public int kpiNumber(String label) {
        List<String> values = kpis().getOrDefault(label, List.of());
        return values.isEmpty() ? -1 : PortalDom.parseIntOr(values.getLast(), -1);
    }

    // --- sections ---

    public WebElement resultsSection() {
        return PortalDom.waitForSection(driver, HomeCurrentResultsMessages.SECTION_TITLE);
    }

    public List<ResultCard> resultCards() {
        return ClubWidgetReader.resultCards(driver, resultsSection());
    }

    public WebElement upcomingSection() {
        return PortalDom.waitForSection(driver, HomeOverviewMessages.UPCOMING_SECTION_TITLE_PLURAL);
    }

    public List<UpcomingCard> upcomingCards() {
        return ClubWidgetReader.upcoming(driver, upcomingSection());
    }

    public WebElement recentAnalysesSection() {
        return PortalDom.waitForSection(driver, HomeOverviewMessages.RECENT_SECTION_TITLE);
    }

    /** Analysis card titles («نادي الصقر × الأخدود») in display order. */
    public List<String> recentAnalysisTitles() {
        return PortalDom.rows(driver,
                        "return [...arguments[0].querySelectorAll('article')].map(a => ({t: (a.innerText || '').split('\\n')[0].trim()}));",
                        recentAnalysesSection())
                .stream().map(r -> PortalDom.str(r, "t")).filter(t -> !t.isBlank()).toList();
    }

    public WebElement standingsSection() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return new WebDriverWait(driver, Duration.ofSeconds(25))
                .ignoring(RuntimeException.class)
                .until(d -> d.findElement(STANDINGS_SECTION));
    }

    public String standingsTitle() {
        return standingsSection().findElements(By.xpath(".//h1|.//h2|.//h3")).stream()
                .map(e -> PortalDom.normalize(e.getText()))
                .filter(t -> !t.isBlank())
                .findFirst()
                .orElse("");
    }

    public StandingsTable standingsTable() {
        return new StandingsTable(driver, standingsSection().findElement(By.cssSelector(".standings-datatable")));
    }

    // --- «عرض المزيد» links ---

    public String viewMoreHref(WebElement section) {
        WebElement link = PortalDom.actionInSection(section, VIEW_MORE);
        return link.getAttribute("href") == null ? "" : link.getAttribute("href");
    }

    public void clickViewMore(WebElement section) {
        click(PortalDom.actionInSection(section, VIEW_MORE));
    }

    public String fullStandingsHref() {
        return PortalDom.actionInSection(standingsSection(), STANDINGS_FULL_TABLE_LINK).getAttribute("href");
    }

    public void clickFullStandings() {
        click(PortalDom.actionInSection(standingsSection(), STANDINGS_FULL_TABLE_LINK));
    }

    private void click(WebElement target) {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", target);
        target.click();
    }
}
