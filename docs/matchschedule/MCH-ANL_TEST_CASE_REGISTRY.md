# MCH-ANL Test Case Registry — US-FAP-MCH-ANALYSIS (work item 33009)

**Story:** Start Official Match Analysis from Match Schedule  
**Class:** `com.taf.systems.zulfi.features.matchschedule.MatchScheduleAnalysisAcceptanceTest` (single reference for the story)  
**Suites:** `match-schedule-analysis-suite.xml` (read-only, never submits) · `match-schedule-analysis-submit-suite.xml` (authorized submit)  
**Gate:** `-Dmatch.schedule.analysis.phase=a|b|c` (default none → every method is skipped)  
**Reports:** each test publishes one *Test case specification* (objective, AC, BR, preconditions, test data, expected final result) and, per step, an expected-vs-actual table with its screenshot. Failures produce a bug report (`<output.dir>/bug-reports/LOCAL-*.md`) with the executed steps, root-cause analysis, classification, test data and links to the real screenshots and browser video.

> This registry replaces the previous MCH-ANL / MCH-SCHED-ANL registries. The nine overlapping classes they described were
> merged into the class above (their real checks were moved, the rest removed). Historical execution reports under
> `docs/matchschedule/MCH_ANL_010_*` and `test-execution-reports/` are kept unchanged as evidence.

## Observed product behaviour that shapes the tests

| Observation | Consequence |
|---|---|
| **Match Type is editable** on the schedule-bound view (list shows «رسمية» / «ودية»), although the story says Match Type is set by the System and not editable (screenshots 2026-10-21) | MCH-ANL-019 fails as PRODUCT_DEFECT until the control is locked. The friendly path is not part of the story and is not automated |
| The "pop-up" is the full page `/analysis/create?externalEventId=<id>` (leaving it = browser back) | Tests assert the route and the bound `externalEventId`; "close" means leaving the page |
| The Official Match selector does not show a numeric **#ID** | MCH-ANL-004 reports it as an open discrepancy (INCONCLUSIVE, pending BA) — see `discrepancies/` |
| The Description label shows a required marker `*` | MCH-ANL-007 fails as a PRODUCT_DEFECT (open) — see `defects/` |
| The internal Official Match ID is never displayed or exposed | Cannot be verified from the UI: **blocked**, needs backend evidence — see `backend-requests/` and `acceptance-blockers/` |

## Test cases

| ID | Phase | Covers | Title | Mutates data | Status |
|----|-------|--------|-------|--------------|--------|
| MCH-ANL-001 | a | AC Display Analysis Action · BR-001 | «تحليل المباراة» shown for a played, «غير محللة» match and bound to that match | No | Implemented |
| MCH-ANL-017 | a | BR-001 (played) | Unplayed matches offer no action | No | Implemented |
| MCH-ANL-013 | a | AC Prevent Duplicate · BR-001/006/007 | «قيد التحليل» / «اكتمل التحليل» matches offer no action | No | Implemented |
| MCH-ANL-002 | a | AC Open Pop-up · BR-002 | Action opens the create view for the clicked match; Match Type «رسمية», disabled | No | Implemented |
| MCH-ANL-003 | a | AC Preselect · BR-003 | Match preselected, selector locked, unchanged while editing | No | Implemented |
| MCH-ANL-004 | a | AC Display Official Match Info · AC Preserve Metadata Source · BR-004/009 | Clubs, date, result and #ID shown read-only and equal to the schedule row | No | Implemented — **expected to fail** on result/#ID until BA confirms (INCONCLUSIVE) |
| MCH-ANL-005 | a | AC Do Not Request Duplicate Metadata · BR-004 | No manual name/date/type/match pick; a Video URL alone enables submit | No | Implemented |
| MCH-ANL-006 | a | AC Close Without Submission | Leaving the view: match stays «غير محللة» with action; input discarded on reopen | No | Implemented |
| MCH-ANL-007 | b | AC Complete Existing Inputs | Description optional, Video URL mandatory | No | Implemented — **fails** on the open Description-marker defect |
| MCH-ANL-008 | b | BR-009 | Club names primary, #ID never before them | No | Implemented |
| MCH-ANL-009 | b | AC Complete Existing Inputs (YouTube/Drive) | YouTube/Drive accepted; malformed/unsupported blocked | No | Implemented (backend accessibility check not probed) |
| MCH-ANL-010 | c | AC Link Analysis to Official Match · BR-005 | One submit → exactly one official analysis; success UI; persists after refresh | **Yes (1 record)** | Implemented, gated |
| MCH-ANL-011 | c | AC Preserve Metadata Source · BR-004 | Details show the schedule match's clubs, the entered description and video | (grouped in 010) | Grouped in MCH-ANL-010 |
| MCH-ANL-012 | c | AC Match Identity Integrity · BR-005 | Record belongs to the clicked match; its schedule row becomes «قيد/اكتمل» without action | (grouped in 010) | Grouped in MCH-ANL-010 · internal-ID part **Blocked** |
| MCH-ANL-014 | c+probe | AC Concurrent Eligibility Change | Stale form rejected with «تم بدء تحليل هذه المباراة بالفعل. حدّث الصفحة لعرض حالتها الحالية.» | No new record expected | Grouped in MCH-ANL-010, needs `duplicateCheck.enabled` |
| MCH-ANL-015 | c+probe | BR-008 | Submit was still enabled in the stale form, yet the server revalidated and rejected | No new record expected | Grouped in MCH-ANL-010, needs `duplicateCheck.enabled` |
| MCH-ANL-016 | c+probe | AC Prevent Duplicate · BR-006/007 | No second record exists (listing) and the match still has one analysis | No new record expected | Grouped in MCH-ANL-010, needs `duplicateCheck.enabled` |
| MCH-ANL-019 | b | Data table: Match Type not editable · BR-002/003 | Match Type is system-set to «رسمية» and cannot be switched to «ودية» | No | Implemented — **fails** while the control is editable |
| MCH-ANL-018 | — | BR-005 / AC Link (internal ID) | Stored internal Official Match ID equals the schedule match | — | **Blocked** — not observable via UI; needs API/DB evidence |

