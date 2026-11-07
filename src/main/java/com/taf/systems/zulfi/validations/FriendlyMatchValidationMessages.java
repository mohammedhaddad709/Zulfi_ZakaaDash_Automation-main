package com.taf.systems.zulfi.validations;

/** US-FAP-ANL-FRIENDLY — expected Arabic validation copy. */
public final class FriendlyMatchValidationMessages {

    public static final String FIELD_REQUIRED = "هذا الحقل مطلوب.";
    public static final String NAME_MIN_LENGTH = "يجب ألا يقل اسم المباراة عن 3 أحرف.";
    public static final String NAME_INVALID_CHARACTERS = "يحتوي اسم المباراة على أحرف غير مسموح بها.";
    public static final String DATE_NOT_TODAY_OR_PAST =
            "يجب أن يكون تاريخ المباراة اليوم أو تاريخاً سابقاً.";

    private FriendlyMatchValidationMessages() {}
}
