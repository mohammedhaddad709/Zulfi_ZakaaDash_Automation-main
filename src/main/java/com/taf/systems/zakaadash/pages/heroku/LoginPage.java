package com.taf.systems.zakaadash.pages.heroku;

import com.taf.systems.zakaadash.config.ConfigManager;
import com.taf.systems.zakaadash.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final String LOGIN_PATH = "/login";

    private static final By USERNAME_INPUT = By.id("username");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By FLASH_MESSAGE = By.id("flash");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage openPage() {
        open(ConfigManager.config().baseUrl() + LOGIN_PATH);
        return this;
    }

    public void loginAs(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public String getFlashMessage() {
        return getText(FLASH_MESSAGE);
    }
}
