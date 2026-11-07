package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifest;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestEntry;
import com.taf.systems.zulfi.analysis.fixture.OfficialFixtureManifestStore;
import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Orchestrates official match analysis creation — exactly-once submit enforced. */
public final class CreateAnalysisFlow {

    private final WebDriver driver;

    public CreateAnalysisFlow(WebDriver driver) {
        this.driver = driver;
    }

    public record OfficialAnalysisFormSnapshot(
            String fixtureLabel,
            String matchDate,
            String homeTeamPlaceholder,
            String awayTeamPlaceholder) {}

    @Step("Open New Match Analysis popup from Home hero")
    public CreateAnalysisPage openCreatePopupFromHome() {
        new HomePage(driver).awaitReady().clickStartAnalysis();
        return new CreateAnalysisPage(driver).waitForCreatePopupReady();
    }

    @Step("Select configured official fixture before submit")
    public CreateAnalysisPage completeOfficialMatchSelection(CreateAnalysisPage form) {
        form.ensureMatchTypeOfficial();
        String configured = resolveAuthorizedFixtureLabel(form);
        if (KnownFixtureExclusions.isKnownDuplicateFixture(configured)) {
            throw new IllegalStateException(
                    "Refusing known duplicate fixture for submission: "
                            + configured
                            + ". "
                            + KnownFixtureExclusions.knownDuplicateReason());
        }
        if (AnalysisCreateGate.configuredFixtureIndex().isPresent()) {
            form.selectOfficialFixtureByIndex(AnalysisCreateGate.configuredFixtureIndex().getAsInt());
        } else {
            form.selectOfficialFixtureByExactLabel(configured);
        }
        if (!form.isOfficialMatchSelected()) {
            throw new IllegalStateException("Configured fixture not selected: " + configured);
        }
        if (!configured.equals(form.getOfficialMatchSelectedLabel())) {
            throw new IllegalStateException(
                    "Selected fixture identity mismatch. Expected="
                            + configured
                            + "; actual="
                            + form.getOfficialMatchSelectedLabel());
        }
        if (!form.isOfficialMatchDateDisplayValid()) {
            throw new IllegalStateException(
                    "Match date invalid after fixture selection; date=" + form.getMatchDateValue());
        }
        if (!form.hasPopulatedTeamPlaceholders()) {
            throw new IllegalStateException(
                    "Team placeholders empty after fixture selection; home="
                            + form.getHomeTeamPlaceholder()
                            + "; away="
                            + form.getAwayTeamPlaceholder());
        }
        var editability = form.verifyTeamFieldsNonEditable();
        if (!editability.bothNonEditable()) {
            throw new IllegalStateException(
                    "Team fields must be non-editable for official match; " + editability.toDiagnosticBlock());
        }
        return form;
    }

    private String resolveAuthorizedFixtureLabel(CreateAnalysisPage form) {
        if (AnalysisCreateGate.configuredFixtureIndex().isPresent()) {
            int index = AnalysisCreateGate.configuredFixtureIndex().getAsInt();
            OfficialFixtureManifest manifest = OfficialFixtureManifestStore.load();
            if (manifest != null) {
                OfficialFixtureManifestEntry entry = manifest.entryAt(index);
                AnalysisCreateGate.optionalExpectedFixtureLabelOverride()
                        .ifPresent(
                                expected -> {
                                    if (!expected.equals(entry.displayLabel())) {
                                        throw new IllegalStateException(
                                                "Fixture index "
                                                        + index
                                                        + " label "
                                                        + entry.displayLabel()
                                                        + " does not match expected override "
                                                        + expected);
                                    }
                                });
                return entry.displayLabel();
            }
            return form.getOfficialFixtureOptionMetadata(index).displayLabel();
        }
        return AnalysisCreateGate.requireConfiguredOfficialFixtureLabel();
    }

    @Step("Fill description and video URL")
    public CreateAnalysisPage fillDescriptionAndVideo(CreateAnalysisPage form, String description, String videoUrl) {
        form.enterOptionalDescription(description).enterVideoUrl(videoUrl);
        if (form.getVideoUrlValue() == null || form.getVideoUrlValue().isBlank()) {
            throw new IllegalStateException("Video URL field empty after entry");
        }
        return form;
    }

    public OfficialAnalysisFormSnapshot captureFormSnapshot(CreateAnalysisPage form) {
        return new OfficialAnalysisFormSnapshot(
                form.getOfficialMatchSelectedLabel(),
                form.getMatchDateValue(),
                form.getHomeTeamPlaceholder(),
                form.getAwayTeamPlaceholder());
    }

    /**
     * Performs at most one submit click. No post-submit fixture rotation or retries.
     */
    @Step("Submit official analysis exactly once")
    public AnalysisPostSubmitOutcome submitOfficialAnalysisExactlyOnce(
            CreateAnalysisPage form, AnalysisSubmissionAttempt attempt) {
        AnalysisCreateGate.requireCreationEnabled();
        if (!form.isSubmitEnabled()) {
            throw new IllegalStateException("Submit disabled before authorized single submission");
        }
        attempt.recordSubmitClick();
        form.submitOnce();
        AnalysisPostSubmitOutcome outcome = form.waitForPostSubmitOutcome();
        attempt.recordOutcome(outcome);
        return outcome;
    }
}
