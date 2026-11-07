# MCH-ANL-010 — Post-Submit Read-Only Verification Report

**Phase:** MCH-ANL-010 — POST-SUBMISSION RECOVERY  
**Date:** 2026-10-17  
**User story:** US-FAP-MCH-ANALYSIS  
**Test case:** MCH-ANL-010 (follow-up only — **no second submit**)

---

## Linked authorized submission

| Field | Value |
|-------|-------|
| Original submission (local) | **2026-10-17 ~17:17** (archive `20261017-141715-845`) |
| Target fixture | `externalEventId=16651987` |
| Home / Away | نادي الطائي × نادي الجندل |
| Match date | 31/08/2026 |
| Result | 2-1 |
| Unique description (submit form) | `MCH-ANL-010 authorized E2E 20261017-171620` |
| Success UI observed (original run) | **«تم إرسال المباراة للتحليل»** |
| Original Allure run archive | `test-execution-reports/history/20261017-141715-845` |
| Original TestNG status | **FAILED** — `POST_SUBMIT_AUTOMATION_DEFECT` (unchanged) |

---

## Read-only follow-up execution

| Field | Value |
|-------|-------|
| Suite | `match-schedule-analysis-mch-anl-010-readonly-followup-suite.xml` |
| Test | `MatchScheduleAnalysisE2eReadOnlyFollowUpTest#readOnlyListingProbeByDescriptionMarker` |
| Test ID | `MCH-ANL-010-POST-SUBMIT-READONLY` |
| Run archive | `test-execution-reports/history/20261017-142333-930` |
| Maven / TestNG | **BUILD SUCCESS** — 1 test, 0 failures |
| Gates | `-Danalysis.create.enabled=false` `-Dmatch.schedule.analysis.submit.enabled=false` `-Dexecution.pace=demo` |
| Marker | `-Dmch.e2e.description.marker=MCH-ANL-010 authorized E2E 20261017-171620` |

**Command (credentialed terminal):**

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-readonly-followup-suite.xml" `
  "-Danalysis.create.enabled=false" "-Dmatch.schedule.analysis.submit.enabled=false" `
  "-Dexecution.pace=demo" `
  "-Dmch.e2e.description.marker=MCH-ANL-010 authorized E2E 20261017-171620"
