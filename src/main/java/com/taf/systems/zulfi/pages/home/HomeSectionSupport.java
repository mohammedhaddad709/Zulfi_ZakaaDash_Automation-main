package com.taf.systems.zulfi.pages.home;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared DOM helpers for Home overview sections. */
final class HomeSectionSupport {

    private static final Pattern FIRST_INTEGER = Pattern.compile("(\\d+)");

    private HomeSectionSupport() {}

    static WebElement resolveSectionRoot(WebDriver driver, By sectionTitleLocator) {
        WebElement title = driver.findElement(sectionTitleLocator);
        List<WebElement> sectionAncestors = title.findElements(By.xpath("./ancestor::section[1]"));
        if (!sectionAncestors.isEmpty()) {
            return sectionAncestors.getFirst();
        }
        List<WebElement> regionAncestors =
                title.findElements(By.xpath("./ancestor::*[@role='region' or contains(@class,'card')][1]"));
        if (!regionAncestors.isEmpty()) {
            return regionAncestors.getFirst();
        }
        return title.findElement(By.xpath("./ancestor::div[1]"));
    }

    static boolean pageContains(WebDriver driver, String fragment) {
        return fragment != null && !fragment.isBlank() && driver.getPageSource().contains(fragment);
    }

    static boolean visibleBodyContains(WebDriver driver, String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return false;
        }
        WebElement body = driver.findElement(By.tagName("body"));
        String text = body.getText();
        if (text != null && text.contains(fragment)) {
            return true;
        }
        return pageContains(driver, fragment);
    }

    static OptionalInt parseFirstInteger(String text) {
        if (text == null || text.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = FIRST_INTEGER.matcher(text);
        if (matcher.find()) {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        }
        return OptionalInt.empty();
    }

    static OptionalInt parseKpiNearLabel(WebDriver driver, String label) {
        By labelLocator =
                By.xpath(
                        "//*[contains(normalize-space(.),'"
                                + label
                                + "') and not(self::script)]");
        List<WebElement> hits = driver.findElements(labelLocator);
        for (WebElement hit : hits) {
            if (!hit.isDisplayed()) {
                continue;
            }
            OptionalInt fromSelf = parseFirstInteger(hit.getText());
            if (fromSelf.isPresent()) {
                return fromSelf;
            }
            List<WebElement> cardAncestors =
                    hit.findElements(
                            By.xpath(
                                    "./ancestor::*[self::article or self::li or contains(@class,'card')][1]"));
            if (!cardAncestors.isEmpty()) {
                OptionalInt fromCard = parseFirstInteger(cardAncestors.getFirst().getText());
                if (fromCard.isPresent()) {
                    return fromCard;
                }
            }
            List<WebElement> parent = hit.findElements(By.xpath("./ancestor::div[1]"));
            if (!parent.isEmpty()) {
                OptionalInt fromParent = parseFirstInteger(parent.getFirst().getText());
                if (fromParent.isPresent()) {
                    return fromParent;
                }
            }
        }
        return OptionalInt.empty();
    }

    static Optional<String> parseDisplayNearLabel(WebDriver driver, String label) {
        By labelLocator =
                By.xpath(
                        "//*[contains(normalize-space(.),'"
                                + label
                                + "') and not(self::script)]");
        for (WebElement hit : driver.findElements(labelLocator)) {
            if (!hit.isDisplayed()) {
                continue;
            }
            Optional<String> fromSelf = extractValueAfterLabel(hit.getText(), label);
            if (fromSelf.isPresent()) {
                return fromSelf;
            }
            List<WebElement> cardAncestors =
                    hit.findElements(
                            By.xpath(
                                    "./ancestor::*[self::article or self::li or contains(@class,'card')][1]"));
            if (!cardAncestors.isEmpty()) {
                Optional<String> fromCard =
                        extractValueAfterLabel(cardAncestors.getFirst().getText(), label);
                if (fromCard.isPresent()) {
                    return fromCard;
                }
            }
            List<WebElement> parent = hit.findElements(By.xpath("./ancestor::div[1]"));
            if (!parent.isEmpty()) {
                Optional<String> fromParent = extractValueAfterLabel(parent.getFirst().getText(), label);
                if (fromParent.isPresent()) {
                    return fromParent;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractValueAfterLabel(String text, String label) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.replace('\u00a0', ' ').trim();
        int labelIndex = normalized.indexOf(label);
        if (labelIndex < 0) {
            return Optional.empty();
        }
        String remainder = normalized.substring(labelIndex + label.length()).trim();
        if (remainder.startsWith(":")) {
            remainder = remainder.substring(1).trim();
        }
        if (remainder.isBlank()) {
            return Optional.empty();
        }
        String[] lines = remainder.split("\\R");
        String firstLine = lines[0].trim();
        if (firstLine.isBlank() || firstLine.equals(label)) {
            return Optional.empty();
        }
        return Optional.of(firstLine);
    }
}
