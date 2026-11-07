package com.taf.systems.zulfi.reporting;

import com.taf.core.pacing.ExecutionPace;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.WebDriver;

/** Per-test execution metadata for reporting (ThreadLocal, one test method at a time). */
public final class TestExecutionContext {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private TestExecutionContext() {}

    public static void begin(TestCaseMetadata metadata) {
        CONTEXT.set(new Context(metadata));
    }

    /** Backward-compatible entry used by legacy listener paths. */
    public static void start(String testCaseId, String title, String feature) {
        begin(TestCaseMetadata.minimal(testCaseId, title, feature, ""));
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Context current() {
        return CONTEXT.get();
    }

    public static Optional<TestCaseMetadata> metadata() {
        Context ctx = CONTEXT.get();
        return ctx == null ? Optional.empty() : Optional.of(ctx.metadata());
    }

    public static String testCaseId() {
        return metadata().map(TestCaseMetadata::id).orElse("UNKNOWN");
    }

    public static int nextStepNumber() {
        Context ctx = requireContext();
        return ctx.nextStepNumber();
    }

    public static void recordActiveStep(String stepTitle) {
        Context ctx = CONTEXT.get();
        if (ctx != null) {
            ctx.lastStepTitle = stepTitle;
        }
    }

    public static String lastStepTitle() {
        Context ctx = CONTEXT.get();
        return ctx == null || ctx.lastStepTitle == null || ctx.lastStepTitle.isBlank()
                ? "Unnamed step"
                : ctx.lastStepTitle;
    }

    public static void markSetupFailure() {
        Context ctx = CONTEXT.get();
        if (ctx != null) {
            ctx.setupFailure = true;
        }
    }

    public static boolean setupFailure() {
        Context ctx = CONTEXT.get();
        return ctx != null && ctx.setupFailure;
    }

    public static void registerDriver(WebDriver driver) {
        Context ctx = CONTEXT.get();
        if (ctx != null) {
            ctx.driver = driver;
        }
    }

    public static Optional<WebDriver> driver() {
        Context ctx = CONTEXT.get();
        return ctx == null || ctx.driver == null ? Optional.empty() : Optional.of(ctx.driver);
    }

    /** One executed step as reported (used verbatim as "Steps to reproduce" in the bug report). */
    public record StepRecord(
            int number, String title, String action, String expected, String actual, String status,
            long durationMs, Path screenshot) { }

    public static void recordStep(StepRecord step) {
        Context ctx = CONTEXT.get();
        if (ctx != null) ctx.steps.add(step);
    }

    public static List<StepRecord> steps() {
        Context ctx = CONTEXT.get();
        return ctx == null ? List.of() : List.copyOf(ctx.steps);
    }

    /** The first failed step is the primary failure; later failed checks are listed separately in the report. */
    public static Optional<StepRecord> failedStep() {
        return steps().stream().filter(s -> "FAIL".equals(s.status())).findFirst();
    }

    public static List<StepRecord> failedSteps() {
        return steps().stream().filter(s -> "FAIL".equals(s.status())).toList();
    }

    /** Explicit classification set by the step that failed; wins over message heuristics. */
    public static void classifyFailure(DefectClassification classification, String reason) {
        Context ctx = CONTEXT.get();
        if (ctx != null && ctx.classification == null) {
            ctx.classification = classification;
            ctx.classificationReason = reason;
        }
    }

    public static Optional<DefectClassification> explicitClassification() {
        Context ctx = CONTEXT.get();
        return ctx == null ? Optional.empty() : Optional.ofNullable(ctx.classification);
    }

    public static String classificationReason() {
        Context ctx = CONTEXT.get();
        return ctx == null || ctx.classificationReason == null ? "" : ctx.classificationReason;
    }

    /** Concrete data used by the test (match id, clubs, URLs...) for reproduction. Never put secrets here. */
    public static void recordTestData(String key, String value) {
        Context ctx = CONTEXT.get();
        if (ctx != null && key != null) ctx.testData.put(key, value == null ? "" : value);
    }

    public static Map<String, String> testData() {
        Context ctx = CONTEXT.get();
        return ctx == null ? Map.of() : Collections.unmodifiableMap(ctx.testData);
    }

    public static void markSpecificationPublished() {
        Context ctx = CONTEXT.get();
        if (ctx != null) ctx.specificationPublished = true;
    }

    public static boolean specificationPublished() {
        Context ctx = CONTEXT.get();
        return ctx != null && ctx.specificationPublished;
    }

    private static Context requireContext() {
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("Test execution context was not initialized for this thread");
        }
        return ctx;
    }

    public static final class Context {
        private final TestCaseMetadata metadata;
        private int stepNumber;
        private String lastStepTitle;
        private boolean setupFailure;
        private WebDriver driver;
        private Integer fixtureIndex;
        private String fixtureLabel;
        private String matchId;
        private String externalEventId;
        private final List<StepRecord> steps = new ArrayList<>();
        private final Map<String, String> testData = new LinkedHashMap<>();
        private DefectClassification classification;
        private String classificationReason;
        private boolean specificationPublished;

        private Context(TestCaseMetadata metadata) {
            this.metadata = metadata;
        }

        public void bindFixtureContext(
                int index, String fixtureLabel, String matchId, String externalEventId) {
            this.fixtureIndex = index;
            this.fixtureLabel = fixtureLabel;
            this.matchId = matchId;
            this.externalEventId = externalEventId;
        }

        public TestCaseMetadata metadata() {
            return metadata;
        }

        public String testCaseId() {
            return metadata.id();
        }

        public String title() {
            return metadata.title();
        }

        public String feature() {
            return metadata.feature();
        }

        public Integer fixtureIndex() {
            return fixtureIndex;
        }

        public String fixtureLabel() {
            return fixtureLabel;
        }

        public String matchId() {
            return matchId;
        }

        public String externalEventId() {
            return externalEventId;
        }

        public String executionPace() {
            return ExecutionPace.current().name();
        }

        public int nextStepNumber() {
            return ++stepNumber;
        }
    }
}
