package com.taf.systems.zulfi.reporting;

import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.validations.DuplicateFixtureToastRules;
import org.openqa.selenium.WebDriver;

public final class PostSubmitOutcomeHandler {

    private PostSubmitOutcomeHandler() {}

    public static FailureDiagnostics diagnoseCreateAnalysisOutcome(
            WebDriver driver, CreateAnalysisPage form, AnalysisPostSubmitOutcome outcome) {
        String toast = form.getVisibleErrorToastBody();
        return switch (outcome) {
            case SUCCESS -> null;
            case DUPLICATE_FIXTURE -> new FailureDiagnostics(
                    "Submit official analysis",
                    "Arabic user-facing error explaining that analysis already exists for this match",
                    toast == null || toast.isBlank() ? "Duplicate fixture outcome" : toast,
                    DuplicateFixtureToastRules.isUnlocalizedTechnicalDuplicateToast(toast)
                            ? "English technical toast (externalEventId/matchId) on Arabic UI"
                            : "Duplicate analysis for fixture",
                    safeUrl(driver),
                    "AnalysisPostSubmitOutcome.DUPLICATE_FIXTURE",
                    DuplicateFixtureToastRules.isUnlocalizedTechnicalDuplicateToast(toast)
                            ? DefectClassification.PRODUCT_DEFECT
                            : DefectClassification.TEST_DATA_ERROR);
            case STILL_ON_CREATE, TIMEOUT -> new FailureDiagnostics(
                    "Submit official analysis",
                    "Success dialog or analysis detail route",
                    toast == null || toast.isBlank() ? outcome.name() : toast,
                    outcome.name(),
                    safeUrl(driver),
                    "AnalysisPostSubmitOutcome." + outcome.name(),
                    DefectClassification.INCONCLUSIVE);
        };
    }

    private static String safeUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception ex) {
            return "unavailable";
        }
    }
}
