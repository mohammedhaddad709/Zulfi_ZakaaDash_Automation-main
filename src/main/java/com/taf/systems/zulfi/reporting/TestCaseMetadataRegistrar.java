package com.taf.systems.zulfi.reporting;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.testng.ITestResult;

/** Applies standardized Allure labels and attachments from {@link TestCaseReport} or @Test description. */
public final class TestCaseMetadataRegistrar {

    private TestCaseMetadataRegistrar() {}

    public static TestCaseMetadata register(ITestResult result) {
        TestCaseReport report = findReport(result);
        String feature = featureName(result);
        TestCaseMetadata metadata =
                report != null
                        ? TestCaseMetadata.fromAnnotation(report)
                        : TestCaseMetadata.fromDescription(
                                result.getMethod().getDescription(), result.getName(), feature);

        if (TestExecutionContext.metadata().isEmpty()) {
            TestExecutionContext.begin(metadata);
        }

        Allure.label("testCaseId", metadata.id());
        Allure.label("testTitle", metadata.title());
        Allure.label("feature", metadata.feature());
        if (!metadata.testType().isBlank()) {
            Allure.label("testType", metadata.testType());
        }
        if (!metadata.allureSeverity().isBlank()) {
            Allure.label("severity", metadata.allureSeverity());
        }
        if (!metadata.userStoryId().isBlank()) {
            Allure.label("story", metadata.userStoryId());
        }
        if (!metadata.requirementId().isBlank()) {
            Allure.label("tag", "requirement:" + metadata.requirementId());
        }
        metadata.acceptanceCriteria().forEach(ac -> Allure.label("tag", ac));
        metadata.businessRules().forEach(br -> Allure.label("tag", br));

        Allure.feature(metadata.feature());
        try {
            Allure.getLifecycle()
                    .updateTestCase(
                            testCase -> {
                                testCase.setName(metadata.id() + " — " + metadata.title());
                                if (metadata.description() != null && !metadata.description().isBlank()) {
                                    testCase.setDescriptionHtml(escapeHtml(metadata.description()));
                                }
                                List<Label> labels =
                                        testCase.getLabels() == null ? new ArrayList<>() : testCase.getLabels();
                                labels.add(new Label().setName("testCaseId").setValue(metadata.id()));
                                testCase.setLabels(labels);
                            });
        } catch (Exception ignored) {
            // Lifecycle update is best-effort when Allure test context is not yet active.
        }

        if (metadata.isFullSpecification()) {
            // One specification block per test (previously the same data was attached several times).
            Allure.addAttachment("Test case specification", "text/markdown", metadata.specificationMarkdown());
            TestExecutionContext.markSpecificationPublished();
        }
        return metadata;
    }

    private static TestCaseReport findReport(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        if (method != null && method.isAnnotationPresent(TestCaseReport.class)) {
            return method.getAnnotation(TestCaseReport.class);
        }
        Class<?> clazz = result.getTestClass().getRealClass();
        if (clazz.isAnnotationPresent(TestCaseReport.class)) {
            return clazz.getAnnotation(TestCaseReport.class);
        }
        return null;
    }

    private static String featureName(ITestResult result) {
        if (result.getTestClass() == null) {
            return "Unknown";
        }
        String name = result.getTestClass().getRealClass().getSimpleName();
        return name.endsWith("Test") ? name.substring(0, name.length() - 4) : name;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }
}
