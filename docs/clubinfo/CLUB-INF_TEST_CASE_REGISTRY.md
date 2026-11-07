# CLUB-INF Test Case Registry — US-FAP-CLUB-001

> **Retired 2026-10-22.** The sidebar «عن النادي» page no longer exists; coverage moved to **CLUB-DET** (club page
> `/clubs/{id}`). Mapping and decision: [`docs/testing/REBASELINE_2026-10-22.md`](../testing/REBASELINE_2026-10-22.md).
> The class stays in the repo and skips with the retirement reason unless `-Dclub.info.legacy=true`.

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| CLUB-INF-001 | AC-01 | Sidebar **عن النادي** access (authenticated) | a | Implemented | clubInf001_sidebarAccess | |
| CLUB-INF-002 | AC-02 | Opens Club Information page | a | Implemented | clubInf002_opensPage | |
| CLUB-INF-003 | AC-03 | Same access for supported roles (smoke) | a | Implemented | clubInf003_rolesSmoke | single credential |
| CLUB-INF-004 | AC-04 | Section **نبذة عن النادي** | a | Implemented | clubInf004_overviewSection | |
| CLUB-INF-005 | AC-05 | Overview content | a | Implemented | clubInf005_overviewContent | |
| CLUB-INF-006 | AC-06 | Section **تاريخ النادي** | a | Implemented | clubInf006_historySection | |
| CLUB-INF-007 | AC-07 | History content | a | Implemented | clubInf007_historyContent | |
| CLUB-INF-008 | AC-08 | Overview & history read-only | a | Implemented | clubInf008_overviewHistoryReadOnly | |
| CLUB-INF-009 | AC-09 | Section **مجلس الإدارة** | a | Implemented | clubInf009_boardSection | |
| CLUB-INF-010 | AC-10 | Board member cards | b | Implemented | clubInf010_boardCards | |
| CLUB-INF-011 | AC-11 | Board name + role | b | Implemented | clubInf011_boardNameRole | |
| CLUB-INF-012 | AC-12 | Board photo when available | b | Implemented | clubInf012_boardPhoto | |
| CLUB-INF-013 | AC-13 | Board placeholder (no broken image) | b | Implemented | clubInf013_boardPlaceholder | heuristic |
| CLUB-INF-014 | AC-14 | Section **الفريق الإداري** | b | Implemented | clubInf014_adminSection | |
| CLUB-INF-015 | AC-15 | Admin member cards | b | Implemented | clubInf015_adminCards | |
| CLUB-INF-016 | AC-16 | Admin name + role | b | Implemented | clubInf016_adminNameRole | |
| CLUB-INF-017 | AC-17 | Admin photo or placeholder | b | Implemented | clubInf017_adminPhoto | |
| CLUB-INF-018 | AC-18 | Section **الفريق الرياضي** | b | Implemented | clubInf018_footballSection | |
| CLUB-INF-019 | AC-19 | Football team cards | b | Implemented | clubInf019_footballCards | |
| CLUB-INF-020 | AC-20 | Football name + role | b | Implemented | clubInf020_footballNameRole | |
| CLUB-INF-021 | AC-21 | Football photo or placeholder | b | Implemented | clubInf021_footballPhoto | |
| CLUB-INF-022 | AC-22 | No player profile navigation on football cards | b | Implemented | clubInf022_noPlayerNav | |
| CLUB-INF-023 | AC-23 | No add / edit / delete | b | Implemented | clubInf023_noMutations | |
| CLUB-INF-024 | AC-24 | Viewing does not modify data | b | Implemented | clubInf024_viewReadOnly | |
| CLUB-INF-025 | AC-25 | Refresh / revisit retrieves current info | b | Implemented | clubInf025_refreshRevisit | |
| CLUB-INF-026 | AC-26 | Loading state while retrieving | c | Skip | - | hooks |
| CLUB-INF-027 | AC-27 | Error **تعذر تحميل بيانات النادي. حاول مرة أخرى.** | c | Skip | - | hooks |
| CLUB-INF-028 | AC-28 | **إعادة المحاولة** on error | c | Skip | - | hooks |
| CLUB-INF-029 | AC-29 | Retry requests data again | c | Skip | - | hooks |
| CLUB-INF-030 | AC-30 | Successful retry restores content | c | Skip | - | hooks |

**Test class:** `com.taf.systems.zulfi.features.clubinfo.ClubInformationAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.ClubInfoGate` (`-Dclub.info.phase=`)

**Coverage note:** Phase **a** + **b** implemented; phase **c** error/loading ACs **Planned** (Skip until hooks). Registry complete; not a missing suite.
