package com.taf.systems.zulfi.pages.analysis;

/** DOM evidence for required/optional field labeling (MCH-ANL-007). */
public record CreateAnalysisFieldRequirementEvidence(
        String fieldRootId,
        String visibleLabelText,
        boolean labelShowsRequiredMarker,
        boolean rootMarkedRequired,
        boolean ariaRequiredOnControl,
        boolean htmlRequiredOnControl,
        String controlTagName,
        String visibleValidationText,
        String labelDomSnippet) {

    public String toDiagnosticBlock() {
        return "field="
                + fieldRootId
                + "; label="
                + visibleLabelText
                + "; requiredMarker="
                + labelShowsRequiredMarker
                + "; ariaRequired="
                + ariaRequiredOnControl
                + "; htmlRequired="
                + htmlRequiredOnControl
                + "; validation="
                + visibleValidationText;
    }
}
