package com.taf.systems.zulfi.analysis.preflight;

import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import java.util.ArrayList;
import java.util.List;

/** Classifies fixtures using UI evidence only (non-authoritative backend). */
public final class OfficialFixtureEligibilityEvaluator {

    public record InspectionContext(String analysesListEvidence, String selectedMatchDate) {}

    private OfficialFixtureEligibilityEvaluator() {}

    public static List<OfficialFixtureEligibilityRecord> evaluate(
            List<OfficialFixtureOptionSnapshot> options, InspectionContext context) {
        List<OfficialFixtureEligibilityRecord> records = new ArrayList<>();
        for (OfficialFixtureOptionSnapshot option : options) {
            if (!"FIXTURE".equals(option.rowKind())) {
                continue;
            }
            records.add(evaluateOne(option, context));
        }
        return records;
    }

    private static OfficialFixtureEligibilityRecord evaluateOne(
            OfficialFixtureOptionSnapshot option, InspectionContext context) {
        String fixture = option.displayLabel();
        String matchDate = "Not available";
        String home = option.homeTeam();
        String away = option.awayTeam();
        String enabled = option.enabled() ? "Yes" : "No";
        String matchId =
                KnownFixtureExclusions.MATCH_ID.equals(option.matchId()) ? option.matchId() : option.matchId();
        String listingEvidence = context.analysesListEvidence();

        if (KnownFixtureExclusions.isKnownDuplicateFixture(fixture)) {
            return new OfficialFixtureEligibilityRecord(
                    fixture,
                    matchDate,
                    home,
                    away,
                    enabled,
                    KnownFixtureExclusions.knownDuplicateReason(),
                    KnownFixtureExclusions.MATCH_ID,
                    EligibilityStatus.INELIGIBLE,
                    "Known duplicate fixture from historical verified failure",
                    "Historical run + label match",
                    "Do not use for E2E until business confirms eligibility change");
        }

        if (!option.enabled()) {
            return new OfficialFixtureEligibilityRecord(
                    fixture,
                    matchDate,
                    home,
                    away,
                    enabled,
                    listingEvidence,
                    matchId,
                    EligibilityStatus.INELIGIBLE,
                    "Fixture row disabled in official-match dropdown",
                    "PrimeVue dropdown",
                    "Exclude from automated creation");
        }

        if (listingEvidence != null
                && listingEvidence.toLowerCase().contains("visible analysis card title matches")) {
            return new OfficialFixtureEligibilityRecord(
                    fixture,
                    matchDate,
                    home,
                    away,
                    enabled,
                    listingEvidence,
                    matchId,
                    EligibilityStatus.INELIGIBLE,
                    "Existing analysis evidence found in accessible Analyses listing",
                    "Analyses list UI search",
                    "Exclude unless record is confirmed stale");
        }

        if (listingEvidence != null && listingEvidence.contains("Page source contains")) {
            return new OfficialFixtureEligibilityRecord(
                    fixture,
                    matchDate,
                    home,
                    away,
                    enabled,
                    listingEvidence,
                    matchId,
                    EligibilityStatus.UNKNOWN,
                    "Possible listing match in DOM but not confirmed on visible card",
                    "Analyses list UI search",
                    "Manual confirmation required");
        }

        return new OfficialFixtureEligibilityRecord(
                fixture,
                matchDate,
                home,
                away,
                enabled,
                listingEvidence == null ? "Not checked" : listingEvidence,
                matchId,
                EligibilityStatus.POTENTIALLY_ELIGIBLE,
                "Selectable in UI; no conflicting analysis found in accessible listing view",
                "Create Analysis dropdown + Analyses list UI",
                "Candidate for authorized E2E after business sign-off");
    }

    public static OfficialFixtureEligibilityRecord enrichAfterSelection(
            OfficialFixtureEligibilityRecord base, CreateAnalysisPage form) {
        String date = form.getMatchDateValue();
        if (date == null || date.isBlank()) {
            date = "Not available";
        }
        String homePh = form.getHomeTeamPlaceholder();
        String awayPh = form.getAwayTeamPlaceholder();
        boolean placeholdersOk = form.hasPopulatedTeamPlaceholders();
        boolean readonly = form.areTeamFieldsReadOnly();
        EligibilityStatus status = base.eligibilityStatus();
        String reason = base.eligibilityReason();
        if (base.eligibilityStatus() == EligibilityStatus.POTENTIALLY_ELIGIBLE) {
            if (placeholdersOk && readonly && form.isOfficialMatchDateDisplayValid()) {
                reason =
                        "Autofill verified: date displayed, team placeholders populated, fields non-editable";
            } else {
                status = EligibilityStatus.UNKNOWN;
                reason =
                        "Selection autofill incomplete: placeholdersOk="
                                + placeholdersOk
                                + ", readonly="
                                + readonly
                                + ", date="
                                + date;
            }
        }
        return new OfficialFixtureEligibilityRecord(
                base.fixture(),
                date,
                homePh.isBlank() ? base.homeTeam() : homePh,
                awayPh.isBlank() ? base.awayTeam() : awayPh,
                base.enabled(),
                base.existingAnalysisEvidence(),
                base.matchId(),
                status,
                reason,
                base.evidenceSource() + "; form autofill",
                base.recommendedAction());
    }
}
