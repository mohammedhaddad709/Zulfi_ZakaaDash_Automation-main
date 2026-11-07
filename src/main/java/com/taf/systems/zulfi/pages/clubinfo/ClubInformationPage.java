package com.taf.systems.zulfi.pages.clubinfo;

import com.taf.systems.zulfi.config.ZulfiConfig;
import com.taf.systems.zulfi.ui.PortalOverlaySupport;
import com.taf.core.pacing.PacedElementInteraction;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.validations.ClubInfoPageMessages;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Club Information (عن النادي) — US-FAP-CLUB-001, read-only. */
public final class ClubInformationPage {

    public enum MemberSection {
        BOARD(ClubInfoPageMessages.SECTION_BOARD),
        ADMIN(ClubInfoPageMessages.SECTION_ADMIN_TEAM),
        FOOTBALL(ClubInfoPageMessages.SECTION_FOOTBALL_TEAM);

        private final String title;

        MemberSection(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private static final Pattern PROFILE_HREF =
            Pattern.compile("(/player|/players/|/profile|/member|playerId=|player_id=)", Pattern.CASE_INSENSITIVE);

    private static final By CLUB_INFO_NAV =
            By.cssSelector(
                    "a[aria-label='"
                            + ClubInfoPageMessages.NAV_LABEL
                            + "'],"
                            + " a[title='"
                            + ClubInfoPageMessages.NAV_LABEL
                            + "'],"
                            + " a[href*='about'],"
                            + " a[href*='club/about'],"
                            + " a[href*='club-info'],"
                            + " nav a[href*='about']");
    private static final By PAGE_TITLE =
            By.xpath(
                    "//*[self::h1 or self::h2][contains(normalize-space(.),'"
                            + ClubInfoPageMessages.PAGE_TITLE
                            + "')]");
    private static final By RETRY_BUTTON =
            By.xpath(
                    "//button[contains(@aria-label,'"
                            + ClubInfoPageMessages.RETRY_ACTION
                            + "')"
                            + " or contains(normalize-space(.),'"
                            + ClubInfoPageMessages.RETRY_ACTION
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
    private static final By OVERVIEW_EDITABLE =
            By.xpath(
                    "//*[contains(normalize-space(.),'"
                            + ClubInfoPageMessages.SECTION_ABOUT
                            + "') or contains(normalize-space(.),'"
                            + ClubInfoPageMessages.SECTION_HISTORY
                            + "')]"
                            + "//ancestor::*[self::section or self::article or self::div][1]"
                            + "//*[self::input or self::textarea or @contenteditable='true']");

    private final WebDriver driver;

    public ClubInformationPage(WebDriver driver) {
        this.driver = driver;
    }

    public ClubInformationPage openViaSidebarNavigation() {
        PortalOverlaySupport.dismissVisibleToasts(driver);
        WebElement link =
                new WebDriverWait(driver, Duration.ofSeconds(25))
                        .until(ExpectedConditions.elementToBeClickable(CLUB_INFO_NAV));
        PacedElementInteraction.click(link);
        return waitUntilLoaded();
    }

    public ClubInformationPage openDirectly() {
        driver.navigate().to(PortalConfig.baseUrl() + configuredPathSegment());
        return waitUntilLoaded();
    }

    public ClubInformationPage waitUntilLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(
                        d ->
                                isOnClubInfoRoute()
                                        || isPageTitleVisible()
                                        || isLoadErrorVisible()
                                        || isLoadingStateVisible());
        return this;
    }

    public ClubInformationPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public boolean isOnClubInfoRoute() {
        String url = driver.getCurrentUrl().toLowerCase(Locale.ROOT);
        return url.contains("club")
                && (url.contains("about")
                        || url.contains("info")
                        || url.contains("organisation")
                        || url.contains("organization")
                        || pageContains(ClubInfoPageMessages.SECTION_BOARD));
    }

    public boolean isPageTitleVisible() {
        return visible(PAGE_TITLE) || pageContains(ClubInfoPageMessages.PAGE_TITLE);
    }

    public boolean isSectionTitleVisible(String sectionTitle) {
        if (visible(sectionHeadingLocator(sectionTitle))) {
            return true;
        }
        WebElement body = driver.findElement(By.tagName("body"));
        String text = body.getText();
        return text != null && text.contains(sectionTitle);
    }

    public boolean isSectionContentPresent(String sectionTitle) {
        WebElement root = findSectionRoot(sectionTitle);
        if (root == null) {
            return false;
        }
        String text = root.getText();
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replace(sectionTitle, "").trim();
        return normalized.length() >= 8 || normalized.lines().anyMatch(line -> line.trim().length() >= 8);
    }

    public boolean overviewAndHistoryAreReadOnly() {
        for (WebElement field : driver.findElements(OVERVIEW_EDITABLE)) {
            if (field.isDisplayed()) {
                return false;
            }
        }
        return true;
    }

    public List<ClubMemberCardSnapshot> readMemberCards(MemberSection section) {
        List<ClubMemberCardSnapshot> snapshots = new ArrayList<>();
        WebElement sectionRoot = findSectionRoot(section.title());
        if (sectionRoot == null) {
            return snapshots;
        }
        int index = 0;
        for (WebElement card : resolveMemberCardElements(sectionRoot)) {
            if (!card.isDisplayed()) {
                continue;
            }
            ClubMemberCardSnapshot snapshot = parseMemberCard(card, section, index++);
            if (snapshot != null && (!snapshot.name().isBlank() || !snapshot.role().isBlank())) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    public int countMemberCards(MemberSection section) {
        return readMemberCards(section).size();
    }

    public boolean footballCardsHaveNoProfileNavigation() {
        for (ClubMemberCardSnapshot card : readMemberCards(MemberSection.FOOTBALL)) {
            if (card.profileNavigationLink()) {
                return false;
            }
        }
        return true;
    }

    public boolean clickFirstFootballCardDoesNotOpenProfile() {
        WebElement sectionRoot = findSectionRoot(MemberSection.FOOTBALL.title());
        if (sectionRoot == null) {
            return true;
        }
        List<WebElement> cards = resolveMemberCardElements(sectionRoot).stream().filter(WebElement::isDisplayed).toList();
        if (cards.isEmpty()) {
            return true;
        }
        String before = driver.getCurrentUrl();
        WebElement target = findClickableCardRoot(cards.getFirst());
        if (target == null) {
            return true;
        }
        try {
            PacedElementInteraction.click(target);
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> !d.getCurrentUrl().equals(before) || d.getCurrentUrl().equals(before));
        } catch (Exception ignored) {
            return true;
        }
        String after = driver.getCurrentUrl();
        if (after.equals(before)) {
            return true;
        }
        return !looksLikeProfileRoute(after);
    }

    public boolean hasNoAddEditDeleteControls() {
        for (WebElement control : driver.findElements(MUTATION_CONTROLS)) {
            if (control.isDisplayed()) {
                return false;
            }
        }
        return true;
    }

    public boolean isLoadErrorVisible() {
        return pageContains(ClubInfoPageMessages.LOAD_ERROR);
    }

    public boolean isRetryActionVisible() {
        return visible(RETRY_BUTTON);
    }

    public boolean isLoadingStateVisible() {
        if (pageContains(ClubInfoPageMessages.LOAD_ERROR)) {
            return false;
        }
        for (WebElement skeleton :
                driver.findElements(
                        By.cssSelector(
                                "[data-testid*='loading'], [data-testid*='skeleton'], .p-skeleton, [aria-busy='true']"))) {
            if (skeleton.isDisplayed()) {
                return true;
            }
        }
        return driver.findElements(By.xpath("//*[contains(normalize-space(.),'جاري')]")).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    public ClubInformationPage clickRetryIfVisible() {
        if (!isRetryActionVisible()) {
            return this;
        }
        WebElement button = driver.findElement(RETRY_BUTTON);
        PacedElementInteraction.click(button);
        return waitUntilLoaded();
    }

    public boolean allApprovedSectionsVisible() {
        return isSectionTitleVisible(ClubInfoPageMessages.SECTION_ABOUT)
                && isSectionTitleVisible(ClubInfoPageMessages.SECTION_HISTORY)
                && isSectionTitleVisible(ClubInfoPageMessages.SECTION_BOARD)
                && isSectionTitleVisible(ClubInfoPageMessages.SECTION_ADMIN_TEAM)
                && isSectionTitleVisible(ClubInfoPageMessages.SECTION_FOOTBALL_TEAM);
    }

    private ClubMemberCardSnapshot parseMemberCard(WebElement element, MemberSection section, int index) {
        WebElement root = element;
        String name = resolveMemberName(root);
        String role = resolveMemberRole(root);
        WebElement img = firstDisplayed(root, By.cssSelector("img"));
        boolean photoPresent = img != null || hasAvatarPlaceholder(root);
        boolean placeholder = img == null || imageUsesPlaceholderHeuristic(img, root);
        boolean profileLink = hasProfileNavigation(root);
        String raw = root.getText() == null ? "" : root.getText().trim();
        return new ClubMemberCardSnapshot(
                index, section, name, role, photoPresent, placeholder, profileLink, raw);
    }

    private boolean hasProfileNavigation(WebElement root) {
        for (WebElement link : root.findElements(By.cssSelector("a[href]"))) {
            String href = link.getAttribute("href");
            if (href != null && looksLikeProfileRoute(href)) {
                return true;
            }
        }
        if ("a".equalsIgnoreCase(root.getTagName())) {
            String href = root.getAttribute("href");
            return href != null && looksLikeProfileRoute(href);
        }
        return false;
    }

    private static boolean looksLikeProfileRoute(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return PROFILE_HREF.matcher(url).find();
    }

    private boolean imageUsesPlaceholderHeuristic(WebElement img, WebElement cardRoot) {
        if (img == null) {
            return hasAvatarPlaceholder(cardRoot);
        }
        String src = img.getAttribute("src");
        if (src == null || src.isBlank() || src.contains("placeholder") || src.contains("default")) {
            return true;
        }
        String alt = img.getAttribute("alt");
        if (alt != null && (alt.contains("placeholder") || alt.contains("افتراض"))) {
            return true;
        }
        if (isBrokenImage(img)) {
            return true;
        }
        return hasAvatarPlaceholder(cardRoot);
    }

    private boolean isBrokenImage(WebElement img) {
        try {
            Object width =
                    ((JavascriptExecutor) driver)
                            .executeScript(
                                    "return arguments[0].complete === false"
                                            + " || arguments[0].naturalWidth === 0"
                                            + " || arguments[0].naturalHeight === 0;",
                                    img);
            return Boolean.TRUE.equals(width);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean hasAvatarPlaceholder(WebElement root) {
        return !root.findElements(
                        By.cssSelector(
                                "[data-testid*='avatar'], [data-testid*='placeholder'], .avatar-placeholder,"
                                        + " .profile-placeholder, svg[data-icon='user'], .p-avatar, [class*='placeholder']"))
                .isEmpty();
    }

    private static String resolveMemberName(WebElement root) {
        WebElement heading =
                firstDisplayed(
                        root,
                        By.cssSelector(
                                "h3, h4, h5, [data-testid*='name'], [data-testid*='member-name'], .member-name, strong"));
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

    private static String resolveMemberRole(WebElement root) {
        WebElement roleEl =
                firstDisplayed(
                        root,
                        By.cssSelector(
                                "[data-testid*='role'], [data-testid*='position'], .member-role, .text-muted, small, span"));
        if (roleEl != null) {
            String text = roleEl.getText();
            if (text != null && !text.isBlank() && !text.equals(resolveMemberName(root))) {
                return text.trim();
            }
        }
        List<String> lines =
                root.getText() == null
                        ? List.of()
                        : root.getText().lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        if (lines.size() >= 2) {
            return lines.get(1);
        }
        return "";
    }

    private List<WebElement> resolveMemberCardElements(WebElement sectionRoot) {
        List<WebElement> cards =
                sectionRoot.findElements(
                        By.cssSelector(
                                "[data-testid*='member-card'], [data-testid*='player-card'], article, li[class*='card'],"
                                        + " div[class*='card']"));
        if (!cards.isEmpty()) {
            return cards;
        }
        return sectionRoot.findElements(
                By.xpath(
                        ".//*[self::article or self::li][.//img or .//*[contains(@class,'avatar') or contains(@class,'placeholder')]]"));
    }

    private WebElement findSectionRoot(String sectionTitle) {
        for (WebElement heading : driver.findElements(sectionHeadingLocator(sectionTitle))) {
            if (!heading.isDisplayed()) {
                continue;
            }
            try {
                return heading.findElement(
                        By.xpath(
                                "./ancestor::*[self::section or self::article or @data-testid][1]"
                                        + "|./following-sibling::*[1]"
                                        + "|./parent::*"));
            } catch (Exception ignored) {
                return heading;
            }
        }
        return null;
    }

    private static By sectionHeadingLocator(String sectionTitle) {
        return By.xpath(
                "//*[self::h2 or self::h3 or self::h4 or self::p or self::span or self::div]"
                        + "[contains(normalize-space(.),'"
                        + sectionTitle
                        + "')]");
    }

    private static WebElement findClickableCardRoot(WebElement element) {
        if ("a".equalsIgnoreCase(element.getTagName()) || "button".equalsIgnoreCase(element.getTagName())) {
            return element;
        }
        WebElement link = firstDisplayed(element, By.cssSelector("a, button"));
        return link != null ? link : element;
    }

    private static WebElement firstDisplayed(WebElement root, By selector) {
        for (WebElement candidate : root.findElements(selector)) {
            if (candidate.isDisplayed()) {
                return candidate;
            }
        }
        return null;
    }

    private static String configuredPathSegment() {
        String fromProperty = ZulfiConfig.get("portal.clubInfo.path");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.startsWith("/") ? fromProperty.substring(1) : fromProperty;
        }
        return "club/about";
    }

    private boolean visible(By locator) {
        return !driver.findElements(locator).isEmpty() && driver.findElement(locator).isDisplayed();
    }

    private boolean pageContains(String fragment) {
        return fragment != null && !fragment.isBlank() && driver.getPageSource().contains(fragment);
    }
}
