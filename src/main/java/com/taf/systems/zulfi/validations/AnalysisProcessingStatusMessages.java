package com.taf.systems.zulfi.validations;

/** Arabic copy and labels for US-FAP-ANL-002 (analysis processing lifecycle). */
public final class AnalysisProcessingStatusMessages {

    // Lifecycle status (must match Home / List / Details)
    public static final String STATUS_IN_PROGRESS = "قيد التحليل";
    public static final String STATUS_COMPLETED = "اكتمل التحليل";

    // Breadcrumb (approved)
    public static final String BREADCRUMB_ANALYSES = "التحليلات";
    public static final String BREADCRUMB_DETAILS = "تفاصيل التحليل";

    // Processing result section
    // Product copy updated before the 2026-10-22 run (was «التحليل قيد التنفيذ» / «نعمل حالياً على تحليل المباراة…»);
    // see docs/testing/UI_COPY_CHANGELOG.md
    public static final String PROCESSING_TITLE = "النتائج قيد التجهيز";
    public static final String PROCESSING_DESCRIPTION =
            "نعمل حاليًا على تحليل الفيديو وتجهيز النتائج. ستظهر هذه البيانات تلقائيًا بعد اكتمال المعالجة.";

    // Completed outputs (details page)
    public static final String INTERACTIVE_DASHBOARD_LABEL = "لوحة التحليل التفاعلية";
    public static final String PDF_REPORT_LABEL = "تقرير التحليل الفني (PDF)";

    // Card / list actions (shared with Home)
    public static final String ACTION_VIEW_DETAILS = HomeOverviewMessages.ACTION_VIEW_DETAILS;
    public static final String ACTION_VIEW_DASHBOARD = HomeOverviewMessages.ACTION_VIEW_DASHBOARD;

    // Field labels (details metadata)
    public static final String MATCH_NAME_LABEL = "اسم المباراة";
    public static final String VIDEO_SOURCE_LABEL = "مصدر الفيديو";
    public static final String VIDEO_URL_LABEL = "رابط الفيديو";
    public static final String DESCRIPTION_LABEL = "الوصف";
    public static final String SUBMISSION_DATE_LABEL = "تاريخ الإرسال";

    // First release — must not appear on details
    public static final String FAILED_STATUS = "فشل التحليل";
    public static final String RETRY_ANALYSIS_ACTION = "إعادة التحليل";
    public static final String DELETE_ACTION = HomeOverviewMessages.DELETE_ACTION;

    // Known design-defect markers (documented, not asserted as pass)
    public static final String LEGACY_BREADCRUMB_VIDEO = "تفاصيل الفيديو";
    public static final String LEGACY_BREADCRUMB_MODELING = "نمذجة البيانات";
    public static final String LEGACY_VIDEO_NAME_LABEL = "اسم الفيديو";

    private AnalysisProcessingStatusMessages() {}
}
