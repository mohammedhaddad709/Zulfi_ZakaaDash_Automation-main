package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** MCH-ANL-010 — schedule-sourced official analysis (no Home fixture dropdown). */
public final class MatchScheduleAnalysisHappyPathFlow {

    private static final Pattern HASH_ID = Pattern.compile("#\\s*(\\d+)");
    private static final Pattern ARABIC_MATCH_ID = Pattern.compile("مباراة\\s*#\\s*(\\d+)");

    private final WebDriver driver;
    private final MatchScheduleAnalysisFlow scheduleFlow;

    public MatchScheduleAnalysisHappyPathFlow(WebDriver driver) {
        this.driver = driver;
        this.scheduleFlow = new MatchScheduleAnalysisFlow(driver);
    }

    @Step("MCH-ANL-010 pre-submit verification (no submit click)")
    public ScheduleHappyPathPreSubmitSnapshot runPreSubmitVerification() {
        MatchScheduleAnalysisGate.requirePreSubmitVerificationMode();
        if (MatchScheduleAnalysisGate.isExplicitScheduleSubmitEnabled()) {
            throw new SkipException(
                    "Pre-submit suite must not run with -D"
                            + MatchScheduleAnalysisGate.SUBMIT_ENABLED_PROPERTY
                            + "=true.");
        }

        Optional<String> target = MatchScheduleAnalysisGate.configuredTargetExternalEventId();
        SchedulePreparedCreateContext prepared =
                prepareCreateFormThroughPreSubmitChecks(target, false);
        MatchScheduleAnalysisRowContext row = prepared.scheduleRow();
        CreateAnalysisPage form = prepared.form();
        ScheduleOfficialAnalysisPopupPage createView = prepared.createView();

        ScheduleHappyPathPreSubmitSnapshot snapshot =
                new ScheduleHappyPathPreSubmitSnapshot(
                        row.rowText(),
                        row.externalEventId(),
                        row.matchIdNumeric(),
                        form.readExternalEventIdFromCurrentUrl(),
                        prepared.identityEvidence().bestKnownOfficialMatchId(),
                        prepared.homeTeamDisplay(),
                        prepared.awayTeamDisplay(),
                        prepared.matchDateValue(),
                        row.scoreDisplay(),
                        prepared.videoUrl(),
                        true,
                        form.isSubmitEnabled(),
                        AnalysisCreateGate.isCreationEnabled(),
                        MatchScheduleAnalysisGate.isExplicitScheduleSubmitEnabled(),
                        false);

        createView.closeWithoutSubmit();
        return snapshot;
    }

    @Step("MCH-ANL-010 authorized end-to-end submit and persistence")
    public ScheduleHappyPathSubmitResult runAuthorizedEndToEndSubmit(AnalysisSubmissionAttempt attempt) {
        MatchScheduleAnalysisGate.requireExplicitScheduleSubmitAuthorized();
        String exactTarget = MatchScheduleAnalysisGate.requireExactTargetExternalEventIdForSubmit();
        SchedulePreparedCreateContext prepared =
                prepareCreateFormThroughPreSubmitChecks(Optional.of(exactTarget), true);

        if (prepared.identityEvidence().isDiscrepancyBlocker()) {
            throw new IllegalStateException(
                    "Identity DISCREPANCY before submit: " + prepared.identityEvidence().diagnostic());
        }

        ScheduleCreateAnalysisFlow submitFlow = new ScheduleCreateAnalysisFlow(driver);
        AnalysisPostSubmitOutcome outcome = submitFlow.submitScheduleOfficialExactlyOnce(prepared, attempt);

        MatchScheduleAnalysisVerificationFlow verifyFlow = new MatchScheduleAnalysisVerificationFlow(driver);
        Long analysisId = submitFlow.readAnalysisIdAfterSubmit(prepared.form());
        if (analysisId == null) {
            analysisId = verifyFlow.readAnalysisIdFromBrowserUrl();
        }

        if (outcome != AnalysisPostSubmitOutcome.SUCCESS) {
            return new ScheduleHappyPathSubmitResult(
                    prepared,
                    outcome,
                    analysisId,
                    "",
                    "",
                    false,
                    false,
                    prepared.identityEvidence().linkageStatus(),
                    prepared.identityEvidence().isMappingUnverified());
        }

        verifyFlow.verifySuccessUi();
        ListingVerificationResult listingResult =
                verifyFlow.verifyListingAndPersistence(prepared, analysisId);
        String listingStatus = listingResult.listingStatus();
        if (analysisId == null) {
            analysisId = listingResult.analysisId();
        }

        if (analysisId == null) {
            throw new IllegalStateException(
                    "INCONCLUSIVE: submit succeeded but analysis ID could not be captured");
        }

        return new ScheduleHappyPathSubmitResult(
                prepared,
                outcome,
                analysisId,
                AnalysisSuccessDialog.SUCCESS_MESSAGE,
                listingStatus,
                true,
                true,
                prepared.identityEvidence().linkageStatus(),
                prepared.identityEvidence().isMappingUnverified());
    }

