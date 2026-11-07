package com.taf.systems.zulfi.validations;

/** US-FAP-CLUBS — expected Arabic copy for Yellow League clubs listing. */
public final class ClubsPageMessages {

    public static final String NAV_LABEL = "الأندية";
    public static final String PAGE_TITLE = "الأندية";
    // Re-baselined 2026-10-22 (docs/testing/REBASELINE_2026-10-22.md): the long description was replaced by the
    // league subtitle under «الأندية» (was «استعرض أندية دوري يلو واطّلع على معلومات كل نادٍ ولاعبيه.»).
    public static final String PAGE_DESCRIPTION = "دوري الدرجة الأولى";

    public static final String EMPTY_TITLE = "لا توجد أندية متاحة حالياً";
    public static final String EMPTY_DESCRIPTION =
            "لم يتم العثور على أندية دوري يلو للعرض في الوقت الحالي.";

    public static final String LOAD_ERROR = "تعذر تحميل بيانات الأندية. حاول مرة أخرى.";
    public static final String RETRY_ACTION = "إعادة المحاولة";

    private ClubsPageMessages() {}
}
