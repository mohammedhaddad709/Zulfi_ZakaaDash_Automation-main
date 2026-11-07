# MCH-ANL-010 — Final E2E Execution Report (Authorized Single Submit)

**Date:** 2026-10-17  
**Test case:** MCH-ANL-010  
**User story:** US-FAP-MCH-ANALYSIS  
**Authorization:** Explicit user approval for **one** submit on `externalEventId=16651987`

**Canonical handoff:** [MCH_ANALYSIS_EXECUTION_AND_DEFECT_REPORT.md](MCH_ANALYSIS_EXECUTION_AND_DEFECT_REPORT.md)

| Handoff flag | Status |
|--------------|--------|
| CREATION_CONFIRMED | **YES** |
| PERSISTENCE_VERIFIED | **YES** |
| INTERNAL_MATCH_MAPPING_UNVERIFIED | **YES** |
| DESCRIPTION_UI_DEFECT_OPEN | **YES** (MCH-ANL-007) |
| FULL_STORY_ACCEPTANCE_PENDING | **YES** |

---

## Fixture

| Field | Value |
|-------|-------|
| externalEventId | **16651987** |
| Home | نادي الطائي |
| Away | نادي الجندل |
| Date | 31/08/2026 |
| Result | 2-1 |
| Unique Description | `MCH-ANL-010 authorized E2E 20261017-171620` |
| Video URL | `AnalysisCreateGate.resolveVideoUrl()` (default YouTube test URL) |

---

## Execution summary (authorized submit — historical)

| Item | Value |
|------|-------|
| **Suite** | `match-schedule-analysis-mch-anl-010-submit-suite.xml` |
| **Method** | `mchAnl010_authorizedHappyPathSubmitFromSchedule` |
| **Run archive** | `test-execution-reports/history/20261017-141715-845` |
| **Submission timestamp (local)** | **~17:17** on 2026-10-17 |
| **Maven exit code** | **1** |
| **TestNG final status** | **FAILED** |
| **Failure class** | **POST_SUBMIT_AUTOMATION_DEFECT** |
| **Submission attempted** | **YES** |
| **Submit click count** | **1** (log: single click «بدء التحليل» at 17:17:13) |
| **Success message observed** | **«تم إرسال المباراة للتحليل»** (`verifySuccessUi()` completed) |

---

## Verdict matrix (required distinction)

| Dimension | Result | Evidence |
|-----------|--------|----------|
| **CREATION_CONFIRMED** | **YES** (via separate read-only follow-up) | Analysis **ID 15** on listing; teams; official type; status **قيد التحليل**; card time **02:17 PM** 17-Oct-2026. See [post-submit report](MCH_ANL_010_POST_SUBMIT_VERIFICATION_REPORT.md). |
| **PERSISTENCE_VERIFIED** | **YES** (read-only follow-up) | Same record after refresh (ID **15**). |
| **MAPPING_VERIFIED** | **NO** — **MAPPING_UNVERIFIED** | No internal Official Match ID; `16651987` ≠ analysis **15**. |
| **Original TestNG MCH-ANL-010 submit** | **FAILED** (unchanged) | Post-submit automation only — not retroactively PASS. |
| **Open defects** | **MCH-ANL-007-DESC-REQ-16651987** remains **OPEN** (FAILED unchanged) |

**Created Analysis ID (submit run automation):** **NOT CAPTURED** at failure time.

**Created Analysis ID (read-only recovery):** **15**

**Description on listing:** **Not visible** on card DOM; marker verified at submit only.

---

## Workflow evidence (steps 01–30)

| Steps | Outcome |
|-------|---------|
| 01–16 Pre-submit / form | **Executed** — login, schedule, target row, create URL, teams, date, description, video |
| 20 Pre-submit screenshot | Partial — post-submit screenshot at failure boundary |
| 22 Single submit | **YES** — exactly one click |
| 23–24 Success outcome | **SUCCESS UI verified** |
| 25 Analysis ID | **Not captured** in submit run |
| 26–30 Listing + refresh | **NOT COMPLETED** in submit run — sidebar blocked by dialog mask |

---

## Failure (no retry in submit suite)

**Classification:** **POST_SUBMIT_AUTOMATION_DEFECT** — not a product bug for Selenium click interception.

**Exception:** `ElementClickInterceptedException`  
**Location:** `AnalysesListPage.openViaSidebarNavigation` while success dialog mask (`p-dialog-mask`) covered the page.

**Actual URL at failure:** `.../analysis/create?externalEventId=16651987`

**Investigation artifact:** `test-execution-reports/history/20261017-141715-845/allure-results/8addcbae-762c-40ca-8ec8-55471d9ee696-attachment.md`

---

## Post-submit read-only recovery (linked execution)

| Item | Value |
|------|-------|
| Report | [MCH_ANL_010_POST_SUBMIT_VERIFICATION_REPORT.md](MCH_ANL_010_POST_SUBMIT_VERIFICATION_REPORT.md) |
| Suite | `match-schedule-analysis-mch-anl-010-readonly-followup-suite.xml` |
| Run archive | `test-execution-reports/history/20261017-142333-930` |
| TestNG | **PASSED** (`MCH-ANL-010-POST-SUBMIT-READONLY`) |
| Gates | `analysis.create.enabled=false`, `match.schedule.analysis.submit.enabled=false` |

This execution is **separate evidence** for creation and persistence; it does **not** rewrite the historical submit test result.

---

## Gates used (authorized submit only)

```
-Dmatch.schedule.analysis.phase=c
-Dexecution.pace=demo
-Danalysis.create.enabled=true
-Dmatch.schedule.analysis.submit.enabled=true
-Dmatch.schedule.analysis.targetExternalEventId=16651987
MCH_ANALYSIS_DESCRIPTION=MCH-ANL-010 authorized E2E 20261017-171620
```

---

## Automation fix (for future runs — no resubmit performed)

`MatchScheduleAnalysisVerificationFlow.verifyListingAndPersistence` updated to prefer **«عرض حالة التحليل»** or **`openDirectly()`** instead of sidebar click under success modal mask.

Read-only listing verification uses **`PostSubmitReadOnlyVerificationFlow`** with stable card snapshots (`AnalysesListPage.readVisibleAnalysisCards()`).

---

## Related open defect (unchanged)

[MCH-ANL-007-DESC-REQ-16651987](defects/MCH-ANL-007-DESC-REQ-16651987_description_required_indicator.md) — Description `*` vs optional AC — **FAILED** test result preserved.

---

## Screenshots / Allure

| Run | Archive |
|-----|---------|
| Authorized submit (FAILED) | `test-execution-reports/history/20261017-141715-845/allure-results/` |
| Read-only follow-up (PASSED) | `test-execution-reports/history/20261017-142333-930/allure-results/` |

---

## Final test status

| Test | Status |
|------|--------|
| **MCH-ANL-010 authorized E2E submit** | **FAILED** — post-submit automation |
| **MCH-ANL-010-POST-SUBMIT-READONLY** | **PASSED** |
| **Recovered evidence** | **CREATION_CONFIRMED**, **PERSISTENCE_VERIFIED**, **MAPPING_UNVERIFIED** |

**Do not submit again** for this fixture unless product confirms duplicate handling and BA re-authorizes.

---

## QA acceptance blocker & backend request

- **Acceptance blocker:** [MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md](acceptance-blockers/MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md)
- **Backend evidence request:** [MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md](backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md)
- **#ID visibility (pending product):** [MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md](discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md)
