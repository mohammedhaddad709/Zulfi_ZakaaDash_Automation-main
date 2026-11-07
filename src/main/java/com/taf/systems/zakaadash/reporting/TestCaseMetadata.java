package com.taf.systems.zakaadash.reporting;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record TestCaseMetadata(
        String testCaseId,
        String title,
        String feature,
        String priority,
        String executionTypes,
        String testType) {

    private static final String NOT_SET = "N/A";
    private static final List<String> EXECUTION_GROUPS = List.of("smoke", "sanity", "regression", "e2e");

    public static TestCaseMetadata from(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        List<String> groups = List.of(result.getMethod().getGroups());
        boolean isE2e = groups.contains("e2e");
        boolean isFeature = groups.stream().anyMatch(group -> group.startsWith("feature-"));

        TmsLink tmsLink = method.getAnnotation(TmsLink.class);
        Story story = method.getAnnotation(Story.class);
        Feature feature = method.getAnnotation(Feature.class) != null
                ? method.getAnnotation(Feature.class)
                : method.getDeclaringClass().getAnnotation(Feature.class);
        Severity severity = method.getAnnotation(Severity.class);

        return new TestCaseMetadata(
                tmsLink != null ? tmsLink.value() : method.getName(),
                story != null ? story.value() : method.getName(),
                feature != null ? feature.value() : NOT_SET,
                severity != null ? toPriority(severity.value()) : NOT_SET,
                toExecutionTypes(groups, isFeature),
                isE2e ? "E2E" : isFeature ? "FEATURE" : NOT_SET);
    }

    // Allure has no "priority" label, so priority is carried by @Severity.
    private static String toPriority(SeverityLevel level) {
        return switch (level) {
            case BLOCKER -> "Blocker";
            case CRITICAL -> "Critical";
            case NORMAL -> "High";
            case MINOR -> "Medium";
            case TRIVIAL -> "Low";
        };
    }

    private static String toExecutionTypes(List<String> groups, boolean isFeature) {
        List<String> types = new ArrayList<>();
        if (isFeature) {
            types.add("FEATURE");
        }
        EXECUTION_GROUPS.stream()
                .filter(groups::contains)
                .map(group -> group.toUpperCase(Locale.ROOT))
                .forEach(types::add);
        return types.isEmpty() ? NOT_SET : String.join(", ", types);
    }
}
