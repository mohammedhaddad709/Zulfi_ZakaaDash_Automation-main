package com.taf.systems.zulfi.pages.analysis;

public record TeamFieldsEditabilityVerdict(
        boolean homeNonEditable,
        boolean awayNonEditable,
        TeamFieldEditabilityObservation home,
        TeamFieldEditabilityObservation away) {

    public boolean bothNonEditable() {
        return homeNonEditable && awayNonEditable;
    }

    public String toDiagnosticBlock() {
        return """
                homeNonEditable=%s
                awayNonEditable=%s
                bothNonEditable=%s

                --- Home ---
                %s
                --- Away ---
                %s
                """
                .formatted(
                        homeNonEditable,
                        awayNonEditable,
                        bothNonEditable(),
                        home.toDiagnosticBlock(),
                        away.toDiagnosticBlock());
    }
}
