# ANL-LST Test Case Registry — US-FAP-ANL-003

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ANL-LST-001 | AC-01 | Sidebar → **التحليلات** | a | Implemented | anlLst001_ac01_openAnalysesFromSidebar | also `anlLstHomeCross` |
| ANL-LST-002 | AC-02 | Page title **التحليلات** | a | Grouped | anlLst002_ac02Through04_pageChromeAndPrimaryCta | |
| ANL-LST-003 | AC-03 | Primary CTA **تحليل مباراة جديدة** | a | Grouped | anlLst002_ac02Through04_pageChromeAndPrimaryCta | |
| ANL-LST-004 | AC-04 | CTA opens new analysis | a | Grouped | anlLst002_ac02Through04_pageChromeAndPrimaryCta | dialog only |
| ANL-LST-005 | AC-05 | Newest-first order | a | Implemented | anlLst005_ac05_cardsOrderedNewestFirst | |
| ANL-LST-006 | AC-06 | Card fields | a | Implemented | anlLst006_ac06_cardDisplaysRequiredFields | |
| ANL-LST-007 | AC-07 | Video source YouTube/Drive | a | Implemented | anlLst007_ac07_videoSourceOnCards | Skip if unparsed |
| ANL-LST-008 | AC-08 | Tenant scope | a | Implemented | anlLst008_ac08_authenticatedUserScopeOnly | single credential BR |
| ANL-LST-009 | AC-09 | Processing status label | a | Grouped | anlLst009_ac09Through14_statusDrivesDashboardAvailability | |
| ANL-LST-010 | AC-10 | Processing → view details enabled | a | Grouped | anlLst009_ac09Through14_statusDrivesDashboardAvailability | |
| ANL-LST-011 | AC-11 | Processing → dashboard disabled | a | Grouped | anlLst009_ac09Through14_statusDrivesDashboardAvailability | |
| ANL-LST-012 | AC-12 | Completed status label | a | Grouped | anlLst009_ac09Through14_statusDrivesDashboardAvailability | |
| ANL-LST-013 | AC-13 | Completed → both actions enabled | a | Grouped | anlLst009_ac09Through14_statusDrivesDashboardAvailability | |
| ANL-LST-014 | AC-14 | Actions target selected record | a | Implemented | anlLst014_ac14_viewDetailsNavigationSmoke | |
| ANL-LST-015 | AC-15 | No delete on cards | a | Implemented | anlLst015_ac15_noDeleteActionOnCards | |
| ANL-LST-016 | AC-16 | Search placeholder | a | Implemented | anlLst016_ac16_searchFieldPlaceholder | |
| ANL-LST-017 | AC-17 | Full name search | b | Grouped | anlLst017_ac17Through21_searchMatchNameRules | |
| ANL-LST-018 | AC-18 | Partial search | b | Grouped | anlLst017_ac17Through21_searchMatchNameRules | |
| ANL-LST-019 | AC-19 | English case-insensitive | b | Grouped | anlLst017_ac17Through21_searchMatchNameRules | Skip if no Latin in title |
| ANL-LST-020 | AC-20 | Trim search spaces | b | Grouped | anlLst017_ac17Through21_searchMatchNameRules | |
| ANL-LST-021 | AC-21 | Clear search restores filter-only set | b | Grouped | anlLst017_ac17Through21_searchMatchNameRules | |
| ANL-LST-022 | AC-22 | Search read-only | b | Implemented | anlLst022_ac22_searchDoesNotMutateRecords | |
| ANL-LST-023 | AC-23 | From/To date controls | c | Implemented | anlLst023_ac23_dateFilterControlsPresent | |
| ANL-LST-024 | AC-24 | From date only | c | Grouped | anlLst024_ac24Through26_dateRangeSmoketest | |
| ANL-LST-025 | AC-25 | To date only | c | Grouped | anlLst024_ac24Through26_dateRangeSmoketest | |
| ANL-LST-026 | AC-26 | Inclusive range | c | Grouped | anlLst024_ac24Through26_dateRangeSmoketest | |
| ANL-LST-027 | AC-27 | Reject To < From | c | Grouped | anlLst027_ac27Through28_invalidDateRangeValidation | |
| ANL-LST-028 | AC-28 | Invalid range message | c | Grouped | anlLst027_ac27Through28_invalidDateRangeValidation | |
| ANL-LST-029 | AC-29 | Status options | b | Grouped | anlLst029_ac29Through33_statusFilter | |
| ANL-LST-030 | AC-30 | **الكل** default | b | Grouped | anlLst029_ac29Through33_statusFilter | |
| ANL-LST-031 | AC-31 | Filter **قيد التحليل** | b | Grouped | anlLst029_ac29Through33_statusFilter | Skip if none |
| ANL-LST-032 | AC-32 | Filter **اكتمل التحليل** | b | Grouped | anlLst029_ac29Through33_statusFilter | Skip if none |
| ANL-LST-033 | AC-33 | **الكل** clears status | b | Grouped | anlLst029_ac29Through33_statusFilter | |
| ANL-LST-034 | AC-34 | Source options | b | Grouped | anlLst034_ac34Through38_videoSourceFilter | |
| ANL-LST-035 | AC-35 | Source default **الكل** | b | Grouped | anlLst034_ac34Through38_videoSourceFilter | |
| ANL-LST-036 | AC-36 | YouTube filter | b | Grouped | anlLst034_ac34Through38_videoSourceFilter | |
| ANL-LST-037 | AC-37 | Google Drive filter | b | Grouped | anlLst034_ac34Through38_videoSourceFilter | |
| ANL-LST-038 | AC-38 | Source **الكل** | b | Grouped | anlLst034_ac34Through38_videoSourceFilter | |
| ANL-LST-039 | AC-39 | Combined criteria usable | d | Grouped | anlLst039_ac39Through42_combinedSearchAndFilters | |
| ANL-LST-040 | AC-40 | AND logic | d | Grouped | anlLst039_ac39Through42_combinedSearchAndFilters | |
| ANL-LST-041 | AC-41 | Change one criterion | d | Partial | anlLst039_ac39Through42_combinedSearchAndFilters | via combined test |
| ANL-LST-042 | AC-42 | Cards retain fields after filter | d | Grouped | anlLst039_ac39Through42_combinedSearchAndFilters | |
| ANL-LST-043 | AC-43 | **إعادة تعيين الفلاتر** | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-044 | AC-44 | Reset clears dates | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-045 | AC-45 | Reset status → **الكل** | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-046 | AC-46 | Reset source → **الكل** | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-047 | AC-47 | Reset keeps search | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-048 | AC-48 | Recalc after reset | b | Grouped | anlLst043_ac43Through48_resetFiltersPreservesSearch | |
| ANL-LST-049 | AC-49 | Global empty title | d | Skip | anlLst049_ac49_globalEmptyStateCopy | empty tenant + flag |
| ANL-LST-050 | AC-50 | Empty CTA | d | Grouped | anlLst049_ac49_globalEmptyStateCopy | with AC-49 |
| ANL-LST-051 | AC-51 | No-results title | d | Grouped | anlLst051_ac51Through53_noResultsDistinctFromEmpty | |
| ANL-LST-052 | AC-52 | No-results description | d | Grouped | anlLst051_ac51Through53_noResultsDistinctFromEmpty | |
| ANL-LST-053 | AC-53 | Distinct empty vs no-results | d | Grouped | anlLst051_ac51Through53_noResultsDistinctFromEmpty | |
| ANL-LST-054 | AC-54 | Loading state | d | Skip | anlLst054_ac54_loadingState | hooks |
| ANL-LST-055 | AC-55 | No empty while loading | d | Skip | anlLst055_ac55_emptyNotBeforeLoadCompletes | hooks |
| ANL-LST-056 | AC-56 | Load error copy | d | Skip | anlLst056_ac56_listRetrievalErrorCopy | hooks |
| ANL-LST-057 | AC-57 | **إعادة المحاولة** | d | Skip | anlLst057_ac57_errorRetryAction | hooks |
| ANL-LST-058 | AC-58 | Refresh latest status | d | Implemented | anlLst058_ac58_refreshShowsLatestStatus | |
| ANL-LST-059 | AC-59 | No duplicate records | d | Implemented | anlLst059_ac59_browseOperationsDoNotDuplicateRecords | |

**Test class:** `com.taf.systems.zulfi.features.analysis.listing.AnalysesListingAcceptanceTest`

**Suite:** `src/test/resources/suites/zulfi/anl-lst-suite.xml` (implemented; was listed as pending in older map)

**Gate:** `com.taf.systems.zulfi.config.AnlsLstGate` (`-Danl.lst.phase=`)

**Cross-reference:** HOME-001-007/028 navigation to list; ANL-STS-012 label consistency; do not duplicate full card matrix with HOME recent section in same Tier-1 batch.
