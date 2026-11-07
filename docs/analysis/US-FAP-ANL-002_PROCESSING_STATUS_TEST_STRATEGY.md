# US-FAP-ANL-002 — Test Strategy (Track Match Analysis Processing Status)

**User story:** Lifecycle `قيد التحليل` → `اكتمل التحليل`; Analysis Details breadcrumb `التحليلات > تفاصيل التحليل`; processing copy `التحليل قيد التنفيذ` + description; Dashboard/PDF disabled while processing, enabled when complete; no duplicate records on refresh; consistent Arabic labels across Home / List / Details.

**Related automation:** HOME-001 (recent cards AC-30…36), ANL-SYNC (schedule status), ANL-OFF-HP-001 (optional submit observe), ANL-003 (listing — shared `AnalysesListPage`).

---

## 1. Scope

| In scope | Reuses | Out of scope |
|----------|--------|--------------|
| Status retrieval on **Analysis Details** | `HomeRecentAnalysesSection`, `AnalysesListPage`, `CreateAnalysisPage` post-submit URL | Full ANL-004 field matrix |
| Processing vs completed UI (copy, dashboard/PDF DOM) | `HomeOverviewMessages` status labels | Dashboard interactions (ANL-005) |
| Cross-surface label consistency (AC-12) | `AnalysisStatusTrackingFlow` | PDF download bytes (ANL-005) |
| Data integrity read-only snapshots | Bound analysis IDs from env | Backend job orchestration |

---

## 2. Phases

| Phase | `-Danl.sts.phase=` | Focus | Submit? |
|-------|-------------------|-------|---------|
| **none** | default | All tests skipped | No |
| **a** | `a` | Label consistency, open first list details, lifecycle scope (AC-09, AC-12, AC-34, AC-35) | No |
| **b** | `b` | Processing record UI + navigation resilience (requires processing id) | No |
| **c** | `c` | Completed record outputs + integrity (requires completed id) | No |
| **d** | `d` | Post-submit observe **only** with `-Danl.sts.observeSubmit=true` **and** `-Danalysis.create.enabled=true` | **One** submit in `ANL-STS-001` |

**Analysis IDs (live data):**

| Property | Env | Purpose |
|----------|-----|---------|
| `-Danl.sts.processingAnalysisId=` | `ZULFI_PROCESSING_ANALYSIS_ID` | Phase **b** processing UI |
| `-Danl.sts.completedAnalysisId=` | `ZULFI_COMPLETED_ANALYSIS_ID` | Phase **c** completed UI |

---

## 3. Design gaps (document only — do not “fix” product in tests)

1. **Processing variant missing** — design shows completed state only; automation expects processing copy without enabled Dashboard/PDF.
2. **Breadcrumb** — product may show `نمذجة البيانات > تفاصيل الفيديو`; story requires `التحليلات > تفاصيل التحليل` (`AnalysisDetailsPage.breadcrumbShowsApprovedTrail()`).
3. **Entity naming** — `اسم الفيديو` vs **`اسم المباراة`**; `تفاصيل الفيديو` vs **`تفاصيل التحليل`**.
4. **Drive + YouTube mismatch** — AC-29 / BR-ANL-STS-010; test **fails** when source label and URL disagree (defect evidence).
5. **Delete on screen** — First Release must not expose delete on details (`pageExposesDeleteAction()` — informational in extended runs).
6. **PDF mock size** — e.g. 6.8 GB mock called out in story; expect dynamic size in production.

---

## 4. Architecture

```
AnalysisProcessingStatusAcceptanceTest
  → AnalysisStatusTrackingFlow (Home / list / direct id)
  → AnalysisDetailsPage (breadcrumb, status, processing copy, actions, metadata)
  → validations: AnalysisProcessingStatusMessages
  → gate: AnlsStsGate
```

Registry: **`ANL-STS_TEST_CASE_REGISTRY.md`**  
Cross-story wiring: **`ANL_CROSS_STORY_INTEGRATION.md`**

---

## 5. Run examples

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation

# Phase A — read-only label / scope smoke
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" `
  "-Danl.sts.phase=a" "-Dexecution.pace=fast"

# Phase B — processing analysis (portal id required)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" `
  "-Danl.sts.phase=b" "-DZULFI_PROCESSING_ANALYSIS_ID=123" "-Dexecution.pace=fast"

# Phase C — completed analysis
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" `
  "-Danl.sts.phase=c" "-DZULFI_COMPLETED_ANALYSIS_ID=456" "-Dexecution.pace=fast"

# Phase D — single observe submit (explicit)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" `
  "-Danl.sts.phase=d" "-Danl.sts.observeSubmit=true" `
  "-Danalysis.create.enabled=true" "-Danalysis.fixture.index=2" "-Dexecution.pace=demo"
```
