package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/** Business-level verification after analysis creation. */
public final class AnalysisVerificationFlow {

    private final WebDriver driver;

    public AnalysisVerificationFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Verify success confirmation dialog")
    public AnalysisSuccessDialog verifySuccessDialog() {
        AnalysisSuccessDialog dialog = new AnalysisSuccessDialog(driver);
        dialog.waitForSuccessMessage();
        Assert.assertTrue(dialog.isSuccessMessageVisible(), "Expected success message on dialog");
        Assert.assertTrue(dialog.isViewStatusActionVisible(), "Expected «عرض حالة التحليل» action");
        Assert.assertTrue(dialog.isCreateAnotherActionVisible(), "Expected «تحليل مباراة أخرى» action");
        return dialog;
    }

    @Step("Verify analysis appears on Analyses list and survives refresh")
    public void verifyPersistenceOnAnalysesList(String fixtureLabel, String descriptionMarker) {
        AnalysesListPage list = new AnalysesListPage(driver).openViaSidebarNavigation();
        assertListed(list, fixtureLabel, descriptionMarker);
        list.refresh();
        assertListed(list, fixtureLabel, descriptionMarker);
    }

    private void assertListed(AnalysesListPage list, String fixtureLabel, String descriptionMarker) {
        list.searchByMatchName(fixtureLabel);
        Assert.assertTrue(
                list.containsVisibleMatchTitle(fixtureLabel),
                "Expected analysis card for fixture: " + fixtureLabel);
        Assert.assertTrue(list.pageShowsOfficialMatchType(), "Expected official match type on listing");
        if (descriptionMarker != null && !descriptionMarker.isBlank()) {
            Assert.assertTrue(
                    list.containsDescriptionMarker(descriptionMarker),
                    "Expected description marker on page: " + descriptionMarker);
        }
    }

    public Long readAnalysisIdFromUrlIfPresent() {
        return CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl());
    }
}
