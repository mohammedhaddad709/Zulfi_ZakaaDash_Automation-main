package com.taf.systems.zulfi.pages.matchschedule;

import com.taf.systems.zulfi.clubdata.Fixture;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.pages.clubdata.ClubWidgetReader;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Match Schedule ({@code /matches}) as data: competition tabs, the «الفريق» filter state and every rendered round.
 * Complements {@link MatchSchedulePage} (analysis actions) without changing it.
 */
public final class ScheduleFixturesView {

    public static final String LEAGUE_TAB = "دوري يلو";
    public static final String CUP_TAB = "كأس خادم الحرمين الشريفين";
    public static final String TEAM_FILTER = "الفريق";

    private static final By ROUND_SECTION = By.cssSelector("section[id^='round-']");
    private static final By COMPETITION_TAB = By.cssSelector("[role='tablist'] [role='tab']");

    private final WebDriver driver;

    public ScheduleFixturesView(WebDriver driver) {
        this.driver = driver;
    }

    public ScheduleFixturesView open() {
        driver.navigate().to(PortalConfig.baseUrl() + "matches");
        return waitUntilLoaded();
    }

    public ScheduleFixturesView waitUntilLoaded() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .ignoring(RuntimeException.class)
                .until(d -> d.getCurrentUrl().contains("/matches")
                        && d.findElements(ROUND_SECTION).stream().anyMatch(WebElement::isDisplayed));
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return this;
    }

    public boolean isOnRoute() {
        return driver.getCurrentUrl().contains("/matches");
    }

    public String activeCompetition() {
        return driver.findElements(COMPETITION_TAB).stream()
                .filter(t -> "true".equals(t.getAttribute("data-p-active")))
                .map(t -> PortalDom.normalize(t.getText()))
                .findFirst()
                .orElse("");
    }

    public ScheduleFixturesView selectCompetition(String label) {
        WebElement tab = driver.findElements(COMPETITION_TAB).stream()
                .filter(t -> PortalDom.normalize(t.getText()).equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Competition tab not found: " + label));
        if (!"true".equals(tab.getAttribute("data-p-active"))) {
            String before = visibleRoundsSignature();
            PortalOverlaySupport.dismissVisibleToasts(driver);
            tab.click();
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> label.equals(activeCompetition()));
            // the tab flips before the other competition's rounds replace the list
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .ignoring(RuntimeException.class)
                    .until(d -> {
                        String now = visibleRoundsSignature();
                        return !now.isEmpty() && !now.equals(before);
                    });
            waitUntilLoaded();
        }
        return this;
    }

    /** Text of the «الفريق» select: the placeholder itself when no team is selected. */
    public String teamFilterLabel() {
        // Filter toolbar = the container of the search field; the team select is its last select (after round/type).
        List<WebElement> toolbar = driver.findElements(By.xpath("//*[contains(@class,'p-iconfield')]/parent::*"));
        if (toolbar.isEmpty()) {
            return "";
        }
        List<WebElement> labels = toolbar.getFirst().findElements(By.cssSelector(".p-select-label"));
        return labels.isEmpty() ? "" : PortalDom.normalize(labels.getLast().getText());
    }

    public boolean teamFilterIsPlaceholder() {
        return TEAM_FILTER.equals(teamFilterLabel());
    }

    /** All rendered rounds for the active competition tab (every round section, newest first as shown). */
    public List<Fixture> fixtures() {
        String competition = activeCompetition();
        // The other competition's panel stays in the DOM hidden; the reader skips invisible rows.
        return ClubWidgetReader.fixtures(driver, driver.findElement(By.tagName("body")), competition);
    }

    private String visibleRoundsSignature() {
        return driver.findElements(ROUND_SECTION).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .map(WebElement::getText)
                .orElse("");
    }

    public List<Integer> roundsRendered() {
        return driver.findElements(ROUND_SECTION).stream()
                .filter(WebElement::isDisplayed)
                .map(s -> ClubWidgetReader.roundNumber(s.getAttribute("id").replace("round-", "")))
                .toList();
    }
}
