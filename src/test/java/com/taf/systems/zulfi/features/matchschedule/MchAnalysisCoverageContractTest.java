package com.taf.systems.zulfi.features.matchschedule;

import com.taf.systems.zulfi.reporting.TestCaseReport;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the 33009 coverage honest without a browser: every acceptance criterion and business rule of the story is
 * covered by at least one annotated test, every annotated ID is in the registry, and no test lacks a full specification.
 */
public class MchAnalysisCoverageContractTest {

    /** Acceptance criteria exactly as named in the user story (names are used in {@code @TestCaseReport}). */
    private static final List<String> STORY_ACCEPTANCE_CRITERIA = List.of(
            "AC — Display Analysis Action for Eligible Match",
            "AC — Open Official Analysis Pop-up",
            "AC — Preselect Selected Match",
            "AC — Display Official Match Information",
            "AC — Do Not Request Duplicate Match Metadata",
            "AC — Complete Existing Analysis Inputs",
            "AC — Link Analysis to Official Match",
            "AC — Preserve Official Match Metadata Source",
            "AC — Prevent Duplicate Analysis",
            "AC — Concurrent Eligibility Change",
            "AC — Close Pop-up Without Submission",
            "AC — Match Identity Integrity",
            // the story's "Official Match Data Behavior" table: Match Type is set by the System, Editable = No
            "Official Match Data Behavior — Match Type is system-set and not editable");

    private static final List<String> STORY_BUSINESS_RULES = List.of(
            "BR-MCH-ANL-001", "BR-MCH-ANL-002", "BR-MCH-ANL-003", "BR-MCH-ANL-004", "BR-MCH-ANL-005",
            "BR-MCH-ANL-006", "BR-MCH-ANL-007", "BR-MCH-ANL-008", "BR-MCH-ANL-009");

    private static final Path REGISTRY = Path.of("docs/matchschedule/MCH-ANL_TEST_CASE_REGISTRY.md");

    private static List<TestCaseReport> reports() {
        List<TestCaseReport> reports = new ArrayList<>();
        for (Method method : MatchScheduleAnalysisAcceptanceTest.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(org.testng.annotations.Test.class)) {
                TestCaseReport report = method.getAnnotation(TestCaseReport.class);
                Assert.assertNotNull(report, method.getName() + " is a @Test without @TestCaseReport");
                reports.add(report);
            }
        }
        return reports;
    }

    @Test(description = "CORE-COV-001 — Every acceptance criterion of the story is covered by at least one test")
    public void everyAcceptanceCriterionIsCovered() {
        Set<String> covered = new LinkedHashSet<>();
        reports().forEach(r -> covered.addAll(List.of(r.acceptanceCriteria())));
        List<String> missing = new ArrayList<>(STORY_ACCEPTANCE_CRITERIA);
        missing.removeAll(covered);
        Assert.assertTrue(missing.isEmpty(), "Acceptance criteria without a test: " + missing);
        Set<String> unknown = new TreeSet<>(covered);
        unknown.removeAll(STORY_ACCEPTANCE_CRITERIA);
        Assert.assertTrue(unknown.isEmpty(), "Tests reference criteria that are not in the story: " + unknown);
    }

    @Test(description = "CORE-COV-002 — Every business rule of the story is covered by at least one test")
    public void everyBusinessRuleIsCovered() {
        Set<String> covered = new LinkedHashSet<>();
        reports().forEach(r -> covered.addAll(List.of(r.businessRules())));
        List<String> missing = new ArrayList<>(STORY_BUSINESS_RULES);
        missing.removeAll(covered);
        Assert.assertTrue(missing.isEmpty(), "Business rules without a test: " + missing);
    }

    @Test(description = "CORE-COV-003 — Every test has a complete specification (objective, expected result, priority, preconditions, data)")
    public void everyTestHasCompleteSpecification() {
        for (TestCaseReport r : reports()) {
            String id = r.id();
            Assert.assertFalse(r.title().isBlank(), id + " title");
            Assert.assertFalse(r.description().isBlank(), id + " description");
            Assert.assertFalse(r.testObjective().isBlank(), id + " objective");
            Assert.assertFalse(r.expectedFinalResult().isBlank(), id + " expected final result");
            Assert.assertTrue(r.priority().matches("P[123]"), id + " priority must be P1/P2/P3 but was '" + r.priority() + "'");
            Assert.assertTrue(r.preconditions().length > 0, id + " preconditions");
            Assert.assertFalse(r.testDataReference().isBlank(), id + " test data reference");
            Assert.assertTrue(r.acceptanceCriteria().length > 0, id + " must name the acceptance criteria it covers");
            Assert.assertTrue(r.userStoryId().contains("33009"), id + " must reference work item 33009");
        }
    }

    @Test(description = "CORE-COV-004 — Test IDs are unique and every one is listed in the registry")
    public void idsAreUniqueAndRegistered() throws Exception {
        Set<String> ids = new LinkedHashSet<>();
        for (TestCaseReport r : reports()) {
            Assert.assertTrue(ids.add(r.id()), "Duplicate test case id " + r.id());
        }
        String registry = Files.readString(REGISTRY);
        for (String id : ids) {
            Assert.assertTrue(registry.contains("| " + id + " |"), id + " is missing from " + REGISTRY);
        }
        // grouped IDs promised by MCH-ANL-010 must also be described in the registry
        Matcher grouped = Pattern.compile("\\[(MCH-ANL-\\d{3})]").matcher(registryDescriptionOf010());
        while (grouped.find()) {
            Assert.assertTrue(registry.contains("| " + grouped.group(1) + " |"), grouped.group(1) + " (grouped in 010) is not in the registry");
        }
    }

    private static String registryDescriptionOf010() {
        for (TestCaseReport r : reports()) {
            if ("MCH-ANL-010".equals(r.id())) return r.description();
        }
        throw new AssertionError("MCH-ANL-010 is missing");
    }
}
