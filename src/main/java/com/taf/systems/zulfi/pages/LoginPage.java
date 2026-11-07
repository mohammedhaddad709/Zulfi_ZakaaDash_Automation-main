package com.taf.systems.zulfi.pages;

import com.taf.core.pacing.HumanPacingManager;
import com.taf.core.utils.actions.ElementActions;
import com.taf.systems.zulfi.config.PortalConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/** Login Page Object only — no test assertions/test cases yet. */
public final class LoginPage {
    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.cssSelector("form button[type='submit'][aria-label='تسجيل الدخول']");
    private static final By FORM = By.cssSelector("form[data-pc-name='form']");
    /** PrimeVue Password: eye icon is SVG sibling of #password (data-pc-section toggles unmask/mask). */
    private static final By PASSWORD_VISIBILITY_TOGGLE =
            By.cssSelector(
                    ".p-password svg[data-pc-section='unmaskicon'],"
                            + " .p-password svg[data-pc-section='maskicon']");
    private static final By REJECTION_MESSAGE =
            By.cssSelector(
                    ".p-toast-message, .p-message, [role='alert'], .p-inline-message, small.p-error, .p-error");
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final ElementActions actions;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.actions = new ElementActions(driver);
    }

    public LoginPage open() {
        driver.get(PortalConfig.loginUrl());
        awaitReady();
        HumanPacingManager.pauseForTransition();
        return this;
    }

    public LoginPage awaitReady() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM));
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL));
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD));
        return this;
    }

    public LoginPage enterEmail(String email) {
        actions.type(EMAIL, email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        actions.typeSecure(PASSWORD, password);
        return this;
    }

    public void submit() {
        actions.click(SUBMIT);
        HumanPacingManager.pauseForTransition();
    }

    public boolean isDisplayed() {
        return !driver.findElements(EMAIL).isEmpty() && driver.findElement(EMAIL).isDisplayed();
    }

    public boolean isPasswordFieldVisible() {
        return !driver.findElements(PASSWORD).isEmpty() && driver.findElement(PASSWORD).isDisplayed();
    }

    public String loginPageObservation() {
        return "URL="
                + driver.getCurrentUrl()
                + ", emailVisible="
                + isDisplayed()
                + ", passwordVisible="
                + isPasswordFieldVisible()
                + ", submitVisible="
                + isSubmitVisible();
    }

    public boolean isEmailMasked() {
        return "email".equalsIgnoreCase(driver.findElement(EMAIL).getAttribute("type"));
    }

    public String getPasswordInputType() {
        String type = driver.findElement(PASSWORD).getAttribute("type");
        return type == null ? "" : type.trim().toLowerCase();
    }

    public boolean isPasswordMasked() {
        return "password".equals(getPasswordInputType());
    }

    public boolean isPasswordVisible() {
        return "text".equals(getPasswordInputType());
    }

    public boolean isSubmitVisible() {
        return !driver.findElements(SUBMIT).isEmpty() && driver.findElement(SUBMIT).isDisplayed();
    }

    public String emailValue() {
        return driver.findElement(EMAIL).getAttribute("value");
    }

    public LoginPage clearFields() {
        actions.clearField(EMAIL);
        actions.clearField(PASSWORD);
        return this;
    }

    public boolean isPasswordVisibilityTogglePresent() {
        return driver.findElements(PASSWORD_VISIBILITY_TOGGLE).stream()
                .anyMatch(
                        element -> {
                            try {
                                return element.isDisplayed();
                            } catch (StaleElementReferenceException ex) {
                                return false;
                            }
                        });
    }

    /** @deprecated use {@link #isPasswordVisibilityTogglePresent()} */
    @Deprecated
    public boolean isPasswordTogglePresent() {
        return isPasswordVisibilityTogglePresent();
    }

    public LoginPage clickPasswordVisibilityToggle() {
        actions.clickPrimeVueTarget(PASSWORD_VISIBILITY_TOGGLE);
        return this;
    }

    /** Length only — never log or report the raw password value. */
    public int passwordValueLength() {
        String value = driver.findElement(PASSWORD).getAttribute("value");
        return value == null ? 0 : value.length();
    }

    public String passwordToggleObservation() {
        return "passwordType="
                + getPasswordInputType()
                + ", togglePresent="
                + isPasswordVisibilityTogglePresent()
                + ", valueLength="
                + passwordValueLength();
    }

    /**
     * After submit, waits (bounded) until the portal either leaves the login page or shows a rejection message,
     * so "still on login" is judged after the server answered, not before.
     */
    public LoginPage awaitSubmitOutcome() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(d -> !d.getCurrentUrl().contains("/auth/login") || !visibleErrorText().isBlank());
        } catch (TimeoutException noVisibleOutcome) {
            // Judged by the caller: staying on the login page without a message is still "not authenticated".
        }
        return this;
    }

    /**
     * Text of any visible rejection message (PrimeVue toast / message / alert / field error), or empty.
     * Wording is observed and reported, not asserted: it is not yet confirmed against the approved test case.
     */
    public String visibleErrorText() {
        StringBuilder text = new StringBuilder();
        for (WebElement element : driver.findElements(REJECTION_MESSAGE)) {
            try {
                if (element.isDisplayed() && !element.getText().isBlank()) {
                    if (text.length() > 0) text.append(" | ");
                    text.append(element.getText().trim());
                }
            } catch (StaleElementReferenceException ignored) {
                // element re-rendered between lookup and read
            }
        }
        return text.toString();
    }

    public boolean remainsOnLoginPage() {
        return isDisplayed() && driver.getCurrentUrl().contains("/auth/login");
    }
}
