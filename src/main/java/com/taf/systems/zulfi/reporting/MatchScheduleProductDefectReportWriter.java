package com.taf.systems.zulfi.reporting;

import com.taf.systems.zulfi.config.PortalConfig;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisFieldRequirementEvidence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Local PRODUCT_DEFECT markdown for US-FAP-MCH-ANALYSIS UI inconsistencies. */
public final class MatchScheduleProductDefectReportWriter {

    private MatchScheduleProductDefectReportWriter() {}

    public static Path writeDescriptionRequiredIndicatorDefect(
            String bugLocalId,
            CreateAnalysisFieldRequirementEvidence descriptionEvidence,
            String externalEventId,
            String browserSummary,
            Path screenshotPath)
            throws IOException {
        Path dir = Path.of("docs", "matchschedule", "defects");
        Files.createDirectories(dir);
        Path file = dir.resolve(bugLocalId + "_description_required_indicator.md");
        String capturedAt =
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(ZoneId.systemDefault()));
        String body =
                """
                # [MINOR][Match Schedule Analysis] Description field incorrectly displays a required indicator although it is optional

                | Field | Value |
                |-------|-------|
                | **Bug ID / local reference** | %s |
                | **Related User Story** | US-FAP-MCH-ANALYSIS |
                | **Related Test Case** | MCH-ANL-007 |
                | **Related Acceptance Criterion** | Complete Existing Analysis Inputs |
                | **Classification** | PRODUCT_DEFECT |
                | **Severity (proposed)** | Minor |
                | **Priority (proposed)** | P2 (pending triage) |
                | **Status** | Open |
                | **Reproducibility** | Observed on live schedule create route (%s) |

                ## Environment
                - Portal: %s
                - Browser: %s
                - Captured: %s
                - Schedule target externalEventId: %s

                ## Preconditions
                - Authenticated user with access to Match Schedule analysis entry
                - Played official match eligible for analysis
                - Navigate: Match Schedule → «بدء تحليل المباراة» → `/analysis/create?externalEventId=...`
                - No form submission performed during defect capture

                ## Reproduction Steps
                1. Open Create Analysis from Match Schedule for externalEventId `%s`.
                2. Locate the Description field (`#analysis-description`).
                3. Observe the visible label and required indicator in the DOM.

                ## Expected Result
                Description is **optional** and must **not** be represented as mandatory (no misleading required marker).

                ## Actual Result
                %s

                ## DOM evidence
                - Visible label: `%s`
                - Label DOM snippet: `%s`
                - aria-required on control: %s
                - HTML required on control: %s

                ## Screenshot
                %s

                ## Impact
                Users may believe Description is mandatory, conflicting with approved US-FAP-MCH-ANALYSIS acceptance criteria. Non-blocking for MCH-ANL-010 when Description is populated.

                ## Recommended fix
                Remove required marker from Description label for schedule/create official flow; align PrimeVue field metadata with optional AC.

                ## Verification steps after fix
                1. Re-run MCH-ANL-007 CHECK A1 — label must not show required marker.
                2. Re-run CHECK A2 — empty Description with valid video must not show client-side required validation.
                3. Confirm Video URL remains required (CHECK A3).

                > Local defect artifact — not an external ticket. Attach to Allure for triage.
                """
                        .formatted(
                                bugLocalId,
                                externalEventId,
                                PortalConfig.baseUrl(),
                                browserSummary,
                                capturedAt,
                                externalEventId,
                                externalEventId,
                                descriptionEvidence.toDiagnosticBlock(),
                                descriptionEvidence.visibleLabelText(),
                                truncate(descriptionEvidence.labelDomSnippet(), 500),
                                descriptionEvidence.ariaRequiredOnControl(),
                                descriptionEvidence.htmlRequiredOnControl(),
                                screenshotPath == null ? "n/a" : screenshotPath.toString());
        Files.writeString(file, body);
        return file;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
