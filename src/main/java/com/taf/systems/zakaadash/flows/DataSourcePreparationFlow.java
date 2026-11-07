package com.taf.systems.zakaadash.flows;

import com.taf.systems.zakaadash.pages.DataVersionsPage;
import com.taf.systems.zakaadash.pages.UploadDataModal;
import com.taf.systems.zakaadash.utils.BusinessStep;
import com.taf.systems.zakaadash.utils.TestDataUtil;
import com.taf.systems.zakaadash.utils.ZakaaDashLog;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test setup: turns a CSV fixture into a uniquely named data source. A case creates its source once per JVM run
 * (no cleanup exists yet); later tests of the same case reuse it after confirming it is still listed.
 */
public class DataSourcePreparationFlow {

    private static final String DATA_VERSIONS_PAGE = "Data Versions";
    private static final Map<String, String> SOURCE_NAME_BY_CASE_ID = new ConcurrentHashMap<>();

    public String prepareSource(DataVersionsPage dataVersionsPage, String caseId, Path csvFile, String sourceNamePrefix) {
        String sourceName = SOURCE_NAME_BY_CASE_ID.get(caseId);
        if (sourceName == null) {
            sourceName = TestDataUtil.uniqueName(sourceNamePrefix);
            createSource(dataVersionsPage, csvFile, sourceName);
            ZakaaDashLog.info("Original source created: " + sourceName);
            SOURCE_NAME_BY_CASE_ID.put(caseId, sourceName);
        }
        BusinessStep.recordSourceName(sourceName);
        return confirmListed(dataVersionsPage, sourceName);
    }

    private void createSource(DataVersionsPage dataVersionsPage, Path csvFile, String sourceName) {
        UploadFlow uploadFlow = new UploadFlow();
        UploadDataModal uploadModal = new DataVersionsFlow().openUploadModal(dataVersionsPage);
        uploadFlow.selectFileSourceAndAttach(uploadModal, sourceName, csvFile);
        uploadFlow.saveSource(uploadModal);
    }

    private String confirmListed(DataVersionsPage dataVersionsPage, String sourceName) {
        BusinessStep.run(DATA_VERSIONS_PAGE, "Confirm source " + sourceName + " is listed", () ->
                dataVersionsPage.waitUntilLoaded().waitForSource(sourceName));
        return sourceName;
    }
}
