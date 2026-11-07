# QA Acceptance Blocker — Internal Official Match ID Mapping (MCH-ANL-010)

| Field | Value |
|-------|-------|
| **Blocker ID** | MCH-ANL-010-AC-BLOCKER-INTERNAL-MATCH-MAPPING |
| **User story** | US-FAP-MCH-ANALYSIS |
| **Related test cases** | MCH-ANL-010, MCH-ANL-011, MCH-ANL-012 |
| **Type** | **Acceptance blocker** (missing verification evidence — not a proven backend defect) |
| **Status** | **OPEN** |
| **Analysis under review** | **15** |
| **Schedule external event** | **16651987** |
| **Fixture** | نادي الطائي × نادي الجندل — 31/08/2026 — 2-1 |

---

## Business outcomes already verified (automation + read-only follow-up)

| Outcome | Status | Evidence |
|---------|--------|----------|
| Single authorized submission | **YES** (once) | Run `20261017-141715-845` |
| Success UI «تم إرسال المباراة للتحليل» | **Observed** | Submit flow before post-submit automation failure |
| Analysis record exists | **YES** | Analysis ID **15** |
| Unique description marker | **YES** | `MCH-ANL-010 authorized E2E 20261017-171620` on `/analysis/15` |
| Fixture identity (teams) | **YES** | Details match name |
| Analysis status | **قيد التحليل** | Details + listing |
| Persistence after refresh | **YES** | Read-only follow-up `20261017-142333-930` |
| Second submission | **NO** | Gates and phase discipline |

**Historical submit TestNG result:** **FAILED** (`POST_SUBMIT_AUTOMATION_DEFECT`) — **must not** be rewritten to PASS.

---

## What is blocked

The user story requires storing the **correct Official Match ID** and displaying **#ID** within the selected official match information.

Automation and read-only UI probes **cannot** establish:

1. The **internal Official Match ID** stored for Analysis **15**.
2. The authoritative mapping **`externalEventId=16651987` ↔ internal Official Match ID**.
3. That the stored ID equals the schedule row’s internal match identity (when distinct from `externalEventId`).

**Verdict:** **`INTERNAL_MATCH_MAPPING_UNVERIFIED`** (same as historical `MAPPING_UNVERIFIED`).

We **do not** claim the backend relationship is broken — only that **verification is not possible** with current UI and undocumented API access.

---

## What was tried (read-only; no creation)

| Probe | Result |
|-------|--------|
| Analyses listing + refresh | Record **15** found; description not on card |
| `/analysis/15` details | Marker + teams confirmed |
| Page source JSON identity keys | **None** observed |
| Session GET to heuristic API paths | **404** — no documented contract |
| `/analysis/create?externalEventId=16651987` (read-only) | URL binding **16651987**; combobox **#ID** **not visible** |
| Schedule row scan (post-analysis) | Row not located in automation scroll window |

Detail: [MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md](../MCH_ANL_010_ANALYSIS_15_IDENTITY_MAPPING_REPORT.md)

---

## Unblock criteria

Acceptance for MCH-ANL-010 / MCH-ANL-011 / MCH-ANL-012 can proceed when **authoritative backend evidence** is provided (see [Backend evidence request](../backend-requests/MCH-ANL-010-BACKEND-EVIDENCE-REQUEST.md)), then:

1. Implement a **small read-only contract test** (GET analysis by id) using the **documented** endpoint and auth — **without** recreating or mutating Analysis **15**.
2. Compare returned Official Match ID with `externalEventId` **16651987** per backend’s defined semantics.
3. If product confirms #ID must appear on Create Analysis UI, re-evaluate [requirement discrepancy](../discrepancies/MCH-ANL-010-CREATE-UI-OFFICIAL-MATCH-ID-VISIBILITY.md) separately from backend mapping.

---

## Separate from product defects

| Item | Classification |
|------|----------------|
| MCH-ANL-007 Description `*` vs optional | **PRODUCT_DEFECT** — **OPEN** — [defect file](../defects/MCH-ANL-007-DESC-REQ-16651987_description_required_indicator.md) |
| Post-submit sidebar / dialog mask | **AUTOMATION_DEFECT** — not a product bug filing |
| Internal match mapping | **ACCEPTANCE BLOCKER** — this document |

---

## Allure / run archives (preserved — do not delete)

| Run | Purpose |
|-----|---------|
| `test-execution-reports/history/20261017-141715-845` | Authorized submit (FAILED) |
| `test-execution-reports/history/20261017-142333-930` | Read-only listing / persistence (PASSED) |
| `test-execution-reports/history/20261017-143400-664` | Analysis 15 details / mapping probe (PASSED) |
