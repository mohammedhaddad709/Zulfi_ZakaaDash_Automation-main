package com.taf.systems.zulfi.login;

import com.taf.systems.zulfi.features.login.LoginHappyPathTest;
import com.taf.systems.zulfi.features.login.LoginRegressionTest;
import com.taf.systems.zulfi.reporting.TestCaseReport;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Ensures {@link TestCaseReport} IDs on login suites match {@link LoginRegressionSuiteGuard} inventory. */
public final class LoginRegressionSuiteGuardContractTest {

    @Test
    public void regressionInventoryMatchesTestCaseReportAnnotations() {
        Set<String> annotated = collectReportIds(LoginHappyPathTest.class, LoginRegressionTest.class);
        Assert.assertEquals(
                annotated,
                LoginRegressionSuiteGuard.LOGIN_REGRESSION_SUITE_INVENTORY,
                "Login @TestCaseReport IDs must match LoginRegressionSuiteGuard inventory");
    }

    private static Set<String> collectReportIds(Class<?>... classes) {
        Set<String> ids = new HashSet<>();
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(org.testng.annotations.Test.class)) {
                    continue;
                }
                TestCaseReport report = method.getAnnotation(TestCaseReport.class);
                Assert.assertNotNull(
                        report,
                        clazz.getSimpleName() + "#" + method.getName() + " must declare @TestCaseReport");
                ids.add(report.id());
            }
        }
        return ids;
    }
}
