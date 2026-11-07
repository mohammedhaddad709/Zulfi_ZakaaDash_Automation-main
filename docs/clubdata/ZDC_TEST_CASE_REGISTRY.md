# ZDC Test Case Registry — US-FAP-ZULFI-DATA (cross-page E2E)

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ZDC-001 | Home results | Home last-5 results = last 5 played matches in جدول المباريات (opponent + club-first score, newest first) | b | Implemented | zdc001_homeResultsMatchSchedule | league + cup merged chronologically |
| ZDC-002 | Home ↔ club | Home results widget = club «نظرة عامة» results widget | b | Implemented | zdc002_homeResultsMatchClubOverview | |
| ZDC-003 | Form | Standings «آخر 5 مباريات» = last 5 league outcomes in the schedule | b | Implemented | zdc003_standingsFormMatchesSchedule | badges oldest → newest |
| ZDC-004 | Totals | Club P/W/D/L/GF/GA/GD/Pts recomputed from schedule = standings row | b | Implemented | zdc004_standingsMatchScheduleTotals | **FAIL 2026-10-22 — PRODUCT_DEFECT** DEF-ZUL-001 |
| ZDC-005 | Totals | Standings row = club «إحصائيات الفريق» | b | Implemented | zdc005_standingsMatchClubStats | |
| ZDC-006 | Goals | Home «عدد الأهداف» = standings GF = club «له» | b | Implemented | zdc006_teamGoalsEverywhere | |
| ZDC-007 | Top scorer | Home «هداف الفريق» = club «أفضل الهدافين» leader (goals = max, name ∈ tied leaders) | b | Implemented | zdc007_topScorerConsistent | ties allowed |
| ZDC-008 | Rank | Club badge «ترتيب رقم N» = standings position = «الأندية» #N for the Default Club | b | Implemented | zdc008_rankConsistent | other clubs: STD-009 |
| ZDC-009 | Upcoming | Home «المباريات القادمة» = next unplayed fixtures (round, teams, date, time) | b | Implemented | zdc009_homeUpcomingMatchesSchedule | |
| ZDC-010 | Next match | Club «المباراة القادمة» = first upcoming schedule fixture = Home first card | b | Implemented | zdc010_nextMatchConsistent | |
| ZDC-011 | Fixtures tab | Club fixtures tab rows = schedule rows for the club (both directions, same score) | b | Implemented | zdc011_clubFixturesTabMatchesSchedule | |
| ZDC-012 | View More | Home results «عرض المزيد» → جدول المباريات filtered to the club | b | Implemented | zdc012_resultsViewMoreFiltersSchedule | **FAIL — PRODUCT_DEFECT** DEF-ZUL-002 |
| ZDC-013 | View More | Home upcoming «عرض المزيد» → جدول المباريات filtered to the club | b | Implemented | zdc013_upcomingViewMoreFiltersSchedule | **FAIL — PRODUCT_DEFECT** DEF-ZUL-002 |
| ZDC-014 | View More | Home «آخر التحليلات» «عرض المزيد» → «التحليلات» starts with the same cards | b | Implemented | zdc014_recentAnalysesViewMore | |
| ZDC-015 | Analysis status | Status per played match identical on schedule and club fixtures tab | b | Implemented | zdc015_analysisStatusConsistent | |
| ZDC-016 | Whole league | Every standings row = totals recomputed from the schedule (18 clubs) | b | Implemented | zdc016_wholeLeagueStandingsMatchSchedule | **FAIL — PRODUCT_DEFECT** DEF-ZUL-001 |

**Test class:** `com.taf.systems.zulfi.features.clubdata.ZulfiDataConsistencyE2ETest`
**Gate:** `FeaturePhaseGate.CLUB_DATA` (`-Dclub.data.phase=`) · **Suite:** `zulfi-data-consistency-suite.xml`
**Defects:** [`docs/clubdata/DEFECTS_2026-10-22.md`](DEFECTS_2026-10-22.md)
