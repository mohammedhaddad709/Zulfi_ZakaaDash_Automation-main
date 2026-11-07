package com.taf.systems.zulfi.validations;

/** Arabic copy for US-FAP-ANL-003 (Analyses list browse / search / filter). */
public final class AnalysesListingMessages {

    public static final String PAGE_TITLE = "التحليلات";
    public static final String PAGE_DESCRIPTION =
            "استعرض تحليلات مبارياتك وتابع حالة كل تحليل ونتائجه.";

    public static final String PRIMARY_CTA = "تحليل مباراة جديدة";

    public static final String SEARCH_PLACEHOLDER = "ابحث باسم المباراة";

    public static final String FILTER_DATE_LABEL = "التاريخ";
    public static final String FILTER_FROM_DATE = "من تاريخ";
    public static final String FILTER_TO_DATE = "إلى تاريخ";
    // Product renamed the toolbar selects (see docs/testing/UI_COPY_CHANGELOG.md): «الحالة» → «حالة التحليل»,
    // «مصدر الفيديو» → «نوع الفيديو»; «نوع المباراة» was added.
    public static final String FILTER_STATUS_LABEL = "حالة التحليل";
    public static final String FILTER_MATCH_TYPE_LABEL = "نوع المباراة";
    public static final String FILTER_VIDEO_SOURCE_LABEL = "نوع الفيديو";
    public static final String FILTER_ALL = "الكل";

    public static final String RESET_FILTERS = "إعادة تعيين الفلاتر";

    public static final String VIDEO_SOURCE_YOUTUBE = "YouTube";
    public static final String VIDEO_SOURCE_GOOGLE_DRIVE = "Google Drive";

    public static final String EMPTY_TITLE = "لا توجد تحليلات حتى الآن";
    public static final String EMPTY_DESCRIPTION =
            "ابدأ بتحليل أول مباراة لك وستظهر تحليلاتك هنا.";

    public static final String NO_RESULTS_TITLE = "لا توجد نتائج مطابقة";
    public static final String NO_RESULTS_DESCRIPTION = "جرّب تعديل البحث أو عوامل التصفية.";

    public static final String LOAD_ERROR = "تعذر تحميل التحليلات. حاول مرة أخرى.";
    public static final String RETRY_ACTION = HomeOverviewMessages.RETRY_ACTION;

    public static final String DATE_RANGE_VALIDATION =
            "يجب أن يكون تاريخ النهاية مساوياً لتاريخ البداية أو لاحقاً له.";

    // Re-export shared lifecycle / card actions
    public static final String STATUS_IN_PROGRESS = AnalysisProcessingStatusMessages.STATUS_IN_PROGRESS;
    public static final String STATUS_COMPLETED = AnalysisProcessingStatusMessages.STATUS_COMPLETED;
    public static final String ACTION_VIEW_DETAILS = AnalysisProcessingStatusMessages.ACTION_VIEW_DETAILS;
    public static final String ACTION_VIEW_DASHBOARD = AnalysisProcessingStatusMessages.ACTION_VIEW_DASHBOARD;
    public static final String DELETE_ACTION = AnalysisProcessingStatusMessages.DELETE_ACTION;

    private AnalysesListingMessages() {}
}
