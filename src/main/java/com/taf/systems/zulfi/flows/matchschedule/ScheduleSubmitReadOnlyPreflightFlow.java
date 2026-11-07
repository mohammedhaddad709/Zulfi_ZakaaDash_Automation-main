package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.analysis.preflight.KnownFixtureExclusions;
import com.taf.systems.zulfi.config.MatchScheduleAnalysisGate;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysesListCardSnapshot;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;
import com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** MCH-ANL-010 read-only preflight: existing analysis + identity mapping (no submit). */
public final class ScheduleSubmitReadOnlyPreflightFlow {

    private static final Pattern ANALYSIS_ID_IN_TEXT = Pattern.compile("(?i)(?:analysis[/\\s#]|تحليل\\s*#?\\s*)(\\d{2,})");

    private final WebDriver driver;
    private final MatchScheduleAnalysisFlow scheduleFlow;

    public ScheduleSubmitReadOnlyPreflightFlow(WebDriver driver) {
        this.driver = driver;
        this.scheduleFlow = new MatchScheduleAnalysisFlow(driver);
    }

    public record ReadOnlyPreflightResult(
            ExistingAnalysisPreflightEvidence existingAnalysis,
            ScheduleOfficialMatchIdentityEvidence identityMapping,
            MatchScheduleAnalysisRowContext scheduleRow,
            String createFormOfficialMatchLabel) {}

    @Step("Read-only preflight for schedule submit eligibility")
    public ReadOnlyPreflightResult run(String externalEventId) {
        MatchSchedulePage schedule = scheduleFlow.openMatchSchedule();
        MatchScheduleAnalysisRowContext row =
                schedule
                        .findEligibleAnalysisRowContext(Optional.of(externalEventId))
                        .orElseGet(() -> locateRowForPreflight(schedule, externalEventId));

        ExistingAnalysisPreflightEvidence existing =
                probeExistingAnalysis(externalEventId, row.matchIdNumeric(), row);

        ScheduleAnalysisCreateFormOpener opener = new ScheduleAnalysisCreateFormOpener(driver);
        ScheduleAnalysisCreateFormOpener.OpenedScheduleCreateForm opened =
                opener.openForExternalEventId(externalEventId);
        CreateAnalysisPage form = opened.form();
        ScheduleOfficialMatchIdentityEvidence identity =
                ScheduleOfficialMatchIdentityResolver.resolve(externalEventId, opened.scheduleRow(), form);
        String label = form.getOfficialMatchSelectedLabel();
        opened.createView().closeWithoutSubmit();

        return new ReadOnlyPreflightResult(existing, identity, row, label);
    }

    private MatchScheduleAnalysisRowContext locateRowForPreflight(
            MatchSchedulePage schedule, String externalEventId) {
        throw new org.testng.SkipException(
                "Schedule row not found for externalEventId="
                        + externalEventId
                        + " (may no longer be eligible «"
                        + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                        + "»).");
    }

