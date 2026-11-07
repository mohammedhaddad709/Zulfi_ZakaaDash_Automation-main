package com.taf.systems.zakaadash.flows;

import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.utils.BusinessStep;
import org.openqa.selenium.WebDriver;

public class DataVersionsFlow {

    private static final String DATA_VERSIONS_PAGE = "Data Versions";

    public UploadDataModal loginAndOpenUploadModal(WebDriver driver) {
        return openUploadModal(new AuthenticationFlow(driver).loginSuccessfully());
    }

    public UploadDataModal openUploadModal(DataVersionsPage dataVersionsPage) {
        BusinessStep.run(DATA_VERSIONS_PAGE, "Confirm Data Versions loaded", dataVersionsPage::waitUntilLoaded);
        return BusinessStep.get(DATA_VERSIONS_PAGE, "Open Upload Data modal", dataVersionsPage::openUploadDataModal);
    }
}
