package com.taf.systems.zulfi.framework;

import com.taf.systems.zulfi.pages.LoginPage;
import org.testng.annotations.BeforeMethod;

/** Login test cases MUST start logged out, never pre-authenticated. */
public abstract class UnauthenticatedUiTest extends BaseUiTest {
    @BeforeMethod(alwaysRun = true)
    public void openLoginInFreshBrowser() { new LoginPage(browser()).open(); }
}
