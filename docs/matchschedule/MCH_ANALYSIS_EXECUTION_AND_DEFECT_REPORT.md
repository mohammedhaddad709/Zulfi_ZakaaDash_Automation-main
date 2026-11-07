# MCH Analysis — Canonical Execution & QA Handoff (US-FAP-MCH-ANALYSIS)

**Last updated:** 2026-10-17 — **FINAL QA HANDOFF (MCH-ANL-010)**  
**Project:** `Football_Auto_TAF_Foundation`  
**Primary test case:** MCH-ANL-010  
**Analysis ID:** **15**  
**Schedule external event:** **16651987**  
**Fixture:** نادي الطائي × نادي الجندل — 31/08/2026 — 2-1  

---

## Handoff status matrix (authoritative)

| Flag | Status |
|------|--------|
| **CREATION_CONFIRMED** | **YES** — single submit; success UI; Analysis **15** with unique description marker |
| **PERSISTENCE_VERIFIED** | **YES** — read-only follow-up after refresh |
| **INTERNAL_MATCH_MAPPING_UNVERIFIED** | **YES** — internal Official Match ID not verifiable via current UI / documented API |
| **DESCRIPTION_UI_DEFECT_OPEN** | **YES** — MCH-ANL-007 (`وصف *` vs optional AC) **FAILED / OPEN** |
| **FULL_STORY_ACCEPTANCE_PENDING** | **YES** — blocked on mapping evidence + open AC/defect items |

**Do not** interpret the above as **PASS** on the historical authorized **submit** TestNG method — that run remains **FAILED** (`POST_SUBMIT_AUTOMATION_DEFECT`).

---

## Verified business outcomes

| Evidence | Detail |
|----------|--------|
| Submissions performed | **Exactly one** (authorized) |
| Success UI | «تم إرسال المباراة للتحليل» observed in submit flow |
| Analysis ID | **15** (≠ `16651987`, ≠ externalEventId used as analysis id) |
| Description marker | `MCH-ANL-010 authorized E2E 20261017-171620` on **`/analysis/15`** |
| Teams | نادي الطائي × نادي الجندل |
| Status | **قيد التحليل** |
| Persistence | Same record after browser refresh (read-only suite) |
| Second submission | **None** |

---

## Test & automation results (historical — preserved)

| Execution | Archive | TestNG | Notes |
|-----------|---------|--------|-------|
| MCH-ANL-010 authorized submit | `test-execution-reports/history/20261017-141715-845` | **FAILED** | Post-submit listing automation; **1×** «بدء التحليل» |
| MCH-ANL-010 post-submit read-only | `test-execution-reports/history/20261017-142333-930` | **PASSED** | Listing + persistence; separate evidence |
| MCH-ANL-010 details / mapping read-only | `test-execution-reports/history/20261017-143400-664` | **PASSED** | Analysis 15 metadata; mapping still unverified |
| MCH-ANL-010 pre-submit | `test-execution-reports/history/20261017-140612-377` | **PASSED** | No submit |
| MCH-ANL-007 | See defect + suite history | **FAILED** | Product defect — unchanged |

**Allure:** Historical results under `test-execution-reports/history/*` — **preserve; do not overwrite or delete.**

---

## Confirmed product defects vs acceptance blockers

| ID | Type | Status | Document |
|----|------|--------|----------|
| **MCH-ANL-007-DESC-REQ-16651987** | **PRODUCT_DEFECT** (Description required indicator) | **OPEN** / test **FAILED** | [defects/MCH-ANL-007-DESC-REQ-16651987_description_required_indicator.md](defects/MCH-ANL-007-DESC-REQ-16651987_description_required_indicator.md) |
| **MCH-ANL-010-AC-BLOCKER-INTERNAL-MATCH-MAPPING** | **ACCEPTANCE BLOCKER** (missing verification evidence) | **OPEN** | [acceptance-blockers/MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md](acceptance-blockers/MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md) |
| Post-submit dialog mask / sidebar | **AUTOMATION_DEFECT** | Documented | Not a product bug for click interception |
| Create UI **#ID** visibility vs US wording | **Discrepancy (pending product)** | **Pending** | [discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md](discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md) |

QA **does not** claim the backend storage relationship is broken without authoritative backend data.

---

## Remaining blocker (summary)

**Internal Official Match ID** for Analysis **15** and its relationship to **`externalEventId=16651987`** cannot be verified with available UI and undocumented API access → **`INTERNAL_MATCH_MAPPING_UNVERIFIED`**.

User story AC (store correct Official Match ID; show **#ID** in official match info) is **not fully accepted** until backend evidence and/or product clarification.

**Backend request:** [backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md](backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md)

**After authoritative mapping:** implement **read-only API contract check** only — **no** submit, **no** recreate Analysis **15**.

---

## Phase discipline (STOP)

| Action | Allowed |
|--------|---------|
| Read-only listing / details / documented GET | Yes (after backend documents API) |
| Authorized submit suite for 16651987 | **NO** (authorization consumed) |
| `analysis.create.enabled=true` / submit gates for this fixture | **NO** |
| Additional creation tests | **NO** |
| Rewrite submit TestNG FAILED → PASS | **NO** |

---

## Detailed reports (index)

| Report | Purpose |
|--------|---------|
| [MCH_ANL_010_FINAL_E2E_EXECUTION_REPORT.md](MCH_ANL_010_FINAL_E2E_EXECUTION_REPORT.md) | Submit run + recovery summary |
| [MCH_ANL_010_POST_SUBMIT_VERIFICATION_REPORT.md](MCH_ANL_010_POST_SUBMIT_VERIFICATION_REPORT.md) | Read-only persistence |
| [MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md](MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md) | Details + mapping probe |
| [MCH_ANL_010_HAPPY_PATH_READINESS_REPORT.md](MCH_ANL_010_HAPPY_PATH_READINESS_REPORT.md) | BA / DOM baseline |

---

## Human / team next steps

1. **Backend:** Respond to [evidence request](backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md).  
2. **Product / BA:** Confirm **#ID** visibility requirement on schedule create route ([discrepancy doc](discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md)).  
3. **QA Automation:** Wire read-only contract test when API is documented; keep MCH-ANL-007 **FAILED**.  
4. **All:** Do **not** resubmit fixture **16651987** without duplicate check and explicit re-authorization.

**End of handoff.**
