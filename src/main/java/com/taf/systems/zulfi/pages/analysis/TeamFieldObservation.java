package com.taf.systems.zulfi.pages.analysis;

/** Read-only observation of how a Home/Away team name is rendered in the DOM. */
public record TeamFieldObservation(
        String fieldId,
        String resolvedDisplayText,
        String resolutionSource,
        String rootTagName,
        String inputTagName,
        String domPropertyValue,
        String domPropertyPlaceholder,
        String attributeValue,
        String attributePlaceholder,
        String inputTextContent,
        String rootTextContent,
        boolean inputReadonly,
        boolean inputDisabled,
        boolean inputDisplayed) {

    public String toDiagnosticBlock() {
        return """
                fieldId=%s
                resolvedDisplayText=%s
                resolutionSource=%s
                rootTagName=%s
                inputTagName=%s
                domPropertyValue=%s
                domPropertyPlaceholder=%s
                attributeValue=%s
                attributePlaceholder=%s
                inputTextContent=%s
                rootTextContent=%s
                inputReadonly=%s
                inputDisabled=%s
                inputDisplayed=%s
                """
                .formatted(
                        fieldId,
                        resolvedDisplayText,
                        resolutionSource,
                        rootTagName,
                        inputTagName,
                        domPropertyValue,
                        domPropertyPlaceholder,
                        attributeValue,
                        attributePlaceholder,
                        inputTextContent,
                        rootTextContent,
                        inputReadonly,
                        inputDisabled,
                        inputDisplayed);
    }
}
