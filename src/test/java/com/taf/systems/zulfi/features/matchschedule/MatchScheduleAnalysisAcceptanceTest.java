package com.taf.systems.zulfi.features.matchschedule;

import com.taf.core.media.ScreenshotsManager;
import com.taf.core.media.VideoEvidence;
import com.taf.systems.zulfi.config.AnalysisCreateGate;
import com.taf.systems.zulfi.config.CredentialProvider;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate.Phase;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisTargetRegistry;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.flows.analysis.AnalysisSubmissionAttempt;
import com.taf.systems.zulfi.flows.matchschedule.MatchScheduleAnalysisFlow;
import com.taf.systems.zulfi.flows.matchschedule.MatchScheduleAnalysisHappyPathFlow;
import com.taf.systems.zulfi.flows.matchschedule.MatchScheduleTableFlow;
import com.taf.systems.zulfi.flows.matchschedule.PostSubmitReadOnlyVerificationFlow;
import com.taf.systems.zulfi.flows.matchschedule.PostSubmitReadOnlyVerificationResult;
import com.taf.systems.zulfi.flows.matchschedule.ScheduleSubmitSuccessSnapshot;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.matchschedule.MchAnalysisTestIds;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisDetailsPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage.RowActionAvailability;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import com.taf.systems.zulfi.reporting.DefectClassification;
import com.taf.systems.zulfi.reporting.TestCaseReport;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepAction;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import com.taf.systems.zulfi.validations.MatchDateDisplayRules;
import com.taf.systems.zulfi.validations.MatchScheduleRowRules;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * US-FAP-MCH-ANALYSIS (work item 33009) — start an official match analysis from the Match Schedule.
 *
 * <p>Single reference for the whole story. Phases ({@code -Dmatch.schedule.analysis.phase=}):
 * <ul>
 *   <li><b>a</b> — schedule eligibility and the create view opened from a match row (read-only);</li>
 *   <li><b>b</b> — create-form inputs and video URL rules (read-only, never submits);</li>
 *   <li><b>c</b> — the one authorized submit: link to the official match, identity, status sync, and (with
 *       {@code -Dmatch.schedule.analysis.duplicateCheck.enabled=true}) the stale-form duplicate probe.</li>
 * </ul>
 * Submit needs {@code -Danalysis.create.enabled=true -Dmatch.schedule.analysis.submit.enabled=true}. The
 * single-submit rule (AGENTS.md) holds for the creating click; the duplicate probe click must be rejected.
 */
@Epic("Zulfi Portal")
@Feature(MchAnalysisTestIds.FEATURE)
@VideoEvidence(VideoEvidence.Mode.ON_FAILURE)
public class MatchScheduleAnalysisAcceptanceTest extends AuthenticatedUiTest {

    private static final String STORY = "US-FAP-MCH-ANALYSIS (33009)";
    private static final String P1 = "P1";
    private static final String P2 = "P2";
    private static final String CREATE_ROUTE = "analysis/create?externalEventId=";
    private static final Pattern HASH_ID = Pattern.compile("#\\s*\\d+");
    private static final String YOUTUBE_WATCH = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String YOUTUBE_SHORT = "https://youtu.be/dQw4w9WgXcQ";
    private static final String GOOGLE_DRIVE = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view?usp=sharing";
    private static final String UNSUPPORTED_HOST = "https://vimeo.com/76979871";
    private static final String MALFORMED_URL = "://invalid-video-url";

    private MatchScheduleAnalysisFlow flow;
    private MatchScheduleTableFlow tableFlow;
    private String currentTestId = "";
    /** Checks that failed in the current test; the test keeps going so one run reports every finding. */
    private final List<String> failedChecks = new ArrayList<>();

    @Override
    @BeforeMethod(alwaysRun = true)
    public void loginBeforeEachTestCase() {
        if (!CredentialProvider.isConfigured()) {
            throw new SkipException("ZULFI_USER and ZULFI_PASSWORD are required (env or config/credentials.local.properties).");
        }
        super.loginBeforeEachTestCase();
        flow = new MatchScheduleAnalysisFlow(browser());
        tableFlow = new MatchScheduleTableFlow(browser());
    }

    @AfterMethod(alwaysRun = true)
    public void clearPinnedTarget() {
        MatchScheduleAnalysisTargetRegistry.clear();
    }

    // ------------------------------------------------------------------------------------------------ phase a

