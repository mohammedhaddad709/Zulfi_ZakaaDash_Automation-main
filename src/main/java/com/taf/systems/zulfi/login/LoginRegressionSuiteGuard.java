package com.taf.systems.zulfi.login;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Inventory for {@code login-regression-suite.xml} and {@code login-hp-only-suite.xml}. UI and validation
 * cases live in {@link com.taf.systems.zulfi.features.login.LoginRegressionTest}; happy path in
 * {@link com.taf.systems.zulfi.features.login.LoginHappyPathTest}.
 */
public final class LoginRegressionSuiteGuard {

    public static final Set<String> LOGIN_REGRESSION_SUITE_INVENTORY =
            Collections.unmodifiableSet(
                    new LinkedHashSet<>(
                            Arrays.asList(
                                    LoginTestIds.LOGIN_HP_001,
                                    LoginTestIds.LOGIN_UI_001,
                                    LoginTestIds.LOGIN_UI_002,
                                    LoginTestIds.LOGIN_UI_003,
                                    LoginTestIds.LOGIN_UI_004,
                                    LoginTestIds.LOGIN_VAL_001,
                                    LoginTestIds.LOGIN_VAL_002,
                                    LoginTestIds.LOGIN_VAL_003,
                                    LoginTestIds.LOGIN_VAL_004,
                                    LoginTestIds.LOGIN_VAL_005,
                                    LoginTestIds.LOGIN_VAL_006,
                                    LoginTestIds.LOGIN_AUTH_001)));

    public static final Set<String> LOGIN_HP_ONLY_SUITE_INVENTORY =
            Collections.unmodifiableSet(Set.of(LoginTestIds.LOGIN_HP_001));

    private LoginRegressionSuiteGuard() {}

    public static boolean isInRegressionInventory(String testCaseId) {
        return testCaseId != null && LOGIN_REGRESSION_SUITE_INVENTORY.contains(testCaseId.trim());
    }
}
