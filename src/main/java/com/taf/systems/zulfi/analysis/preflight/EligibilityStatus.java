package com.taf.systems.zulfi.analysis.preflight;

/** Evidence-based fixture eligibility for preflight (not backend-authoritative by default). */
public enum EligibilityStatus {
    ELIGIBLE_VERIFIED,
    POTENTIALLY_ELIGIBLE,
    INELIGIBLE,
    UNKNOWN
}
