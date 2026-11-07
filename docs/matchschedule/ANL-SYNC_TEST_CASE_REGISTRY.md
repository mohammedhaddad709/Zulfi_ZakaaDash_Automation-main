# ANL-SYNC Test Case Registry — US-FAP-ANL-SYNC

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ANL-SYNC-001 | AC Played without analysis | **غير محللة** + **تحليل المباراة** | a | Implemented | anlSync001_playedWithoutAnalysis | MCH-ANL-001 overlap; pick one in smoke |
| ANL-SYNC-002 | AC Sync after creation | **غير محللة** → **قيد التحليل** after valid create | d | Gated | anlSync002_syncAfterCreation | MCH-ANL-010 submit + lifecycle triple gate |
| ANL-SYNC-003 | AC Remove start during processing | **قيد التحليل** hides **تحليل المباراة** | b | Implemented | anlSync003_processingHidesStart | MCH-ANL-013 partial |
| ANL-SYNC-004 | AC Completed sync | **قيد** → **اكتمل التحليل** on completion | d | Partial | anlSync004_completedSync | observes env; no auto-complete |
| ANL-SYNC-005 | AC Completed actions | **عرض التفاصيل** + **عرض لوحة التحليل** | b | Implemented | anlSync005_completedActions | |
| ANL-SYNC-006 | AC Correct details | **عرض التفاصيل** opens linked Analysis A | c | Implemented | anlSync006_correctDetails | |
| ANL-SYNC-007 | AC Correct dashboard | **عرض لوحة التحليل** opens Analysis A dashboard | c | Implemented | anlSync007_correctDashboard | |
| ANL-SYNC-008 | AC Latest status | Refresh returns current status | b | Implemented | anlSync008_latestStatus | |
| ANL-SYNC-009 | AC / BR-009 | Retrieval failure must not infer **غير محللة** | d | Skip | - | hook |
| ANL-SYNC-010 | AC / BR-004 | Non-complete must not show **اكتمل التحليل** | b | Implemented | anlSync010_noFalseCompleted | |
| ANL-SYNC-011 | AC / BR-010 | Match scores unchanged when status displayed | b | Implemented | anlSync011_scoresUnchanged | |
| ANL-SYNC-012 | AC / BR-011 | Friendly status change does not update official schedule | d | Implemented | anlSync012_friendlyIsolation | ANL-FRD owner |
| ANL-SYNC-013 | AC Prevent duplicate | **قيد** / **اكتمل** rows hide start analysis | b | Implemented | anlSync013_preventDuplicateStart | MCH-ANL-013, 016 |

**Test class:** `com.taf.systems.zulfi.features.matchschedule.AnalysisStatusSyncAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.AnlsSyncGate` (`-Danl.sync.phase=`; lifecycle: `anl.sync.lifecycle.enabled`, `anl.sync.creation.enabled`, `analysis.create.enabled`)

**Messages:** `com.taf.systems.zulfi.validations.MatchScheduleAnalysisStatusMessages`
