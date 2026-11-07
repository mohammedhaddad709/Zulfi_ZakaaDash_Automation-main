package com.taf.systems.zulfi.validations;

/** US-FAP-ANL-MATCH-TYPE — expected Arabic validation and empty-state copy. */
public final class MatchTypeValidationMessages {

    public static final String MATCH_TYPE_REQUIRED = "يرجى اختيار نوع المباراة.";
    public static final String OFFICIAL_MATCH_REQUIRED = "يرجى اختيار المباراة.";
    public static final String NO_ELIGIBLE_OFFICIAL_MATCHES =
            "لا توجد مباريات رسمية متاحة للتحليل حالياً.";
    public static final String OFFICIAL_MATCH_NO_LONGER_ELIGIBLE =
            "هذه المباراة لم تعد متاحة لبدء تحليل جديد. يرجى اختيار مباراة أخرى أو تحديث القائمة.";

    private MatchTypeValidationMessages() {}
}
