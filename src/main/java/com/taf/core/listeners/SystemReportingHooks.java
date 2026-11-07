package com.taf.core.listeners;

import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

/**
 * System-specific reporting plugged into the shared {@link TestNGListeners} lifecycle.
 * Implementations are discovered with {@link java.util.ServiceLoader}
 * ({@code META-INF/services/com.taf.core.listeners.SystemReportingHooks}).
 *
 * <p>Test-level callbacks fire only for tests the hook {@linkplain #ownsTest owns}; execution-level
 * callbacks fire only when the hook {@linkplain #ownsSystem owns} the run's {@code active.system}.
 * Ordering inside {@code afterInvocation}: soft assertions → {@link #afterAssertions} → screenshot
 * (named by {@link #evidenceStem}) → {@link #onTestFailure} → video → {@link #onTestThrowable} →
 * {@link #afterTest}.
 */
public interface SystemReportingHooks {
    boolean ownsSystem(String activeSystem);

    boolean ownsTest(ITestResult result);

    default void onExecutionStart() { }

    default void onExecutionFinish() { }

    default void onTestOutcome(ITestResult result) { }

    default void beforeTest(ITestResult result, WebDriver driver) { }

    default void afterAssertions(ITestResult result, WebDriver driver) { }

    /** Screenshot file stem prefix (test case id); {@code null} uses the method name. */
    default String evidenceStem(ITestResult result) { return null; }

    default void onTestFailure(ITestResult result, WebDriver driver) { }

    default void onTestThrowable(ITestResult result, WebDriver driver) { }

    default void afterTest(ITestResult result) { }

    default void onConfigurationFailure(ITestResult result, WebDriver driver) { }
}
