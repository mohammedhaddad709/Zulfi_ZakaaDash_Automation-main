package com.taf.systems.zulfi.analysis.preflight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class OfficialFixturePreflightReportWriter {

    private OfficialFixturePreflightReportWriter() {}

    public static Path writeMarkdown(
            Path target,
            List<OfficialFixtureOptionSnapshot> allObserved,
            List<OfficialFixtureEligibilityRecord> evaluated,
            String limitations,
            String recommendedFixture)
            throws IOException {
        Files.createDirectories(target.getParent());
        int totalOptions = allObserved.size();
        long fixtureRows = allObserved.stream().filter(o -> "FIXTURE".equals(o.rowKind())).count();
        long enabled =
                allObserved.stream().filter(o -> "FIXTURE".equals(o.rowKind()) && o.enabled()).count();
        long disabled = fixtureRows - enabled;
        Map<EligibilityStatus, Long> counts = new EnumMap<>(EligibilityStatus.class);
        for (EligibilityStatus status : EligibilityStatus.values()) {
            counts.put(status, 0L);
        }
        for (OfficialFixtureEligibilityRecord record : evaluated) {
            counts.merge(record.eligibilityStatus(), 1L, Long::sum);
        }

        StringBuilder md = new StringBuilder();
        md.append("# Phase 12 — Official Fixture Preflight Report\n\n");
        md.append("**Generated:** ")
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                .append("\n\n");
        md.append("**Mode:** Read-only (no Create Analysis submission)\n\n");
        md.append("## Summary\n\n");
        md.append("| Metric | Count |\n|--------|------:|\n");
        md.append("| Total dropdown rows observed | ").append(totalOptions).append(" |\n");
        md.append("| Fixture rows | ").append(fixtureRows).append(" |\n");
        md.append("| Enabled fixtures | ").append(enabled).append(" |\n");
        md.append("| Disabled fixtures | ").append(disabled).append(" |\n");
        md.append("| Known duplicates (policy) | 1 (Al Taee × Al Bukayriyah) |\n");
        md.append("| POTENTIALLY_ELIGIBLE | ").append(counts.get(EligibilityStatus.POTENTIALLY_ELIGIBLE)).append(" |\n");
        md.append("| ELIGIBLE_VERIFIED | ").append(counts.get(EligibilityStatus.ELIGIBLE_VERIFIED)).append(" |\n");
        md.append("| INELIGIBLE | ").append(counts.get(EligibilityStatus.INELIGIBLE)).append(" |\n");
        md.append("| UNKNOWN | ").append(counts.get(EligibilityStatus.UNKNOWN)).append(" |\n\n");
        md.append("## Inspection limitations\n\n").append(limitations).append("\n\n");
        md.append("## Recommended fixture for next authorized E2E\n\n");
        md.append(recommendedFixture == null || recommendedFixture.isBlank() ? "None — manual review required" : recommendedFixture)
                .append("\n\n");
        md.append("## Fixture inventory\n\n");
        md.append("| Fixture | Match Date | Home Team | Away Team | Enabled | Existing Analysis Evidence | Match ID | Eligibility Status | Eligibility Reason | Evidence Source | Recommended Action |\n");
        md.append("|---------|------------|-----------|-----------|---------|----------------------------|----------|--------------------|--------------------|-----------------|--------------------|\n");
        for (OfficialFixtureEligibilityRecord r : evaluated) {
            md.append("| ")
                    .append(escape(r.fixture()))
                    .append(" | ")
                    .append(escape(r.matchDate()))
                    .append(" | ")
                    .append(escape(r.homeTeam()))
                    .append(" | ")
                    .append(escape(r.awayTeam()))
                    .append(" | ")
                    .append(escape(r.enabled()))
                    .append(" | ")
                    .append(escape(r.existingAnalysisEvidence()))
                    .append(" | ")
                    .append(escape(r.matchId()))
                    .append(" | ")
                    .append(r.eligibilityStatus())
                    .append(" | ")
                    .append(escape(r.eligibilityReason()))
                    .append(" | ")
                    .append(escape(r.evidenceSource()))
                    .append(" | ")
                    .append(escape(r.recommendedAction()))
                    .append(" |\n");
        }
        Files.writeString(target, md.toString());
        return target;
    }

    private static String escape(String value) {
        if (value == null) {
            return "Not available";
        }
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
