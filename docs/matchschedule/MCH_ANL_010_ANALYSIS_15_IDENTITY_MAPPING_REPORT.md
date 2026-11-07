# MCH-ANL-010 — Analysis 15 Read-Only Identity & Mapping Probe

**Date:** 2026-10-17  
**Scope:** Read-only — no submit, gates off  
**Run archive (latest):** `test-execution-reports/history/20261017-143400-664`  
**Test:** `MCH-ANL-010-DETAILS-MAPPING-READONLY`  
**Suite:** `match-schedule-analysis-mch-anl-010-details-mapping-suite.xml`

---

## Command

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-details-mapping-suite.xml" `
  "-Danalysis.create.enabled=false" "-Dmatch.schedule.analysis.submit.enabled=false" `
  "-Dexecution.pace=demo"
```

---

## What was checked

| Layer | Action |
|-------|--------|
| **Details UI** | `GET` `/analysis/15` — read labels (description, match name, status) |
| **Details HTML** | Scan page source for JSON keys (`externalEventId`, `officialMatchId`, `matchId`) |
| **Session HTTP GET** | Cookie-authenticated probes to heuristic API paths (all **404**) |
| **Schedule** | Read-only scroll on `/matches` for fixture identity (row **not** found in scan window) |
| **Create route (read-only)** | Navigate to `/analysis/create?externalEventId=16651987` — read URL + Official Match combobox `#ID` only (**no** «بدء التحليل») |

---

## Confirmed evidence (Analysis 15)

| Field | Value |
|-------|-------|
| **Analysis ID** | **15** |
| **Description (details page)** | `MCH-ANL-010 authorized E2E 20261017-171620` |
| **Match name** | نادي الطائي × نادي الجندل |
| **Status** | قيد التحليل |
| **Target schedule binding** | `externalEventId=16651987` (from read-only create URL; **not** in details visible text) |

This **conclusively ties Analysis 15** to the authorized MCH-ANL-010 run (unique description + fixture teams). That is stronger than listing-only proof (listing did not show the description text).

---

## Official Match internal ID

| Check | Result |
|-------|--------|
| Details page — internal Official Match ID | **Not shown** |
| Create form — `#ID` / combobox numeric Official Match | **Empty** (same as pre-submit `20261017-140612-377`) |
| Page source JSON keys | **None** captured on details or create |
| Guessed REST endpoints (session cookie) | All **404** — real API path not documented in automation |
| Schedule row `#ID` / `matchIdNumeric` | **Not captured** (row not located after analysis; eligible-row scanner excludes analyzed fixtures) |

---

## Required verdicts

| Verdict | Status |
|---------|--------|
| **CREATION_CONFIRMED** | **YES** — details description marker + teams |
| **PERSISTENCE_VERIFIED** | **YES** (prior follow-up `20261017-142333-930`) |
| **MAPPING_VERIFIED** (internal Official Match ID ↔ `externalEventId`) | **NO** — **MAPPING_UNVERIFIED** |
| **External event binding** (Analysis 15 ↔ `16651987`) | **Observed** via authorized description + create URL binding; **not** the same as internal DB Official Match ID |

**Do not** treat `externalEventId` **16651987** as the internal Official Match ID or as Analysis ID **15**.

---

## Automation added

- `AnalysisDetailsReadOnlyMappingFlow` / `AnalysisDetailsReadOnlyMappingResult`
- `AnalysisPageSourceIdentityScanner`, `PortalSessionReadOnlyHttp`, `PortalReadOnlyApiCandidates`
- `MatchSchedulePage.findRowContextByFixtureIdentityReadOnly(...)` (post-analysis schedule rows)
- `MatchScheduleAnalysisDetailsMappingReadOnlyTest`

---

## Recommendations (product / backend)

1. Expose **internal Official Match ID** (or explicit `externalEventId`) on **Analysis details** for official analyses.  
2. Document the **read-only GET** contract for analysis by id (path + auth) for QA/backend verification.  
3. Optional: show description on **listing cards** for traceability.

**Acceptance blocker unchanged:** internal Official Match ID mapping for US-FAP-MCH-ANALYSIS AC.