```

---

## Verification outcomes (required taxonomy)

| Verdict | Status | Notes |
|---------|--------|-------|
| **CREATION_CONFIRMED** | **YES** (recovered evidence) | Listing shows analysis **ID 15** for نادي الطائي × نادي الجندل, official type, status **قيد التحليل**, card timestamp **١٧ أكتوبر ٢٠٢٦ في ٠٢:١٧ م** aligned with authorized submit minute. Not re-written as original TestNG PASS. |
| **PERSISTENCE_VERIFIED** | **YES** | After browser refresh, record still found via analysis ID **15** (read-only re-query). |
| **MAPPING_VERIFIED** | **NO** | **MAPPING_UNVERIFIED** — no internal Official Match ID exposed; `externalEventId=16651987` ≠ Analysis ID **15**. |

---

## Record found (live DOM)

| Attribute | Observed value |
|-----------|----------------|
| **Analysis ID** | **15** (distinct from `externalEventId` 16651987) |
| **Teams on card** | نادي الطائي × نادي الجندل |
| **Official classification** | **مباراة رسمية** (visible on card) |
| **Analysis status** | **قيد التحليل** |
| **Listing card excerpt** | See Allure attachment `Post-submit read-only evidence` in run `20261017-142333-930` |

### Description marker on listing

| Check | Result |
|-------|--------|
| Search by full marker | Attempted (among multiple `searchByMatchName` queries) |
| Marker visible on analysis card text | **NO** (`descriptionVisibleOnListing=false`) |
| Marker in page source only | Not used as sole proof |

**Interpretation:** The authorized description was entered at submit time and success UI was shown, but the **Analyses listing cards do not surface the free-text description** in the captured DOM. Identity for this follow-up relies on **Analysis ID 15**, **both team names**, **official type**, **status**, and **creation timestamp correlation** with the single authorized submit — not on description text alone.

---

## Identifier separation

| Identifier | Value | Role |
|------------|-------|------|
| Schedule **externalEventId** | 16651987 | External event / create URL parameter |
| **Analysis ID** | **15** | Portal analysis record identifier (captured read-only) |
| **Internal Official Match ID** | *Not observed* | **MAPPING_UNVERIFIED** |

---

## Automation defect — root cause (original submit run)

Investigation confirms the failure occurred **after** product success UI validation, during post-submit navigation to the analyses listing.

1. **`verifySuccessUi()`** completed — success message **«تم إرسال المباراة للتحليل»** and view-status action were asserted (`MatchScheduleAnalysisVerificationFlow`, `AnalysisSuccessDialog`).
2. **Listing navigation** attempted while the PrimeVue success dialog **modal mask** (`p-dialog-mask`, z-index 1101) still intercepted pointer events.
3. **`AnalysesListPage.openViaSidebarNavigation`** (sidebar «التحليلات») threw **`ElementClickInterceptedException`** — mask received the click instead of the sidebar link.
4. **Analysis ID extraction** from create URL failed because URL remained `/analysis/create?externalEventId=16651987` (no `/analysis/{id}` redirect captured in that run).
5. **Earlier read-only probe** (pre-stabilization) hit **`StaleElementReferenceException`** when reusing card `WebElement` references after Vue re-render.

**Classification:** Automation / timing — **not** filed as a product defect for click interception.

**Stabilization (read-only phase):**

- `PostSubmitReadOnlyVerificationFlow` + `AnalysesListPage.openDirectly()` (bypass blocked sidebar).
- `waitUntilListingResultsStable()`, `readVisibleAnalysisCards()` → **`AnalysesListCardSnapshot`** (immutable text snapshots, no stale element reuse).
- `MatchScheduleAnalysisVerificationFlow.verifyListingAndPersistence` prefers **«عرض حالة التحليل»** then **`openDirectly()`** when success dialog is still present.

**Evidence artifacts (original failure):**

- `test-execution-reports/history/20261017-141715-845/allure-results/8addcbae-762c-40ca-8ec8-55471d9ee696-attachment.md`
- `.../e732c1a9-bdbf-45df-ad6f-b5a7e4db1d9c-attachment.txt`

---

## Files touched for this phase (automation)

| Area | Files |
|------|--------|
| Read-only flow | `PostSubmitReadOnlyVerificationFlow.java`, `PostSubmitReadOnlyVerificationResult.java` |
| Listing stability | `AnalysesListPage.java`, `AnalysesListCardSnapshot.java` |
| Follow-up test | `MatchScheduleAnalysisE2eReadOnlyFollowUpTest.java` |
| Suite | `match-schedule-analysis-mch-anl-010-readonly-followup-suite.xml` |
| Post-submit path (future submits) | `MatchScheduleAnalysisVerificationFlow.java` |

---

## Remaining product defects and acceptance blockers

| Item | Status |
|------|--------|
| **MCH-ANL-007** — Description required indicator (`وصف *` vs optional AC) | **FAILED / OPEN** — unchanged |
| **Official Match internal ID** vs schedule `externalEventId` | **MAPPING_UNVERIFIED** — acceptance blocker |
| **Listing UX** — description not shown on cards | Observation only; blocks marker-based listing proof |

---

## Final verification status (this phase)

| Layer | Status |
|-------|--------|
| **MCH-ANL-010 authorized submit test (historical)** | **FAILED** — `POST_SUBMIT_AUTOMATION_DEFECT` |
| **Post-submit read-only follow-up (`MCH-ANL-010-POST-SUBMIT-READONLY`)** | **PASSED** |
| **Recovered business evidence** | **CREATION_CONFIRMED**, **PERSISTENCE_VERIFIED**, **MAPPING_UNVERIFIED** |

**Stop condition met:** Read-only verification complete. **No second submission** performed.

---

## Follow-up — Analysis 15 details & mapping probe (2026-10-17)

See **[MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md](MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md)**.

| Finding | Result |
|---------|--------|
| Description on **details** `/analysis/15` | **Confirmed** — full authorized marker |
| Listing description | Not shown (prior run) |
| `externalEventId=16651987` | Confirmed via read-only create URL only |
| Internal **Official Match ID** | **Not exposed** — **MAPPING_UNVERIFIED** |
| Run archive | `test-execution-reports/history/20261017-143400-664` |
