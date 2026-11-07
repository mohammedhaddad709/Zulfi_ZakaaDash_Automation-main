# CLUB-DET Test Case Registry — US-FAP-CLUB-DETAILS (`/clubs/{id}`)

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| CLUB-DET-001 | Entry | «عرض تفاصيل النادي» on «الأندية» opens `/clubs/{id}` of that card | a | Implemented | clubDet001_detailsButtonOpensClubById | |
| CLUB-DET-002 | Header | Club name, «# ترتيب رقم N في دوري الدرجة الأولى», logo loads | a | Implemented | clubDet002_header | |
| CLUB-DET-003 | Tabs | نظرة عامة / جدول المباريات والنتائج / قائمة الفريق / عن النادي; overview default; each renders | a | Implemented | clubDet003_tabs | |
| CLUB-DET-004 | Breadcrumb | الرئيسية › الأندية › club; «الأندية» navigates back | a | Implemented | clubDet004_breadcrumb | |
| CLUB-DET-005 | Stats | 8 cards; P=W+D+L, Pts=3W+D, GD=GF−GA; positive GD shows «+» | b | Implemented | clubDet005_seasonStats | |
| CLUB-DET-006 | Results | 1–5 cards; «x - y»; opponent; tag colour = outcome | b | Implemented | clubDet006_seasonResultCards | |
| CLUB-DET-007 | Images | Every overview image loads | b | Implemented | clubDet007_overviewImagesLoad | |
| CLUB-DET-008 | Leaderboard | «أعلى تصنيف»: 3 players, sorted, rating format, photos, profile links | b | Implemented | clubDet008_topRated | |
| CLUB-DET-009 | Leaderboard | «أفضل الهدافين»: same rules, integer goals | b | Implemented | clubDet009_topScorers | |
| CLUB-DET-010 | Leaderboard | «أفضل صانعي الأهداف»: same rules, integer assists | b | Implemented | clubDet010_topAssists | |
| CLUB-DET-011 | Profile | Leaderboard name → `/clubs/players/{id}` showing that player | b | Implemented | clubDet011_playerProfileNavigation | |
| CLUB-DET-012 | Fixtures | «المباراة القادمة» card + rows (club involved, dated, scored + status, chronological) | b | Implemented | clubDet012_fixturesTab | |
| CLUB-DET-013 | Fixtures | Competition filter defaults to «كل البطولات»; league rows listed | b | Implemented | clubDet013_competitionFilter | |
| CLUB-DET-014 | Squad | Toggles الفريق الأول / تحت 21 / تحت 18; groups المدافعون…الإدارة; player groups non-empty | b | Implemented | clubDet014_squadStructure | «الإدارة» may show its empty state |
| CLUB-DET-015 | Squad | Every player photo loads | b | Implemented | clubDet015_squadPhotosLoad | **FAIL — PRODUCT_DEFECT** DEF-ZUL-004 |
| CLUB-DET-016 | Squad | Positions localised to Arabic (no «D/M/F/G», no blank) | b | Implemented | clubDet016_positionsLocalised | **FAIL — PRODUCT_DEFECT** DEF-ZUL-005 |
| CLUB-DET-017 | Squad | Unique shirt numbers; ages 15–45; minutes ≤ apps × 120 | b | Implemented | clubDet017_squadDataSanity | **FAIL — PRODUCT_DEFECT** DEF-ZUL-006 |
| CLUB-DET-018 | About | Biography blocks each have text and name the club | b | Implemented | clubDet018_aboutTab | |
| CLUB-DET-022 | Read-only | No add/edit/delete/save control and no editable field on any tab | b | Implemented | clubDet022_readOnlyEverywhere | migrated from CLUB-INF-008/023/024 |
| CLUB-DET-023 | Refresh | Reload keeps stats and «عن النادي» headings | b | Implemented | clubDet023_refreshKeepsClubData | migrated from CLUB-INF-025 |
| CLUB-DET-024 | Tab logic | Overview «نتائج الفريق للموسم الحالي» = latest ≤5 played matches of the fixtures tab, newest first (opponent + club-first score) | b | Implemented | clubDet024_overviewResultsMatchFixturesTab | same page; cross-page twin ZDC-002 |
| CLUB-DET-025 | Tab logic | «إحصائيات الفريق» = P/W/D/L/GF/GA/GD/Pts recomputed from the league rows (competition filter = league) of the fixtures tab | b | Implemented | clubDet025_statsMatchFixturesTabLeagueRows | **FAIL 2026-10-23 — PRODUCT_DEFECT** DEF-ZUL-001: card P8 W3 D5 GF10 Pts14 vs 6 league results listed (P6 W2 D4 GF8 Pts10) |
| CLUB-DET-026 | Tab logic | «المباراة القادمة» involves the club, is not a listed result, is dated after the last result and its round follows the last league round played (= first unplayed row when the tab lists any) | b | Implemented | clubDet026_nextMatchFollowsLastResult | the tab lists results only (OBS-8); cross-page twin ZDC-010 |
| CLUB-DET-027 | Fixtures | Applying the competition filter: each competition lists only its own matches (league rows have a round), competitions partition «كل البطولات», reset restores all rows | b | Implemented | clubDet027_competitionFilterNarrowsRows | 013 checks only the default |
| CLUB-DET-028 | Squad | «فريق تحت 21 سنة» / «فريق تحت 18 سنة»: own roster (not the first team's), every listed age ≤ 21 / ≤ 18 | b | Implemented | clubDet028_youthSquads | displayed age vs age group; empty roster reported, not failed |
| CLUB-DET-030 | Localisation | No English UI text on «جدول المباريات والنتائج» (round labels, statuses, filter); «VS» graphic allowed | b | Implemented | clubDet030_fixturesTabLabelsArabic | **FAIL 2026-10-23 — PRODUCT_DEFECT** DEF-ZUL-014 «Round of 32» |
| CLUB-DET-029 | Tab logic | Every leaderboard player is on «قائمة الفريق» (first team); top-3 goals and top-3 assists each ≤ club goals in all competitions | b | Implemented | clubDet029_leaderboardsMatchSquadAndGoals | names match after Arabic normalisation / short-name containment |
| CLUB-DET-019 | Design | Desktop: `dir=rtl`, no horizontal page scroll on any tab | c | Implemented | clubDet019_desktopLayout | |
| CLUB-DET-020 | Design | Mobile 390×844: no horizontal page scroll on any tab | c | Implemented | clubDet020_mobileLayout | |
| CLUB-DET-021 | Design | No clipped text (without ellipsis) on overview | c | Implemented | clubDet021_noClippedText | |

**Test class:** `com.taf.systems.zulfi.features.clubdetails.ClubDetailsAcceptanceTest`
**Gate:** `FeaturePhaseGate.CLUB_DETAILS` (`-Dclub.details.phase=`) · **Suite:** `club-details-suite.xml`
**Tab-to-tab logic (024–029):** the overview summarises the other tabs, so each overview widget is recomputed from
the tab that holds its source rows on the same page. Comparators are shared with ZDC in
`com.taf.systems.zulfi.clubdata.ClubDataComparisons` (unit-tested in `ClubDataRulesTest`).
**Relation:** supersedes US-FAP-CLUB-001 (CLUB-INF, retired 2026-10-22). AC-by-AC mapping:
[`docs/testing/REBASELINE_2026-10-22.md`](../testing/REBASELINE_2026-10-22.md).
