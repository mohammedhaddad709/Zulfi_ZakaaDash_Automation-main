# ANL-STS Test Case Registry — US-FAP-ANL-002

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ANL-STS-001 | AC-01, AC-02, AC-03 | Post-submit observe — initial `قيد التحليل`, same id, single list row | d | Gated | anlSts001_ac01Through03_postSubmitProcessingRecord | one submit; observeSubmit + create gates |
| ANL-STS-002 | AC-02 | Status on submitted record | d | Grouped | anlSts001_ac01Through03_postSubmitProcessingRecord | |
| ANL-STS-003 | AC-03 | No extra record on submit | d | Grouped | anlSts001_ac01Through03_postSubmitProcessingRecord | |
| ANL-STS-004 | AC-04 | Processing continues after leaving details | b | Implemented | anlSts004_processingContinuesAfterLeave | |
| ANL-STS-005 | AC-05 | Leave does not cancel analysis | b | Implemented | anlSts005_leaveDoesNotCancel | UI proxy |
| ANL-STS-006 | AC-06 | Return does not restart processing | b | Implemented | anlSts006_returnNoRestart | |
| ANL-STS-007 | AC-07 | Refresh/reopen does not resubmit | b | Implemented | anlSts007_refreshNoResubmit | list cardinality |
| ANL-STS-008 | AC-08 | Return shows existing record | b | Implemented | anlSts008_returnShowsExisting | |
| ANL-STS-009 | AC-09 | Details retrieves latest status | a | Implemented | anlSts009_detailsLatestStatus | |
| ANL-STS-010 | AC-10 | Incomplete → `قيد التحليل` on details | b | Implemented | anlSts010_incompleteProcessingLabel | |
| ANL-STS-011 | AC-11 | Completed → `اكتمل التحليل` on details | c | Implemented | anlSts011_completedLabel | |
| ANL-STS-012 | AC-12 | Home / List / Details label consistency | a | Implemented | anlSts012_crossSurfaceConsistency | overlaps HOME-001 / ANL-LST |
| ANL-STS-013 | AC-13 | Processing title `التحليل قيد التنفيذ` | b | Implemented | anlSts013_processingTitle | |
| ANL-STS-014 | AC-14 | Processing description | b | Implemented | anlSts014_processingDescription | |
| ANL-STS-015 | AC-15 | Match/video visible while processing | b | Implemented | anlSts015_matchVideoVisible | |
| ANL-STS-016 | AC-16 | Dashboard disabled while processing | b | Implemented | anlSts016_dashboardDisabled | |
| ANL-STS-017 | AC-17 | PDF disabled while processing | b | Implemented | anlSts017_pdfDisabled | |
| ANL-STS-018 | AC-18 | No direct result access while processing | b | Implemented | anlSts018_noDirectResultAccess | |
| ANL-STS-019 | AC-19 | Completion status on same record | c | Implemented | anlSts019_completionSameRecord | |
| ANL-STS-020 | AC-20 | Completion does not create new record | c | Implemented | anlSts020_noNewRecordOnComplete | |
| ANL-STS-021 | AC-21 | Processing copy hidden when completed | c | Implemented | anlSts021_processingCopyHidden | |
| ANL-STS-022 | AC-22 | Interactive dashboard available | c | Implemented | anlSts022_dashboardAvailable | |
| ANL-STS-023 | AC-23 | PDF report available | c | Implemented | anlSts023_pdfAvailable | |
| ANL-STS-024 | AC-24 | Outputs on same completed record | c | Implemented | anlSts024_outputsSameRecord | |
| ANL-STS-025 | AC-25 | Match name preserved | c | Implemented | anlSts025_matchNamePreserved | |
| ANL-STS-026 | AC-26 | Description preserved | c | Implemented | anlSts026_descriptionPreserved | read DOM |
| ANL-STS-027 | AC-27 | Video source/url preserved | c | Implemented | anlSts027_videoPreserved | |
| ANL-STS-028 | AC-28 | Submission date unchanged | c | Implemented | anlSts028_submissionDateUnchanged | |
| ANL-STS-029 | AC-29 | Video source matches URL | b | Implemented | anlSts029_videoSourceMatchesUrl | BR-ANL-STS-010 |
| ANL-STS-030 | AC-30 | Refresh does not duplicate record | b | Implemented | anlSts030_refreshNoDuplicate | |
| ANL-STS-031 | AC-31 | Outputs blocked before completion | b | Implemented | anlSts031_outputsBlockedBeforeComplete | |
| ANL-STS-032 | AC-32 | Completed actions only when completed | c | Implemented | anlSts032_completedActionsOnlyWhenComplete | |
| ANL-STS-033 | AC-33 | Outputs scoped to selected id | c | Implemented | anlSts033_outputsScopedToId | |
| ANL-STS-034 | AC-34 | Only approved lifecycle statuses | a | Implemented | anlSts034_approvedStatusesOnly | |
| ANL-STS-035 | AC-35 | No failed status / retry action | a | Implemented | anlSts035_noFailedStatus | |

**Test class:** `com.taf.systems.zulfi.features.analysis.status.AnalysisProcessingStatusAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.AnlsStsGate` (`-Danl.sts.phase=`; observe submit: `-Danl.sts.observeSubmit=true` + `analysis.create.enabled`)

**Cross-reference:** HOME-001 AC-30…36, ANL-SYNC status labels, ANL-LST card status (shared `AnalysesListPage`).
