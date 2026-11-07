package com.taf.systems.zulfi.reporting;

/**
 * The product behaviour could not be verified because the evidence is not observable through the UI
 * (e.g. an internal ID that the portal never displays). Reported as Allure "broken" and classified
 * {@link DefectClassification#INCONCLUSIVE} — never as a pass, never as a product defect.
 */
public final class EvidenceUnavailableException extends RuntimeException {
    public EvidenceUnavailableException(String message) {
        super(message);
    }
}
