# UI Copy Changelog — expected Arabic copy updated to the live product

Expected-copy constants are updated **only** when the element still exists and only its wording changed (the
assertion stays exact). Elements the product **removed or moved** are *not* adapted — those tests keep failing and
are listed under «Requirement drift» in [`docs/clubdata/DEFECTS_2026-10-22.md`](../clubdata/DEFECTS_2026-10-22.md)
until the product owner confirms the new behaviour.

| Date | Constant | Old (story / earlier DOM) | New (live UI) | Tests |
|------|----------|---------------------------|---------------|-------|
| 2026-10-22 | `HomeOverviewMessages.HERO_TITLE` | حلّل مباراتك واكتشف تفاصيل الأداء | حلّل **مبارياتك** واكتشف تفاصيل الأداء | HOME-001-003/004 |
| 2026-10-22 | `HomeOverviewMessages.HERO_DESCRIPTION` | أضف فيديو المباراة وابدأ التحليل للحصول على النتائج ولوحة الأداء. | same sentence **without the final period**; the UI now prefixes it with «البيانات الأساسية لبدء التحليل» (logged as observation OBS-1) | HOME-001-005 |
| 2026-10-22 | `HomeCurrentResultsMessages.SECTION_TITLE` | نتائج الفريق الحالية | نتائج الفريق **للموسم الحالي** | HOME-RES-001/009/014 |
| 2026-10-22 | `MatchSchedulePageMessages.CLUB_FILTER_LABEL` | النادي | الفريق (filter placeholder) | HOME-RES-009, MCH-SCHED filter cases |
| 2026-10-22 | `AnalysesListingMessages.FILTER_STATUS_LABEL` | الحالة | حالة التحليل (select placeholder) | ANL-LST-029/043 |
| 2026-10-22 | `AnalysesListingMessages.FILTER_VIDEO_SOURCE_LABEL` | مصدر الفيديو | نوع الفيديو (select placeholder) | ANL-LST-034 |
| 2026-10-22 | `AnalysesListingMessages.FILTER_MATCH_TYPE_LABEL` | — | نوع المباراة (new toolbar select) | locator order only |
| 2026-10-22 | `CreateAnalysisPage` friendly name field | `#analysis-match-name` wrapper | `input#analysis-name` | ANL-FRD-* |
| 2026-10-22 | `AnalysisProcessingStatusMessages.PROCESSING_TITLE` | التحليل قيد التنفيذ | النتائج قيد التجهيز | ANL-STS-013 |
| 2026-10-22 | `AnalysisProcessingStatusMessages.PROCESSING_DESCRIPTION` | نعمل حالياً على تحليل المباراة. ستظهر النتائج ولوحة التحليل هنا بعد اكتمال العملية. | نعمل حاليًا على تحليل الفيديو وتجهيز النتائج. ستظهر هذه البيانات تلقائيًا بعد اكتمال المعالجة. | ANL-STS-014 |

## Assertion corrections (stricter, not weaker)

| Date | Where | Change |
|------|-------|--------|
| 2026-10-22 | `MatchSchedulePage.clubFilterSelectionContains` | Removed the page-source fallback: the page header always contains «نادي الزلفي», so the check passed even with no filter applied (HOME-RES-009 was a false PASS). |
| 2026-10-22 | `MatchSchedulePage.CLUB_FILTER_COMBO` | Locates the last select of the filter toolbar (the old label-based XPath no longer matched). |
| 2026-10-22 | `AnalysesListPage.clickFirstVisibleViewDetails`, `HomeRecentAnalysesSection` | «No enabled عرض التفاصيل» is now an `AssertionError` (PRODUCT_DEFECT per ANL-003 AC-10) instead of an `IllegalStateException` (was mis-classified AUTOMATION_DEFECT). |
| 2026-10-22 | `MatchScheduleRowRules` / `HomeResultCardRules` score pattern | Hyphen-only scores: «12:30» kick-off times were parsed as scores, so every upcoming fixture counted as played and MCH-SCHED/ANL-SYNC skipped as «no unplayed fixtures». |
| 2026-10-22 | `MatchSchedulePage.waitForScheduleRowsSettled` | Waits for rendered rows; the transient «لا توجد مباريات» flash no longer ends the wait (false «No parseable schedule rows» skips). |
| 2026-10-22 | ANL-FRD-007 | The date field accepts calendar picks only; a future day that is **disabled** in the calendar is recorded as the rejection (prevention), otherwise the validation message is still required. |
