# PRODUCT_DEFECT — Duplicate analysis error toast (i18n / UX)

**Observed:** 2026-10-17 (ANL-OFF-HP-001 authorized submit)  
**Classification:** PRODUCT_DEFECT  
**Area:** Create Analysis → Submit «بدء التحليل»

## Expected

- Arabic portal shows a **clear Arabic** message when an analysis already exists for the selected official fixture.
- Message understandable by club users **without** `externalEventId` / `matchId`.

## Actual

- Toast title: **خطأ** (Arabic)
- Toast body (English, technical):  
  `An analysis already exists for this fixture (externalEventId=16651975, matchId=6)`

## Impact

- Users cannot understand why submit failed.
- QA/automation must treat fixture as **TEST_DATA** duplicate separately from **UX** defect.

## Automation handling

- Fixture **الأخدود × نادي البكيرية** (index 0) added to `KnownFixtureExclusions` for submission.
- Happy Path default submission index **2** (or first eligible non-duplicate from manifest).
- `PostSubmitOutcomeHandler` classifies unlocalized technical duplicate toast as **PRODUCT_DEFECT** and attaches bug markdown.

## Suggested fix (product)

- Return localized Arabic message (e.g. «يوجد تحليل مسبق لهذه المباراة»).
- Log `externalEventId` / `matchId` server-side only, not in user toast.
