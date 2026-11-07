package com.taf.systems.zulfi.pages.analysis;

/** Read-only DOM signals for whether a team field accepts user edits. */
public record TeamFieldEditabilityObservation(
        String fieldId,
        String tagName,
        boolean domReadOnly,
        boolean domDisabled,
        String ariaReadonly,
        String ariaDisabled,
        String pointerEvents,
        String dataPcDisabled,
        boolean focusable,
        String domPropertyValue,
        String domPropertyPlaceholder,
        Boolean editProbeAttempted,
        Boolean editProbeValueUnchanged,
        String editProbeNote) {

    public boolean domSignalsNonEditable() {
        if (domReadOnly || domDisabled) {
            return true;
        }
        if ("true".equalsIgnoreCase(ariaReadonly)) {
            return true;
        }
        if ("true".equalsIgnoreCase(ariaDisabled)) {
            return true;
        }
        if ("none".equalsIgnoreCase(pointerEvents)) {
            return true;
        }
        return "true".equalsIgnoreCase(dataPcDisabled);
    }

    public String toDiagnosticBlock() {
        return """
                fieldId=%s
                tagName=%s
                domReadOnly=%s
                domDisabled=%s
                ariaReadonly=%s
                ariaDisabled=%s
                pointerEvents=%s
                dataPcDisabled=%s
                focusable=%s
                domPropertyValue=%s
                domPropertyPlaceholder=%s
                editProbeAttempted=%s
                editProbeValueUnchanged=%s
                editProbeNote=%s
                domSignalsNonEditable=%s
                """
                .formatted(
                        fieldId,
                        tagName,
                        domReadOnly,
                        domDisabled,
                        ariaReadonly,
                        ariaDisabled,
                        pointerEvents,
                        dataPcDisabled,
                        focusable,
                        domPropertyValue,
                        domPropertyPlaceholder,
                        editProbeAttempted,
                        editProbeValueUnchanged,
                        editProbeNote,
                        domSignalsNonEditable());
    }
}
