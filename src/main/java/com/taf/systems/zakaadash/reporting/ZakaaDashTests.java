package com.taf.systems.zakaadash.reporting;

import org.testng.ITestResult;

/** ZakaaDash listeners are registered globally; they only act on ZakaaDash test classes. */
final class ZakaaDashTests {

    private static final String PACKAGE_PREFIX = "com.taf.systems.zakaadash.";

    private ZakaaDashTests() {
    }

    static boolean owns(ITestResult result) {
        return result.getTestClass() != null && result.getTestClass().getName().startsWith(PACKAGE_PREFIX);
    }
}
