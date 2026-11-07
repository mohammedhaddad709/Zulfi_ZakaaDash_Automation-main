package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Authenticated top bar. Live DOM 2026-11-07: the avatar button toggles its aria-label between
 * "فتح القائمة" and "إغلاق القائمة" and opens a PrimeVue popover that holds "تسجيل الخروج".
 */
public class ZakaaDashAppShellPage extends BasePage {

    private static final By PROFILE_MENU_TRIGGER = By.cssSelector("nav button[aria-label='فتح القائمة']");
    private static final By PROFILE_MENU = By.cssSelector("[data-pc-name='popover']");
    // Success toasts render in the top-left corner (RTL) over the avatar until they auto-dismiss.
    private static final By TOAST_MESSAGE = By.cssSelector(".p-toast-message");
    private static final By LOGOUT_BUTTON = By.cssSelector("[data-pc-name='popover'] button[aria-label='تسجيل الخروج']");

    private final WaitUtil wait;

    public ZakaaDashAppShellPage(WebDriver driver) {
        super(driver);
        this.wait = new WaitUtil(driver);
    }

    public ZakaaDashAppShellPage openProfileMenu() {
        if (!isDisplayed(PROFILE_MENU)) {
            wait.waitForInvisible(TOAST_MESSAGE);
            wait.waitForClickable(PROFILE_MENU_TRIGGER, "Open profile menu").click();
        }
        wait.waitForVisible(LOGOUT_BUTTON);
        HumanPaceUtil.pauseNormalAction();
        ZakaaDashLog.info("Profile menu — opened");
        return this;
    }

    public ZakaaDashLoginPage logout() {
        openProfileMenu();
        wait.waitForClickable(LOGOUT_BUTTON).click();
        ZakaaDashLog.info("Logout — clicked");
        return new ZakaaDashLoginPage(driver).waitUntilLoginScreenVisible();
    }
}
