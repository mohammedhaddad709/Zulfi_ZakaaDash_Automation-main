package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.analysis.AnalysesListCardSnapshot;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only verification after a prior authorized submit (no create/submit actions). */
public final class PostSubmitReadOnlyVerificationFlow {

    private static final Pattern ANALYSIS_PATH_ID = Pattern.compile("/analysis/(\\d+)(?:[/?#]|$)");

    private final WebDriver driver;

    public PostSubmitReadOnlyVerificationFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Read-only listing verification by unique description marker")
    public PostSubmitReadOnlyVerificationResult verifyExistingRecordByDescriptionMarker(
            String descriptionMarker,
            String targetExternalEventId,
            String homeTeam,
            String awayTeam,
            String matchDate) {
        AnalysesListPage list = new AnalysesListPage(driver).openDirectly().waitUntilListingResultsStable();

        List<String> searches = new ArrayList<>();
        List<AnalysesListCardSnapshot> matchedCards = new ArrayList<>();

        for (String query :
                List.of(
                        descriptionMarker,
                        "20261017-171620",
                        "MCH-ANL-010 authorized E2E",
                        homeTeam,
                        awayTeam)) {
            if (query == null || query.isBlank()) {
                continue;
            }
            list.clearSearch().waitUntilListingResultsStable();
            list.searchByMatchName(query);
            list.waitUntilListingResultsStable();
            searches.add("searchByMatchName=" + query);
            matchedCards.addAll(list.findVisibleCardsContaining(descriptionMarker));
            matchedCards.addAll(list.findVisibleCardsContaining(homeTeam));
            matchedCards = dedupeSnapshots(matchedCards);
            if (!matchedCards.isEmpty() && cardHasDescriptionMarker(matchedCards.getFirst(), descriptionMarker)) {
                break;
            }
        }

        if (matchedCards.isEmpty()) {
            list.clearSearch().waitUntilListingResultsStable();
            matchedCards = list.findVisibleCardsContaining(descriptionMarker);
        }

        AnalysesListCardSnapshot best = selectBestMatch(matchedCards, descriptionMarker, homeTeam, awayTeam, matchDate);
        boolean recordFound = best != null;
        boolean descVisible = best != null && cardHasDescriptionMarker(best, descriptionMarker);
        boolean teamsMatch =
                best != null
                        && best.rawText().contains(homeTeam)
                        && best.rawText().contains(awayTeam);
        boolean official = list.pageShowsOfficialMatchType()
                || (best != null && best.rawText().contains("رسمية"));
        String analysisId = best == null ? "" : extractAnalysisIdFromSnapshot(best, list);
        String status = best == null ? "" : best.status();

        boolean persistent = false;
        if (recordFound) {
            list.refresh().waitUntilListingResultsStable();
            if (!descriptionMarker.isBlank()) {
                list.searchByMatchName("20261017-171620");
                list.waitUntilListingResultsStable();
            }
            persistent =
                    list.pageSourceContainsFixture(descriptionMarker)
                            || !list.findVisibleCardsContaining(descriptionMarker).isEmpty()
                            || (analysisId != null
                                    && !analysisId.isBlank()
                                    && list.containsAnalysisIdentityMarker(analysisId));
        }

        String mapping =
                targetExternalEventId != null && !targetExternalEventId.isBlank()
                        ? "MAPPING_UNVERIFIED; scheduleExternalEventId="
                                + targetExternalEventId
                                + "; analysisId="
                                + nullToEmpty(analysisId)
                        : "MAPPING_UNVERIFIED";

        String summary =
                "recordFound="
                        + recordFound
                        + "; descVisible="
                        + descVisible
                        + "; teamsMatch="
                        + teamsMatch
                        + "; analysisId="
                        + nullToEmpty(analysisId)
                        + "; status="
                        + status
                        + "; persistent="
                        + persistent
                        + "; searches="
                        + searches;

        return new PostSubmitReadOnlyVerificationResult(
                descriptionMarker,
                targetExternalEventId,
                homeTeam,
                awayTeam,
                matchDate,
                List.copyOf(searches),
                recordFound,
                descVisible,
                teamsMatch,
                official,
                nullToEmpty(analysisId),
                status,
                best == null ? "" : truncate(best.rawText(), 400),
                persistent,
                mapping,
                summary);
    }

    private static AnalysesListCardSnapshot selectBestMatch(
            List<AnalysesListCardSnapshot> cards,
            String marker,
            String home,
            String away,
            String date) {
        if (cards.isEmpty()) {
            return null;
        }
        for (AnalysesListCardSnapshot card : cards) {
            if (cardHasDescriptionMarker(card, marker)) {
                return card;
            }
        }
        for (AnalysesListCardSnapshot card : cards) {
            if (card.rawText().contains(home) && card.rawText().contains(away)) {
                if (date == null || date.isBlank() || card.rawText().contains(date)) {
                    return card;
                }
            }
        }
        return cards.getFirst();
    }

    private static boolean cardHasDescriptionMarker(AnalysesListCardSnapshot card, String marker) {
        return marker != null
                && !marker.isBlank()
                && card.rawText().contains(marker.trim());
    }

    private static List<AnalysesListCardSnapshot> dedupeSnapshots(List<AnalysesListCardSnapshot> input) {
        List<AnalysesListCardSnapshot> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (AnalysesListCardSnapshot snap : input) {
            String key = snap.rawText();
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            out.add(snap);
        }
        return out;
    }

    private String extractAnalysisIdFromSnapshot(AnalysesListCardSnapshot card, AnalysesListPage list) {
        Matcher inText = ANALYSIS_PATH_ID.matcher(card.rawText());
        if (inText.find()) {
            return inText.group(1);
        }
        Optional<Long> fromNav = list.tryReadAnalysisIdFromFirstMatchingCardLink(card.matchTitle());
        if (fromNav.isPresent()) {
            return String.valueOf(fromNav.get());
        }
        Long urlId = CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl());
        return urlId == null ? "" : String.valueOf(urlId);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
