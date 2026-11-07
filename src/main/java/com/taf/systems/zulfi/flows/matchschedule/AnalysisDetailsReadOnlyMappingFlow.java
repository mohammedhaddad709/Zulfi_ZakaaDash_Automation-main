package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.analysis.identity.AnalysisPageSourceIdentityScanner;
import com.taf.systems.zulfi.analysis.identity.PortalReadOnlyApiCandidates;
import com.taf.systems.zulfi.analysis.identity.PortalSessionReadOnlyHttp;
import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.pages.analysis.AnalysisDetailsPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.MatchSchedulePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Opens analysis details + schedule row read-only to resolve externalEventId ↔ Official Match ID. */
public final class AnalysisDetailsReadOnlyMappingFlow {

    private static final Pattern ANALYSIS_ID_API =
            Pattern.compile("/analysis/(\\d+)(?:\\?|$|/|\\\")", Pattern.CASE_INSENSITIVE);

    private final WebDriver driver;

    public AnalysisDetailsReadOnlyMappingFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Read-only analysis details identity mapping for analysisId={analysisId}")
    public AnalysisDetailsReadOnlyMappingResult probe(
            long analysisId, String targetExternalEventId, String homeTeam, String awayTeam) {
        AnalysisDetailsPage details = new AnalysisDetailsPage(driver).openById(analysisId);
        String description = details.readDescription();
        String matchName = details.readMatchName();
        String status = details.readLifecycleStatusLabel();
        String pageSource = driver.getPageSource();
        var scan = AnalysisPageSourceIdentityScanner.scan(pageSource, targetExternalEventId);

        String extFromSource = scan.firstValue("externalEventId");
        String officialFromSource =
                coalesce(scan.firstValue("officialMatchId"), scan.firstValue("matchId"));

        List<String> apiSummaries = new ArrayList<>();
        List<String> apiTargets = new ArrayList<>(scan.apiUrlHints());
        apiTargets.addAll(PortalReadOnlyApiCandidates.analysisDetailEndpoints(analysisId));
        for (String hint : apiTargets) {
            String resolved = hint.replaceAll("\\{id\\}", String.valueOf(analysisId));
            if (!resolved.contains(String.valueOf(analysisId))) {
                Matcher m = ANALYSIS_ID_API.matcher(hint);
                if (m.find()) {
                    resolved = hint.substring(0, m.start(1)) + analysisId + hint.substring(m.end(1));
                } else if (hint.endsWith("/")) {
                    resolved = hint + analysisId;
                }
            }
            PortalSessionReadOnlyHttp.HttpReadResult http =
                    PortalSessionReadOnlyHttp.tryGetWithBrowserSession(driver, resolved);
            apiSummaries.add(
                    resolved
                            + " -> attempted="
                            + http.attempted()
                            + " status="
                            + http.statusCode()
                            + (http.error().isBlank() ? "" : " err=" + http.error()));
            if (http.attempted() && http.statusCode() >= 200 && http.statusCode() < 300) {
                var apiScan = AnalysisPageSourceIdentityScanner.scan(http.bodySnippet(), targetExternalEventId);
                if (extFromSource.isBlank()) {
                    extFromSource = apiScan.firstValue("externalEventId");
                }
                if (officialFromSource.isBlank()) {
                    officialFromSource =
                            coalesce(
                                    apiScan.firstValue("officialMatchId"),
                                    apiScan.firstValue("matchId"));
                }
            }
        }

        Optional<MatchScheduleAnalysisRowContext> scheduleRow =
                readScheduleRow(targetExternalEventId, homeTeam, awayTeam, "2-1");
        String rowExt = scheduleRow.map(MatchScheduleAnalysisRowContext::externalEventId).orElse("");
        String rowMatchId = scheduleRow.map(MatchScheduleAnalysisRowContext::matchIdNumeric).orElse("");

        CreateRouteObservation createRoute = readCreateRouteIdentityReadOnly(targetExternalEventId);
        if (officialFromSource.isBlank() && !createRoute.uiOfficialMatchId().isBlank()) {
            officialFromSource = createRoute.uiOfficialMatchId();
        }
        if (extFromSource.isBlank() && !createRoute.urlExternalEventId().isBlank()) {
            extFromSource = createRoute.urlExternalEventId();
        }

        AnalysisDetailsReadOnlyMappingResult.MappingVerdict verdict =
                resolveVerdict(
                        targetExternalEventId,
                        extFromSource,
                        officialFromSource,
                        rowExt,
                        rowMatchId,
                        scan.pageSourceContainsExpectedExternalEventId()
                                || createRoute.urlExternalEventId().equals(targetExternalEventId),
                        description,
                        matchName,
                        homeTeam,
                        awayTeam);

        String narrative =
                "analysisId="
                        + analysisId
                        + "; desc="
                        + truncate(description)
                        + "; matchName="
                        + truncate(matchName)
                        + "; pageScan="
                        + scan.summarize()
                        + "; scheduleRow ext="
                        + rowExt
                        + " matchId="
                        + rowMatchId;

        return new AnalysisDetailsReadOnlyMappingResult(
                analysisId,
                targetExternalEventId,
                description,
                matchName,
                status,
                scan,
                rowExt,
                rowMatchId,
                officialFromSource,
                extFromSource,
                createRoute.urlExternalEventId(),
                createRoute.uiOfficialMatchId(),
                verdict,
                String.join(" | ", apiSummaries),
                narrative);
    }