    @Test(description = MchAnalysisTestIds.MCH_ANL_001 + " — Start-analysis action is shown for an eligible match")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_001,
            title = "«تحليل المباراة» is available for a played, not-yet-analysed match",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Display Analysis Action for Eligible Match"},
            businessRules = {"BR-MCH-ANL-001"},
            priority = P1,
            description = "In the official Yellow League Match Schedule, a match that has already been played and whose "
                    + "Analysis Status is «غير محللة» must offer the action «تحليل المباراة», and that action must lead to "
                    + "the create-analysis route bound to that exact match.",
            testObjective = "Prove the entry point exists for eligible matches and is bound to the correct match.",
            preconditions = {"User is authenticated", "The schedule contains at least one played match with status «غير محللة»"},
            testDataReference = "First eligible row of /matches (or -Dmatch.schedule.analysis.targetExternalEventId)",
            expectedFinalResult = "The eligible row shows «تحليل المباراة»; its link targets /analysis/create?externalEventId=<that match>.")
    public void mchAnl001_displayAnalysisActionForEligibleMatch() {
        begin(MchAnalysisTestIds.MCH_ANL_001, Phase.A);
        MatchScheduleAnalysisRowContext row = locateEligibleRow();
        step("Reveal the start-analysis action",
                "Hover the eligible match row",
                "The action «" + MatchScheduleAnalysisStatusMessages.ACTION_START_ANALYSIS_FRAGMENT + "» becomes available on the row.",
                () -> tableFlow.isStartAnalysisVisibleAfterHover(row)
                        ? StepResult.pass("Action is available on the row of match " + row.externalEventId())
                        : StepResult.fail("The row is played and «" + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                                + "» but no start-analysis action appears after hover. Row: " + oneLine(row.rowText())));
        step("Verify the action is bound to this match",
                "Read the link target of the action",
                "Link points to " + CREATE_ROUTE + row.externalEventId(),
                () -> {
                    Optional<String> href = new MatchSchedulePage(browser()).readStartAnalysisHrefForContext(row);
                    if (href.isEmpty()) {
                        return StepResult.fail("The action has no link target to verify.", DefectClassification.INCONCLUSIVE);
                    }
                    boolean bound = row.externalEventId().isBlank() || href.get().contains("externalEventId=" + row.externalEventId());
                    return bound
                            ? StepResult.pass("href=" + href.get())
                            : StepResult.fail("Action links to " + href.get() + " but the row is match " + row.externalEventId());
                });
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_002 + " — Action opens the official analysis create view")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_002,
            title = "«تحليل المباراة» opens the create view with Match Type «رسمية»",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Open Official Analysis Pop-up"},
            businessRules = {"BR-MCH-ANL-002"},
            priority = P1,
            description = "Selecting «تحليل المباراة» opens the Official Match Analysis create view for the selected match. "
                    + "Match Type is set to «رسمية» by the system, not by the user. (The portal opens this as the page "
                    + "/analysis/create?externalEventId=… rather than an overlay.)",
            testObjective = "Confirm the flow opens for the clicked match with Match Type preset by the system.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches",
            expectedFinalResult = "Create view for the clicked match is displayed; Match Type is preset to «رسمية».")
    public void mchAnl002_openOfficialAnalysisCreateView() {
        begin(MchAnalysisTestIds.MCH_ANL_002, Phase.A);
        OpenedForm opened = openFormFromEligibleRow();
        step("Verify the create view belongs to the clicked match",
                "Compare the opened route with the clicked row",
                "Route is " + CREATE_ROUTE + opened.row().externalEventId() + " and the create view is displayed.",
                () -> {
                    String urlId = opened.form().readExternalEventIdFromCurrentUrl();
                    boolean sameMatch = opened.row().externalEventId().isBlank() || opened.row().externalEventId().equals(urlId);
                    return opened.popup().isOpen() && sameMatch
                            ? StepResult.pass("url=" + browser().getCurrentUrl())
                            : StepResult.fail("Opened url=" + browser().getCurrentUrl() + ", clicked match=" + opened.row().externalEventId());
                });
        step("Verify Match Type is preset to «" + CreateAnalysisPage.MATCH_TYPE_OFFICIAL + "»",
                "Read the Match Type control when the view opens",
                "Match Type = «رسمية» without the user choosing it (that it cannot be changed is MCH-ANL-019).",
                () -> {
                    String type = opened.form().getMatchTypeValue();
                    return opened.form().isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)
                            ? StepResult.pass("Match Type=«" + type + "»")
                            : StepResult.fail("Match Type shows «" + type + "» instead of «رسمية».");
                });
        opened.popup().closeWithoutSubmit();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_003 + " — Selected match is preselected and locked")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_003,
            title = "The clicked match is preselected and cannot be replaced",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Preselect Selected Match"},
            businessRules = {"BR-MCH-ANL-003"},
            priority = P1,
            description = "The Official Match chosen in the schedule is already populated and locked for the entire flow: "
                    + "the selector is disabled and nothing the user does in the form changes the selected match.",
            testObjective = "Prevent the user from analysing a different match than the one they clicked.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches",
            expectedFinalResult = "Official Match is selected and locked; its value is identical before and after editing the form.")
    public void mchAnl003_preselectedMatchIsLocked() {
        begin(MchAnalysisTestIds.MCH_ANL_003, Phase.A);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        String[] before = new String[1];
        check("Verify the official match is preselected and locked",
                "Read the Official Match selector",
                "A match is selected and the selector is locked.",
                () -> {
                    before[0] = form.getOfficialMatchSelectedLabel();
                    if (!form.isOfficialMatchSelected()) {
                        return StepResult.fail("No official match is selected; label=«" + before[0] + "».");
                    }
                    return opened.popup().isOfficialMatchSelectorLocked()
                            ? StepResult.pass("Selected=«" + before[0] + "»; selector locked")
                            : StepResult.fail("Selected=«" + before[0] + "» but the selector is not locked: the user could pick another match.");
                });
        check("Edit the form and verify the match does not change",
                "Type a description and a video URL, then read the selected match again",
                "The selected match and the route's externalEventId are unchanged.",
                () -> {
                    form.enterOptionalDescription("MCH-ANL-003 lock check").enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    String after = form.getOfficialMatchSelectedLabel();
                    String urlId = form.readExternalEventIdFromCurrentUrl();
                    boolean unchanged = before[0].equals(after)
                            && (opened.row().externalEventId().isBlank() || opened.row().externalEventId().equals(urlId));
                    return unchanged
                            ? StepResult.pass("Selected stayed «" + after + "»; externalEventId=" + urlId)
                            : StepResult.fail("Selected changed from «" + before[0] + "» to «" + after + "» (externalEventId=" + urlId + ").");
                });
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_004 + " — Official match information is shown read-only")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_004,
            title = "Clubs, result, date and #ID are displayed read-only and match the schedule row",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Display Official Match Information", "AC — Preserve Official Match Metadata Source"},
            businessRules = {"BR-MCH-ANL-004", "BR-MCH-ANL-009"},
            priority = P1,
            description = "The create view shows the Match #ID, both clubs, the match result and the match date as read-only "
                    + "information that matches the clicked schedule row. Club names stay the primary human-readable "
                    + "representation of the match.",
            testObjective = "Verify the user sees the right match, cannot edit its data, and that the data comes from the schedule.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches (clubs, score and date are read from that row)",
            expectedFinalResult = "Clubs, date and result equal the schedule row and are not editable; the #ID is visible.")
    public void mchAnl004_displayOfficialMatchInformationReadOnly() {
        begin(MchAnalysisTestIds.MCH_ANL_004, Phase.A);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        MatchScheduleAnalysisRowContext row = opened.row();
        String home = form.getHomeTeamPlaceholder();
        String away = form.getAwayTeamPlaceholder();
        check("Verify clubs match the schedule row and are read-only",
                "Compare the Home/Away club fields with the schedule row and try to edit them",
                "Both clubs equal the row's clubs and neither field is editable.",
                () -> {
                    boolean same = row.rowText().contains(home) && row.rowText().contains(away) && !home.isBlank() && !away.isBlank();
                    boolean readOnly = form.verifyTeamFieldsNonEditable().bothNonEditable();
                    if (!same) return StepResult.fail("Form clubs «" + home + "» × «" + away + "» are not the row's clubs. Row: " + oneLine(row.rowText()));
                    return readOnly ? StepResult.pass("«" + home + "» × «" + away + "»; both read-only")
                            : StepResult.fail("Clubs «" + home + "» × «" + away + "» can be edited by the user.");
                });
        check("Verify the match date matches the schedule row and is read-only",
                "Read the match date field",
                "Date equals the row's date and the field is not editable.",
                () -> {
                    String formDate = form.getMatchDateValue();
                    boolean sameDate = row.dateToken().isBlank()
                            ? MatchDateDisplayRules.looksLikeCalendarDateDisplay(formDate)
                            : scheduleDateMatchesRow(row.dateToken(), formDate);
                    if (!sameDate) return StepResult.fail("Form date «" + formDate + "» differs from the row's date «" + row.dateToken() + "».");
                    return form.isMatchDateReadOnlyOrDisabled() ? StepResult.pass("date=" + formDate + "; read-only")
                            : StepResult.fail("Match date «" + formDate + "» is editable.");
                });
        check("Verify the match result is displayed",
                "Look for the row's score on the create view",
                "The result " + row.scoreDisplay() + " appears as read-only match information.",
                () -> {
                    String text = form.readVisibleFormText();
                    boolean shown = !row.scoreDisplay().isBlank() && (text.contains(row.scoreDisplay())
                            || text.contains(row.homeScore() + " - " + row.awayScore())
                            || text.contains(row.awayScore() + " - " + row.homeScore())
                            || text.contains(row.homeScore() + "-" + row.awayScore()));
                    return shown ? StepResult.pass("Result " + row.scoreDisplay() + " is displayed")
                            : StepResult.fail("The create view does not show the match result (" + row.scoreDisplay()
                                    + "). The story lists Match Result as displayed information; confirm with BA whether the "
                                    + "schedule-bound view must show it.", DefectClassification.INCONCLUSIVE);
                });
        check("Verify the Match #ID is displayed",
                "Look for a #ID in the selected official match information",
                "A #ID (e.g. #" + row.matchIdNumeric() + ") is visible as supporting metadata.",
                () -> {
                    String text = form.getOfficialMatchSelectedLabel() + " " + form.readVisibleFormText();
                    return HASH_ID.matcher(text).find()
                            ? StepResult.pass("A #ID is visible")
                            : StepResult.fail("No #ID is visible on the create view (the match is bound only by externalEventId="
                                    + form.readExternalEventIdFromCurrentUrl() + " in the URL). Known open discrepancy: "
                                    + "docs/matchschedule/discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md "
                                    + "— pending BA/Product confirmation.", DefectClassification.INCONCLUSIVE);
                });
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_005 + " — User is not asked to re-enter match metadata")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_005,
            title = "Match Name, Match Date, Match Type and match selection are not requested",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Do Not Request Duplicate Match Metadata"},
            businessRules = {"BR-MCH-ANL-004"},
            priority = P1,
            description = "Because the match comes from the schedule, the user must not have to type a match name or date, "
                    + "choose a match type, or pick the match again. Entering only a Video URL must be enough to enable "
                    + "«بدء التحليل».",
            testObjective = "Show the form asks only for the analysis inputs (Description, Video URL).",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches; Video URL " + YOUTUBE_WATCH,
            expectedFinalResult = "No manual match name/date field; type and match locked; submit enabled with a Video URL only.")
    public void mchAnl005_noDuplicateMatchMetadataRequested() {
        begin(MchAnalysisTestIds.MCH_ANL_005, Phase.A);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        check("Verify no manual match-name or match-date entry is requested",
                "Inspect the form for a Match Name input and for an editable Match Date",
                "No Match Name input exists; the Match Date is shown read-only (already filled from the schedule).",
                () -> {
                    boolean name = form.isFriendlyMatchNameFieldVisible();
                    boolean dateEditable = form.isFriendlyMatchDateFieldVisible() && !form.isMatchDateReadOnlyOrDisabled();
                    return !name && !dateEditable ? StepResult.pass("No Match Name input; date read-only=" + form.isMatchDateReadOnlyOrDisabled())
                            : StepResult.fail("Manual inputs requested: matchNameInput=" + name + ", editableMatchDate=" + dateEditable);
                });
        check("Verify only a Video URL is needed to enable submit",
                "Enter a valid Video URL, leave Description empty, read the submit button",
                "«بدء التحليل» is enabled without entering any match information (name, date, type or match).",
                () -> {
                    form.clearDescription().enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    boolean enabled = form.isSubmitEnabled();
                    return enabled ? StepResult.pass("submitEnabled=true with a Video URL only")
                            : StepResult.fail("Submit stays disabled with a valid Video URL only; validation=«"
                                    + form.readVisibleFormValidationMessages() + "». Something other than Video URL is still required.");
                });
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_006 + " — Closing without submitting creates nothing")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_006,
            title = "Closing the create view discards input and leaves the match «غير محللة»",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Close Pop-up Without Submission"},
            businessRules = {"BR-MCH-ANL-001"},
            priority = P1,
            description = "If the user leaves the create view before submitting: no Analysis Record is created, the match stays "
                    + "«غير محللة» and still offers «تحليل المباراة», and the values typed are discarded.",
            testObjective = "Confirm that abandoning the flow has no side effects and loses the entered values.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches; description 'MCH-ANL-006 discard check'",
            expectedFinalResult = "Back on the schedule; the match is «غير محللة» with the action; reopening shows empty inputs.")
    public void mchAnl006_closeWithoutSubmitDiscardsInput() {
        begin(MchAnalysisTestIds.MCH_ANL_006, Phase.A);
        OpenedForm opened = openFormFromEligibleRow();
        MatchScheduleAnalysisRowContext row = opened.row();
        step("Fill the form and close it without submitting",
                "Type a description and a video URL, then leave the create view",
                "The user returns to the schedule; nothing is submitted.",
                () -> {
                    opened.form().enterOptionalDescription("MCH-ANL-006 discard check").enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    opened.popup().closeWithoutSubmit();
                    String url = browser().getCurrentUrl();
                    return url.contains("matches") ? StepResult.pass("Back on " + url)
                            : StepResult.fail("Did not return to the schedule; url=" + url);
                });
        step("Verify the match is unchanged and still offers «تحليل المباراة»",
                "Locate the same match on the schedule",
                "Status is «غير محللة» and the start-analysis action is available again.",
                () -> {
                    new MatchSchedulePage(browser()).waitUntilLoaded();
                    Optional<MatchScheduleAnalysisRowContext> again = tableFlow.findEligibleNotAnalyzedRow(
                            row.externalEventId().isBlank() ? Optional.empty() : Optional.of(row.externalEventId()));
                    if (again.isEmpty()) {
                        return StepResult.fail("The match is no longer eligible after closing without submit. Row was: " + oneLine(row.rowText()));
                    }
                    boolean notAnalysed = again.get().analysisStatus().contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED);
                    return notAnalysed && tableFlow.isStartAnalysisVisibleAfterHover(again.get())
                            ? StepResult.pass("status=«" + again.get().analysisStatus() + "»; action available")
                            : StepResult.fail("Status=«" + again.get().analysisStatus() + "» after closing without submit.");
                });
        step("Verify the entered values were discarded",
                "Reopen the create view for the same match",
                "Description and Video URL are empty.",
                () -> {
                    OpenedForm reopened = openFormForRow(row);
                    String description = reopened.form().getDescriptionValue();
                    String video = reopened.form().getVideoUrlValue();
                    reopened.popup().closeWithoutSubmit();
                    return description.isBlank() && video.isBlank() ? StepResult.pass("Inputs are empty after reopening")
                            : StepResult.fail("Previous input survived: description=«" + description + "», video=«" + video + "».");
                });
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_013 + " — Ineligible matches do not offer the action (already analysed)")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_013,
            title = "Matches «قيد التحليل» or «اكتمل التحليل» offer no start-analysis action",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Prevent Duplicate Analysis"},
            businessRules = {"BR-MCH-ANL-001", "BR-MCH-ANL-006", "BR-MCH-ANL-007"},
            priority = P1,
            description = "A match that already has an analysis in progress or completed must not allow another analysis to be "
                    + "started from the schedule (first-release rule: one analysis per official match).",
            testObjective = "Close the UI door to duplicates for analysed matches.",
            preconditions = {"User is authenticated", "The schedule contains at least one analysed match"},
            testDataReference = "Rows of /matches whose status is «قيد التحليل» or «اكتمل التحليل» (whole schedule, up to 60 checked)",
            expectedFinalResult = "No analysed match shows «بدء تحليل المباراة».")
    public void mchAnl013_analysedMatchesOfferNoStartAction() {
        begin(MchAnalysisTestIds.MCH_ANL_013, Phase.A);
        List<RowActionAvailability> analysed = scanSchedule("analysed matches",
                text -> MatchScheduleAnalysisStatusMessages.isPostSubmitAnalysisStatus(MatchScheduleRowRules.findKnownAnalysisStatus(text)));
        if (analysed.isEmpty()) {
            throw new SkipException("Precondition not met: no match with status «قيد التحليل» / «اكتمل التحليل» exists on the whole "
                    + "schedule (test data: analyse one match first).");
        }
        TestStepReporter.recordTestData("Analysed rows scanned", String.valueOf(analysed.size()));
        step("Verify analysed matches do not offer the action",
                "Hover each analysed match and look for «تحليل المباراة»",
                "None of the " + analysed.size() + " analysed matches shows the action.",
                () -> {
                    List<String> offenders = analysed.stream().filter(RowActionAvailability::startAnalysisAvailable)
                            .map(r -> "«" + r.analysisStatus() + "» " + r.shortLabel()).toList();
                    return offenders.isEmpty() ? StepResult.pass(analysed.size() + " analysed matches, none offers the action")
                            : StepResult.fail("These analysed matches still offer «تحليل المباراة»: " + String.join(" ; ", offenders));
                });
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_017 + " — Unplayed matches do not offer the action")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_017,
            title = "Matches that have not been played offer no start-analysis action",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Display Analysis Action for Eligible Match"},
            businessRules = {"BR-MCH-ANL-001"},
            priority = P1,
            description = "Only an official match that has already been played may start an analysis. Upcoming matches (kick-off "
                    + "time shown, no result) must not offer the action, whatever their status.",
            testObjective = "Negative counterpart of MCH-ANL-001 for the 'played' part of BR-MCH-ANL-001.",
            preconditions = {"User is authenticated", "The schedule contains at least one upcoming (unplayed) match"},
            testDataReference = "Rows of /matches without a recorded result (whole schedule, up to 60 checked)",
            expectedFinalResult = "No unplayed match shows «بدء تحليل المباراة».")
    public void mchAnl017_unplayedMatchesOfferNoStartAction() {
        begin(MchAnalysisTestIds.MCH_ANL_017, Phase.A);
        List<RowActionAvailability> unplayed = scanSchedule("unplayed matches", MatchScheduleRowRules::isUnplayedRow);
        if (unplayed.isEmpty()) {
            throw new SkipException("Precondition not met: no unplayed (upcoming) match exists on the whole schedule.");
        }
        TestStepReporter.recordTestData("Unplayed rows scanned", String.valueOf(unplayed.size()));
        step("Verify unplayed matches do not offer the action",
                "Hover each unplayed match and look for «تحليل المباراة»",
                "None of the " + unplayed.size() + " unplayed matches shows the action.",
                () -> {
                    List<String> offenders = unplayed.stream().filter(RowActionAvailability::startAnalysisAvailable)
                            .map(RowActionAvailability::shortLabel).toList();
                    return offenders.isEmpty() ? StepResult.pass(unplayed.size() + " unplayed matches, none offers the action")
                            : StepResult.fail("Unplayed matches offering «تحليل المباراة»: " + String.join(" ; ", offenders));
                });
    }

    // ------------------------------------------------------------------------------------------------ phase b

    @Test(description = MchAnalysisTestIds.MCH_ANL_007 + " — Description optional, Video URL required")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_007,
            title = "Description is optional and Video URL is mandatory",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Complete Existing Analysis Inputs"},
            businessRules = {"BR-MCH-ANL-004"},
            priority = P2,
            description = "The existing analysis inputs remain available: Description is optional (no required marker, empty "
                    + "value accepted) and Video URL is mandatory (submit disabled / required marker while empty).",
            testObjective = "Verify the input contract of the create form.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches; Video URL " + YOUTUBE_WATCH,
            expectedFinalResult = "Description has no required indicator and accepts an empty value; Video URL is required.")
    public void mchAnl007_descriptionOptionalVideoRequired() {
        begin(MchAnalysisTestIds.MCH_ANL_007, Phase.B);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        check("Verify both inputs are available",
                "Look for the Description and Video URL fields",
                "Both fields are displayed.",
                () -> form.isDescriptionFieldVisible() && form.isVideoUrlFieldVisible()
                        ? StepResult.pass("Description and Video URL are displayed")
                        : StepResult.fail("description=" + form.isDescriptionFieldVisible() + ", videoUrl=" + form.isVideoUrlFieldVisible()));
        check("Verify Description is not marked as required",
                "Read the Description label and control attributes",
                "The label shows no required marker (*) and the control is not required.",
                () -> {
                    var evidence = form.observeDescriptionFieldRequirement();
                    return evidence.labelShowsRequiredMarker()
                            ? StepResult.fail("The Description label shows a required marker although the story says it is optional. "
                                    + evidence.toDiagnosticBlock() + " Known open defect: docs/matchschedule/defects/"
                                    + "MCH-ANL-007-DESC-REQ-16651987_description_required_indicator.md")
                            : StepResult.pass("No required marker; " + evidence.toDiagnosticBlock());
                });
        check("Verify an empty Description is accepted",
                "Leave Description empty and enter a valid Video URL",
                "No Description validation message; submit is enabled.",
                () -> {
                    form.clearDescription().enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    String messages = form.readVisibleFormValidationMessages();
                    boolean rejected = form.hasVisibleClientValidationNearDescription();
                    boolean enabled = form.isSubmitEnabled();
                    return !rejected && enabled ? StepResult.pass("No description validation; submitEnabled=true")
                            : StepResult.fail("Empty Description was rejected or blocked submit. validation=«" + messages + "», submitEnabled=" + enabled);
                });
        check("Verify Video URL is mandatory",
                "Clear the Video URL and read the field and submit button",
                "Video URL is marked required or submit is disabled while it is empty.",
                () -> {
                    form.clearVideoUrl();
                    var evidence = form.observeVideoUrlFieldRequirement();
                    boolean submitDisabled = !form.isSubmitEnabledQuick();
                    return evidence.labelShowsRequiredMarker() || submitDisabled
                            ? StepResult.pass("requiredMarker=" + evidence.labelShowsRequiredMarker() + "; submitDisabled=" + submitDisabled)
                            : StepResult.fail("Video URL is empty but is neither marked required nor blocks submit. " + evidence.toDiagnosticBlock());
                });
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_008 + " — Club names are the primary match representation")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_008,
            title = "Club names stay primary; the #ID is only supporting metadata",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Display Official Match Information"},
            businessRules = {"BR-MCH-ANL-009"},
            priority = P2,
            description = "The selected match must be identified to the user by its club names. A #ID, when shown, supports "
                    + "identification but never replaces or precedes the clubs.",
            testObjective = "Protect the human-readable representation of the match.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches",
            expectedFinalResult = "Both club names appear in the selected-match information, before any #ID.")
    public void mchAnl008_clubNamesArePrimaryRepresentation() {
        begin(MchAnalysisTestIds.MCH_ANL_008, Phase.B);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        step("Verify the clubs identify the selected match",
                "Read the selected official match information",
                "Home and Away club names are present and no #ID appears before them.",
                () -> {
                    String home = form.getHomeTeamPlaceholder();
                    String away = form.getAwayTeamPlaceholder();
                    String label = form.getOfficialMatchSelectedLabel();
                    String text = label + " | " + form.readVisibleFormText();
                    if (home.isBlank() || away.isBlank()) {
                        return StepResult.fail("Club names are not displayed (home=«" + home + "», away=«" + away + "»).");
                    }
                    var hash = HASH_ID.matcher(label);
                    if (hash.find() && label.indexOf(home) > hash.start() && label.indexOf(away) > hash.start()) {
                        return StepResult.fail("The #ID precedes the club names in «" + label + "»; clubs must stay primary.");
                    }
                    return StepResult.pass("home=«" + home + "», away=«" + away + "», label=«" + label + "», idShown=" + HASH_ID.matcher(text).find());
                });
        opened.popup().closeWithoutSubmit();
    }

    @Test(description = MchAnalysisTestIds.MCH_ANL_009 + " — Video URL rules (YouTube and Google Drive)")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_009,
            title = "YouTube and Google Drive URLs are accepted; malformed and unsupported URLs are not",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Complete Existing Analysis Inputs"},
            businessRules = {"BR-MCH-ANL-004"},
            priority = P2,
            description = "The approved video validation keeps working on the schedule-bound form: supported sources (YouTube, "
                    + "Google Drive) are accepted client-side. Malformed or unsupported URLs are blocked client-side when the form does so; "
                    + "otherwise the step is reported BLOCKED because the story validates video accessibility in the backend at submit "
                    + "(proving it would create data).",
            testObjective = "Confirm client-side video validation on this entry point.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = YOUTUBE_WATCH + " | " + YOUTUBE_SHORT + " | " + GOOGLE_DRIVE + " | " + UNSUPPORTED_HOST + " | " + MALFORMED_URL,
            expectedFinalResult = "Valid YouTube/Drive URLs enable submit without an error. Invalid ones are blocked client-side or reported "
                    + "as BLOCKED (their rejection is validated by the backend at submit).")
    public void mchAnl009_videoUrlRules() {
        begin(MchAnalysisTestIds.MCH_ANL_009, Phase.B);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        for (String valid : List.of(YOUTUBE_WATCH, YOUTUBE_SHORT, GOOGLE_DRIVE)) {
            check("Accept supported source: " + valid,
                    "Enter the URL and read validation and submit state",
                    "No validation error and submit is enabled.",
                    () -> {
                        form.clearVideoUrl().enterVideoUrl(valid);
                        boolean error = form.hasVisibleClientValidationNearVideoUrl();
                        boolean enabled = form.isSubmitEnabled();
                        return !error && enabled ? StepResult.pass("accepted (submit enabled)")
                                : StepResult.fail("Supported URL rejected: validation=«" + form.readVisibleFormValidationMessages()
                                        + "», submitEnabled=" + enabled);
                    });
        }
        for (String invalid : List.of(MALFORMED_URL, UNSUPPORTED_HOST)) {
            form.clearVideoUrl().enterVideoUrl(invalid);
            boolean error = form.hasVisibleClientValidationNearVideoUrl();
            boolean blocked = !form.isSubmitEnabledQuick();
            ScreenshotsManager.takeFullPageScreenshot(browser(), currentTestId + "_invalid_source");
            // The story validates video accessibility in the backend at submit; a client that accepts the text is not a
            // story violation, and proving the rejection would require creating data. Reported as BLOCKED, never as a pass.
            TestStepReporter.recordStep("Invalid source: " + invalid,
                    "Enter the URL and read validation and submit state",
                    "A validation error is shown or submit stays disabled (client-side).",
                    error || blocked
                            ? "Blocked client-side (validationShown=" + error + ", submitDisabled=" + blocked + ")"
                            : "Accepted client-side (submit enabled, no error). Rejection can only be verified at submit, which would "
                                    + "create data: NOT VERIFIED here.",
                    error || blocked ? "PASS" : "BLOCKED");
        }
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    // ------------------------------------------------------------------------------------------------ phase b: Match Type

    @Test(description = MchAnalysisTestIds.MCH_ANL_019 + " — Match Type cannot be changed from the schedule entry")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_019,
            title = "Match Type is system-set to «رسمية» and cannot be switched to «ودية»",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"Official Match Data Behavior — Match Type is system-set and not editable"},
            businessRules = {"BR-MCH-ANL-002", "BR-MCH-ANL-003"},
            priority = P1,
            description = "The story's \"Official Match Data Behavior\" table says Match Type comes from the System and is not "
                    + "editable (BR-MCH-ANL-002), and BR-MCH-ANL-003 locks the selected official match for the entire flow. "
                    + "Starting an analysis from a schedule match is therefore only ever an official analysis.",
            testObjective = "Apply the story's rule that Match Type is system-set and not editable on this entry point.",
            preconditions = {"User is authenticated", "An eligible match exists"},
            testDataReference = "First eligible row of /matches",
            expectedFinalResult = "The Match Type control is disabled and still shows «رسمية» after the user tries to change it.")
    public void mchAnl019_matchTypeCannotBeSwitchedToFriendly() {
        begin(MchAnalysisTestIds.MCH_ANL_019, Phase.B);
        OpenedForm opened = openFormFromEligibleRow();
        CreateAnalysisPage form = opened.form();
        check("Verify the Match Type control is locked",
                "Read the Match Type control state",
                "The control is disabled.",
                () -> form.isMatchTypeSelectorDisabled()
                        ? StepResult.pass("Match Type control is disabled")
                        : StepResult.fail("The Match Type control is ENABLED. The story says Match Type is set by the system and not "
                                + "editable, but the user can open the list and choose «ودية»."));
        check("Try to switch Match Type to «" + CreateAnalysisPage.MATCH_TYPE_FRIENDLY + "»",
                "Open the Match Type list and pick «ودية»",
                "Match Type still shows «رسمية» (the switch is impossible).",
                () -> {
                    if (form.isMatchTypeSelectorDisabled()) {
                        return StepResult.pass("Switch impossible: control disabled");
                    }
                    form.selectMatchTypeFriendlyByUser();
                    return form.isMatchType(CreateAnalysisPage.MATCH_TYPE_FRIENDLY)
                            ? StepResult.fail("The user changed Match Type to «ودية» from the schedule entry. The story says Match Type is "
                                    + "set by the System (Editable: No) and the official match stays locked for the whole flow.")
                            : StepResult.pass("Match Type stayed «" + form.getMatchTypeValue() + "»");
                });
        opened.popup().closeWithoutSubmit();
        assertChecks();
    }

    // ------------------------------------------------------------------------------------------------ phase c

    @Test(description = MchAnalysisTestIds.MCH_ANL_010 + " — Submit creates one linked official analysis (+ identity, sync, duplicate probe)")
    @TestCaseReport(
            id = MchAnalysisTestIds.MCH_ANL_010,
            title = "Submitting from the schedule creates exactly one official analysis linked to the clicked match",
            feature = MchAnalysisTestIds.FEATURE,
            userStoryId = STORY,
            acceptanceCriteria = {"AC — Link Analysis to Official Match", "AC — Preserve Official Match Metadata Source",
                    "AC — Match Identity Integrity", "AC — Prevent Duplicate Analysis", "AC — Concurrent Eligibility Change"},
            businessRules = {"BR-MCH-ANL-005", "BR-MCH-ANL-006", "BR-MCH-ANL-007", "BR-MCH-ANL-008"},
            priority = P1,
            description = "One authorized submit from the Match Schedule. Grouped verifications: [MCH-ANL-010] exactly one official "
                    + "analysis is created and the success UI appears; [MCH-ANL-011] its metadata comes from the schedule match; "
                    + "[MCH-ANL-012] it belongs to the clicked match and not another; then, when "
                    + "-Dmatch.schedule.analysis.duplicateCheck.enabled=true, a stale second form for the same match is submitted once "
                    + "and must be rejected: [MCH-ANL-014] with the required Arabic message, [MCH-ANL-015] although the form was "
                    + "still enabled (server-side revalidation), [MCH-ANL-016] without creating a second record.",
            testObjective = "Prove the creation, linking, identity and duplicate-protection rules end to end with one created record.",
            preconditions = {"-Dmatch.schedule.analysis.phase=c", "-Danalysis.create.enabled=true",
                    "-Dmatch.schedule.analysis.submit.enabled=true",
                    "Optional: -Dmatch.schedule.analysis.targetExternalEventId=<id>; -Dmatch.schedule.analysis.duplicateCheck.enabled=true",
                    "An eligible match exists (it is consumed by this test)"},
            testDataReference = "Target match (pinned id or first eligible row); unique description marker 'MCH-ANL-010 auto <timestamp>'",
            expectedFinalResult = "One 'رسمية' analysis exists for the clicked match, status «قيد التحليل», row without start action; "
                    + "a stale duplicate submit is rejected with the Arabic message and creates nothing.")
    public void mchAnl010_submitCreatesOneLinkedAnalysisAndRejectsDuplicates() {
        begin(MchAnalysisTestIds.MCH_ANL_010, Phase.C);
        MatchScheduleAnalysisGate.requireExplicitScheduleSubmitAuthorized();
        List<String> failedGroups = new ArrayList<>();
        WebDriver driver = browser();
        String primaryHandle = driver.getWindowHandle();

        String target = resolveTarget();
        MatchScheduleAnalysisTargetRegistry.setExternalEventId(target);
        TestStepReporter.recordTestData("externalEventId", target);
        String marker = "MCH-ANL-010 auto " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        if (System.getProperty(MatchScheduleAnalysisGate.HAPPY_PATH_DESCRIPTION_PROPERTY) == null) {
            System.setProperty(MatchScheduleAnalysisGate.HAPPY_PATH_DESCRIPTION_PROPERTY, marker);
        } else {
            marker = MatchScheduleAnalysisGate.resolveHappyPathDescription();
        }
        final String descriptionMarker = marker;
        TestStepReporter.recordTestData("descriptionMarker", descriptionMarker);

        String staleHandle = null;
        boolean staleSubmitEnabled = false;
        final String probeMarker = descriptionMarker + " DUP-PROBE";
        if (MatchScheduleAnalysisGate.isDuplicateProbeEnabled()) {
            staleHandle = openStaleSecondForm(driver, target, probeMarker);
            driver.switchTo().window(primaryHandle);
        } else {
            TestStepReporter.recordStep("Prepare stale second form", "Skipped",
                    "Needs -D" + MatchScheduleAnalysisGate.DUPLICATE_PROBE_ENABLED_PROPERTY + "=true",
                    "Duplicate probe is not authorized for this run; MCH-ANL-014/015/016 are not executed.", "SKIP");
        }

        AnalysisSubmissionAttempt attempt = AnalysisSubmissionAttempt.authorized("schedule:" + target);
        ScheduleSubmitSuccessSnapshot[] holder = new ScheduleSubmitSuccessSnapshot[1];
        step("[MCH-ANL-010] Submit the analysis once from the schedule",
                "Open the match from the schedule, enter the Video URL and Description, click «بدء التحليل» once",
                "Outcome SUCCESS: the success message «" + AnalysisSuccessDialog.SUCCESS_MESSAGE + "» is shown.",
                () -> {
                    holder[0] = new MatchScheduleAnalysisHappyPathFlow(driver).runAuthorizedSubmitThroughSuccessDialog(attempt);
                    ScheduleSubmitSuccessSnapshot snapshot = holder[0];
                    if (snapshot.outcome() != AnalysisPostSubmitOutcome.SUCCESS) {
                        return StepResult.fail("Submit outcome was " + snapshot.outcome() + " instead of SUCCESS. Visible error: «"
                                + snapshot.prepared().form().getVisibleErrorToastBody() + "».");
                    }
                    boolean dialog = snapshot.dialog() != null && snapshot.dialog().isSuccessMessageVisible();
                    return dialog && attempt.submitClickCount() == 1
                            ? StepResult.pass("SUCCESS; analysisId=" + snapshot.analysisId() + "; submitClicks=" + attempt.submitClickCount())
                            : StepResult.fail("successDialog=" + dialog + ", submitClicks=" + attempt.submitClickCount());
                });
        ScheduleSubmitSuccessSnapshot snapshot = holder[0];
        Long analysisId = snapshot.analysisId();
        String home = snapshot.prepared().homeTeamDisplay();
        String away = snapshot.prepared().awayTeamDisplay();
        TestStepReporter.recordTestData("analysisId", String.valueOf(analysisId));
        TestStepReporter.recordTestData("match", home + " × " + away);

        group(failedGroups, MchAnalysisTestIds.MCH_ANL_010, "Exactly one official analysis exists for the match",
                "Search the analyses list for the unique description marker",
                "One record with type «رسمية» for " + home + " × " + away + "; it persists after a refresh.",
                () -> {
                    PostSubmitReadOnlyVerificationResult result = new PostSubmitReadOnlyVerificationFlow(driver)
                            .verifyExistingRecordByDescriptionMarker(descriptionMarker, target, home, away, snapshot.prepared().matchDateValue());
                    int count = new AnalysesListPage(driver).openDirectly().waitUntilListingResultsStable().findVisibleCardsContaining(descriptionMarker).size();
                    boolean ok = result.creationConfirmed() && result.officialTypeObserved() && result.teamsMatchOnCard()
                            && result.persistenceVerified() && count == 1;
                    return ok ? StepResult.pass("records=" + count + "; official=true; teamsMatch=true; persistent=true; status=" + result.analysisStatusObserved())
                            : StepResult.fail("records=" + count + ", " + result.evidenceSummary());
                });

        group(failedGroups, MchAnalysisTestIds.MCH_ANL_011, "Analysis metadata comes from the schedule match",
                "Open the analysis details and compare with the schedule match",
                "Match name contains " + home + " and " + away + "; the description is the one entered; the video URL is the one entered.",
                () -> {
                    AnalysisDetailsPage details = new AnalysisDetailsPage(driver).openById(analysisId).waitUntilLoaded();
                    String name = details.readMatchName();
                    boolean clubs = name.contains(home) && name.contains(away);
                    boolean description = details.readDescription().contains(descriptionMarker);
                    boolean video = details.readVideoUrl().contains(videoToken());
                    return clubs && description && video
                            ? StepResult.pass("matchName=«" + name + "»; description ok; video ok")
                            : StepResult.fail("matchName=«" + name + "» (clubs match=" + clubs + "), description ok=" + description + ", video ok=" + video);
                });

        group(failedGroups, MchAnalysisTestIds.MCH_ANL_012, "Analysis belongs to the clicked match",
                "Verify the details page identity and the schedule row of the clicked match",
                "Details show the clicked match's clubs; its schedule row is «قيد التحليل»/«اكتمل التحليل» without a start action.",
                () -> {
                    var verdict = tableFlow.verifyPostSubmitScheduleRow(target, snapshot.prepared().scheduleRow().matchIdNumeric(),
                            home, away, snapshot.prepared().scheduleRow().scoreDisplay());
                    String name = new AnalysisDetailsPage(driver).openById(analysisId).waitUntilLoaded().readMatchName();
                    boolean identity = name.contains(home) && name.contains(away);
                    return verdict.meetsExpectation() && identity
                            ? StepResult.pass("scheduleRow: " + verdict.reportingLine() + "; detailsMatch=«" + name + "»")
                            : StepResult.fail("scheduleRow: " + verdict.reportingLine() + "; detailsMatch=«" + name + "», identityOk=" + identity);
                });
        TestStepReporter.recordStep("[MCH-ANL-012] Internal Official Match ID stored in the record",
                "Look for the stored internal Official Match ID in the UI",
                "The stored ID equals the schedule match's ID.",
                "NOT OBSERVABLE through the UI (the portal binds by externalEventId and never displays the internal ID). "
                        + "Needs backend evidence: docs/matchschedule/backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md",
                "BLOCKED");

        if (staleHandle != null) {
            final String stale = staleHandle;
            final boolean enabledBefore = staleSubmitEnabled;
            AnalysisPostSubmitOutcome[] probeOutcome = new AnalysisPostSubmitOutcome[1];
            String[] probeMessage = new String[1];
            boolean[] probeStaleEnabled = new boolean[1];
            group(failedGroups, MchAnalysisTestIds.MCH_ANL_014, "Stale form is rejected with the required Arabic message",
                    "Switch to the second form (opened before the analysis existed), enter the Video URL and click «بدء التحليل» once",
                    "Submit is rejected and the message «" + MatchScheduleAnalysisStatusMessages.DUPLICATE_ALREADY_STARTED + "» is shown.",
                    () -> {
                        driver.switchTo().window(stale);
                        CreateAnalysisPage form = new CreateAnalysisPage(driver);
                        probeStaleEnabled[0] = form.isSubmitEnabled();
                        form.submitOnce();
                        probeOutcome[0] = form.waitForPostSubmitOutcome(20);
                        probeMessage[0] = form.getVisibleErrorToastBody();
                        ScreenshotsManager.takeFullPageScreenshot(driver, MchAnalysisTestIds.MCH_ANL_014 + "_stale_submit_result");
                        if (probeOutcome[0] == AnalysisPostSubmitOutcome.SUCCESS) {
                            return StepResult.fail("The stale duplicate submit was ACCEPTED (outcome SUCCESS): a second analysis may have been created for the same match.");
                        }
                        boolean exact = probeMessage[0].contains(MatchScheduleAnalysisStatusMessages.DUPLICATE_ALREADY_STARTED);
                        return exact ? StepResult.pass("rejected; message=«" + probeMessage[0] + "»")
                                : StepResult.fail("Rejected (" + probeOutcome[0] + ") but the message is «" + probeMessage[0]
                                        + "» instead of «" + MatchScheduleAnalysisStatusMessages.DUPLICATE_ALREADY_STARTED + "».");
                    });
            group(failedGroups, MchAnalysisTestIds.MCH_ANL_015, "Eligibility is revalidated at submit, not only in the UI",
                    "Check that the stale form still allowed submit before the click",
                    "The form's submit was enabled (UI unaware) yet the server rejected the request.",
                    () -> probeStaleEnabled[0] && probeOutcome[0] != AnalysisPostSubmitOutcome.SUCCESS
                            ? StepResult.pass("submitEnabledInStaleForm=true; serverOutcome=" + probeOutcome[0])
                            : StepResult.fail("submitEnabledInStaleForm=" + probeStaleEnabled[0] + ", serverOutcome=" + probeOutcome[0]
                                    + (probeStaleEnabled[0] ? "" : " — the stale form had already disabled submit, so server revalidation was not exercised."),
                            probeStaleEnabled[0] ? DefectClassification.PRODUCT_DEFECT : DefectClassification.INCONCLUSIVE));
            group(failedGroups, MchAnalysisTestIds.MCH_ANL_016, "No second analysis record is created",
                    "Search the analyses list for the duplicate-probe marker and re-read the schedule row",
                    "No record carries the probe marker; the match still has exactly one analysis and no start action.",
                    () -> {
                        int probeRecords = new AnalysesListPage(driver).openDirectly().waitUntilListingResultsStable()
                                .findVisibleCardsContaining(probeMarker).size();
                        int primaryRecords = new AnalysesListPage(driver).findVisibleCardsContaining(descriptionMarker).size();
                        var verdict = tableFlow.verifyPostSubmitScheduleRow(target, snapshot.prepared().scheduleRow().matchIdNumeric(),
                                home, away, snapshot.prepared().scheduleRow().scoreDisplay());
                        return probeRecords == 0 && primaryRecords == 1 && verdict.meetsExpectation()
                                ? StepResult.pass("probeRecords=0; primaryRecords=1; " + verdict.reportingLine())
                                : StepResult.fail("probeRecords=" + probeRecords + ", primaryRecords=" + primaryRecords + "; " + verdict.reportingLine());
                    });
            try {
                driver.switchTo().window(stale).close();
            } catch (RuntimeException ignored) {
                // stale tab may already be closed by the portal
            }
            driver.switchTo().window(primaryHandle);
        }

        Assert.assertTrue(failedGroups.isEmpty(), "Verification groups failed: " + String.join(", ", failedGroups)
                + ". See the step list for expected vs actual of each group.");
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private void begin(String testCaseId, Phase minimum) {
        this.currentTestId = testCaseId;
        this.failedChecks.clear();
        MatchScheduleAnalysisGate.requirePhaseAtLeast(minimum);
        if (minimum != Phase.C && AnalysisCreateGate.isCreationEnabled()) {
            throw new SkipException(testCaseId + " is read-only and refuses to run with -Danalysis.create.enabled=true "
                    + "(a shared session could consume a match). Run phase a/b with creation disabled.");
        }
    }

    /** Like {@link #step} but a failure is remembered and the test continues; call {@link #assertChecks()} at the end. */
    private void check(String title, String action, String expected, StepAction stepAction) {
        try {
            step(title, action, expected, stepAction);
        } catch (SkipException skip) {
            throw skip;
        } catch (Throwable failure) {
            failedChecks.add(title);
        }
    }

    private void assertChecks() {
        Assert.assertTrue(failedChecks.isEmpty(), failedChecks.size() + " check(s) failed: " + String.join(" | ", failedChecks)
                + ". See the step list and the bug report for expected vs actual of each.");
    }

    private void step(String title, String action, String expected, StepAction stepAction) {
        TestStepReporter.runStep(title, action, expected, stepAction, browser(), currentTestId);
    }

    /** Runs one grouped verification; a failure is recorded and execution continues so the single submit is not wasted. */
    private void group(List<String> failures, String id, String title, String action, String expected, StepAction stepAction) {
        try {
            TestStepReporter.runStep("[" + id + "] " + title, action, expected, stepAction, browser(), currentTestId + "_" + id);
        } catch (Throwable failure) {
            failures.add(id);
        }
    }

    private record OpenedForm(MatchScheduleAnalysisRowContext row, ScheduleOfficialAnalysisPopupPage popup) {
        CreateAnalysisPage form() {
            return popup.form();
        }
    }

    private MatchScheduleAnalysisRowContext locateEligibleRow() {
        MatchScheduleAnalysisRowContext[] holder = new MatchScheduleAnalysisRowContext[1];
        step("Locate an eligible match on the schedule",
                "Open «جدول المباريات» and look for a played match with status «غير محللة»",
                "A played match with status «غير محللة» is found.",
                () -> {
                    tableFlow.openScheduleDirect();
                    Optional<MatchScheduleAnalysisRowContext> row = tableFlow.findEligibleNotAnalyzedRow(MatchScheduleAnalysisGate.configuredTargetExternalEventId());
                    if (row.isEmpty()) {
                        throw new SkipException("Precondition not met: no played match with status «غير محللة» on the schedule"
                                + MatchScheduleAnalysisGate.configuredTargetExternalEventId().map(id -> " (pinned id " + id + " is not eligible)").orElse("")
                                + ". Test data must be refreshed.");
                    }
                    holder[0] = row.get();
                    return StepResult.pass("externalEventId=" + row.get().externalEventId() + "; score=" + row.get().scoreDisplay()
                            + "; date=" + row.get().dateToken() + "; status=" + row.get().analysisStatus());
                });
        MatchScheduleAnalysisRowContext row = holder[0];
        TestStepReporter.recordTestData("externalEventId", row.externalEventId());
        TestStepReporter.recordTestData("matchId", row.matchIdNumeric());
        TestStepReporter.recordTestData("score", row.scoreDisplay());
        TestStepReporter.recordTestData("date", row.dateToken());
        return row;
    }

    private OpenedForm openFormFromEligibleRow() {
        return openFormForRow(locateEligibleRow());
    }

    private OpenedForm openFormForRow(MatchScheduleAnalysisRowContext row) {
        ScheduleOfficialAnalysisPopupPage[] holder = new ScheduleOfficialAnalysisPopupPage[1];
        step("Open the create view from the match row",
                "Hover the row and click «تحليل المباراة»",
                "The create-analysis view opens for that match.",
                () -> {
                    tableFlow.hoverToRevealStartAnalysis(row);
                    holder[0] = tableFlow.openCreateAnalysisFromRow(row);
                    return StepResult.pass("url=" + browser().getCurrentUrl());
                });
        return new OpenedForm(row, holder[0]);
    }

    /** Scans the whole schedule but only hovers rows accepted by the filter; never clicks anything. */
    private List<RowActionAvailability> scanSchedule(String what, Predicate<String> rowTextFilter) {
        List<RowActionAvailability>[] holder = new List[1];
        step("Scan the schedule for " + what,
                "Open «جدول المباريات», scroll through every row and hover the " + what + " (up to 60), recording the actions each offers",
                "Rows are read without clicking anything.",
                () -> {
                    tableFlow.openScheduleDirect();
                    holder[0] = new MatchSchedulePage(browser()).scanRowActionAvailability(60, rowTextFilter);
                    return StepResult.pass(holder[0].size() + " " + what + " found and checked");
                });
        return holder[0];
    }

    private String resolveTarget() {
        tableFlow.openScheduleDirect();
        Optional<String> configured = MatchScheduleAnalysisGate.configuredTargetExternalEventId();
        if (configured.isPresent()) {
            if (tableFlow.findEligibleNotAnalyzedRow(configured).isPresent()) {
                return configured.get();
            }
            throw new SkipException("Pinned match " + configured.get() + " is no longer «غير محللة»; refusing to switch to another match silently.");
        }
        return tableFlow.findEligibleNotAnalyzedRow(Optional.empty())
                .orElseThrow(() -> new SkipException("Precondition not met: no «غير محللة» match to analyse; pin one with -D"
                        + MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY + "=<id>."))
                .externalEventId();
    }

    /** Opens a second tab on the create view of the same match and leaves it ready to submit (the "stale form"). */
    private String openStaleSecondForm(WebDriver driver, String target, String probeMarker) {
        String[] handle = new String[1];
        TestStepReporter.runStep("Prepare a second create form for the same match (to become stale)",
                "Open a new tab on " + CREATE_ROUTE + target + " and fill it, without submitting",
                "The second form is loaded, bound to the same match, with submit enabled.",
                () -> {
                    driver.switchTo().newWindow(WindowType.TAB);
                    handle[0] = driver.getWindowHandle();
                    driver.get(PortalConfig.baseUrl() + CREATE_ROUTE + target);
                    CreateAnalysisPage form = new CreateAnalysisPage(driver).waitForScheduleEntryFromMatchSchedule();
                    form.enterOptionalDescription(probeMarker).enterVideoUrl(AnalysisCreateGate.resolveVideoUrl());
                    boolean bound = target.equals(form.readExternalEventIdFromCurrentUrl());
                    boolean enabled = form.isSubmitEnabled();
                    return bound && enabled ? StepResult.pass("second form bound to " + target + "; submitEnabled=true")
                            : StepResult.fail("bound=" + bound + ", submitEnabled=" + enabled, DefectClassification.AUTOMATION_DEFECT);
                }, driver, currentTestId + "_stale_form");
        return handle[0];
    }

    private static String videoToken() {
        String url = AnalysisCreateGate.resolveVideoUrl();
        Matcher id = Pattern.compile("(?:v=|youtu\\.be/|/d/)([A-Za-z0-9_-]{6,})").matcher(url);
        return id.find() ? id.group(1) : url;
    }

    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static boolean scheduleDateMatchesRow(String rowDateToken, String formDate) {
        if (formDate == null || formDate.isBlank()) {
            return false;
        }
        String form = formDate.trim();
        String row = rowDateToken.trim();
        return form.contains(row) || row.contains(form)
                || (MatchDateDisplayRules.looksLikeCalendarDateDisplay(form) && MatchDateDisplayRules.looksLikeCalendarDateDisplay(row));
    }
}
