package com.taf.core.pacing;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public final class PacedElementInteraction {

    private PacedElementInteraction() {}

    public static void focus(WebElement element) {
        element.click();
        HumanPacingManager.pauseForAction();
    }

    public static void typeText(WebElement element, String text, boolean secureField) {
        focus(element);
        element.clear();
        ExecutionPace pace = ExecutionPace.current();
        if (text == null) {
            text = "";
        }
        if (pace == ExecutionPace.FAST || text.isEmpty()) {
            element.sendKeys(text);
            PacingCoverageTracker.recordBulkTyping(text.length());
            return;
        }
        for (char character : text.toCharArray()) {
            element.sendKeys(String.valueOf(character));
            PacingCoverageTracker.recordProgressiveCharacters(1);
            HumanPacingManager.pauseBetweenCharacters();
        }
    }

    public static void click(WebElement element) {
        element.click();
        afterClick();
    }

    public static void clickWithActions(WebDriver driver, WebElement element) {
        new Actions(driver).moveToElement(element).click().perform();
        afterClick();
    }

    public static void clearField(WebElement element) {
        focus(element);
        element.clear();
    }

    public static void afterClick() {
        HumanPacingManager.pauseForAction();
        PacingCoverageTracker.recordPacedClick();
    }

    /** @deprecated use {@link #afterClick()} only; pre-click transition pacing caused double delays. */
    @Deprecated
    public static void beforeClick() {
        // Intentionally no-op — one action pause is applied after the click.
    }
}
