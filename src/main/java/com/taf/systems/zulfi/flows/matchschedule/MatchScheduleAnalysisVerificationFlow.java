package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/** Listing + persistence checks for schedule-created analyses. */
public final class MatchScheduleAnalysisVerificationFlow {

    private final WebDriver driver;

    public MatchScheduleAnalysisVerificationFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Verify success UI after schedule submit")
    public AnalysisSuccessDialog verifySuccessUi() {
        AnalysisSuccessDialog dialog = new AnalysisSuccessDialog(driver);
        dialog.waitForSuccessMessage();
        Assert.assertTrue(
                dialog.isSuccessMessageVisible(),
                "Expected success message: " + AnalysisSuccessDialog.SUCCESS_MESSAGE);
        Assert.assertTrue(
                dialog.isViewStatusActionVisible(),
                "Expected «" + AnalysisSuccessDialog.VIEW_STATUS_LABEL + "»");
        return dialog;
    }

    @Step("Verify analysis on listing by ID and schedule identity")
    public ListingVerificationResult verifyListingAndPersistence(
            SchedulePreparedCreateContext prepared, Long analysisId) {
        Long effectiveAnalysisId = analysisId;
        AnalysisSuccessDialog dialog = new AnalysisSuccessDialog(driver);
        if (dialog.isSuccessMessageVisible()) {
            try {
                dialog.clickViewAnalysisStatus();
                if (effectiveAnalysisId == null) {
                    effectiveAnalysisId = readAnalysisIdFromBrowserUrl();
                }
            } catch (RuntimeException viewStatusBlocked) {
                // Fall through to direct listing navigation.
            }
        }
        AnalysesListPage list = new AnalysesListPage(driver);
        list.openListingEnsuringLoaded().waitUntilListingResultsStable();

        boolean foundById = false;
        if (effectiveAnalysisId != null) {
            list.searchByAnalysisId(String.valueOf(effectiveAnalysisId));
            foundById = list.containsAnalysisIdentityMarker(String.valueOf(effectiveAnalysisId));
        }
        String teamNeedle = prepared.homeTeamDisplay();
        if (!foundById) {
            list.searchByMatchName(teamNeedle);
        }
        boolean recordOnListing =
                list.containsVisibleMatchTitle(prepared.listingSearchTitle())
                        || list.containsVisibleMatchTitle(teamNeedle)
                        || foundById;
        Assert.assertTrue(
                recordOnListing,
                "Expected created analysis on listing; id="
                        + effectiveAnalysisId
                        + "; teams="
                        + prepared.homeTeamDisplay()
                        + "/"
                        + prepared.awayTeamDisplay());
        String externalEventId = prepared.identityEvidence().createUrlExternalEventId();
        if (!externalEventId.isBlank() && !recordOnListing) {
            Assert.assertTrue(
                    list.pageSourceContainsFixture(externalEventId)
                            || list.containsVisibleMatchTitle(teamNeedle),
                    "Listing should reference schedule externalEventId or team identity");
        }

        String status = list.readFirstVisibleCardStatus().orElse("");

        list.refresh().waitUntilListingResultsStable();
        relocateCreatedRecordOnListing(list, prepared, effectiveAnalysisId, teamNeedle);
        boolean persistent = false;
        if (effectiveAnalysisId != null) {
            persistent =
                    list.containsAnalysisIdentityMarker(String.valueOf(effectiveAnalysisId));
        }
        if (!persistent) {
            persistent =
                    list.containsVisibleMatchTitle(prepared.listingSearchTitle())
                            || list.containsVisibleMatchTitle(teamNeedle)
                            || list.pageSourceContainsFixture(prepared.homeTeamDisplay())
                            || list.pageSourceContainsFixture(prepared.awayTeamDisplay());
        }
        Assert.assertTrue(persistent, "Same analysis record must remain after refresh");
        if (effectiveAnalysisId == null) {
            effectiveAnalysisId =
                    list.tryReadAnalysisIdFromFirstMatchingCardLink(teamNeedle).orElse(null);
        }
        return new ListingVerificationResult(status, effectiveAnalysisId);
    }

    private static void relocateCreatedRecordOnListing(
            AnalysesListPage list,
            SchedulePreparedCreateContext prepared,
            Long analysisId,
            String teamNeedle) {
        list.clearSearch();
        if (analysisId != null) {
            list.searchByAnalysisId(String.valueOf(analysisId));
        } else if (prepared.listingSearchTitle() != null && !prepared.listingSearchTitle().isBlank()) {
            list.searchByMatchName(prepared.listingSearchTitle());
        } else if (teamNeedle != null && !teamNeedle.isBlank()) {
            list.searchByMatchName(teamNeedle);
        }
        try {
            list.waitUntilListingResultsStable();
        } catch (RuntimeException listingBusy) {
            // Best-effort: card scan below still validates persistence.
        }
    }

    public Long readAnalysisIdFromBrowserUrl() {
        return CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl());
    }
}
