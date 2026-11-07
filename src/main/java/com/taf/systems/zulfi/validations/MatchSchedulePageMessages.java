package com.taf.systems.zulfi.validations;

/** US-FAP-MCH — Match Schedule (Yellow League) expected Arabic copy. */
public final class MatchSchedulePageMessages {

    public static final String PAGE_TITLE = "جدول المباريات";
    public static final String PAGE_DESCRIPTION =
            "عرض مباريات دوري الصفراء للموسم الحالي مع حالة التحليل للمباريات التي أُقيمت.";

    // Product renamed the filter placeholder «النادي» → «الفريق» (see docs/testing/UI_COPY_CHANGELOG.md)
    public static final String CLUB_FILTER_LABEL = "الفريق";
    public static final String FILTER_ALL_CLUBS = "جميع الأندية";

    public static final String EMPTY_FILTER_TITLE = "لا توجد مباريات";
    public static final String EMPTY_FILTER_DESCRIPTION =
            "لم يتم العثور على مباريات مطابقة للنادي المحدد في الموسم الحالي.";

    public static final String LOAD_ERROR = "تعذر تحميل جدول المباريات. حاول مرة أخرى.";
    public static final String RETRY_ACTION = "إعادة المحاولة";

    /** Must not appear on schedule unplayed rows (fixtures use date+time instead). */
    public static final String UPCOMING_ROW_LABEL = "قادمة";

    public static final String STATUS_NOT_ANALYZED = HomeCurrentResultsMessages.STATUS_NOT_ANALYZED;
    public static final String STATUS_IN_PROGRESS = HomeCurrentResultsMessages.STATUS_IN_PROGRESS;
    public static final String STATUS_COMPLETED = HomeCurrentResultsMessages.STATUS_COMPLETED;

    public static final String FRIENDLY_MATCH_MARKER = "ودية";

    private MatchSchedulePageMessages() {}

    /** Product copy may vary slightly; keep assertions tolerant via substring checks. */
    public static String pageDescriptionFragment() {
        return "دوري الصفراء";
    }
}
