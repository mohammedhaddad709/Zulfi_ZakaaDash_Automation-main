# MCH-ANL-010 Happy Path — Readiness Report

**User story:** US-FAP-MCH-ANALYSIS  
**Test case:** MCH-ANL-010 — Happy Path submit from Match Schedule  
**Status:** Pre-submit automation **ready**; full submit (steps 17–22) **blocked** until explicit authorization.

---

## Requirement vs implementation (BA confirmation required)

| User story expectation | Verified live behavior (MCH-ANL-002 baseline) |
|------------------------|---------------------------------------------|
| Official analysis opens as a **popup/modal** | Full page: `/analysis/create?externalEventId={id}` |
| Visible **Official Match #ID** on entry | Identity often bound via **URL `externalEventId`**; `#ID` may appear in combobox label or not on schedule card |
| Popup title «تحليل مباراة جديدة» | Same form `#analysis-create-form`; schedule route may omit dialog chrome |

**Automation stance:** Tests assert schedule row ↔ URL `externalEventId` ↔ form metadata. They **do not** treat URL identity alone as proof of database Official Match linkage. When schedule `matchIdNumeric` ≠ `externalEventId`, reporting uses `MAPPING_UNVERIFIED` / `DISCREPANCY` markers for BA/backend confirmation.

---

## Automation deliverables

| Artifact | Purpose |
|----------|---------|
| `MatchScheduleAnalysisHappyPathFlow` | Steps 1–16 pre-submit orchestration (reuse MCH-ANL-002 navigation; **no** Home fixture dropdown) |
| `MatchScheduleAnalysisHappyPathAcceptanceTest#mchAnl010_preSubmitHappyPathVerificationFromSchedule` | Dedicated pre-submit test |
| `match-schedule-analysis-mch-anl-010-presubmit-suite.xml` | Isolated suite (not ANL-OFF-HP-001) |
| `MatchScheduleAnalysisGate` | `requirePreSubmitVerificationMode()`, `SUBMIT_ENABLED_PROPERTY`, `targetExternalEventId` |
| `KnownFixtureExclusions` | Skips fixtures with verified prior analyses during scan |

**Phase A suite placeholder:** `MatchScheduleAnalysisAcceptanceTest#mchAnl010_happyPathSubmitFromSchedule` **SKIPs** with pointer to this suite (avoids accidental submit with old gate).

---

## Pre-submit scope (implemented)

1. Login (AuthenticatedUiTest)  
2. Navigate to Match Schedule (`/matches`)  
3. Find played Yellow League row «غير محللة» (optional target id)  
4. Capture row identity, teams, score, date  
5. Hover card → verify «بدء تحليل المباراة» visible  
6. Open create page  
7. Assert `externalEventId` in URL matches row when row exposes it  
8. Assert official match type  
9. Assert locked/bound official match + home/away + date + score on row  
10. Description left empty (optional)  
11. Enter valid video URL (`AnalysisCreateGate.resolveVideoUrl()`)  
12. Assert submit **enabled**  
13. Assert fixture **not** in `KnownFixtureExclusions`  
14. **Do not** click submit; close via back navigation  
15. Allure/DEMO steps + screenshot on pass  

**Gates enforced in pre-submit run:**

- `-Dmatch.schedule.analysis.phase=c`  
- `-Danalysis.create.enabled=false` (default)  
- `-Dmatch.schedule.analysis.submit.enabled=false` (default; test **fails skip** if true)

---

## Full happy path (steps 17–22) — not executed in safe mode

| Step | Action | Gate |
|------|--------|------|
| 17 | Submit exactly once | `-Danalysis.create.enabled=true` **and** `-Dmatch.schedule.analysis.submit.enabled=true` |
| 18 | Success toast/dialog | Live run only |
| 19–21 | Analysis ID, official match linkage, analyses listing | `AnalysisVerificationFlow` (reuse from ANL-OFF-HP-001 patterns; schedule-specific submit flow TBD) |
| 22 | Refresh persistence | Live run only |

**Safety rules for authorized submit run:**

- Bind fixture explicitly: `-Dmatch.schedule.analysis.targetExternalEventId=` or env `MCH_ANALYSIS_EXTERNAL_EVENT_ID` / `MCH_ANALYSIS_MATCH_ID`  
- Confirm row still «غير محللة» immediately before submit  
- Do **not** run in same session as `ANL-OFF-HP-001`  
- No retries after any submit attempt  
- Do not use known duplicate fixtures (see `KnownFixtureExclusions`)

---

## Commands

### 1) Compile

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn -q test-compile
```

### 2) Safe pre-submit verification (recommended now)

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
$env:ZULFI_USER = "<portal email>"
$env:ZULFI_PASSWORD = "<portal password>"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-presubmit-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" `
  "-Dexecution.pace=demo" `
  "-Danalysis.create.enabled=false" `
  "-Dmatch.schedule.analysis.submit.enabled=false"
```

Optional — pin a specific schedule row (when multiple eligible rows exist):

```powershell
  "-Dmatch.schedule.analysis.targetExternalEventId=<id>"
# or
  "-DMCH_ANALYSIS_EXTERNAL_EVENT_ID=<id>"
```

### 3) Authorized full submit (human gate — **do not run** until eligible match confirmed)

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
$env:ZULFI_USER = "<portal email>"
$env:ZULFI_PASSWORD = "<portal password>"
# TODO: enable dedicated submit test method + suite when Phase C submit orchestration is merged
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-presubmit-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" `
  "-Dexecution.pace=demo" `
  "-Danalysis.create.enabled=true" `
  "-Dmatch.schedule.analysis.submit.enabled=true" `
  "-Dmatch.schedule.analysis.targetExternalEventId=<verified-eligible-id>"
```

> **Note:** Submit orchestration (steps 17–22) is **not** wired in the pre-submit suite. The command above documents the **intended** authorization flags only. Implement `mchAnl010_authorizedSubmitFromSchedule` before running creation.

---

## Reporting fields (Allure / step result)

Pre-submit step emits:

- Selected fixture row text  
- Schedule `externalEventId` / `matchIdNumeric`  
- URL `externalEventId`  
- Authoritative Official Match ID (UI / mapping status)  
- Home / Away teams  
- Score and date  
- Video URL  
- `creationGate`, `submitGate`, `submitEnabled`, `submitAttempted=false`  
- Screenshot: `{MCH-ANL-010}_pre_submit_complete`

---

## Execution status

| Run | Credentials | Result | Notes |
|-----|-------------|--------|-------|
| Agent compile | — | **PASS** (`mvn test-compile`) | 2026-10-17 |
| Agent pre-submit browser | `ZULFI_USER` / `ZULFI_PASSWORD` in shell | **PASS** | Suite `match-schedule-analysis-mch-anl-010-presubmit-suite.xml`; phase `c`; create/submit gates **off**; ~49s; archive `test-execution-reports/history/20261017-134440-724` |
| Authorized submit (steps 17–22) | Not run | **BLOCKED** | Requires explicit target fixture + both creation gates; submit test method not merged yet |

---

## Next engineering tasks (post pre-submit PASS)

1. Add `mchAnl010_authorizedSubmitFromSchedule` + `match-schedule-analysis-mch-anl-010-submit-suite.xml` using `CreateAnalysisFlow.submitOfficialAnalysisExactlyOnce` (schedule form, no fixture index).  
2. Extend verification to search analyses list by team names / schedule label when `#ID` absent.  
3. BA sign-off on `externalEventId` ↔ Official Match ID mapping rules.