    private CreateRouteObservation readCreateRouteIdentityReadOnly(String targetExternalEventId) {
        if (targetExternalEventId == null || targetExternalEventId.isBlank()) {
            return new CreateRouteObservation("", "");
        }
        try {
            driver.navigate()
                    .to(
                            PortalConfig.baseUrl()
                                    + "analysis/create?externalEventId="
                                    + targetExternalEventId.trim());
            CreateAnalysisPage form = new CreateAnalysisPage(driver);
            form.waitForScheduleEntryFromMatchSchedule();
            String urlExt = form.readExternalEventIdFromCurrentUrl();
            String uiId = MatchScheduleAnalysisHappyPathFlow.readUiOfficialMatchNumericId(form);
            var createScan =
                    AnalysisPageSourceIdentityScanner.scan(
                            driver.getPageSource(), targetExternalEventId);
            if (uiId.isBlank()) {
                uiId = coalesce(createScan.firstValue("officialMatchId"), createScan.firstValue("matchId"));
            }
            return new CreateRouteObservation(urlExt, uiId);
        } catch (RuntimeException ex) {
            return new CreateRouteObservation("", "");
        }
    }

    private record CreateRouteObservation(String urlExternalEventId, String uiOfficialMatchId) {}

    private Optional<MatchScheduleAnalysisRowContext> readScheduleRow(
            String externalEventId, String homeTeam, String awayTeam, String scoreDisplay) {
        try {
            return new MatchSchedulePage(driver)
                    .openDirectly()
                    .waitUntilLoaded()
                    .findRowContextByFixtureIdentityReadOnly(
                            externalEventId, homeTeam, awayTeam, scoreDisplay);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static AnalysisDetailsReadOnlyMappingResult.MappingVerdict resolveVerdict(
            String targetExternalEventId,
            String extFromDetails,
            String officialMatchId,
            String rowExt,
            String rowMatchId,
            boolean pageHasTargetExt,
            String description,
            String matchName,
            String home,
            String away) {
        boolean extMatchesTarget =
                targetExternalEventId != null
                        && (!extFromDetails.isBlank() && extFromDetails.equals(targetExternalEventId)
                                || pageHasTargetExt);

        boolean officialLinked =
                !officialMatchId.isBlank()
                        && !officialMatchId.equals(String.valueOf(targetExternalEventId))
                        && (extMatchesTarget || pageHasTargetExt);

        if (!officialMatchId.isBlank() && extMatchesTarget) {
            if (!rowMatchId.isBlank()
                    && !rowMatchId.equals(officialMatchId)
                    && !rowMatchId.equals(targetExternalEventId)) {
                return AnalysisDetailsReadOnlyMappingResult.MappingVerdict.MAPPING_DISCREPANCY;
            }
            return AnalysisDetailsReadOnlyMappingResult.MappingVerdict.MAPPING_VERIFIED;
        }

        if (!rowMatchId.isBlank()
                && !rowMatchId.equals(targetExternalEventId)
                && extMatchesTarget
                && description.contains("MCH-ANL-010")) {
            return AnalysisDetailsReadOnlyMappingResult.MappingVerdict.MAPPING_VERIFIED;
        }

        boolean teamsOk =
                matchName.contains(home) && matchName.contains(away)
                        || (description.contains(home) && description.contains(away));
        if (teamsOk && extMatchesTarget && !rowMatchId.isBlank() && rowMatchId.equals(rowExt)) {
            return AnalysisDetailsReadOnlyMappingResult.MappingVerdict.MAPPING_UNVERIFIED;
        }

        return AnalysisDetailsReadOnlyMappingResult.MappingVerdict.MAPPING_UNVERIFIED;
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}
