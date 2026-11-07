package com.taf.systems.zakaadash.tests.preprocessing.review;

import com.taf.systems.zakaadash.base.ZakaaDashBaseTest;
import com.taf.systems.zakaadash.flows.preprocessing.AdvancedPreprocessingFlow;
import com.taf.systems.zakaadash.pages.preprocessing.AdvancedPreprocessingWizard;
import com.taf.systems.zakaadash.pages.preprocessing.DataReviewStep;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.taf.systems.zakaadash.tests.TestGroups.FEATURE_PREPROCESSING;
import static com.taf.systems.zakaadash.tests.TestGroups.PP_REVIEW;
import static com.taf.systems.zakaadash.tests.TestGroups.REGRESSION;

/** Step 3 structure only; type-review rules need approved cases first. */
@Epic("ZakaaDash")
@Feature("Preprocessing")
public class DataReviewTests extends ZakaaDashBaseTest {

    @Test(groups = {FEATURE_PREPROCESSING, PP_REVIEW, REGRESSION},
            description = "PP-REV-001: Step 3 lists every column of the source file")
    @TmsLink("PP-REV-001")
    @Story("Step 3 lists every column of the source file")
    @Severity(SeverityLevel.NORMAL)
    public void reviewStepListsEverySourceColumn() {
        PreprocessingCase baseline = PreprocessingCase.load(TestDataUtil.PREPROCESSING_BASELINE_CASE);
        List<String> expectedColumns = csvHeader(baseline.datasetFile());
        AdvancedPreprocessingFlow flow = new AdvancedPreprocessingFlow(driver());
        AdvancedPreprocessingWizard wizard = flow.openAdvancedPreprocessing(baseline);
        flow.continueFromSettings(wizard);

        DataReviewStep review = flow.continueFromSuggestions(wizard);

        BusinessStep.run("Advanced Preprocessing Wizard", "Verify review table columns", () ->
                Assert.assertEquals(review.getColumnTitles(), expectedColumns,
                        "Step 3 should list the source columns in file order"));
    }

    private static List<String> csvHeader(Path csvFile) {
        try {
            return Arrays.stream(Files.readAllLines(csvFile).get(0).split(",")).map(String::trim).toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read the header of " + csvFile, ex);
        }
    }
}