### Acceptance-criteria coverage

| Acceptance criterion | Covered by |
|---|---|
| Display Analysis Action for Eligible Match | 001, 017, 013 |
| Open Official Analysis Pop-up | 002 |
| Preselect Selected Match | 003 |
| Display Official Match Information | 004, 008 |
| Do Not Request Duplicate Match Metadata | 005 |
| Complete Existing Analysis Inputs | 007, 009 |
| Link Analysis to Official Match | 010 |
| Preserve Official Match Metadata Source | 004, 011 |
| Prevent Duplicate Analysis | 013, 016 |
| Concurrent Eligibility Change | 014 |
| Close Pop-up Without Submission | 006 |
| Match Identity Integrity | 012 (UI-observable part), 018 (blocked) |
| Data table — Match Type system-set, not editable | 019 |
| Preselect Selected Match (entire flow) | 003 |

| Business rule | Covered by |
|---|---|
| BR-001 | 001, 017, 013 |
| BR-002 | 002 |
| BR-003 | 003 |
| BR-004 | 004, 005, 011 |
| BR-005 | 010, 012, 018 (blocked) |
| BR-006 / BR-007 | 013, 016 |
| BR-008 | 015 |
| BR-009 | 004, 008 |

## How to run

```powershell
# Read-only (phases a+b) — safe, never submits
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-suite.xml" "-Dmatch.schedule.analysis.phase=b" "-Dreport.video=ON_FAILURE"

# Authorized submit — creates ONE real analysis and consumes one «غير محللة» match
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-submit-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" "-Danalysis.create.enabled=true" "-Dmatch.schedule.analysis.submit.enabled=true" `
  "-Dmatch.schedule.analysis.targetExternalEventId=<id>" "-Dmatch.schedule.analysis.duplicateCheck.enabled=true" "-Dreport.video=ALWAYS"
```

* Phase a/b refuse to run with `-Danalysis.create.enabled=true`.
* The duplicate probe submits a **second, stale** form once; it must be rejected. It is the only deliberate extra click and needs its own flag.
* A pinned `targetExternalEventId` that is no longer «غير محللة» is **skipped**, never replaced silently.

## Evidence and bug reports

* **Screenshots:** one per step (`<id>_STEPnn_PASS|FAIL`), registered in the run's evidence list.
* **Video:** `@VideoEvidence(ON_FAILURE)` records the browser page (Chromium, also headless) and keeps the MP4 only for failures; `-Dreport.video=ALWAYS|OFF` overrides. The MP4 is attached to Allure and linked from the bug report.
* **Classification** is decided by the failing step (explicit), not by message keywords: `PRODUCT_DEFECT` (portal contradicts the story), `INCONCLUSIVE` (needs triage / pending BA), `TEST_DATA_ERROR`, `CONFIGURATION_ERROR`, `ENVIRONMENT_ERROR`, `AUTOMATION_DEFECT`.
* Missing preconditions (no eligible/unplayed/analysed match) are **skipped with the reason**, not failed.
