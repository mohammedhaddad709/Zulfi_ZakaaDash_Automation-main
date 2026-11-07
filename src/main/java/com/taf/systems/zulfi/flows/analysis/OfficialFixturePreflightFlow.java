package com.taf.systems.zulfi.flows.analysis;

import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import com.taf.systems.zulfi.pages.HomePage;
import com.taf.systems.zulfi.pages.analysis.AnalysesListPage;
import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import org.openqa.selenium.WebDriver;

import java.util.List;

/** Read-only navigation for official fixture preflight — never submits analysis. */
public final class OfficialFixturePreflightFlow {

    private final WebDriver driver;

    public OfficialFixturePreflightFlow(WebDriver driver) {
        this.driver = driver;
    }

    public HomePage verifyHomeReady() {
        return new HomePage(driver).awaitReady();
    }

    public CreateAnalysisPage openCreateAnalysisPopupFromHome() {
        CreateAnalysisPage form = new CreateAnalysisPage(driver);
        if (form.isCreatePopupDisplayed() && form.isDialogTitleVisible()) {
            return form.waitForCreatePopupReady();
        }
        new HomePage(driver).awaitReady().clickStartAnalysis();
        form = new CreateAnalysisPage(driver).waitForCreatePopupReady();
        if (!form.isDialogTitleVisible()) {
            throw new IllegalStateException("Create Analysis dialog title not visible");
        }
        return form;
    }

    public CreateAnalysisPage selectOfficialMatchType(CreateAnalysisPage form) {
        form.ensureMatchTypeOfficial();
        if (!form.isOfficialMatchSelectorVisible()) {
            throw new IllegalStateException("Official match selector not visible after selecting رسمية");
        }
        return form;
    }

    public List<OfficialFixtureOptionSnapshot> inspectOfficialFixtures(
            CreateAnalysisPage form, int scrollPasses) {
        return form.collectOfficialFixtureOptionsReadOnly(scrollPasses);
    }

    public AnalysesListPage openAnalysesListFromUi(CreateAnalysisPage form) {
        form.closeCreateAnalysisDialogIfOpen();
        return new AnalysesListPage(driver).openViaSidebarNavigation();
    }

    public String analysesEvidenceForFixture(String fixtureLabel) {
        AnalysesListPage list = new AnalysesListPage(driver).openDirectly();
        return list.searchEvidenceSummary(fixtureLabel);
    }
}
