package com.taf.systems.zakaadash.tests.upload;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DataVersionsFlow;
import com.taf.systems.zakaadash.flows.UploadFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.pages.UploadDataModal.Control;
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
import java.util.Arrays;
import java.util.List;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_UPLOAD;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;

@Epic("ZakaaDash")
@Feature("Upload Data")
public class UploadModalTests extends ZakaaDashBaseTest {

    private static final String UPLOAD_MODAL = "Upload Data modal";
    private static final String DATA_VERSIONS_PAGE = "Data Versions";
    private static final int SOURCE_NAME_MAX_LENGTH = 50;

    @Test(groups = {FEATURE_UPLOAD, SANITY, REGRESSION, CRITICAL},
            description = "UPLOAD-001: Upload Data modal displays required controls")
    @TmsLink("UPLOAD-001")
    @Story("Upload Data modal displays required controls")
    @Severity(SeverityLevel.CRITICAL)
    public void uploadDataModalDisplaysRequiredControls() {
        UploadDataModal uploadModal = new DataVersionsFlow().loginAndOpenUploadModal(driver());

        BusinessStep.run(UPLOAD_MODAL, "Verify required controls", () -> {
            List<Control> missingControls = Arrays.stream(Control.values())
                    .filter(control -> !uploadModal.isControlVisible(control))
                    .toList();
            Assert.assertTrue(missingControls.isEmpty(), "Upload modal controls not visible: " + missingControls);
        });
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "UPLOAD-002: Close modal without saving")
    @TmsLink("UPLOAD-002")
    @Story("Close modal without saving")
    @Severity(SeverityLevel.NORMAL)
    public void closingUploadModalWithoutSavingCreatesNoSource() {
        String sourceName = TestDataUtil.uniqueName("AUTO_UPLOAD_CLOSE");
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();
        UploadDataModal uploadModal = new DataVersionsFlow().openUploadModal(dataVersionsPage);
        UploadFlow uploadFlow = new UploadFlow();
        uploadFlow.selectFileSourceAndAttach(uploadModal, sourceName,
                TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV));

