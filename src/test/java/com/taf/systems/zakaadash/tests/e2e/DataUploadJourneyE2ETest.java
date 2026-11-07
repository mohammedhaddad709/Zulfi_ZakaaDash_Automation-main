package com.taf.systems.zakaadash.tests.e2e;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DataVersionsFlow;
import com.taf.systems.zakaadash.flows.UploadFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.UploadDataModal;
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

import java.nio.file.Path;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.E2E;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

@Epic("ZakaaDash")
@Feature("Data Upload Journey")
public class DataUploadJourneyE2ETest extends ZakaaDashBaseTest {

    @Test(groups = {E2E, REGRESSION, CRITICAL},
            description = "E2E-DV-001: Authenticated user can prepare a valid CSV for upload")
    @TmsLink("E2E-DV-001")
    @Story("Authenticated user can prepare a valid CSV for upload")
    @Severity(SeverityLevel.CRITICAL)
    public void authenticatedUserCanPrepareValidCsvForUpload() {
        Path baselineCsv = TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV);
        String fileName = baselineCsv.getFileName().toString();

        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();
        UploadDataModal uploadModal = new DataVersionsFlow().openUploadModal(dataVersionsPage);
        new UploadFlow().selectFileSourceAndAttach(uploadModal, TestDataUtil.uniqueName("AUTO_UPLOAD_E2E"), baselineCsv);

        BusinessStep.run("Upload Data modal", "Verify file recognition", () ->
                Assert.assertTrue(uploadModal.isFileRecognized(fileName), "UI should recognize " + fileName));
    }
}
