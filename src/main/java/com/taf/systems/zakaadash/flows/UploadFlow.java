package com.taf.systems.zakaadash.flows;

import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.TestDataUtil;

import java.nio.file.Path;

public class UploadFlow {

    private static final String UPLOAD_MODAL = "Upload Data modal";

    public UploadDataModal selectFileSource(UploadDataModal modal) {
        return BusinessStep.get(UPLOAD_MODAL, "Select File source", modal::selectFileSource);
    }

    public UploadDataModal attachValidCsv(UploadDataModal modal, Path csvFile) {
        BusinessStep.recordDataset(TestDataUtil.datasetLabel(csvFile));
        return BusinessStep.get(UPLOAD_MODAL, "Attach CSV " + csvFile.getFileName(), () -> modal.attachFile(csvFile));
    }

    public UploadDataModal enterSourceName(UploadDataModal modal, String sourceName) {
        return BusinessStep.get(UPLOAD_MODAL, "Enter source name " + sourceName, () -> modal.enterSourceName(sourceName));
    }

    public void saveSource(UploadDataModal modal) {
        BusinessStep.run(UPLOAD_MODAL, "Save data source", modal::save);
        BusinessStep.run(UPLOAD_MODAL, "Wait for Upload Data modal to close", modal::waitUntilClosed);
    }

    public UploadDataModal removeAttachedFile(UploadDataModal modal) {
        return BusinessStep.get(UPLOAD_MODAL, "Remove attached file", modal::removeAttachedFile);
    }

    public void closeWithoutSaving(UploadDataModal modal) {
        BusinessStep.run(UPLOAD_MODAL, "Close Upload Data modal without saving", modal::close);
    }

    /** Product order: the source cards are enabled only after a source name is entered. */
    public UploadDataModal selectFileSourceAndAttach(UploadDataModal modal, String sourceName, Path csvFile) {
        enterSourceName(modal, sourceName);
        return attachValidCsv(selectFileSource(modal), csvFile);
    }
}