        uploadFlow.closeWithoutSaving(uploadModal);

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify modal closed and no source was created", () -> {
            Assert.assertFalse(uploadModal.isLoaded(), "Upload Data modal should be closed");
            Assert.assertTrue(dataVersionsPage.isLoaded(), "Data Versions should remain visible");
            dataVersionsPage.searchSources(sourceName);
            Assert.assertTrue(dataVersionsPage.isNoSearchResultsShown(sourceName),
                    "No data source named " + sourceName + " should exist, but listed: "
                            + dataVersionsPage.getListedSourceNames());
        });
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "UPLOAD-003: Source cards stay disabled until a source name is entered")
    @TmsLink("UPLOAD-003")
    @Story("Source cards stay disabled until a source name is entered")
    @Severity(SeverityLevel.NORMAL)
    public void sourceCardsStayDisabledUntilNameEntered() {
        UploadDataModal uploadModal = new DataVersionsFlow().loginAndOpenUploadModal(driver());

        BusinessStep.run(UPLOAD_MODAL, "Verify source cards disabled without a name", () ->
                Assert.assertTrue(uploadModal.areSourceCardsDisabled(), "Source cards should be disabled before a name"));

        new UploadFlow().enterSourceName(uploadModal, TestDataUtil.uniqueName("AUTO_UPLOAD_CARDS"));

        BusinessStep.run(UPLOAD_MODAL, "Verify source cards enabled after a name", () ->
                Assert.assertTrue(uploadModal.areSourceCardsEnabled(), "Source cards should be enabled after a name"));
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "UPLOAD-004: Source name is limited to 50 characters")
    @TmsLink("UPLOAD-004")
    @Story("Source name is limited to 50 characters")
    @Severity(SeverityLevel.MINOR)
    public void sourceNameIsLimitedToFiftyCharacters() {
        String sixtyCharacters = "AUTO_UPLOAD_LIMIT_" + "x".repeat(SOURCE_NAME_MAX_LENGTH + 10 - "AUTO_UPLOAD_LIMIT_".length());
        UploadDataModal uploadModal = new DataVersionsFlow().loginAndOpenUploadModal(driver());

        new UploadFlow().enterSourceName(uploadModal, sixtyCharacters);

        BusinessStep.run(UPLOAD_MODAL, "Verify the name was cut at 50 characters", () ->
                Assert.assertEquals(uploadModal.getSourceNameValue(), sixtyCharacters.substring(0, SOURCE_NAME_MAX_LENGTH),
                        "Source name field should keep only the first " + SOURCE_NAME_MAX_LENGTH + " characters"));
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "UPLOAD-005: Save stays disabled until a data source and a file are provided")
    @TmsLink("UPLOAD-005")
    @Story("Save stays disabled until a data source and a file are provided")
    @Severity(SeverityLevel.NORMAL)
    public void saveStaysDisabledUntilSourceAndFileProvided() {
        UploadDataModal uploadModal = new DataVersionsFlow().loginAndOpenUploadModal(driver());
        UploadFlow uploadFlow = new UploadFlow();

        BusinessStep.run(UPLOAD_MODAL, "Verify Save disabled on an empty form", () ->
                Assert.assertFalse(uploadModal.isSaveEnabled(), "Save should be disabled on an empty form"));

        uploadFlow.enterSourceName(uploadModal, TestDataUtil.uniqueName("AUTO_UPLOAD_REQUIRED"));
        // "اختر مصدر البيانات *" and "رفع البيانات *" are marked required by the product itself.
        BusinessStep.run(UPLOAD_MODAL, "Verify Save disabled with only a name", () ->
                Assert.assertFalse(uploadModal.isSaveEnabled(),
                        "Save should stay disabled while the required data source and file are missing"));

        uploadFlow.selectFileSource(uploadModal);
        BusinessStep.run(UPLOAD_MODAL, "Verify Save disabled before a file is attached", () ->
                Assert.assertFalse(uploadModal.isSaveEnabled(), "Save should stay disabled until a file is attached"));

        uploadFlow.attachValidCsv(uploadModal, TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV));
        BusinessStep.run(UPLOAD_MODAL, "Verify Save enabled after a valid file", () -> {
            Assert.assertTrue(uploadModal.waitForUploadSuccessMessage().isPresent(), "Upload success should be shown");
            Assert.assertTrue(uploadModal.isSaveEnabled(), "Save should be enabled once a valid file is analysed");
        });
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "UPLOAD-006: Removing the attached file restores the drop zone and disables Save")
    @TmsLink("UPLOAD-006")
    @Story("Removing the attached file restores the drop zone and disables Save")
    @Severity(SeverityLevel.NORMAL)
    public void removingAttachedFileRestoresDropZone() {
        Path baselineCsv = TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV);
        String fileName = baselineCsv.getFileName().toString();
        UploadDataModal uploadModal = new DataVersionsFlow().loginAndOpenUploadModal(driver());
        UploadFlow uploadFlow = new UploadFlow();
        uploadFlow.selectFileSourceAndAttach(uploadModal, TestDataUtil.uniqueName("AUTO_UPLOAD_REMOVE"), baselineCsv);
        BusinessStep.run(UPLOAD_MODAL, "Wait for the file analysis", () ->
                Assert.assertTrue(uploadModal.waitForUploadSuccessMessage().isPresent(), "Upload success should be shown"));

        uploadFlow.removeAttachedFile(uploadModal);

        BusinessStep.run(UPLOAD_MODAL, "Verify the file is gone", () -> {
            Assert.assertTrue(uploadModal.isDropZoneVisible(), "Drop zone should be shown again");
            Assert.assertFalse(uploadModal.isUploadSuccessShown(), "Upload success message should disappear");
            Assert.assertFalse(uploadModal.isFileListed(fileName), "Removed file should no longer be listed");
            Assert.assertFalse(uploadModal.isSaveEnabled(), "Save should be disabled without a file");
        });
    }
}
