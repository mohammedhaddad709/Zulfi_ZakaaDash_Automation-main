# STD Test Case Registry — US-FAP-STANDINGS (Home snippet + `/standings`)

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| STD-001 | Snippet | Home snippet columns (# … آخر 5 مباريات) and exactly one highlighted row = Default Club | a | Implemented | std001_homeSnippetColumnsAndOwnClubHighlight | |
| STD-002 | View full | «عرض الجدول الدوري كامل» → `/standings`, heading, one row per club on «الأندية» | a | Implemented | std002_fullTableLinkOpensStandingsPage | |
| STD-003 | Arithmetic | Every row: P=W+D+L, Pts=3W+D, GD=GF−GA | b | Implemented | std003_rowsInternallyConsistent | |
| STD-004 | Order | Positions 1..N; points non-increasing | b | Implemented | std004_orderedByPoints | tie-break rule not asserted (head-to-head) |
| STD-005 | Form | ≤5 W/D/L badges; colour = letter; within totals | b | Implemented | std005_formBadges | |
| STD-006 | Snippet = full | Each Home snippet row equals the same position on `/standings` | b | Implemented | std006_homeSnippetMatchesFullTable | |
| STD-007 | Zones | Legend (الصعود المباشر / التأهل إلى ملحق الصعود / الهبوط); zones contiguous; #1 green; last red | b | Implemented | std007_zonesAndLegend | |
| STD-008 | Logos | Every club logo loads | b | Implemented | std008_logosLoad | |
| STD-009 | Clubs grid | «الأندية» #N = standings position for every club | b | Implemented | std009_clubsGridRankMatchesStandings | **FAIL — PRODUCT_DEFECT** DEF-ZUL-003 |
| STD-010 | Copy | League name spelled «الدرجة الأولى» on Home snippet and `/standings` | c | Implemented | std010_leagueNameConsistentAcrossPages | **FAIL — PRODUCT_DEFECT** DEF-ZUL-007 |

**Test class:** `com.taf.systems.zulfi.features.standings.StandingsAcceptanceTest`
**Gate:** `FeaturePhaseGate.STANDINGS` (`-Dstandings.phase=`) · **Suite:** `standings-suite.xml`
