# HOME-RES Test Case Registry — US-FAP-HOME-CURRENT-RESULTS

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| HOME-RES-001 | Display section | **نتائج الفريق الحالية** visible | a | Implemented | homeRes001_displaySectionVisible | |
| HOME-RES-002 | Current season only | No previous-season rows | b | Partial | homeRes002_currentSeasonOnly | needs season marker in card/API |
| HOME-RES-003 | Default club | Cards only for configured club | b | Implemented | homeRes003_defaultClubScope | |
| HOME-RES-004 | Played only | Scores present; no upcoming | b | Implemented | homeRes004_playedOnly | |
| HOME-RES-005 | Result display | Home/away names + scores + date | b | Implemented | homeRes005_resultDisplay | |
| HOME-RES-006 | Analysis status | غير محللة / قيد / اكتمل | b | Partial | homeRes006_analysisStatus | |
| HOME-RES-007 | Order | Most recent first | b | Implemented | homeRes007_orderMostRecentFirst | |
| HOME-RES-008 | Limit | Bounded card count (layout config) | b | Implemented | homeRes008_limitBoundedCount | soft max property |
| HOME-RES-009 | View More | Schedule + **النادي** filter | a | Implemented | homeRes009_viewMoreDeepLink | do not duplicate MCH-SCHED-014 same batch |
| HOME-RES-010 | Not hardcoded | Uses `DefaultClubConfig` | a/b | Implemented | homeRes010_notHardcodedClub | |
| HOME-RES-011 | No results empty | Empty copy | c | Skip | - | needs empty season data |
| HOME-RES-012 | Loading | Skeleton before empty | c | Skip | - | hooks |
| HOME-RES-013 | Retrieval error | Error + **إعادة المحاولة** | c | Skip | - | hooks |
| HOME-RES-014 | Home resilience | **ابدأ التحليل** still available | a | Implemented | homeRes014_homeResilience | |

**Test class:** `com.taf.systems.zulfi.features.home.HomeCurrentResultsAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.HomeCurrentResultsGate` (`-Dhome.current.results.phase=`)
