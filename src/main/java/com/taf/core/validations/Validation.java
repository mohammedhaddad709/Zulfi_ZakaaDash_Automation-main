package com.taf.core.validations;

import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.asserts.SoftAssert;

/** Soft assertion state is isolated per test thread and reset after each test. */
public class Validation extends BaseAssertion {
    private static final ThreadLocal<SoftAssert> ASSERTIONS = ThreadLocal.withInitial(SoftAssert::new);
    private static final ThreadLocal<Boolean> USED = ThreadLocal.withInitial(() -> false);
    public Validation() { super(); }
    public Validation(WebDriver driver) { super(driver); }
    @Override protected void assertTrue(boolean condition, String message) {
        USED.set(true); ASSERTIONS.get().assertTrue(condition, message);
    }
    @Override protected void assertFalse(boolean condition, String message) {
        USED.set(true); ASSERTIONS.get().assertFalse(condition, message);
    }
    @Override protected void assertEquals(String actual, String expected, String message) {
        USED.set(true); ASSERTIONS.get().assertEquals(actual, expected, message);
    }
    public static void assertAll(ITestResult result) {
        try {
            if (USED.get()) ASSERTIONS.get().assertAll();
        } catch (AssertionError e) {
            result.setStatus(ITestResult.FAILURE);
            if (result.getThrowable() != null) e.addSuppressed(result.getThrowable());
            result.setThrowable(e);
        } finally {
            ASSERTIONS.remove();
            USED.remove();
        }
    }
}
