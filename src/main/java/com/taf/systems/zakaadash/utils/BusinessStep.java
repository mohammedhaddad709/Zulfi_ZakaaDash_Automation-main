package com.taf.systems.zakaadash.utils;

import io.qameta.allure.Allure;
import org.testng.ITestResult;
import org.testng.Reporter;

import java.util.function.Supplier;

public final class BusinessStep {

    public static final String CURRENT_PAGE = "zakaadash.currentPage";
    public static final String CURRENT_STEP = "zakaadash.currentStep";
    public static final String LAST_COMPLETED_STEP = "zakaadash.lastCompletedStep";
    public static final String DATASET = "zakaadash.dataset";
    public static final String SOURCE_NAME = "zakaadash.sourceName";
    public static final String PROCESSED_VERSION_NAME = "zakaadash.processedVersionName";
    public static final String PROCESSED_COUNT_BEFORE = "zakaadash.processedCountBefore";
    public static final String CATEGORY = "zakaadash.category";
    public static final String CONFIGURATION = "zakaadash.configuration";
    public static final String FAILURE_URL = "zakaadash.failureUrl";
    public static final String PAGE_TITLE = "zakaadash.pageTitle";
    public static final String SCREENSHOT_PATH = "zakaadash.screenshotPath";
    public static final String ENVIRONMENT = "zakaadash.environment";
    public static final String PREPROCESSING_STEP = "zakaadash.preprocessing.step";
    public static final String EXPECTED_RESULT = "zakaadash.preprocessing.expected";

    private BusinessStep() {
    }

    public static void run(String page, String step, Runnable action) {
        get(page, step, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T get(String page, String step, Supplier<T> action) {
        setAttribute(CURRENT_PAGE, page);
        setAttribute(CURRENT_STEP, step);
        Allure.ThrowableRunnable<T> allureStep = action::get;
        T value = Allure.step(page + " | " + step, allureStep);
        setAttribute(LAST_COMPLETED_STEP, step);
        setAttribute(CURRENT_STEP, null);
        return value;
    }

    public static void recordDataset(String datasetName) {
        recordParameter(DATASET, "Dataset", datasetName);
    }

    public static void recordSourceName(String sourceName) {
        recordParameter(SOURCE_NAME, "Source Name", sourceName);
    }

    public static void recordCategory(String category) {
        recordParameter(CATEGORY, "Category", category);
    }

    public static void recordConfiguration(String configuration) {
        recordParameter(CONFIGURATION, "Configuration", configuration);
    }

    private static void recordParameter(String attribute, String label, String value) {
        setAttribute(attribute, value);
        Allure.parameter(label, value);
    }

    public static void record(String attributeName, Object value) {
        setAttribute(attributeName, value);
    }

    public static String readAttribute(ITestResult result, String name) {
        Object value = result.getAttribute(name);
        return value != null ? value.toString() : "N/A";
    }

    public static void writeAttribute(ITestResult result, String name, Object value) {
        if (value == null) {
            result.removeAttribute(name);
        } else {
            result.setAttribute(name, value);
        }
    }

    private static void setAttribute(String name, Object value) {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result == null) {
            return;
        }
        if (value == null) {
            result.removeAttribute(name);
        } else {
            result.setAttribute(name, value);
        }
    }
}
