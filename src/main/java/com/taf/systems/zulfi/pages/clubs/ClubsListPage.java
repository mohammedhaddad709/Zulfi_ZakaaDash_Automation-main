package com.taf.systems.zulfi.pages.clubs;

import com.taf.systems.zulfi.ui.PortalDom;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.core.pacing.PacedElementInteraction;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.validations.ClubsPageMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Yellow League clubs listing — US-FAP-CLUBS. */
public final class ClubsListPage {

    private static final Pattern CLUB_ID_IN_HREF =
            Pattern.compile(
                    "(?:/clubs/|/club/|clubs/|club/)([^/?#&]+)|[?&](?:clubId|club_id|id)=([^&#]+)",
                    Pattern.CASE_INSENSITIVE);

    private static final By CLUBS_NAV =
            By.cssSelector(
                    "a[aria-label='"
                            + ClubsPageMessages.NAV_LABEL
                            + "'],"
                            + " a[href*='club'][aria-label='"
                            + ClubsPageMessages.NAV_LABEL
                            + "']");
    private static final By PAGE_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2][contains(normalize-space(.),'"
                            + ClubsPageMessages.PAGE_TITLE
                            + "')]");
    private static final By PAGE_DESCRIPTION =
            By.xpath(
                    "//*[self::p or self::span or self::div][contains(normalize-space(.),'"
                            + ClubsPageMessages.PAGE_DESCRIPTION
                            + "')]");
    // Grid cards only: a bare a[href*='/club'] also matched the sidebar «الأندية» link (/clubs) and the breadcrumb.
    private static final By CLUB_CARD =
            By.cssSelector(
                    "[data-testid='club-card'],"
                            + " article[data-club-id],"
                            + " article:has(a[href*='/clubs/'])");
    private static final By RETRY_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + ClubsPageMessages.RETRY_ACTION
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + ClubsPageMessages.RETRY_ACTION
                            + "')]");
    private static final By MUTATION_CONTROLS =
            By.xpath(
                    "//button[contains(normalize-space(.),'إضافة')"
                            + " or contains(normalize-space(.),'تعديل')"
                            + " or contains(normalize-space(.),'حذف')"
                            + " or contains(@aria-label,'إضافة')"
                            + " or contains(@aria-label,'تعديل')"
                            + " or contains(@aria-label,'حذف')]"
                            + "|//a[contains(normalize-space(.),'إضافة') and contains(normalize-space(.),'ناد')]");

    private final WebDriver driver;

    public ClubsListPage(WebDriver driver) {
        this.driver = driver;
    }

    public ClubsListPage openViaSidebarNavigation() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(25))
                        .until(ExpectedConditions.elementToBeClickable(CLUBS_NAV));
        PacedElementInteraction.click(link);
        return waitUntilLoaded();
    }

    public ClubsListPage openDirectly() {
        driver.navigate().to(PortalConfig.baseUrl() + "clubs");
        return waitUntilLoaded();
    }

    public ClubsListPage waitUntilLoaded() {
        // The URL flips before the grid renders: require the route AND rendered content (title, empty or error state).
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(
                        d ->
                                isOnClubsRoute()
                                        && (isPageTitleVisible()
                                                || isEmptyStateVisible()
                                                || isLoadErrorVisible()));
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        return this;
    }

    public boolean isOnClubsRoute() {
        String url = driver.getCurrentUrl().toLowerCase();
        return url.contains("club");
    }

    public boolean isPageTitleVisible() {
        return visible(PAGE_TITLE);
    }

    public boolean isPageDescriptionVisible() {
        return visible(PAGE_DESCRIPTION) || pageContains(ClubsPageMessages.PAGE_DESCRIPTION);
    }

    public boolean isEmptyStateVisible() {
        return pageContains(ClubsPageMessages.EMPTY_TITLE)
                && (pageContains(ClubsPageMessages.EMPTY_DESCRIPTION) || hasSecondaryEmptyCopy());
    }

    public boolean isLoadErrorVisible() {
        return pageContains(ClubsPageMessages.LOAD_ERROR);
    }

    public boolean isRetryActionVisible() {
        return visible(RETRY_BUTTON);
    }

    public boolean hasNoAddEditDeleteControls() {
        for (WebElement control : driver.findElements(MUTATION_CONTROLS)) {
            if (control.isDisplayed()) {
                return false;
            }
        }
        return true;
    }

    public List<ClubCardSnapshot> readVisibleClubCards() {
        List<ClubCardSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        for (WebElement candidate : resolveClubCardElements()) {
            if (!candidate.isDisplayed()) {
                continue;
            }
            ClubCardSnapshot snapshot = parseCard(candidate, index++);
            if (snapshot != null && !snapshot.name().isBlank()) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    public int countVisibleClubCards() {
        return readVisibleClubCards().size();
    }

    public ClubsListPage clickClubCardByIndex(int index) {
        List<WebElement> cards = resolveClubCardElements().stream().filter(WebElement::isDisplayed).toList();
        if (index < 0 || index >= cards.size()) {
            throw new IllegalArgumentException("Club card index out of range: " + index + "; visible=" + cards.size());
        }
        WebElement target = cards.get(index);
        WebElement clickable =
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(findClickableCardRoot(target)));
        PacedElementInteraction.click(clickable);
        return this;
    }

    public boolean currentUrlContainsClubId(String clubId) {
        if (clubId == null || clubId.isBlank()) {
            return false;
        }
        return driver.getCurrentUrl().contains(clubId.trim());
    }

    /** League card on the clubs grid: «#N» badge, club name, «عرض تفاصيل النادي» target and logo. */
    public record LeagueClubCard(int rank, String name, String detailsHref, String logoSrc) {}

    public List<LeagueClubCard> readLeagueClubCards() {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(d -> !d.findElements(By.cssSelector("article a[href*='/clubs/']")).isEmpty());
        return PortalDom.rows(driver,
                        "return [...document.querySelectorAll('article')].filter(a => a.querySelector(\"a[href*='/clubs/']\"))"
                                + ".map(a => ({ rank: (a.querySelector(':scope > span') || {textContent: ''}).textContent.trim(),"
                                + "  name: (a.querySelector('h2') || {textContent: ''}).textContent.trim(),"
                                + "  href: a.querySelector(\"a[href*='/clubs/']\").getAttribute('href'),"
                                + "  logo: (a.querySelector('h2') && a.querySelector('h2').parentElement.querySelector('img'))"
                                + "    ? a.querySelector('h2').parentElement.querySelector('img').getAttribute('src') : '' }));")
                .stream()
                .map(r -> new LeagueClubCard(
                        PortalDom.parseIntOr(PortalDom.str(r, "rank"), -1),
                        PortalDom.str(r, "name"),
                        PortalDom.str(r, "href"),
                        PortalDom.str(r, "logo")))
                .toList();
    }

    public static String extractClubIdFromHref(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        Matcher matcher = CLUB_ID_IN_HREF.matcher(href);
        if (matcher.find()) {
            String id = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return id == null ? "" : id.trim();
        }
        return "";
    }

    private List<WebElement> resolveClubCardElements() {
        List<WebElement> found = driver.findElements(CLUB_CARD);
        if (!found.isEmpty()) {
            return found;
        }
        return driver.findElements(
                By.xpath(
                        "//main//a[contains(@href,'club') and not(contains(@href,'clubs') and @aria-label='"
                                + ClubsPageMessages.NAV_LABEL
                                + "')]"));
    }

    private ClubCardSnapshot parseCard(WebElement element, int index) {
        WebElement root = findClickableCardRoot(element);
        String href = root.getAttribute("href");
        if (href == null || href.isBlank()) {
            WebElement nestedLink = firstDisplayed(root, By.cssSelector("a[href*='club']"));
            if (nestedLink != null) {
                href = nestedLink.getAttribute("href");
            }
        }
        String clubId = root.getAttribute("data-club-id");
        if (clubId == null || clubId.isBlank()) {
            clubId = extractClubIdFromHref(href);
        }
        String name = resolveClubName(root);
        boolean logoPresent = !root.findElements(By.cssSelector("img, [data-testid='club-logo'], .club-logo")).isEmpty();
        String raw = root.getText() == null ? "" : root.getText().trim();
        return new ClubCardSnapshot(index, clubId, name, href == null ? "" : href, logoPresent, raw);
    }

    private static WebElement findClickableCardRoot(WebElement element) {
        if ("a".equalsIgnoreCase(element.getTagName())) {
            return element;
        }
        WebElement link = firstDisplayed(element, By.cssSelector("a[href*='club']"));
        return link != null ? link : element;
    }

    private static String resolveClubName(WebElement root) {
        WebElement heading =
                firstDisplayed(
                        root,
                        By.cssSelector(
                                "h2, h3, h4, [data-testid='club-name'], .font-bold, .club-name, strong"));
        if (heading != null) {
            String text = heading.getText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        String text = root.getText();
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.lines().map(String::trim).filter(line -> !line.isBlank()).findFirst().orElse("").trim();
    }

    private static WebElement firstDisplayed(WebElement root, By selector) {
        for (WebElement candidate : root.findElements(selector)) {
            if (candidate.isDisplayed()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasSecondaryEmptyCopy() {
        for (WebElement title :
                driver.findElements(
                        By.xpath(
                                "//*[contains(normalize-space(.),'"
                                        + ClubsPageMessages.EMPTY_TITLE
                                        + "')]"))) {
            if (!title.isDisplayed()) {
                continue;
            }
            try {
                WebElement container =
                        title.findElement(
                                By.xpath("./ancestor::*[self::section or self::article or @role='status'][1]"));
                String text = container.getText();
                return text != null && text.lines().count() >= 2;
            } catch (NoSuchElementException | StaleElementReferenceException noContainer) {
                // no wrapping container (or re-rendered): this title cannot prove a secondary line
            }
        }
        return false;
    }

    private boolean visible(By locator) {
        return !driver.findElements(locator).isEmpty() && driver.findElement(locator).isDisplayed();
    }

    private boolean pageContains(String fragment) {
        return fragment != null && !fragment.isBlank() && driver.getPageSource().contains(fragment);
    }
}
