package com.taf.systems.zakaadash.tests.dataversions;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DataSourcePreparationFlow;
import com.taf.systems.zakaadash.flows.DataVersionsFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.testdata.PreprocessingCase;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.TestDataUtil;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_DATA_VERSIONS;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_UPLOAD;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;
import static com.taf.systems.zakaadash.tests.TestGroups.SMOKE;

@Epic("ZakaaDash")
@Feature("Data Versions")
public class DataVersionsTests extends ZakaaDashBaseTest {

    private static final String DATA_VERSIONS_PAGE = "Data Versions";

    @Test(groups = {FEATURE_DATA_VERSIONS, SMOKE, SANITY, REGRESSION, CRITICAL},
            description = "DV-001: Authenticated user can access Data Versions")
    @TmsLink("DV-001")
    @Story("Authenticated user can access Data Versions")
    @Severity(SeverityLevel.CRITICAL)
    public void authenticatedUserCanAccessDataVersions() {
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify Data Versions loaded", () -> {
            Assert.assertTrue(driver().getCurrentUrl().contains("/data-versions"),
                    "URL should contain /data-versions but was: " + driver().getCurrentUrl());
            Assert.assertTrue(dataVersionsPage.isLoaded(), "Data Versions UI should be loaded");
        });
    }

    @Test(groups = {FEATURE_DATA_VERSIONS, FEATURE_UPLOAD, SMOKE, SANITY, REGRESSION, CRITICAL},
            description = "DV-002: Upload Data action opens Upload Data modal")
    @TmsLink("DV-002")
    @Story("Upload Data action opens Upload Data modal")
    @Severity(SeverityLevel.CRITICAL)
    public void uploadDataActionOpensUploadDataModal() {
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify Upload CTA", () -> {
            Assert.assertTrue(dataVersionsPage.isUploadDataVisible(), "Upload CTA should be visible");
            Assert.assertTrue(dataVersionsPage.isUploadDataEnabled(), "Upload CTA should be enabled");
        });

        UploadDataModal uploadModal = new DataVersionsFlow().openUploadModal(dataVersionsPage);
        BusinessStep.run("Upload Data modal", "Verify Upload modal loaded", () ->
                Assert.assertTrue(uploadModal.isLoaded(), "Upload Data modal should be loaded"));
    }

    @Test(groups = {FEATURE_DATA_VERSIONS, REGRESSION},
            description = "DV-003: Search by source name lists only the matching source")
    @TmsLink("DV-003")
    @Story("Search by source name lists only the matching source")
    @Severity(SeverityLevel.NORMAL)
    public void searchBySourceNameListsOnlyMatchingSource() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();
        String sourceName = new DataSourcePreparationFlow().prepareSource(dataVersionsPage, baseline.testCaseId(),
                baseline.datasetFile(), baseline.sourceNamePrefix());

        BusinessStep.run(DATA_VERSIONS_PAGE, "Search for " + sourceName, () -> dataVersionsPage.searchSources(sourceName));

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify only the searched source is listed", () ->
                Assert.assertEquals(dataVersionsPage.getListedSourceNames(), List.of(sourceName),
                        "Search should list exactly the matching source"));
    }

    @Test(groups = {FEATURE_DATA_VERSIONS, REGRESSION},
            description = "DV-004: Search without matches shows the no-results message")
    @TmsLink("DV-004")
    @Story("Search without matches shows the no-results message")
    @Severity(SeverityLevel.NORMAL)
    public void searchWithoutMatchesShowsNoResultsMessage() {
        String unknownName = TestDataUtil.uniqueName("AUTO_NO_SUCH_SOURCE");
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();

        BusinessStep.run(DATA_VERSIONS_PAGE, "Search for " + unknownName, () -> dataVersionsPage.searchSources(unknownName));

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify empty search result", () -> {
            Assert.assertTrue(dataVersionsPage.isNoSearchResultsShown(unknownName),
                    "No-results message naming the search text should be shown");
            Assert.assertTrue(dataVersionsPage.getListedSourceNames().isEmpty(),
                    "No source should be listed but saw: " + dataVersionsPage.getListedSourceNames());
        });
    }
}
