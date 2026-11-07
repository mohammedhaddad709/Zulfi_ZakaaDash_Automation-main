package com.taf.systems.zakaadash.pages.preprocessing;

/** Step 4 improvement areas; UI labels are owned here only. */
public enum ImprovementTab {
    DATA_CLEANING("تنظيف البيانات"),
    MISSING_VALUES("اكمال القيم الناقصة"),
    OUTLIERS("تحليل القيم المتطرفة"),
    ENCODING("ترميز البيانات"),
    CLASSIFICATION("تصنيف"),
    NORMALIZATION("التطبيع"),
    TEXT_STANDARDIZATION("توحيد النصوص");

    private final String label;

    ImprovementTab(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
