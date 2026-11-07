package com.taf.systems.zakaadash.tests.heroku;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.pages.heroku.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HerokuLoginTest extends ZakaaDashBaseTest {

    @Test
    public void validUserCanLogin() {
        LoginPage loginPage = new LoginPage(driver()).openPage();
        loginPage.loginAs("tomsmith", "SuperSecretPassword!");

        String flashMessage = loginPage.getFlashMessage();
        Assert.assertTrue(flashMessage.contains("You logged into a secure area!"),
                "Expected success message but was: " + flashMessage);
    }
}
