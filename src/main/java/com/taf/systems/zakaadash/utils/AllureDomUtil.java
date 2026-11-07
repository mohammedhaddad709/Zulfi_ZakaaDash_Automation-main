package com.taf.systems.zakaadash.utils;

import io.qameta.allure.Allure;
import org.openqa.selenium.WebElement;

public final class AllureDomUtil {

    private static final int MAX_HTML_LENGTH = 12_000;

    private AllureDomUtil() {
    }

    public static void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", truncate(content), ".txt");
    }

    public static String outerHtml(WebElement element) {
        if (element == null) {
            return "null element";
        }
        try {
            return truncate(element.getAttribute("outerHTML"));
        } catch (RuntimeException ex) {
            return "outerHTML unavailable: " + ex.getMessage();
        }
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_HTML_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_HTML_LENGTH) + "\n... [truncated]";
    }
}
