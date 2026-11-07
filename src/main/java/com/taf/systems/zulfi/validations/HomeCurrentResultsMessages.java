package com.taf.systems.zulfi.validations;

/** US-FAP-HOME-CURRENT-RESULTS — expected Arabic copy. */
public final class HomeCurrentResultsMessages {

    // Product copy updated before 2026-10-22 run (was «نتائج الفريق الحالية»); see docs/testing/UI_COPY_CHANGELOG.md
    public static final String SECTION_TITLE = "نتائج الفريق للموسم الحالي";
    public static final String VIEW_MORE = "عرض المزيد";
    public static final String EMPTY_TITLE = "لا توجد نتائج متاحة حالياً";
    public static final String EMPTY_DESCRIPTION =
            "لم تُسجل نتائج مباريات للفريق في الموسم الحالي حتى الآن.";
    public static final String LOAD_ERROR =
            "تعذر تحميل نتائج الفريق الحالية. حاول مرة أخرى.";
    public static final String RETRY_ACTION = "إعادة المحاولة";

    public static final String STATUS_NOT_ANALYZED = "غير محللة";
    public static final String STATUS_IN_PROGRESS = "قيد التحليل";
    public static final String STATUS_COMPLETED = "اكتمل التحليل";

    private HomeCurrentResultsMessages() {}
}