    /** Authorized submit through Arabic success dialog — skips التحليلات listing checks. */
    @Step("Schedule submit through success dialog (listing verify excluded)")
    public ScheduleSubmitSuccessSnapshot runAuthorizedSubmitThroughSuccessDialog(
            AnalysisSubmissionAttempt attempt) {
        MatchScheduleAnalysisGate.requireExplicitScheduleSubmitAuthorized();
        String exactTarget = MatchScheduleAnalysisGate.requireExactTargetExternalEventIdForSubmit();
        SchedulePreparedCreateContext prepared =
                prepareCreateFormThroughPreSubmitChecks(Optional.of(exactTarget), true);

        if (prepared.identityEvidence().isDiscrepancyBlocker()) {
            throw new IllegalStateException(
                    "Identity DISCREPANCY before submit: " + prepared.identityEvidence().diagnostic());
        }

        ScheduleCreateAnalysisFlow submitFlow = new ScheduleCreateAnalysisFlow(driver);
        AnalysisPostSubmitOutcome outcome = submitFlow.submitScheduleOfficialExactlyOnce(prepared, attempt);

        Long analysisId = submitFlow.readAnalysisIdAfterSubmit(prepared.form());
        if (analysisId == null) {
            analysisId =
                    new MatchScheduleAnalysisVerificationFlow(driver).readAnalysisIdFromBrowserUrl();
        }

        if (outcome != AnalysisPostSubmitOutcome.SUCCESS) {
            return new ScheduleSubmitSuccessSnapshot(prepared, outcome, null, analysisId);
        }

        AnalysisSuccessDialog dialog =
                new MatchScheduleAnalysisVerificationFlow(driver).verifySuccessUi();
        if (analysisId == null) {
            analysisId =
                    CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl());
        }
        return new ScheduleSubmitSuccessSnapshot(prepared, outcome, dialog, analysisId);
    }

    private SchedulePreparedCreateContext prepareCreateFormThroughPreSubmitChecks(
            Optional<String> targetExternalEventId, boolean requireExactRowMatch) {
        MatchSchedulePage schedule = scheduleFlow.openMatchSchedule();
        MatchScheduleAnalysisRowContext row =
                schedule
                        .findEligibleAnalysisRowContext(targetExternalEventId)
                        .filter(this::isSafePreSubmitCandidate)
                        .orElseThrow(
                                () ->
                                        new SkipException(
                                                buildNoEligibleRowMessage(targetExternalEventId)));

        if (requireExactRowMatch && targetExternalEventId.isPresent()) {
            assertRowMatchesConfiguredTarget(row, targetExternalEventId.get());
        }

        schedule.waitForStartAnalysisActionVisibleForContext(row);
        schedule.clickStartAnalysisForRowContext(row);
        ScheduleOfficialAnalysisPopupPage createView =
                new ScheduleOfficialAnalysisPopupPage(driver).waitUntilReady();
        CreateAnalysisPage form = createView.form();

        assertScheduleBoundForm(row, form, createView);

        String configuredTarget = targetExternalEventId.orElse(form.readExternalEventIdFromCurrentUrl());
        ScheduleOfficialMatchIdentityEvidence identity =
                ScheduleOfficialMatchIdentityResolver.resolve(configuredTarget, row, form);

        String authoritativeId = identity.bestKnownOfficialMatchId();
        if (authoritativeId.isBlank()) {
            authoritativeId =
                    resolveAuthoritativeOfficialMatchId(
                            form.readExternalEventIdFromCurrentUrl(),
                            readUiOfficialMatchNumericId(form),
                            row);
        }

        String videoUrl = AnalysisCreateGate.resolveVideoUrl();
        form.clearDescription();
        form.enterOptionalDescription(MatchScheduleAnalysisGate.resolveHappyPathDescription());
        form.enterVideoUrl(videoUrl);
        if (form.getVideoUrlValue() == null || form.getVideoUrlValue().isBlank()) {
            throw new IllegalStateException("Video URL not applied on create form");
        }
        if (!form.isSubmitEnabled()) {
            throw new IllegalStateException(
                    "Submit must be enabled after valid video URL; video=" + videoUrl);
        }

        assertFixtureEligibilityForScheduleRow(row, form, authoritativeId);

        return new SchedulePreparedCreateContext(
                configuredTarget,
                row,
                createView,
                form,
                videoUrl,
                form.getHomeTeamPlaceholder(),
                form.getAwayTeamPlaceholder(),
                form.getMatchDateValue(),
                identity);
    }

    private static void assertRowMatchesConfiguredTarget(
            MatchScheduleAnalysisRowContext row, String configuredTarget) {
        String target = configuredTarget.trim();
        boolean hrefMatch =
                !row.externalEventId().isBlank() && row.externalEventId().equals(target);
        boolean rowIdMatch = !row.matchIdNumeric().isBlank() && row.matchIdNumeric().equals(target);
        if (!hrefMatch && !rowIdMatch) {
            throw new SkipException(
                    "Configured target "
                            + target
                            + " does not match selected schedule row externalEventId="
                            + row.externalEventId()
                            + " matchId="
                            + row.matchIdNumeric()
                            + ". Refusing silent fixture switch.");
        }
        if (!row.analysisStatus().contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED)) {
            throw new SkipException(
                    "Schedule row no longer «"
                            + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                            + "» for target="
                            + target);
        }
    }

    private void assertScheduleBoundForm(
            MatchScheduleAnalysisRowContext row,
            CreateAnalysisPage form,
            ScheduleOfficialAnalysisPopupPage createView) {
        if (!row.played()) {
            throw new IllegalStateException("Schedule row must be played");
        }
        if (!createView.isOpen()) {
            throw new IllegalStateException("Create analysis view did not open");
        }
        if (!form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)) {
            throw new IllegalStateException("Official match type not selected");
        }
        if (!createView.isMatchTypeOfficialLocked()
                && !form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)) {
            throw new IllegalStateException("Match type not locked to official on schedule route");
        }
        String urlEventId = form.readExternalEventIdFromCurrentUrl();
        if (urlEventId.isBlank()) {
            throw new IllegalStateException("Expected /analysis/create?externalEventId= on schedule route");
        }
        if (!row.externalEventId().isBlank() && !urlEventId.equals(row.externalEventId())) {
            throw new IllegalStateException(
                    "URL externalEventId mismatch; row="
                            + row.externalEventId()
                            + "; url="
                            + urlEventId);
        }

        if (!form.hasPopulatedTeamPlaceholders()) {
            throw new IllegalStateException("Home/Away teams not populated from schedule");
        }
        String home = form.getHomeTeamPlaceholder();
        String away = form.getAwayTeamPlaceholder();
        if (!row.rowText().contains(home) || !row.rowText().contains(away)) {
            throw new IllegalStateException(
                    "Team labels mismatch schedule row; home="
                            + home
                            + "; away="
                            + away
                            + "; row="
                            + row.rowText());
        }

        String formDate = form.getMatchDateValue();
        if (!row.dateToken().isBlank()) {
            if (!scheduleDateMatchesRow(row.dateToken(), formDate)) {
                throw new IllegalStateException(
                        "Date mismatch; row=" + row.dateToken() + "; form=" + formDate);
            }
        } else if (!MatchDateDisplayRules.looksLikeCalendarDateDisplay(formDate)) {
            throw new IllegalStateException("Form date invalid: " + formDate);
        }

        if (!createView.isOfficialMatchSelectorLocked() && row.externalEventId().isBlank()) {
            throw new IllegalStateException("Official match selector must be locked for schedule flow");
        }
        if (form.isFriendlyMatchNameFieldVisible()) {
            throw new IllegalStateException("Friendly match name must not appear on schedule official flow");
        }
    }

    private void assertFixtureEligibilityForScheduleRow(
            MatchScheduleAnalysisRowContext row, CreateAnalysisPage form, String authoritativeId) {
        if (KnownFixtureExclusions.isKnownDuplicateExternalEventId(row.externalEventId())
                || KnownFixtureExclusions.isKnownDuplicateExternalEventId(
                        form.readExternalEventIdFromCurrentUrl())) {
            throw new SkipException(
                    "Selected row is a known fixture with existing analysis (externalEventId). "
                            + KnownFixtureExclusions.knownDuplicateReason());
        }
        if (KnownFixtureExclusions.isKnownDuplicateMatchId(row.matchIdNumeric())
                || KnownFixtureExclusions.isKnownDuplicateMatchId(authoritativeId)) {
            throw new SkipException(
                    "Selected row is a known fixture with existing analysis (matchId). "
                            + KnownFixtureExclusions.knownDuplicateReason());
        }
        String fixtureLabel = form.getOfficialMatchSelectedLabel();
        if (KnownFixtureExclusions.isKnownDuplicateFixture(fixtureLabel)) {
            throw new SkipException(KnownFixtureExclusions.knownDuplicateReason(fixtureLabel));
        }
    }

    private boolean isSafePreSubmitCandidate(MatchScheduleAnalysisRowContext row) {
        if (KnownFixtureExclusions.isKnownDuplicateExternalEventId(row.externalEventId())) {
            return false;
        }
        if (KnownFixtureExclusions.isKnownDuplicateMatchId(row.matchIdNumeric())) {
            return false;
        }
        return !KnownFixtureExclusions.isKnownDuplicateFixture(extractFixtureLabelFromRow(row));
    }

    private static String extractFixtureLabelFromRow(MatchScheduleAnalysisRowContext row) {
        return row.rowText() == null ? "" : row.rowText().replaceAll("\\s+", " ").trim();
    }

    private static String buildNoEligibleRowMessage(Optional<String> target) {
        String targetHint =
                target.map(t -> " target=" + t).orElse(" (no target; scans first safe eligible row)");
        return "No played Yellow League row «غير محللة» with schedule analysis action"
                + targetHint
                + ". Configure -D"
                + MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY
                + "= or "
                + MatchScheduleAnalysisGate.TARGET_MATCH_ID_ENV
                + " when multiple rows exist.";
    }

    /**
     * BA note: US expects visible Official Match #ID; live route binds {@code externalEventId} in URL first.
     * When UI shows #ID, compare to URL; when only URL is present, authoritative DB linkage is not proven here.
     */
    public static String resolveAuthoritativeOfficialMatchId(
            String urlExternalEventId, String uiNumericId, MatchScheduleAnalysisRowContext row) {
        if (!uiNumericId.isBlank() && !urlExternalEventId.isBlank() && !uiNumericId.equals(urlExternalEventId)) {
            return "DISCREPANCY:url="
                    + urlExternalEventId
                    + ";ui="
                    + uiNumericId
                    + ";rowMatchId="
                    + row.matchIdNumeric();
        }
        if (!row.matchIdNumeric().isBlank()
                && !urlExternalEventId.isBlank()
                && !row.matchIdNumeric().equals(urlExternalEventId)
                && uiNumericId.isBlank()) {
            return "MAPPING_UNVERIFIED:scheduleMatchId="
                    + row.matchIdNumeric()
                    + ";externalEventId="
                    + urlExternalEventId;
        }
        if (!uiNumericId.isBlank()) {
            return uiNumericId;
        }
        if (!row.matchIdNumeric().isBlank()) {
            return row.matchIdNumeric();
        }
        return urlExternalEventId.isBlank() ? "UNKNOWN" : "URL_ONLY:" + urlExternalEventId;
    }

    public static String readUiOfficialMatchNumericId(CreateAnalysisPage form) {
        String label = form.getOfficialMatchSelectedLabel();
        Matcher m = HASH_ID.matcher(label);
        if (m.find()) {
            return m.group(1);
        }
        m = ARABIC_MATCH_ID.matcher(label);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static boolean scheduleDateMatchesRow(String rowDateToken, String formDate) {
        if (formDate == null || formDate.isBlank()) {
            return false;
        }
        String normalizedForm = formDate.trim();
        String normalizedRow = rowDateToken.trim();
        if (normalizedForm.contains(normalizedRow) || normalizedRow.contains(normalizedForm)) {
            return true;
        }
        return MatchDateDisplayRules.looksLikeCalendarDateDisplay(normalizedForm)
                && MatchDateDisplayRules.looksLikeCalendarDateDisplay(normalizedRow);
    }
}
