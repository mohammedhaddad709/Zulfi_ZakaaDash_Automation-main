package com.taf.systems.zulfi.validations;

/** Arabic copy for US-FAP-HOME-001 (Home overview). */
public final class HomeOverviewMessages {

    // Hero
    // Product copy updated before 2026-10-22 run (was «حلّل مباراتك …»); see docs/testing/UI_COPY_CHANGELOG.md
    public static final String HERO_TITLE = "حلّل مبارياتك واكتشف تفاصيل الأداء";
    // Core sentence; live UI dropped the trailing period and prefixes «البيانات الأساسية لبدء التحليل» (logged as copy observation).
    public static final String HERO_DESCRIPTION =
            "أضف فيديو المباراة وابدأ التحليل للحصول على النتائج ولوحة الأداء";
    public static final String HERO_PRIMARY_ACTION = "ابدأ التحليل";
    public static final String HERO_SECONDARY_ACTION = "استعراض التحليلات";

    // Analysis summary KPI labels
    public static final String KPI_TOTAL_ANALYSES = "إجمالي التحليلات";
    public static final String KPI_IN_PROGRESS = "قيد التحليل";
    public static final String KPI_COMPLETED = "اكتمل التحليل";
    public static final String KPI_PLAYER_COUNT = "عدد اللاعبين";
    public static final String KPI_TOP_SCORER = "هداف الفريق";

    // Upcoming match
    public static final String UPCOMING_SECTION_TITLE_SINGULAR = "المباراة القادمة";
    public static final String UPCOMING_SECTION_TITLE_PLURAL = "المباريات القادمة";
    public static final String UPCOMING_EMPTY = "لا توجد مباراة قادمة مجدولة حالياً.";
    public static final String UPCOMING_LOAD_ERROR = "تعذر تحميل بيانات المباراة القادمة. حاول مرة أخرى.";

    // Recent analyses
    public static final String RECENT_SECTION_TITLE = "آخر التحليلات";
    public static final String RECENT_VIEW_MORE = "عرض المزيد";
    public static final String RECENT_EMPTY_TITLE = "لا توجد تحليلات حتى الآن";
    public static final String RECENT_EMPTY_DESCRIPTION =
            "ابدأ بتحليل أول مباراة لك وستظهر أحدث تحليلاتك هنا.";
    public static final String RECENT_LOAD_ERROR = "تعذر تحميل آخر التحليلات. حاول مرة أخرى.";

    // Summary error
    public static final String SUMMARY_LOAD_ERROR = "تعذر تحميل ملخص التحليلات. حاول مرة أخرى.";

    // Card actions & status
    public static final String STATUS_IN_PROGRESS = "قيد التحليل";
    public static final String STATUS_COMPLETED = "اكتمل التحليل";
    public static final String ACTION_VIEW_DETAILS = "عرض التفاصيل";
    public static final String ACTION_VIEW_DASHBOARD = "عرض لوحة التحليل";
    public static final String RETRY_ACTION = "إعادة المحاولة";

    // Navigation targets
    public static final String ANALYSES_PAGE_NAV_LABEL = "التحليلات";

    // First release: no delete on Home cards
    public static final String DELETE_ACTION = "حذف";

    private HomeOverviewMessages() {}
}
