package com.taf.systems.zakaadash.tests.upload;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.AuthenticationFlow;
import com.taf.systems.zakaadash.flows.DataVersionsFlow;
import com.taf.systems.zakaadash.flows.UploadFlow;
import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.SourceGroup;
import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.pages.UploadDataModal.SummaryField;
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

import java.nio.file.Files;
import java.nio.file.Path;

import static com.taf.systems.zakaadash.tests.TestGroups.CRITICAL;
import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_UPLOAD;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;
import static com.taf.systems.zakaadash.tests.TestGroups.SANITY;

@Epic("ZakaaDash")
@Feature("Upload Data")
public class FileSelectionTests extends ZakaaDashBaseTest {

    private static final String UPLOAD_MODAL = "Upload Data modal";
    private static final String DATA_VERSIONS_PAGE = "Data Versions";
    private static final String ORIGINAL_FILE_TAG = "الملف الأصلي";
    private static final String BASELINE_ROWS = "20";
    private static final String BASELINE_COLUMNS = "9";

    @Test(groups = {FEATURE_UPLOAD, SANITY, REGRESSION, CRITICAL},
            description = "FILE-001: User can select File as upload source")
    @TmsLink("FILE-001")
    @Story("User can select File as upload source")
    @Severity(SeverityLevel.CRITICAL)
    public void userCanSelectFileAsUploadSource() {
        UploadDataModal uploadModal = openUploadModal();

        BusinessStep.run(UPLOAD_MODAL, "Verify File source offered", () ->
                Assert.assertTrue(uploadModal.isFileSourceAvailable(), "File source option should be visible"));

        UploadFlow uploadFlow = new UploadFlow();
        uploadFlow.enterSourceName(uploadModal, uploadSourceName());
        uploadFlow.selectFileSource(uploadModal);

        BusinessStep.run(UPLOAD_MODAL, "Verify File mode active", () -> {
            Assert.assertTrue(uploadModal.isFileSourceSelected(), "File source card should be marked selected");
            Assert.assertTrue(uploadModal.isFileInputAvailable(),
                    "input[type='file'] should be available after selecting File");
        });
    }

    @Test(groups = {FEATURE_UPLOAD, SANITY, REGRESSION, CRITICAL},
            description = "FILE-002: Valid baseline CSV can be attached and recognized")
    @TmsLink("FILE-002")
    @Story("Valid baseline CSV can be attached and recognized")
    @Severity(SeverityLevel.CRITICAL)
    public void validBaselineCsvCanBeAttachedAndRecognized() {
        Path baselineCsv = TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV);
        String fileName = baselineCsv.getFileName().toString();
        Assert.assertTrue(Files.isRegularFile(baselineCsv), "Baseline fixture should exist: " + baselineCsv);

        UploadDataModal uploadModal = openUploadModal();
        new UploadFlow().selectFileSourceAndAttach(uploadModal, uploadSourceName(), baselineCsv);

        BusinessStep.run(UPLOAD_MODAL, "Verify CSV recognized", () -> {
            Assert.assertTrue(uploadModal.isFileRecognized(fileName), "UI should show the attached file " + fileName);
            Assert.assertTrue(uploadModal.getSelectedFilename(fileName).contains(fileName),
                    "Selected file name evidence should contain " + fileName);
            Assert.assertTrue(uploadModal.waitForUploadSuccessMessage().isPresent(),
                    "Upload success message should be shown after file analysis");
            // Fixture contract (docs/zakaadash/zakaadash-test-coverage.md): 20 rows, 9 columns.
            Assert.assertEquals(uploadModal.getSummaryValue(SummaryField.ROW_COUNT), BASELINE_ROWS,
                    "Row count reported for " + fileName);
            Assert.assertEquals(uploadModal.getSummaryValue(SummaryField.COLUMN_COUNT), BASELINE_COLUMNS,
                    "Column count reported for " + fileName);
            Assert.assertTrue(uploadModal.isSaveEnabled(), "Save should be enabled once a valid file is analysed");
        });
    }

    private UploadDataModal openUploadModal() {
        return new DataVersionsFlow().loginAndOpenUploadModal(driver());
    }

    // The source cards are enabled only after a name is entered; nothing is saved, so no source is created.
    private static String uploadSourceName() {
        return TestDataUtil.uniqueName("AUTO_UPLOAD_FILE");
    }

    @Test(groups = {FEATURE_UPLOAD, SANITY, REGRESSION, CRITICAL},
            description = "FILE-003: Saving a valid CSV creates an original data source")
    @TmsLink("FILE-003")
    @Story("Saving a valid CSV creates an original data source")
    @Severity(SeverityLevel.CRITICAL)
    public void savingValidCsvCreatesOriginalDataSource() {
        Path baselineCsv = TestDataUtil.resolveResource(TestDataUtil.BASELINE_VALID_CSV);
        String sourceName = TestDataUtil.uniqueName("AUTO_UPLOAD_SAVE");
        BusinessStep.recordSourceName(sourceName);
        DataVersionsPage dataVersionsPage = new AuthenticationFlow(driver()).loginSuccessfully();
        UploadDataModal uploadModal = new DataVersionsFlow().openUploadModal(dataVersionsPage);
        UploadFlow uploadFlow = new UploadFlow();
        uploadFlow.selectFileSourceAndAttach(uploadModal, sourceName, baselineCsv);
        BusinessStep.run(UPLOAD_MODAL, "Wait for the file analysis", () ->
                Assert.assertTrue(uploadModal.waitForUploadSuccessMessage().isPresent(), "Upload success should be shown"));

        uploadFlow.saveSource(uploadModal);

        BusinessStep.run(DATA_VERSIONS_PAGE, "Verify the new source is listed as an original file", () -> {
            dataVersionsPage.waitUntilLoaded().waitForSource(sourceName);
            SourceGroup source = dataVersionsPage.findSourceGroup(sourceName);
            Assert.assertEquals(source.getStatusTag(), ORIGINAL_FILE_TAG, "New source status tag");
            Assert.assertEquals(source.getProcessedVersionsCount(), 0, "A new source has no processed versions");
        });
    }

    @Test(groups = {FEATURE_UPLOAD, REGRESSION},
            description = "FILE-004: Unsupported file type is rejected")
    @TmsLink("FILE-004")
    @Story("Unsupported file type is rejected")
    @Severity(SeverityLevel.NORMAL)
    public void unsupportedFileTypeIsRejected() {
        Path textFile = TestDataUtil.resolveResource(TestDataUtil.UNSUPPORTED_TYPE_TXT);
        UploadDataModal uploadModal = openUploadModal();
        // The drop zone states "يمكنك رفع ملفات CSV أو Excel أو JSON"; a .txt file must not be accepted.
        new UploadFlow().selectFileSourceAndAttach(uploadModal, uploadSourceName(), textFile);

        BusinessStep.run(UPLOAD_MODAL, "Verify the .txt file is rejected", () -> {
            String outcome = uploadModal.waitForUploadOutcome();
            Assert.assertFalse(uploadModal.isUploadSuccessShown(),
                    "Unsupported file should not be reported as uploaded; product said: " + outcome);
            Assert.assertFalse(uploadModal.isSaveEnabled(), "Save should stay disabled for an unsupported file");
        });
    }
}
