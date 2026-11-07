package com.taf.systems.zulfi.reporting;

import com.taf.core.utils.OutputPaths;
import com.taf.systems.zulfi.config.PortalConfig;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Detailed PRODUCT_DEFECT artifact for جدول المباريات → إنشاء تحليل (developer handoff). */
public final class ScheduleAnalysisFlowDefectReporter {

    private ScheduleAnalysisFlowDefectReporter() {}

    public record ScheduleFlowEvidence(
            String testCaseId,
            String acceptanceCriterion,
            String failedStep,
            String externalEventId,
            String matchIdNumeric,
            String homeTeam,
            String awayTeam,
            String scoreDisplay,
            String browserUrl,
            Path screenshotPath,
            String videoAttachmentHint) {}

    public static Path reportProductMismatch(
            ScheduleFlowEvidence evidence, String expectedBehavior, String actualBehavior) {
        try {
            String artifactId =
                    "SCHED-FLOW-"
                            + Instant.now().toEpochMilli()
                            + "-"
                            + UUID.randomUUID().toString().substring(0, 8);
            Path docsDir = Path.of("docs", "matchschedule", "defects");
            Files.createDirectories(docsDir);
            Path targetDir = OutputPaths.bugReports();
            Files.createDirectories(targetDir);

            String capturedAt =
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                            Instant.now().atZone(ZoneId.systemDefault()));
            String screenshotLine =
                    evidence.screenshotPath() == null
                            ? "See Allure attachment «"
                                    + evidence.testCaseId()
                                    + "_STEP_FAIL.png» or run folder under test-execution-reports/latest/screenshots/"
                            : evidence.screenshotPath().toAbsolutePath().toString();
            String videoLine =
                    evidence.videoAttachmentHint() == null || evidence.videoAttachmentHint().isBlank()
                            ? "On failure: Allure «Screen recording» (report.video=ON_FAILURE) and video/*.mp4 in project root."
                            : evidence.videoAttachmentHint();

            String body =
                    """
                    # [PRODUCT_DEFECT][Match Schedule → Analysis] %s

                    ## Executive summary (for Dev / BA)
                    During automated acceptance **%s**, the portal did not behave as specified for **US-FAP-MCH-ANALYSIS**.
                    Failure point: **%s**. %s

                    | Field | Value |
                    |-------|-------|
                    | **Artifact ID** | %s |
                    | **Test Case ID** | %s |
                    | **Acceptance criterion** | %s |
                    | **Classification** | PRODUCT_DEFECT |
                    | **Severity (proposed)** | Major (pending triage) |
                    | **Status** | Open |
                    | **Portal URL** | %s |
                    | **Browser URL at capture** | %s |
                    | **Captured** | %s |

                    ## Fixture context (from schedule row)
                    | Field | Value |
                    |-------|-------|
                    | externalEventId | %s |
                    | Match # (schedule) | %s |
                    | Home | %s |
                    | Away | %s |
                    | Score | %s |

                    ## Preconditions
                    1. Authenticated Football Analytics user (`ZULFI_USER` / `ZULFI_PASSWORD`).
                    2. Yellow League **جدول المباريات** contains a **played** row with status **«غير محللة»**.
                    3. Entry path: hover row → **«بدء تحليل المباراة»** → `/analysis/create?externalEventId=...`.
                    4. No manual override of match type or official fixture dropdown (schedule-sourced flow).

                    ## Steps to reproduce (manual)
                    1. Login → sidebar **«جدول المباريات»** (or `/matches`).
                    2. Locate the fixture above (or any **غير محللة** played match).
                    3. Hover the row until **«بدء تحليل المباراة»** appears → click.
                    4. Observe create form binding (teams, **رسمية**, URL `externalEventId`).
                    5. Continue the scenario step that failed in automation: **%s**.

                    ## Expected result
                    %s

                    ## Actual result
                    %s

                    ## Developer notes
                    - Verify API mapping `externalEventId` → official match record and that the create route pre-fills read-only team fields.
                    - Confirm PrimeVue / router state is not cleared on hover or dialog open (Vue re-render).
                    - If failure is post-submit: check analysis persistence, listing API, and Arabic success dialog actions.
                    - Compare with passing run **MCH-SCHED-ANL-HP-001** on the same environment.

                    ## Evidence
                    ### Screenshot
                    %s

                    ### Video
                    %s

                    ### Allure
                    - Step attachments: Expected / Actual / STEP_xx markdown
                    - Full run: `test-execution-reports/latest/allure-html/index.html`

                    > Local defect artifact — attach to Jira/Azure DevOps manually. Not an external ticket ID.
                    """
                            .formatted(
                                    evidence.testCaseId(),
                                    evidence.testCaseId(),
                                    evidence.failedStep(),
                                    devSummaryFromActual(actualBehavior),
                                    artifactId,
                                    evidence.testCaseId(),
                                    evidence.acceptanceCriterion(),
                                    PortalConfig.baseUrl(),
                                    evidence.browserUrl(),
                                    capturedAt,
                                    nullToDash(evidence.externalEventId()),
                                    nullToDash(evidence.matchIdNumeric()),
                                    nullToDash(evidence.homeTeam()),
                                    nullToDash(evidence.awayTeam()),
                                    nullToDash(evidence.scoreDisplay()),
                                    evidence.failedStep(),
                                    expectedBehavior,
                                    actualBehavior,
                                    screenshotLine,
                                    videoLine);

            Path docFile = docsDir.resolve(artifactId + "_" + evidence.testCaseId() + ".md");
            Path targetFile = targetDir.resolve(artifactId + "-" + evidence.testCaseId() + ".md");
            Files.writeString(docFile, body);
            Files.writeString(targetFile, body);
            Allure.addAttachment("Schedule flow PRODUCT_DEFECT (dev)", "text/markdown", body);
            Allure.addAttachment("Schedule flow defect file", "text/plain", docFile.toAbsolutePath().toString());
            return docFile;
        } catch (IOException ex) {
            Allure.addAttachment("Schedule flow defect write error", ex.getMessage());
            return null;
        }
    }

    private static String devSummaryFromActual(String actual) {
        if (actual == null || actual.isBlank()) {
            return "See Actual result below.";
        }
        if (actual.length() > 220) {
            return actual.substring(0, 217) + "...";
        }
        return actual;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
