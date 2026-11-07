package com.taf.systems.zakaadash.pages.preprocessing;

public enum WizardStep {
    SETTINGS("إعدادات المعالجة", "Step 1 — Processing Settings"),
    SUGGESTIONS("اقتراحات النظام", "Step 2 — System Suggestions"),
    REVIEW("مراجعة البيانات", "Step 3 — Data Review"),
    IMPROVEMENT("تحسين البيانات", "Step 4 — Data Improvement"),
    RESULTS("عرض النتائج", "Step 5 — Processing Results");

    private final String title;
    private final String label;

    WizardStep(String title, String label) {
        this.title = title;
        this.label = label;
    }

    public String title() {
        return title;
    }

    public String label() {
        return label;
    }
}
