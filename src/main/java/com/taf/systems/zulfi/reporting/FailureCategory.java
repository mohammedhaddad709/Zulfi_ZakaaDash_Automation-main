package com.taf.systems.zulfi.reporting;

/** High-level failure taxonomy for business-readable diagnostics. */
public enum FailureCategory {
    PRODUCT_DEFECT,
    AUTOMATION_DEFECT,
    TEST_DATA_ERROR,
    CONFIGURATION_ERROR,
    ENVIRONMENT_ERROR,
    TIMEOUT,
    INCONCLUSIVE
}
