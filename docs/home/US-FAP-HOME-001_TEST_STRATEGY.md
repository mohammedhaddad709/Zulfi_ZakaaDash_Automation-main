# US-FAP-HOME-001 — Test Strategy

**User story:** After login, Home shows **Hero**, **Analysis Summary**, **Upcoming Match**, and **Recent Analyses** — read-only browsing with navigation to **تحليل مباراة جديدة** (dialog) and **التحليلات** (list).

**Overlap:** Same physical Home page as **US-FAP-HOME-CURRENT-RESULTS** (`CurrentTeamResultsSection` / `نتائج الفريق الحالية`). HOME-001 does **not** assert current team results.

---

## 1. Phases

| Phase | `-Dhome.overview.phase=` | Focus |
|-------|---------------------------|--------|
| **none** | default | All skipped |
| **a** | `a` | AC-01 login landing, AC-02 Skip (role parity note), AC-03…07 hero & navigation, AC-28 View More, AC-44 hero resilience |
| **b** | `b` | AC-08…14 summary KPIs (incl. **هداف الفريق**) + `إجمالي = قيد + اكتمل`, AC-15…22 upcoming, AC-15 title singular/plural, AC-23…29 recent structure, no delete |
| **c** | `c` | AC-27, AC-30…37 processing/completed dashboard DOM + navigation smoke (not deep ANL-004/005) |
| **d** | `d` | AC-38…45 empty/loading/error — **Skip** unless `-Dhome.overview.allowEmptyStateChecks=true` or `-Dhome.overview.allowErrorStateHooks=true` |

**Mutations:** Opening create-analysis dialog and analyses list is allowed. **Never** click **بدء التحليل** submit on the create form.

---

## 2. Architecture

```
HomeOverviewAcceptanceTest
  → HomeOverviewFlow
  → HomeOverviewPage
       → HomeHeroSection
       → HomeAnalysisSummarySection
       → HomeUpcomingMatchSection
       → HomeRecentAnalysesSection
  → CreateAnalysisPage (dialog title only)
  → AnalysesListPage (list route)
```

Registry: **`HOME-001_TEST_CASE_REGISTRY.md`**

---

## 3. Suite

`src/test/resources/suites/zulfi/home-overview-suite.xml`

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/home-overview-suite.xml" `
  "-Dhome.overview.phase=a" "-Dexecution.pace=fast"
```

Phase **b** (KPI math + recent/upcoming):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/home-overview-suite.xml" `
  "-Dhome.overview.phase=b" "-Dexecution.pace=fast"
```

Phase **c** (status/dashboard navigation):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/home-overview-suite.xml" `
  "-Dhome.overview.phase=c" "-Dexecution.pace=fast"
```
