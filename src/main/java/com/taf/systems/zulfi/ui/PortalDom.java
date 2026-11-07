package com.taf.systems.zulfi.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only DOM helpers shared by the club/standings/schedule page objects: section lookup by heading,
 * image health, layout overflow and Arabic text normalisation. Nothing here clicks or types.
 */
public final class PortalDom {

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(25);

    private PortalDom() {}

    /** The {@code <section>} that owns a heading ({@code h1/h2/h3}) whose text contains {@code headingText}. */
    public static By sectionByHeading(String headingText) {
        return By.xpath(
                "//*[self::h1 or self::h2 or self::h3][contains(normalize-space(.),'" + headingText + "')]"
                        + "/ancestor::section[1]");
    }

    public static WebElement waitForSection(WebDriver driver, String headingText) {
        PortalOverlaySupport.waitForBlockingLoaderToClear(driver);
        By locator = sectionByHeading(headingText);
        return new WebDriverWait(driver, DEFAULT_WAIT)
                .ignoring(RuntimeException.class)
                .until(d -> d.findElements(locator).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
    }

    public static boolean isSectionVisible(WebDriver driver, String headingText) {
        return driver.findElements(sectionByHeading(headingText)).stream().anyMatch(WebElement::isDisplayed);
    }

    /** The link/button labelled «عرض المزيد» (or another label) inside a section, by aria-label or text. */
    public static WebElement actionInSection(WebElement section, String label) {
        return section.findElement(
                By.xpath(".//*[(self::a or self::button) and (@aria-label='" + label + "' or contains(normalize-space(.),'"
                        + label + "'))]"));
    }

    /** Runs a script that returns a JSON-like list of row maps (one DOM round trip for a whole list). */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> rows(WebDriver driver, String script, Object... args) {
        Object raw = ((JavascriptExecutor) driver).executeScript(script, args);
        return raw == null ? List.of() : (List<Map<String, Object>>) raw;
    }

    /** Waits until every image under {@code root} finished loading (or failed), then lists the broken ones. */
    public static List<String> brokenImages(WebDriver driver, WebElement root) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> Boolean.TRUE.equals(js.executeScript(
                            "return [...arguments[0].querySelectorAll('img')].every(i => i.complete);", root)));
        } catch (RuntimeException stillLoading) {
            // report what is broken so far; incomplete images count as broken below
        }
        @SuppressWarnings("unchecked")
        List<String> broken = (List<String>) js.executeScript(
                "return [...arguments[0].querySelectorAll('img')]"
                        + ".filter(i => i.getClientRects().length > 0)"
                        + ".filter(i => !i.complete || i.naturalWidth === 0)"
                        + ".map(i => (i.alt || '(no alt)') + ' <' + i.getAttribute('src') + '>');",
                root);
        return broken == null ? List.of() : new ArrayList<>(broken);
    }

    public static int visibleImageCount(WebDriver driver, WebElement root) {
        Object count = ((JavascriptExecutor) driver).executeScript(
                "return [...arguments[0].querySelectorAll('img')].filter(i => i.getClientRects().length > 0).length;",
                root);
        return count == null ? 0 : ((Number) count).intValue();
    }

    /** Pixels the document is wider than the viewport (0 = no horizontal scroll). */
    public static int horizontalOverflowPx(WebDriver driver) {
        Object value = ((JavascriptExecutor) driver).executeScript(
                "return Math.max(0, document.documentElement.scrollWidth - document.documentElement.clientWidth);");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public static String documentDirection(WebDriver driver) {
        Object value = ((JavascriptExecutor) driver).executeScript(
                "return getComputedStyle(document.body).direction;");
        return value == null ? "" : value.toString();
    }

    /** Elements whose text is clipped horizontally (scrollWidth &gt; clientWidth) without an ellipsis style. */
    public static List<String> clippedTexts(WebDriver driver, WebElement root) {
        @SuppressWarnings("unchecked")
        List<String> clipped = (List<String>) ((JavascriptExecutor) driver).executeScript(
                "return [...arguments[0].querySelectorAll('p,span,h1,h2,h3,a')]"
                        + ".filter(e => e.children.length === 0 && e.getClientRects().length > 0)"
                        + ".filter(e => e.scrollWidth > e.clientWidth + 1 && getComputedStyle(e).textOverflow !== 'ellipsis'"
                        + " && getComputedStyle(e).overflow !== 'visible')"
                        + ".map(e => e.textContent.trim()).filter(t => t.length > 0);",
                root);
        return clipped == null ? List.of() : clipped;
    }

    /** Converts Arabic-Indic digits to ASCII and collapses whitespace so «٢٨ أكتوبر» equals «28 أكتوبر». */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= '٠' && c <= '٩') {
                out.append((char) ('0' + (c - '٠')));
            } else if (c >= '۰' && c <= '۹') {
                out.append((char) ('0' + (c - '۰')));
            } else if (c == ' ' || c == '‏' || c == '‎') {
                out.append(c == ' ' ? ' ' : "");
            } else {
                out.append(c);
            }
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    public static int parseIntOr(String text, int fallback) {
        String digits = normalize(text).replace("+", "").replaceAll("[^0-9\\-]", "");
        if (digits.isEmpty() || digits.equals("-")) {
            return fallback;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static String str(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : normalize(value.toString());
    }
}
