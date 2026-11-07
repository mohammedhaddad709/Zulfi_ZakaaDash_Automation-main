package com.taf.systems.zulfi.framework;

import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.flows.LoginFlow;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

/** Every future protected-feature test receives a FRESH, verified login. */
public abstract class AuthenticatedUiTest extends BaseUiTest {
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD required for authenticated UI tests.");
        }
        new LoginFlow(browser()).loginAsConfiguredUser();
    }
}
