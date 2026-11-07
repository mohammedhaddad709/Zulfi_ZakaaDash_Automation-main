package com.taf.systems.zakaadash.pages;

import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.pages.BasePage;
import com.taf.systems.zakaadash.utils.HumanPaceUtil;
import com.taf.systems.zakaadash.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class ZakaaDashLoginPage extends BasePage {

    private static final String LOGIN_ROUTE = "/auth/login";

    private static final By EMAIL_INPUT = By.cssSelector("#email");
    private static final By PASSWORD_INPUT = By.cssSelector("#password");
    private static final By SUBMIT_BUTTON = By.cssSelector("form button[type='submit']");
    // Live DOM 2026-11-07: the server-side rejection is a PrimeVue message placed directly in the form;
    // field validation messages live inside each .p-formfield, so the two never share a locator.
    private static final By AUTHENTICATION_ERROR = By.cssSelector("form > [data-pc-name='message'][role='alert']");
    private static final By PASSWORD_UNMASK_ICON = By.cssSelector(".p-password svg[data-pc-section='unmaskicon']");
    private static final By PASSWORD_MASK_ICON = By.cssSelector(".p-password svg[data-pc-section='maskicon']");
    private static final By FORGOT_PASSWORD_LINK = By.cssSelector("form a[href$='/auth/forgot-password']");
    private static final By EMAIL_FIELD_ERROR = fieldError("email");
    private static final By PASSWORD_FIELD_ERROR = fieldError("password");

    private final WaitUtil wait;

    public ZakaaDashLoginPage(WebDriver driver) {
        super(driver);
        this.wait = new WaitUtil(driver);
    }

    public ZakaaDashLoginPage open() {
        open(ConfigManager.config().zakaadashBaseUrl() + ConfigManager.config().zakaadashLoginPath());
        wait.waitForVisible(EMAIL_INPUT);
        return this;
    }

    public boolean isLoaded() {
        return isOnLoginRoute() && isDisplayed(EMAIL_INPUT) && isDisplayed(PASSWORD_INPUT);
    }

    public boolean isOnLoginRoute() {
        return driver.getCurrentUrl().contains(LOGIN_ROUTE);
    }

    public ZakaaDashLoginPage waitUntilLoginScreenVisible() {
        wait.waitForUrlToContain(LOGIN_ROUTE);
        wait.waitForVisible(EMAIL_INPUT);
        return this;
    }

    public ZakaaDashLoginPage enterEmail(String email) {
        typeAfterClear(EMAIL_INPUT, email);
        return this;
    }

    public ZakaaDashLoginPage enterPassword(String password) {
        typeAfterClear(PASSWORD_INPUT, password);
        return this;
    }

    public void submitLogin() {
        wait.waitForClickable(SUBMIT_BUTTON).click();
        HumanPaceUtil.pauseBetweenActions();
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        submitLogin();
    }

    public Optional<String> getAuthenticationError() {
        try {
            return Optional.of(wait.waitForVisible(AUTHENTICATION_ERROR).getText().trim());
        } catch (TimeoutException ex) {
            return Optional.empty();
        }
    }

    /** "redirect" query value of the login URL, e.g. "/data-versions" after opening a protected page. */
    public Optional<String> getRedirectTarget() {
        String url = driver.getCurrentUrl();
        int marker = url.indexOf("redirect=");
        return marker < 0 ? Optional.empty() : Optional.of(url.substring(marker + "redirect=".length()));
    }

    public String getPasswordInputType() {
        return wait.waitForVisible(PASSWORD_INPUT).getAttribute("type");
    }

    /** PrimeVue eye icon: shows the password as text, then the mask icon replaces it. */
    public ZakaaDashLoginPage revealPassword() {
        wait.waitForClickable(PASSWORD_UNMASK_ICON).click();
        wait.waitForVisible(PASSWORD_MASK_ICON);
        return this;
    }

    public ZakaaDashLoginPage hidePassword() {
        wait.waitForClickable(PASSWORD_MASK_ICON).click();
        wait.waitForVisible(PASSWORD_UNMASK_ICON);
        return this;
    }

    public ForgotPasswordPage openForgotPassword() {
        wait.waitForClickable(FORGOT_PASSWORD_LINK).click();
        return new ForgotPasswordPage(driver).waitUntilLoaded();
    }

    /** Field-level validation message under the email input, if one is visible. */
    public Optional<String> getEmailValidationMessage() {
        return visibleText(EMAIL_FIELD_ERROR);
    }

    /** Field-level validation message under the password input, if one is visible. */
    public Optional<String> getPasswordValidationMessage() {
        return visibleText(PASSWORD_FIELD_ERROR);
    }

    /** Waits for the client-side validation that blocks an empty submit. */
    public void waitForFieldValidation() {
        wait.waitForAnyVisible(By.cssSelector(".p-formfield [data-pc-name='message']"));
    }

    private Optional<String> visibleText(By locator) {
        return driver.findElements(locator).stream()
                .filter(WebElement::isDisplayed)
                .map(element -> element.getText().trim())
                .filter(text -> !text.isEmpty())
                .findFirst();
    }

    private static By fieldError(String inputId) {
        return By.xpath("//input[@id='" + inputId + "']/ancestor::div[@data-pc-name='formfield'][1]"
                + "//div[@data-pc-name='message']");
    }

    private void typeAfterClear(By locator, String text) {
        WebElement field = wait.waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
        HumanPaceUtil.pauseBetweenActions();
    }
}
