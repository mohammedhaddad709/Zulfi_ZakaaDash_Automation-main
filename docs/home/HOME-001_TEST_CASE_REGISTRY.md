# HOME-001 Test Case Registry — US-FAP-HOME-001

**Class:** `com.taf.systems.zulfi.features.home.HomeOverviewAcceptanceTest`  
**Gate:** `-Dhome.overview.phase=` (`none` | `a` | `b` | `c` | `d`) — see `US-FAP-HOME-001_TEST_STRATEGY.md`  
**Suite:** `src/test/resources/suites/zulfi/home-overview-suite.xml`

| Test ID | AC | Title | Phase | Status | Test method | Notes |
|---------|-----|-------|-------|--------|-------------|-------|
| HOME-001-001 | AC-01 | Login redirects to Home | a | Automated | `home001_001_ac01_successfulLoginRedirectsToHome` | Requires `ZULFI_USER` / `ZULFI_PASSWORD` |
| HOME-001-002 | AC-02 | Same Home for supported roles | a | Skip | `home001_002_ac02_supportedRolesShareHomeExperience` | First Release: single credential; parity covered by AC-03 sections |
| HOME-001-003 | AC-03 | Core sections visible | a | Automated | `home001_003_ac03_homeDisplaysOverviewSections` | Hero, summary, upcoming, recent |
| HOME-001-004 | AC-04 | Hero title | a | Automated | `home001_004_ac04_heroTitle` | |
| HOME-001-005 | AC-05 | Hero description | a | Automated | `home001_005_ac05_heroDescription` | |
| HOME-001-006 | AC-06 | ابدأ التحليل → create dialog | a | Automated | `home001_006_ac06_startAnalysisOpensNewAnalysisDialog` | Read-only; no submit |
| HOME-001-007 | AC-07 | استعراض التحليلات → list | a | Automated | `home001_007_ac07_browseAnalysesOpensAnalysesList` | |
| HOME-001-008 | AC-08 | Summary KPI labels incl. هداف الفريق | b | Automated | `home001_008_ac08_summaryKpiLabelsPresent` | |
| HOME-001-009 | AC-09 | KPI values (total) | b | Automated | `home001_009_ac09Through13_summaryKpiValuesParseable` | Bundled AC-09…13 |
| HOME-001-010 | AC-10 | KPI values (processing) | b | Automated | *(same as 009)* | |
| HOME-001-011 | AC-11 | KPI values (completed) | b | Automated | *(same as 009)* | |
| HOME-001-012 | AC-12 | إجمالي = قيد + اكتمل | b | Automated | `home001_012_ac12_totalEqualsProcessingPlusCompleted` | Skip if KPIs not parseable |
| HOME-001-013 | AC-13 | KPI values (player count) | b | Automated | *(same as 009)* | |
| HOME-001-014 | AC-14 | Refresh retrieves summary | b | Automated | `home001_014_ac14_refreshRetrievesLatestSummary` | |
| HOME-001-015 | AC-15 | Upcoming section title | b | Automated | `home001_015_ac15_upcomingSectionTitle` | المباراة القادمة / المباريات القادمة |
| HOME-001-016 | AC-16 | Upcoming match content | b | Automated | `home001_016_ac16Through21_upcomingMatchContentOrEmpty` | Bundled AC-16…21 |
| HOME-001-017 | AC-17 | *(upcoming)* | b | Automated | *(same as 016)* | |
| HOME-001-018 | AC-18 | *(upcoming)* | b | Automated | *(same as 016)* | |
| HOME-001-019 | AC-19 | *(upcoming)* | b | Automated | *(same as 016)* | |
| HOME-001-020 | AC-20 | *(upcoming)* | b | Automated | *(same as 016)* | |
| HOME-001-021 | AC-21 | *(upcoming)* | b | Automated | *(same as 016)* | |
| HOME-001-022 | AC-22 | No upcoming match copy | b | Conditional | `home001_022_ac22_noUpcomingMatchEmptyCopy` | Skip when env has upcoming match |
| HOME-001-023 | AC-23 | Recent analyses section | b | Conditional | `home001_023_ac23_recentAnalysesWhenDataExists` | Skip if no cards |
| HOME-001-024 | AC-24 | Recent card fields | b | Conditional | `home001_024_ac24_recentCardStructure` | |
| HOME-001-025 | AC-25 | Video source indicator | b | Conditional | `home001_025_ac25_videoSourceIndicator` | Skip if no markers |
| HOME-001-026 | AC-26 | View details on cards | b | Conditional | `home001_026_ac26_viewDetailsOnEachCard` | |
| HOME-001-027 | AC-27 | View details navigation | c | Conditional | `home001_027_ac27_viewDetailsOpensAnalysisRoute` | Smoke only |
| HOME-001-028 | AC-28 | عرض المزيد → التحليلات | a | Conditional | `home001_028_ac28_viewMoreOpensAnalysesList` | Skip if link hidden |
| HOME-001-029 | AC-29 | No delete on Home cards | b | Automated | `home001_029_ac29_noDeleteOnRecentCards` | |
| HOME-001-030 | AC-30 | Processing status label | c | Conditional | `home001_030_ac30_processingStatusLabel` | Needs processing card |
| HOME-001-031 | AC-31 | Processing view details enabled | c | Conditional | `home001_031_ac31_processingViewDetailsEnabled` | |
| HOME-001-032 | AC-32 | Dashboard disabled (processing) | c | Conditional | `home001_032_ac32_processingDashboardDisabled` | |
| HOME-001-033 | AC-33 | Disabled dashboard no nav | c | Conditional | `home001_033_ac33_disabledDashboardNoNavigation` | |
| HOME-001-034 | AC-34 | Completed status label | c | Conditional | `home001_034_ac34_completedStatusLabel` | |
| HOME-001-035 | AC-35 | Dashboard enabled (completed) | c | Conditional | `home001_035_ac35_completedDashboardEnabled` | |
| HOME-001-036 | AC-36 | Dashboard navigation smoke | c | Conditional | `home001_036_ac36_dashboardOpensForCompleted` | |
| HOME-001-037 | AC-37 | Dashboard scoped to analysis | c | Conditional | `home001_037_ac37_dashboardRouteContainsAnalysisId` | |
| HOME-001-038 | AC-38 | Recent empty title | d | Conditional | `home001_038_ac38_recentEmptyStateTitle` | `-Dhome.overview.allowEmptyStateChecks=true` + empty env |
| HOME-001-039 | AC-39 | Empty CTA → create dialog | d | Conditional | `home001_039_ac39_emptyStateStartAnalysisOpensDialog` | |
| HOME-001-040 | AC-40 | Loading state | d | Skip | `home001_040_ac40_dynamicSectionsLoadingState` | Needs hooks / slow API |
| HOME-001-041 | AC-41 | No empty while loading | d | Skip | `home001_041_ac41_emptyNotShownWhileLoading` | |
| HOME-001-042 | AC-42 | Recent retrieval error copy | d | Skip | `home001_042_ac42_recentRetrievalErrorCopy` | `-Dhome.overview.allowErrorStateHooks=true` |
| HOME-001-043 | AC-43 | Recent error retry | d | Skip | `home001_043_ac43_recentErrorRetryAction` | |
| HOME-001-044 | AC-44 | Hero usable when sections fail | a | Automated | `home001_044_ac44_heroActionsRemainAvailable` | |
| HOME-001-045 | AC-45 | Retry restores state | d | Skip | `home001_045_ac45_retryReplacesErrorState` | |

**Out of scope for HOME-001:** `CurrentTeamResultsSection` / **نتائج الفريق الحالية** → `HOME-RES-*` / `home-current-results-suite.xml`.
