package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** US-FAP-ANL-FRIENDLY — open generic New Match Analysis and select «ودية». */
public final class FriendlyMatchAnalysisFlow {

    private final MatchTypeOfficialAnalysisFlow matchTypeFlow;

    public FriendlyMatchAnalysisFlow(WebDriver driver) {
        this.matchTypeFlow = new MatchTypeOfficialAnalysisFlow(driver);
    }

    @Step("Open «تحليل مباراة جديدة» with Match Type «ودية»")
    public CreateAnalysisPage openFriendlyMatchAnalysisForm() {
        CreateAnalysisPage form = matchTypeFlow.openNewMatchAnalysisFromHome();
        matchTypeFlow.selectFriendlyMatchTypeExplicitly(form);
        return form;
    }
}
