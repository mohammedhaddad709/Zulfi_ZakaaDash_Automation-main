package com.taf.systems.zulfi.validations;

/** US-FAP-ANL-SYNC — analysis status labels and schedule row actions (Arabic). */
public final class MatchScheduleAnalysisStatusMessages {

    public static final String STATUS_NOT_ANALYZED = "غير محللة";
    public static final String STATUS_IN_PROGRESS = "قيد التحليل";
    public static final String STATUS_COMPLETED = "اكتمل التحليل";

    /** Product aria-label / visible copy on Yellow League schedule (PrimeVue link). */
    public static final String ACTION_START_ANALYSIS = "بدء تحليل المباراة";

    /** Substring retained for AC wording and cross-story assertions. */
    public static final String ACTION_START_ANALYSIS_FRAGMENT = "تحليل المباراة";

    /** Exact message the story requires when a stale form is submitted for an already analysed match. */
    public static final String DUPLICATE_ALREADY_STARTED =
            "تم بدء تحليل هذه المباراة بالفعل. حدّث الصفحة لعرض حالتها الحالية.";

    public static final String ACTION_VIEW_DETAILS = "عرض التفاصيل";
    public static final String ACTION_VIEW_DASHBOARD = "عرض لوحة التحليل";

    private MatchScheduleAnalysisStatusMessages() {}

    public static boolean isStartAnalysisActionLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String trimmed = label.trim();
        return trimmed.contains(ACTION_START_ANALYSIS_FRAGMENT)
                || trimmed.equals(ACTION_START_ANALYSIS);
    }

    public static boolean isKnownAnalysisStatus(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.trim();
        return normalized.contains(STATUS_NOT_ANALYZED)
                || normalized.contains(STATUS_IN_PROGRESS)
                || normalized.contains(STATUS_COMPLETED);
    }

    /** After submit the schedule row must leave «غير محللة» and show an in-flight or done state. */
    public static boolean isPostSubmitAnalysisStatus(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.trim();
        return normalized.contains(STATUS_IN_PROGRESS) || normalized.contains(STATUS_COMPLETED);
    }

    public static boolean stillNotAnalyzed(String text) {
        return text != null && text.contains(STATUS_NOT_ANALYZED);
    }
}
