package com.taf.systems.zulfi.validations;

import com.taf.systems.zulfi.pages.analysis.TeamFieldObservation;
import java.nio.charset.StandardCharsets;

/** UTF-8 safe expected vs actual diagnostics for Home/Away team assertions. */
public final class TeamFieldComparisonDiagnostics {

    private TeamFieldComparisonDiagnostics() {}

    public static String buildFixtureTeamReport(
            int fixtureIndex,
            String fixtureLabel,
            String matchDate,
            String selectedFixture,
            String expectedHome,
            TeamFieldObservation homeObs,
            String expectedAway,
            TeamFieldObservation awayObs,
            boolean homeMatch,
            boolean awayMatch) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fixture Index: ").append(fixtureIndex).append('\n');
        sb.append("Fixture Label: ").append(nullToEmpty(fixtureLabel)).append('\n');
        sb.append("Selected Fixture: ").append(nullToEmpty(selectedFixture)).append('\n');
        sb.append("Match Date: ").append(nullToEmpty(matchDate)).append('\n');
        sb.append('\n');
        appendTeamBlock(sb, "Home", "#analysis-home-team", expectedHome, homeObs, homeMatch);
        sb.append('\n');
        appendTeamBlock(sb, "Away", "#analysis-away-team", expectedAway, awayObs, awayMatch);
        return sb.toString();
    }

    public static String homeVerificationFailureMessage(
            String expectedHome,
            TeamFieldObservation homeObs,
            String selectedFixture) {
        return """
                Home Team Verification Failed
                Expected Home Team: %s
                Actual Home Team: %s
                Observation Source: %s
                Selected Fixture: %s
                """
                .formatted(
                        expectedHome,
                        homeObs.resolvedDisplayText(),
                        homeObs.resolutionSource(),
                        nullToEmpty(selectedFixture));
    }

    public static String awayVerificationFailureMessage(
            String expectedAway, TeamFieldObservation awayObs, String selectedFixture) {
        return """
                Away Team Verification Failed
                Expected Away Team: %s
                Actual Away Team: %s
                Observation Source: %s
                Selected Fixture: %s
                """
                .formatted(
                        expectedAway,
                        awayObs.resolvedDisplayText(),
                        awayObs.resolutionSource(),
                        nullToEmpty(selectedFixture));
    }

    private static void appendTeamBlock(
            StringBuilder sb,
            String role,
            String selector,
            String expected,
            TeamFieldObservation obs,
            boolean match) {
        sb.append(role).append(" Team Field Selector: ").append(selector).append('\n');
        sb.append("Expected ").append(role).append(" Team: ").append(nullToEmpty(expected)).append('\n');
        sb.append("Observed ").append(role).append(" Team: ").append(obs.resolvedDisplayText()).append('\n');
        sb.append("Comparison Result: ").append(match ? "MATCH" : "MISMATCH").append('\n');
        sb.append("Observation Source: ").append(obs.resolutionSource()).append('\n');
        sb.append("Raw Length (expected/observed): ")
                .append(codeUnitLength(expected))
                .append(" / ")
                .append(codeUnitLength(obs.resolvedDisplayText()))
                .append('\n');
        sb.append("Leading/Trailing Whitespace (expected): ")
                .append(whitespaceFlags(expected))
                .append('\n');
        sb.append("Leading/Trailing Whitespace (observed): ")
                .append(whitespaceFlags(obs.resolvedDisplayText()))
                .append('\n');
        sb.append("Unicode Code Points (expected): ").append(toCodePoints(expected)).append('\n');
        sb.append("Unicode Code Points (observed): ")
                .append(toCodePoints(obs.resolvedDisplayText()))
                .append('\n');
        sb.append("DOM domProperty.value: ").append(obs.domPropertyValue()).append('\n');
        sb.append("DOM domProperty.placeholder: ").append(obs.domPropertyPlaceholder()).append('\n');
        sb.append("DOM attribute value: ").append(obs.attributeValue()).append('\n');
        sb.append("DOM attribute placeholder: ").append(obs.attributePlaceholder()).append('\n');
        sb.append("input readonly: ").append(obs.inputReadonly()).append('\n');
        sb.append("input disabled: ").append(obs.inputDisabled()).append('\n');
        sb.append("rootTag/inputTag: ")
                .append(obs.rootTagName())
                .append(" / ")
                .append(obs.inputTagName())
                .append('\n');
    }

    public static String toCodePoints(String text) {
        if (text == null || text.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        text.codePoints()
                .forEach(
                        cp -> {
                            if (!sb.isEmpty()) {
                                sb.append(' ');
                            }
                            sb.append(String.format("U+%04X", cp));
                        });
        return sb.toString();
    }

    private static int codeUnitLength(String text) {
        return text == null ? 0 : text.length();
    }

    private static String whitespaceFlags(String text) {
        if (text == null || text.isEmpty()) {
            return "n/a";
        }
        boolean lead = Character.isWhitespace(text.charAt(0));
        boolean trail = Character.isWhitespace(text.charAt(text.length() - 1));
        return "leading=" + lead + "; trailing=" + trail;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Ensures attachment bytes are UTF-8 for Allure. */
    public static byte[] utf8Bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
