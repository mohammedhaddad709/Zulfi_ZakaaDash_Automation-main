package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.flows.home.HomeOverviewFlow;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisDetailsPage;
import com.taf.systems.zulfi.pages.home.HomeRecentAnalysesSection;
import com.taf.systems.zulfi.pages.home.RecentAnalysisCardSnapshot;
import com.taf.systems.zulfi.validations.AnalysisProcessingStatusMessages;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

/** US-FAP-ANL-002 — open analysis details from Home, list, or direct id. */
public final class AnalysisStatusTrackingFlow {

    private final WebDriver driver;
    private final HomeOverviewFlow homeFlow;

    public AnalysisStatusTrackingFlow(WebDriver driver) {
        this.driver = driver;
        this.homeFlow = new HomeOverviewFlow(driver);
    }

    @Step("Open Analysis Details by numeric id")
    public AnalysisDetailsPage openDetailsById(long analysisId) {
        return new AnalysisDetailsPage(driver).openById(analysisId);
    }

    @Step("Open التحليلات list from sidebar")
    public AnalysesListPage openAnalysesList() {
        return new AnalysesListPage(driver).openViaSidebarNavigation();
    }

    @Step("Open Analysis Details from list search")
    public AnalysisDetailsPage openFirstDetailsFromListSearch(String matchNameFragment) {
        AnalysesListPage list = openAnalysesList();
        if (matchNameFragment != null && !matchNameFragment.isBlank()) {
            list.searchByMatchName(matchNameFragment);
        }
        list.clickFirstVisibleViewDetails();
        return new AnalysisDetailsPage(driver).waitUntilLoaded();
    }

    @Step("Open Analysis Details from Home recent card (first matching status)")
    public AnalysisDetailsPage openDetailsFromHomeRecentByStatus(String statusLabel) {
        HomeRecentAnalysesSection recent = homeFlow.openHomeOverview().recentAnalyses().waitUntilSectionVisible();
        List<RecentAnalysisCardSnapshot> cards = recent.readVisibleAnalysisCards();
        boolean found =
                cards.stream().anyMatch(card -> statusLabel.equals(card.status()));
        if (!found) {
            throw new IllegalStateException("No Home recent card with status «" + statusLabel + "»");
        }
        recent.clickFirstEnabledViewDetailsForStatus(statusLabel);
        return new AnalysisDetailsPage(driver).waitUntilLoaded();
    }

    @Step("Read Home recent card status for cross-surface consistency")
    public Optional<String> readFirstHomeRecentStatus() {
        HomeRecentAnalysesSection recent = homeFlow.openHomeOverview().recentAnalyses().waitUntilSectionVisible();
        List<RecentAnalysisCardSnapshot> cards = recent.readVisibleAnalysisCards();
        if (cards.isEmpty()) {
            return Optional.empty();
        }
        String status = cards.getFirst().status();
        return status.isBlank() ? Optional.empty() : Optional.of(status);
    }

    @Step("Read list card status label for same record fragment")
    public Optional<String> readListStatusForMatchFragment(String matchFragment) {
        AnalysesListPage list = openAnalysesList();
        if (matchFragment != null && !matchFragment.isBlank()) {
            list.searchByMatchName(matchFragment);
        }
        return list.readFirstVisibleCardStatus();
    }

    public String approvedStatusOrEmpty(AnalysisDetailsPage details) {
        return details.readLifecycleStatusLabel();
    }

    public boolean statusesConsistent(String homeStatus, String listStatus, String detailsStatus) {
        if (detailsStatus == null || detailsStatus.isBlank()) {
            return false;
        }
        if (!AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS.equals(detailsStatus)
                && !AnalysisProcessingStatusMessages.STATUS_COMPLETED.equals(detailsStatus)) {
            return false;
        }
        boolean homeOk = homeStatus == null || homeStatus.isBlank() || detailsStatus.equals(homeStatus);
        boolean listOk = listStatus == null || listStatus.isBlank() || detailsStatus.equals(listStatus);
        return homeOk && listOk;
    }
}
