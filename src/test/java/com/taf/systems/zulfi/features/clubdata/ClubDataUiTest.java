package com.taf.systems.zulfi.features.clubdata;

import com.taf.systems.zulfi.config.FeaturePhaseGate;
import com.taf.systems.zulfi.config.FeaturePhaseGate.Phase;
import com.taf.systems.zulfi.flows.clubdata.ClubDataFlow;
import com.taf.systems.zulfi.framework.AuthenticatedUiTest;
import com.taf.systems.zulfi.reporting.DefectClassification;
import com.taf.systems.zulfi.reporting.TestStepReporter;
import com.taf.systems.zulfi.reporting.TestStepReporter.StepResult;
import java.util.List;

/**
 * Base for the club-data stories (club details, standings, cross-page consistency): fresh login per test,
 * phase gate, and steps that report the expected and the observed values separately.
 */
public abstract class ClubDataUiTest extends AuthenticatedUiTest {

    protected abstract FeaturePhaseGate gate();

    protected abstract String story();

    protected abstract String feature();

    /** Flow bound to this test's fresh browser (stateless, cheap to create). */
    protected ClubDataFlow flow() {
        return new ClubDataFlow(browser());
    }

    protected void requirePhase(Phase minimum, String testCaseId, String title) {
        gate().requireAtLeast(minimum);
        TestStepReporter.publishTestMetadata(
                testCaseId,
                title,
                feature(),
                story() + " — " + title,
                "Authenticated session; Default Club «" + ClubDataFlow.defaultClub() + "»",
                gate().property() + "=" + gate().current().name().toLowerCase());
        TestStepReporter.recordTestData("defaultClub", ClubDataFlow.defaultClub());
    }

    protected void step(String testCaseId, String title, String expected, TestStepReporter.StepAction action) {
        TestStepReporter.runStep(title, title, expected, action, browser(), testCaseId);
    }

    /** Passes when {@code problems} is empty; otherwise fails as a product defect listing every problem. */
    protected static StepResult verdict(String passSummary, List<String> problems) {
        return problems.isEmpty()
                ? StepResult.pass(passSummary)
                : StepResult.fail(String.join(" ; ", problems), DefectClassification.PRODUCT_DEFECT);
    }

    protected static StepResult compare(String label, Object expected, Object actual) {
        return expected.equals(actual)
                ? StepResult.pass(label + " = " + actual)
                : StepResult.fail(label + ": expected " + expected + " but UI shows " + actual,
                        DefectClassification.PRODUCT_DEFECT);
    }
}
