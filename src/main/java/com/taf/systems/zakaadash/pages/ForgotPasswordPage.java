package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** "نسيت كلمة المرور" page (live DOM 2026-11-07). Tests never submit it: it would send a reset e-mail. */
public class ForgotPasswordPage extends BasePage {

    private static final String ROUTE = "/auth/forgot-password";
    private static final By EMAIL_INPUT = By.cssSelector("form input#email");
    private static final By SEND_CODE_BUTTON = By.xpath("//form//button[normalize-space(.)='إرسال رمز التحقق']");
    private static final By BACK_TO_LOGIN_LINK = By.xpath(
            "//*[self::a or self::button][normalize-space(.)='العودة لتسجيل الدخول']");

    private final WaitUtil wait;

    public ForgotPasswordPage(WebDriver driver) {
        super(driver);
        this.wait = new WaitUtil(driver);
    }

    public ForgotPasswordPage waitUntilLoaded() {
        wait.waitForUrlToContain(ROUTE);
        wait.waitForVisible(EMAIL_INPUT);
        return this;
    }

    public boolean isLoaded() {
        return driver.getCurrentUrl().contains(ROUTE) && isDisplayed(EMAIL_INPUT) && isDisplayed(SEND_CODE_BUTTON);
    }

    public ZakaaDashLoginPage backToLogin() {
        wait.waitForClickable(BACK_TO_LOGIN_LINK).click();
        return new ZakaaDashLoginPage(driver).waitUntilLoginScreenVisible();
    }
}
