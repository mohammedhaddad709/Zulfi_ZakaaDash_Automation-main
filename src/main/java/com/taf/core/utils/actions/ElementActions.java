package com.taf.core.utils.actions;

import com.taf.core.pacing.HumanPacingManager;
import com.taf.core.pacing.PacingCoverageTracker;
import org.openqa.selenium.JavascriptExecutor;
import com.taf.core.pacing.PacedElementInteraction;
import com.taf.core.utils.WaitManager;
import com.taf.core.utils.logs.LogsManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.io.File;

public class ElementActions {
    private final WebDriver driver;
    private WaitManager waitManager;

    public ElementActions(WebDriver driver) {
        this.driver = driver;
        this.waitManager = new WaitManager(driver);
    }

    //Clicking
    public ElementActions click(By locator) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        // Wait until the element is stable (not moving)
                        Point initialLocation = element.getLocation();
                        LogsManager.info("initialLocation: " + initialLocation);
                        Point finalLocation = element.getLocation();
                        LogsManager.info("finalLocation: " + finalLocation);
                        if (!initialLocation.equals(finalLocation)) {
                            return false; // still moving, wait longer
                        }
                        PacedElementInteraction.click(element);
                        LogsManager.info("Clicked on element: " + locator);
                        return true;
                    } catch (Exception e) {
                        LogsManager.error("Failed to click on element: " + locator, e.getMessage());
                        return false;
                    }
                }
        );
        return this;
    }

    //Typing
    public ElementActions type(By locator, String text) {
        return typeInternal(locator, text, false);
    }

    /** Password and other secrets — progressive pacing without logging field values. */
    public ElementActions typeSecure(By locator, String text) {
        return typeInternal(locator, text, true);
    }

    private ElementActions typeInternal(By locator, String text, boolean secure) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        PacedElementInteraction.typeText(element, text, secure);
                        LogsManager.info(
                                secure
                                        ? "Typed into secure element: " + locator + " (redacted)"
                                        : "Typed into element: " + locator + " (input content redacted)");
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
        );
        return this;
    }

    public ElementActions clearField(By locator) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        PacedElementInteraction.clearField(element);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
        );
        return this;
    }

    /** PrimeVue icon targets (e.g. password eye SVG) — Actions click with single action pacing. */
    public ElementActions clickPrimeVueTarget(By locator) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        PacedElementInteraction.clickWithActions(driver, element);
                        LogsManager.info("Clicked PrimeVue target: " + locator);
                        return true;
                    } catch (Exception e) {
                        LogsManager.error("Failed PrimeVue click: " + locator, e.getMessage());
                        return false;
                    }
                }
        );
        return this;
    }
    //hovering
    public ElementActions hover(By locator) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        new Actions(d).moveToElement(element).perform();
                        LogsManager.info("Hovered over element: " + locator);
                        return true;
                    } catch (Exception e) {
                        LogsManager.error("Failed to click on element: " + locator, e.getMessage());
                        return false;
                    }
                }
        );
        return this;
    }

    //Getting text
    public String getText(By locator) {
        return waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        String msg = element.getText();
                        LogsManager.info("Retrieved text from element: " + locator + " - Text: " + msg);
                        return !msg.isEmpty() ? msg : null;
                    } catch (Exception e) {
                        return null;
                    }
                }
        );
    }

    //upload file
    public ElementActions uploadFile(By locator,String filePath)
    {
        String fileAbsolute = System.getProperty("user.dir") + File.separator  + filePath ;
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        element.sendKeys(fileAbsolute);
                        LogsManager.info("Uploaded file: " + fileAbsolute + " to element: " + locator);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
        );
        return this;
    }


    //find an element
    public WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    //function to scroll to an element using js
    public void scrollToElementJS(By locator) {
        ((JavascriptExecutor) driver)
                .executeScript(""" 
                        arguments[0].scrollIntoView({behaviour:"auto",block:"center",inline:"center"});""", findElement(locator));
    }

    //select from dropdown
    public ElementActions selectFromDropdown(By locator, String value) {
        waitManager.fluentWait().until(d ->
                {
                    try {
                        WebElement element = d.findElement(locator);
                        scrollToElementJS(locator);
                        Select select = new Select(element);
                        select.selectByVisibleText(value);
                        HumanPacingManager.pauseForAction();
                        PacingCoverageTracker.recordPacedClick();
                        LogsManager.info("Selected value '" + value + "' from dropdown: " + locator);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
        );
        return this;
    }

}
