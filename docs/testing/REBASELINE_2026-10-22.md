# Decision record — re-baseline of requirement-drift tests (2026-10-22)

**Decision owner:** Zulfi automation owner (instruction in session, 2026-10-22: «اعمل re-baseline لتستات الـ requirement drift
على الواجهة الجديدة … ووثّق القرار»).
**Context:** the live portal changed several approved behaviours (see
[`docs/clubdata/DEFECTS_2026-10-22.md`](../clubdata/DEFECTS_2026-10-22.md) §B). The owner accepted the new UI as the
reference for the items below. Product defects (§A of that file) are **not** re-baselined and keep failing.

## Rules applied

1. A test is re-baselined only when the same user goal is still served by the new UI; the assertion stays at least as
   strict (several became stricter — see «Strength» below).
2. Where the old behaviour no longer exists at all, the test is **retired with a visible Skip reason** (never deleted,
   never silently passed); history under `test-execution-reports/` is untouched.
3. Every change cites this file in code comments.

## Re-baselined tests

| Test | Was (story) | Now (new UI reference) | Strength |
|------|-------------|------------------------|----------|
| CLUB-003 | Description «استعرض أندية دوري يلو واطّلع على معلومات كل نادٍ ولاعبيه.» | League subtitle «دوري الدرجة الأولى» under «الأندية» | same (exact copy) |
| MCH-SCHED-003 | Description mentioning «دوري الصفراء» | Competition tabs: «دوري يلو» active by default, «كأس خادم الحرمين الشريفين» offered | same |
| ANL-LST-002 | Title + description + CTA | Title + CTA (+ create view opens); description removed | narrower (description dropped) |
| ANL-LST-029 | Status filter defaults to «الكل»; «الكل» restores all | Cleared select (placeholder «حالة التحليل») is the default; clearing restores all; status options filter cards | same + checks the cleared state after reset |
| ANL-LST-034 | Video source «الكل» option | YouTube / Google Drive filter; clearing the select = all | same |
| ANL-LST-043 | After reset the status shows «الكل» | After reset the status select is cleared; search text kept | same |
| ANL-LST-023 | «من تاريخ» / «إلى تاريخ» controls present | One «التاريخ» date filter (calendar only): placeholder exact, picking a day writes `dd/MM/yyyy`, clear empties it | **stricter** (was a page-source text check) |
| ANL-LST-024 | From/to smoke (`count >= 0`, always true) | Pick the newest card's day → every listed card was submitted that day | **stricter** (old assertion could not fail) |
| ANL-LST-027 | Inverted range shows «يجب أن يكون تاريخ النهاية…» | An inverted range cannot be entered (single date); a day without analyses lists 0 cards and shows an empty state | re-scoped (validation copy no longer reachable) |
| MCH-SCHED-013 | «جميع الأندية» option | Cleared «الفريق» select = all clubs; must list **more** rows than one club | **stricter** (was `>=`) |

Shared support: `PrimeVueDatePicker` (calendar picks; typing is ignored by the portal), `AnalysesListFiltersSection`
(`isCleared`, `pickDate`, `clearDate`; `selectStatusAll`/`selectVideoSourceAll` clear the select), `MatchSchedulePage.selectAllClubs`
(clears the team select when «جميع الأندية» is absent), `ArabicDates.parseDayMonthYear` («٢٠ أكتوبر ٢٠٢٦»).

## Result after re-baseline (live run 2026-10-22)

| Test | Result | Note |
|------|--------|------|
| CLUB-003, MCH-SCHED-003, MCH-SCHED-013, ANL-LST-002, -023, -024, -029, -043 | **PASS** | new UI behaves as re-baselined |
| ANL-LST-027, ANL-LST-034 | FAIL — **product** | re-baselined assertions hit DEF-ZUL-013 (filtered-empty list shows the first-use empty state) |
| CLUB-DET-022, CLUB-DET-023 (new) | **PASS** | |
| CLUB-INF-001…030 | SKIP (retired) | reason printed on every case |

Also adjusted while re-running: `CreateAnalysisPage` waits for the dialog **or** the `/analysis/create` page (its heading
is typeset with tatweel, located by `#analysis-create-page-title`); filter clear icons are clicked through JS (shown
on hover only).

## CLUB-INF (US-FAP-CLUB-001) → CLUB-DET (US-FAP-CLUB-DETAILS)

The sidebar «عن النادي» page was removed; the club page `/clubs/{id}` carries its content. `ClubInformationAcceptanceTest`
is **retired**: excluded from the generated suite (`tools/systems.py`), removed from Tier 1 smoke and runner presets, and
every case skips with the retirement reason unless `-Dclub.info.legacy=true` replays the legacy story.

| CLUB-INF | Coverage now |
|----------|--------------|
| 001–003 access / page / roles | CLUB-DET-001 (grid → club by ID), CLUB-DET-003 (tabs incl. «عن النادي») |
| 004–007 overview & history sections + content | CLUB-DET-018 («عن النادي» blocks: «عن النادي», «المسيرة الرياضية», «ملعب النادي», … each with text) |
| 008, 023, 024 read-only / no mutations | **CLUB-DET-022** (new) — no add/edit/delete/save control and no editable field on any tab |
| 009–017 board & administrative team | CLUB-DET-014 («الإدارة» group: rows or its empty state), CLUB-DET-015 (photos) |
| 018–021 football team cards (photo, name, role) | CLUB-DET-014/015/016 (squad groups, photos, localised positions) |
| 022 no player-profile navigation | **Retired** — the product now opens player profiles; covered positively by CLUB-DET-011 |
| 025 refresh/revisit retrieves current info | **CLUB-DET-023** (new) — reload keeps stats and «عن النادي» headings |
| 026–030 loading / error / retry | Still **Planned** (needs failure hooks), now under the club page |

## Not re-baselined (still failing on purpose)

HOME-001-008 (KPI «عدد اللاعبين» replaced) and ANL-SYNC-003/006 (status chip instead of «عرض التفاصيل») were not in this
decision; they stay as requirement drift awaiting a separate decision. All product defects DEF-ZUL-001…012 stay failing.
