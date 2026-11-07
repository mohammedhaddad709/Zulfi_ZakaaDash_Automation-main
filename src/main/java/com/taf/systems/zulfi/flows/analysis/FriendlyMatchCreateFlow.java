package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.validations.FriendlyMatchNameRules;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;

/** Friendly metadata completion and exactly-once submit (phase e). */
public final class FriendlyMatchCreateFlow {

    private final WebDriver driver;

    public FriendlyMatchCreateFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Enter friendly match name and date")
    public CreateAnalysisPage completeRequiredFriendlyMetadata(
            CreateAnalysisPage form, String matchName, LocalDate matchDate) {
        if (FriendlyMatchNameRules.isEmptyAfterTrim(matchName)
                || !FriendlyMatchNameRules.meetsMinimumLength(matchName)) {
            throw new IllegalArgumentException("Friendly match name invalid for submission: " + matchName);
        }
        form.ensureMatchTypeFriendly();
        form.enterFriendlyMatchName(matchName);
        form.selectFriendlyMatchDate(matchDate);
        if (form.getMatchDateValue().isBlank()) {
            throw new IllegalStateException("Friendly match date empty after selection");
        }
        return form;
    }

    @Step("Fill description and video for friendly analysis")
    public CreateAnalysisPage fillDescriptionAndVideo(
            CreateAnalysisPage form, String description, String videoUrl) {
        form.enterOptionalDescription(description).enterVideoUrl(videoUrl);
        if (form.getVideoUrlValue() == null || form.getVideoUrlValue().isBlank()) {
            throw new IllegalStateException("Video URL empty after entry");
        }
        return form;
    }

    @Step("Submit friendly analysis exactly once")
    public AnalysisPostSubmitOutcome submitFriendlyAnalysisExactlyOnce(
            CreateAnalysisPage form, AnalysisSubmissionAttempt attempt) {
        AnalysisCreateGate.requireFriendlyCreationEnabled();
        if (!form.isMatchType(CreateAnalysisPage.MATCH_TYPE_FRIENDLY)) {
            throw new IllegalStateException("Refusing submit: match type is not «ودية»");
        }
        if (form.isOfficialMatchSelectorVisible()) {
            throw new IllegalStateException("Refusing friendly submit: official match selector visible");
        }
        if (!form.isSubmitEnabled()) {
            throw new IllegalStateException("Submit disabled before authorized friendly submission");
        }
        attempt.recordSubmitClick();
        form.submitOnce();
        AnalysisPostSubmitOutcome outcome = form.waitForPostSubmitOutcome();
        attempt.recordOutcome(outcome);
        return outcome;
    }
}