    private ExistingAnalysisPreflightEvidence probeExistingAnalysis(
            String externalEventId, String internalMatchId, MatchScheduleAnalysisRowContext row) {
        List<String> mechanisms = new ArrayList<>();
        List<ExistingAnalysisPreflightEvidence.MatchingAnalysisRecord> matches = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        AnalysesListPage list = new AnalysesListPage(driver).openViaSidebarNavigation();
        mechanisms.add("AnalysesListPage.openViaSidebarNavigation + search field");

        list.clearSearch();
        list.searchByAnalysisId(externalEventId);
        mechanisms.add("searchByAnalysisId(externalEventId=" + externalEventId + ")");
        collectMatchingCards(list, externalEventId, matches, seen);

        boolean pageHasEventId = list.pageSourceContainsFixture(externalEventId);
        boolean visibleEventId = list.containsAnalysisIdentityMarker(externalEventId);

        if (internalMatchId != null && !internalMatchId.isBlank()) {
            list.clearSearch();
            list.searchByAnalysisId(internalMatchId);
            mechanisms.add("searchByAnalysisId(internalMatchId=" + internalMatchId + ")");
            collectMatchingCards(list, internalMatchId, matches, seen);
        }

        list.clearSearch();
        list.searchByMatchName(row.rowText().contains("الطائي") ? "الطائي" : externalEventId);
        mechanisms.add("secondary searchByMatchName(team fragment) — corroboration only");
        collectMatchingCards(list, externalEventId, matches, seen);

        String scheduleStatus = row.analysisStatus();
        boolean scheduleSaysNotAnalyzed =
                scheduleStatus != null
                        && scheduleStatus.contains(MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED);

        ExistingAnalysisPreflightEvidence.EligibilityVerdict verdict;
        String summary;

        if (!matches.isEmpty() || (visibleEventId && pageHasEventId)) {
            verdict = ExistingAnalysisPreflightEvidence.EligibilityVerdict.EXISTING_ANALYSIS_CONFIRMED;
            summary =
                    "Listing evidence matched fixture identifiers; records="
                            + matches.size()
                            + "; pageContainsEventId="
                            + pageHasEventId;
        } else if (!scheduleSaysNotAnalyzed) {
            verdict = ExistingAnalysisPreflightEvidence.EligibilityVerdict.EXISTING_ANALYSIS_CONFIRMED;
            summary =
                    "Schedule row no longer «"
                            + MatchScheduleAnalysisStatusMessages.STATUS_NOT_ANALYZED
                            + "»; status="
                            + scheduleStatus;
        } else if (scheduleSaysNotAnalyzed && matches.isEmpty() && !pageHasEventId) {
            verdict = ExistingAnalysisPreflightEvidence.EligibilityVerdict.NO_EXISTING_ANALYSIS_EVIDENCE;
            summary =
                    "No listing card matched externalEventId/internalMatchId; schedule still «غير محللة»;"
                            + " no read-only API in framework — absence not backend-proven";
        } else {
            verdict = ExistingAnalysisPreflightEvidence.EligibilityVerdict.NOT_VERIFIED;
            summary =
                    "Inconclusive listing probe; pageHasEventId="
                            + pageHasEventId
                            + "; visibleEventId="
                            + visibleEventId
                            + "; scheduleSaysNotAnalyzed="
                            + scheduleSaysNotAnalyzed;
        }

        return new ExistingAnalysisPreflightEvidence(
                externalEventId,
                internalMatchId == null ? "" : internalMatchId,
                List.copyOf(mechanisms),
                List.copyOf(matches),
                scheduleStatus,
                row.rowText(),
                pageHasEventId,
                visibleEventId,
                matches.stream().anyMatch(m -> m.rawExcerpt().contains(internalMatchId)),
                verdict,
                summary);
    }

    private static void collectMatchingCards(
            AnalysesListPage list,
            String needle,
            List<ExistingAnalysisPreflightEvidence.MatchingAnalysisRecord> matches,
            Set<String> seen) {
        if (needle == null || needle.isBlank()) {
            return;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                for (AnalysesListCardSnapshot card : list.readVisibleAnalysisCards()) {
                    if (!card.rawText().contains(needle) && !card.matchTitle().contains(needle)) {
                        continue;
                    }
                    String key = card.rawText();
                    if (!seen.add(key)) {
                        continue;
                    }
                    matches.add(
                            new ExistingAnalysisPreflightEvidence.MatchingAnalysisRecord(
                                    extractAnalysisIdHint(card.rawText()),
                                    card.status(),
                                    card.matchTitle(),
                                    truncate(card.rawText(), 280)));
                }
                return;
            } catch (StaleElementReferenceException stale) {
                list.waitUntilLoaded();
            }
        }
    }

    private static String extractAnalysisIdHint(String text) {
        if (text == null) {
            return "";
        }
        var matcher = ANALYSIS_ID_IN_TEXT.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    public static boolean isKnownDuplicate(String externalEventId, String internalMatchId) {
        return KnownFixtureExclusions.isKnownDuplicateExternalEventId(externalEventId)
                || KnownFixtureExclusions.isKnownDuplicateMatchId(internalMatchId);
    }

    public static String requireTargetFromGate() {
        return MatchScheduleAnalysisGate.configuredTargetExternalEventId()
                .orElseThrow(
                        () ->
                                new org.testng.SkipException(
                                        "Set -D"
                                                + MatchScheduleAnalysisGate.TARGET_EXTERNAL_EVENT_ID_PROPERTY
                                                + "=16651987"));
    }
}
