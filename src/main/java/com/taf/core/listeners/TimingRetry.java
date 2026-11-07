package com.taf.core.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

/**
 * Opt-in "retry timing failures once" for the embedded runner (added with {@code -listener} only when a run asks for
 * it; Maven runs never load it). A retried test keeps one Allure history id, and the runner reports it as flaky.
 *
 * <p>Only timing failures are retried (timeouts, stale or intercepted elements), at most once per test. Assertion
 * failures without a timing cause are never retried, so no check is weakened. Nothing is retried in a run that
 * enables a create/submit/lifecycle gate ({@code *.create.enabled}, {@code *.submit.enabled},
 * {@code *.lifecycle.enabled}): AGENTS.md allows at most one creating submit and no automatic retries.
 */
public final class TimingRetry implements IAnnotationTransformer, IRetryAnalyzer {

    static final int MAX_RETRIES = 1;
    private static final Set<String> TIMING = Set.of(
            "org.openqa.selenium.TimeoutException",
            "org.openqa.selenium.StaleElementReferenceException",
            "org.openqa.selenium.ElementClickInterceptedException",
            "org.openqa.selenium.ElementNotInteractableException",
            "org.openqa.selenium.ScriptTimeoutException",
            "java.util.concurrent.TimeoutException",
            "java.net.SocketTimeoutException");

    private int retries;

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        Class<? extends IRetryAnalyzer> current = annotation.getRetryAnalyzerClass();
        if (current == null || current == DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(TimingRetry.class);
        }
    }

    @Override
    public boolean retry(ITestResult result) {
        if (retries >= MAX_RETRIES || submitGateOpen() || !isTimingFailure(result.getThrowable())) return false;
        retries++;
        System.out.println("[TAF-RETRY] " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName()
                + " timing failure, retry " + retries + "/" + MAX_RETRIES);
        return true;
    }

    static boolean isTimingFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (TIMING.contains(current.getClass().getName())) return true;
            if (current.getCause() == current) break;
        }
        return false;
    }

    static boolean submitGateOpen() {
        for (String key : System.getProperties().stringPropertyNames()) {
            String lower = key.toLowerCase(Locale.ROOT);
            boolean gate = lower.endsWith(".create.enabled") || lower.endsWith(".submit.enabled")
                    || lower.endsWith(".lifecycle.enabled") || lower.endsWith(".creation.enabled");
            if (gate && Boolean.parseBoolean(System.getProperty(key).strip())) return true;
        }
        return false;
    }
}
