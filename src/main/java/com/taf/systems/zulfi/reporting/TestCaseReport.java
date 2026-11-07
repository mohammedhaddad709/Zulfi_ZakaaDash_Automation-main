package com.taf.systems.zulfi.reporting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Standard metadata contract for Excel-aligned and manual test cases. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestCaseReport {
    String id();

    String title();

    String feature();

    String userStoryId() default "";

    String requirementId() default "";

    /** Acceptance criteria covered, by their story names (e.g. "AC Open Official Analysis Pop-up"). */
    String[] acceptanceCriteria() default {};

    /** Business rules covered (e.g. "BR-MCH-ANL-002"). */
    String[] businessRules() default {};

    String description();

    String testObjective();

    /** P1 (critical path) / P2 (important) / P3 (minor); mapped to Allure severity. */
    String priority() default "";

    String testType() default "Functional";

    String[] preconditions() default {};

    String testDataReference() default "";

    String expectedFinalResult() default "";
}
