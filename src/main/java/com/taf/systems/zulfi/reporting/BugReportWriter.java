package com.taf.systems.zulfi.reporting;

import com.taf.core.utils.OutputPaths;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class BugReportWriter {

    private BugReportWriter() {}

    public static Path writeMarkdown(FailureDiagnostics diagnostics, String testCaseId, String feature) {
        try {
            Path dir = OutputPaths.bugReports();
            Files.createDirectories(dir);
            String safeId = safeTestCaseId(testCaseId);
            String artifactId = "LOCAL-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
            Path file = dir.resolve(artifactId + "-" + safeId + ".md");
            String body =
                    """
                    # Bug Report — %s

                    ## Executive summary
                    Validation failed during «%s»: %s

                    - **Feature:** %s
                    - **Test Case ID:** %s
                    - **Classification:** %s
                    - **Expected:** %s
                    - **Actual:** %s
                    - **URL:** %s

                    ## Exact error
                    ```
                    %s
                    ```

                    > Local investigation artifact only. No external ticket created.
                    """
                            .formatted(
                                    artifactId,
                                    diagnostics.failedStep(),
                                    diagnostics.failureReason(),
                                    feature,
                                    testCaseId,
                                    diagnostics.classification(),
                                    diagnostics.expectedBehavior(),
                                    diagnostics.actualBehavior(),
                                    diagnostics.browserUrl(),
                                    diagnostics.exceptionType());
            Files.writeString(file, body);
            Allure.addAttachment("Bug investigation report (compact)", "text/markdown", body);
            return file;
        } catch (IOException ex) {
            return null;
        }
    }

    private static String safeTestCaseId(String testCaseId) {
        if (testCaseId == null || testCaseId.isBlank()) {
            return "UNKNOWN";
        }
        return testCaseId.trim();
    }
}
