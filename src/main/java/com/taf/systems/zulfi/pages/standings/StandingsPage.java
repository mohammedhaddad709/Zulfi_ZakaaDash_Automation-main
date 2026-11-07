package com.taf.systems.zulfi.pages.standings;

import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** Full league table at {@code /standings} («ترتيب الدوري»). */
public final class StandingsPage {

    public static final String ROUTE = "standings";
    public static final String PAGE_HEADING_FRAGMENT = "ترتيب الدوري";
    public static final List<String> ZONE_LEGEND = List.of("الصعود المباشر", "التأهل إلى ملحق الصعود", "الهبوط");

    private static final By TABLE = By.cssSelector(".standings-datatable");
    private static final By HEADING = By.xpath("//main//h1 | //h1");

    private final WebDriver driver;

    public StandingsPage(WebDriver driver) {
        this.driver = driver;
    }

    public StandingsPage openDirectly() {
        driver.navigate().to(PortalConfig.baseUrl() + ROUTE);
        return waitUntilLoaded();
    }

    public StandingsPage waitUntilLoaded() {
        // Home renders the same .standings-datatable snippet, so the route must be /standings first.
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> isOnRoute());
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .ignoring(RuntimeException.class)
                .until(d -> d.findElement(TABLE).isDisplayed()
                        && !d.findElement(TABLE).findElements(By.cssSelector("tbody tr[role=row]")).isEmpty());
        return this;
    }

    public boolean isOnRoute() {
        return driver.getCurrentUrl().contains("/" + ROUTE);
    }

    public String heading() {
        return driver.findElements(HEADING).stream()
                .filter(WebElement::isDisplayed)
                .map(e -> PortalDom.normalize(e.getText()))
                .filter(t -> t.contains(PAGE_HEADING_FRAGMENT))
                .findFirst()
                .orElse("");
    }

    public StandingsTable table() {
        return new StandingsTable(driver, driver.findElement(TABLE));
    }

    public String bodyText() {
        return PortalDom.normalize(driver.findElement(By.tagName("body")).getText());
    }
}
