# Analysis — Cross-Story Integration Map

Shared Arabic lifecycle labels and navigation entry points across analysis-related user stories.

## Status labels (single source)

| Story | Class | Labels |
|-------|--------|--------|
| US-FAP-ANL-002 / ANL-STS | `AnalysisProcessingStatusMessages` | **قيد التحليل**, **اكتمل التحليل** |
| US-FAP-ANL-SYNC | `MatchScheduleAnalysisStatusMessages` | Same in-progress/completed strings on schedule rows |
| US-FAP-HOME-001 | `HomeOverviewMessages` | Recent cards + KPIs |
| **US-FAP-ANL-003** | `AnalysesListingMessages` → re-exports `AnalysisProcessingStatusMessages` | Listing cards + filters |

Listing assertions for status and card actions (**عرض التفاصيل**, **عرض لوحة التحليل**) must stay aligned with **ANL-STS** details page behavior.

## Navigation into **التحليلات**

| From | Story | Automation |
|------|--------|------------|
| Sidebar **التحليلات** | ANL-LST AC-01 | `AnalysesListPage.openViaSidebarNavigation()` |
| Home Hero **استعراض التحليلات** | HOME-001 AC-07 | `HomeOverviewFlow.openAnalysesListFromHero()` |
| Home **آخر التحليلات** → **عرض المزيد** | HOME-001 AC-28 | `HomeOverviewFlow.navigateToAnalysesViaRecentViewMore()` / `AnalysesListingFlow.openViaHomeRecentViewMore()` |

## Suites

| Story | Suite | Property |
|-------|--------|----------|
| HOME-001 | `home-overview-suite.xml` | `-Dhome.overview.phase=` |
| ANL-STS (ANL-002) | `anl-sts-suite.xml` (when present) | `-Danl.sts.phase=` |
| **ANL-LST (ANL-003)** | `anl-lst-suite.xml` | `-Danl.lst.phase=` |

## Out of scope overlaps

- **ANL-004** — Analysis Details deep assertions (listing only smokes navigation).
- **ANL-005** — Dashboard/PDF (listing only checks enabled/disabled on cards).
- **ANL-001** — Create submit (listing may open **تحليل مباراة جديدة** dialog only).
