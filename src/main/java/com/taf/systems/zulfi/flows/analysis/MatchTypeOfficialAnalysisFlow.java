package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-ANL-MATCH-TYPE — generic New Match Analysis form (Home entry). */
public final class MatchTypeOfficialAnalysisFlow {

    private final WebDriver driver;

    public MatchTypeOfficialAnalysisFlow(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Open «تحليل مباراة جديدة» from Home")
    public CreateAnalysisPage openNewMatchAnalysisFromHome() {
        new HomePage(driver).awaitReady().clickStartAnalysis();
        return new CreateAnalysisPage(driver).waitForCreatePopupReady();
    }

    @Step("User selects match type «رسمية»")
    public CreateAnalysisPage selectOfficialMatchTypeExplicitly(CreateAnalysisPage form) {
        return form.selectMatchTypeOfficialByUser();
    }

    @Step("User selects match type «ودية»")
    public CreateAnalysisPage selectFriendlyMatchTypeExplicitly(CreateAnalysisPage form) {
        return form.selectMatchTypeFriendlyByUser();
    }
}
