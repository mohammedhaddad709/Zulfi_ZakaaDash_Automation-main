# MCH-SCHED Test Case Registry — US-FAP-MCH

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| MCH-SCHED-001 | AC Open schedule | Sidebar → **جدول المباريات** | a | Implemented | mchSched001_openSchedule | |
| MCH-SCHED-002 | AC Page title | **جدول المباريات** visible | a | Implemented | mchSched002_pageTitle | |
| MCH-SCHED-003 | AC Description | Yellow League / current season description | a | Implemented | mchSched003_description | |
| MCH-SCHED-004 | AC League scope | No friendly (**ودية**) rows heuristic | b | Implemented | mchSched004_leagueScope | Not ANL-FRD-* |
| MCH-SCHED-005 | AC Unplayed display | Clubs + date + kickoff time | b | Implemented | mchSched005_unplayedDisplay | |
| MCH-SCHED-006 | AC Unplayed | No **قادمة** upcoming label on fixtures | b | Implemented | mchSched006_noUpcomingLabel | |
| MCH-SCHED-007 | AC Unplayed | No analysis status on unplayed rows | b | Implemented | mchSched007_unplayedNoStatus | Status sync: ANL-SYNC-* |
| MCH-SCHED-008 | AC Played display | Result + club names | b | Implemented | mchSched008_playedDisplay | |
| MCH-SCHED-009 | AC Played status | **غير محللة** / **قيد التحليل** / **اكتمل التحليل** | b | Implemented | mchSched009_playedStatus | MCH-ANL-001 tests action not label set |
| MCH-SCHED-010 | AC Independence | Score shown regardless of status | b | Implemented | mchSched010_scoreIndependent | |
| MCH-SCHED-011 | BR Match #ID | **#ID** secondary to club names in row text | b | Implemented | mchSched011_matchIdSecondary | MCH-ANL-008 pop-up emphasis |
| MCH-SCHED-012 | AC Filter club | **النادي** → specific club (`DefaultClubConfig`) | b | Implemented | mchSched012_filterClub | |
| MCH-SCHED-013 | AC All clubs | **جميع الأندية** restores broader list | b | Implemented | mchSched013_allClubs | |
| MCH-SCHED-014 | AC Home deep link | Home **عرض المزيد** → preselected club | b | Implemented | mchSched014_homeDeepLink | HOME-RES-009 — run once per batch |
| MCH-SCHED-015 | AC Empty filter | No-results copy when filter empty | c | Skip | - | unless `mch.schedule.emptyFilterClub` |
| MCH-SCHED-016 | AC Loading/error | Skeleton / error + **إعادة المحاولة** | c | Skip | - | not controllable |

**Test class:** `com.taf.systems.zulfi.features.matchschedule.MatchScheduleBrowseAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.MchScheduleGate` (`-Dmch.schedule.phase=`)

### Cross-suite notes

| Related suite | Relationship |
|---------------|--------------|
| `home-current-results-suite.xml` (**HOME-RES-009**) | Same View More deep link as MCH-SCHED-014 |
| `match-schedule-analysis-suite.xml` (**MCH-ANL-***) | Same `MatchSchedulePage`; analysis submit not in MCH-SCHED |
| **ANL-SYNC-*** | Post-submit status on schedule — not row browse ACs |
