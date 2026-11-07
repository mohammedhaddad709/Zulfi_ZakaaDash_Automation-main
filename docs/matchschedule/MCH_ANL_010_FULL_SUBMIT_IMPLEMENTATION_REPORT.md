# MCH-ANL-010 — Full Submit Implementation Report

**Test case:** MCH-ANL-010 — Happy Path Submit from Match Schedule  
**User story:** US-FAP-MCH-ANALYSIS  
**Engineering status:** `READY_FOR_SINGLE_AUTHORIZED_SUBMIT`  
**Actual analysis created during engineering:** **NO**

---

## 1. Modified / new files

| File | Change |
|------|--------|
| `MatchScheduleAnalysisHappyPathFlow.java` | Refactored shared `prepareCreateFormThroughPreSubmitChecks`; added `runAuthorizedEndToEndSubmit` (steps 17–30) |
| `MatchScheduleAnalysisGate.java` | `requireExactTargetExternalEventIdForSubmit()` |
| `SchedulePreparedCreateContext.java` | **NEW** — form open after pre-checks |
| `ScheduleHappyPathSubmitResult.java` | **NEW** — post-submit evidence |
| `ScheduleOfficialMatchIdentityEvidence.java` | **NEW** — linkage status enum |
| `ScheduleOfficialMatchIdentityResolver.java` | **NEW** — UI + manifest mapping |
| `ScheduleCreateAnalysisFlow.java` | **NEW** — exactly-once schedule submit |
| `MatchScheduleAnalysisVerificationFlow.java` | **NEW** — success UI + listing + refresh |
| `OfficialFixtureManifest.java` | `findByExternalEventId` / `findByMatchId` |
| `KnownFixtureExclusions.java` | (prior) duplicate externalEventId / matchId guards |
| `AnalysesListPage.java` | `searchByAnalysisId`, `containsAnalysisIdentityMarker`, public `pageSourceContainsFixture` |
| `MatchScheduleAnalysisHappyPathSubmitAcceptanceTest.java` | **NEW** — authorized E2E test |
| `MatchScheduleAnalysisGateContractTest.java` | **NEW** — gate tests without browser |
| `match-schedule-analysis-mch-anl-010-submit-suite.xml` | **NEW** — single-test submit suite |
| `MatchScheduleAnalysisHappyPathAcceptanceTest.java` | Unchanged pre-submit test (still no submit) |

Pre-submit suite `match-schedule-analysis-mch-anl-010-presubmit-suite.xml` — **unchanged**, still non-mutating.

---

## 2. Dedicated submit suite

`src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-submit-suite.xml`

- **One** TestNG test method: `mchAnl010_authorizedHappyPathSubmitFromSchedule`
- **Not** referenced from default smoke/regression suites
- **Do not** run with `ANL-OFF-HP-001` in the same session

---

## 3. Exact TestNG method

`com.taf.systems.zulfi.features.matchschedule.MatchScheduleAnalysisHappyPathSubmitAcceptanceTest#mchAnl010_authorizedHappyPathSubmitFromSchedule`

Pre-submit mode (separate suite / method):

`MatchScheduleAnalysisHappyPathAcceptanceTest#mchAnl010_preSubmitHappyPathVerificationFromSchedule`

---

## 4. Fixture identity validation

Before submit:

1. `-Dmatch.schedule.analysis.targetExternalEventId=<verified-id>` **required** (env `MCH_ANALYSIS_EXTERNAL_EVENT_ID` accepted; `MCH_ANALYSIS_MATCH_ID` **not** accepted alone for submit).
2. Schedule row must match target (`externalEventId` or `matchIdNumeric` on row).
3. Row must remain played + «غير محللة».
4. Create URL `externalEventId` must equal configured target.
5. `KnownFixtureExclusions` rejects known duplicate fixtures.
6. `ScheduleOfficialMatchIdentityResolver` classifies linkage (UI `#ID`, manifest, URL binding, `MAPPING_UNVERIFIED`, `DISCREPANCY`).
7. **DISCREPANCY** → refuse submit (`IllegalStateException`).
8. **MAPPING_UNVERIFIED** → submit may complete but test **fails** at end with acceptance blocker (INCONCLUSIVE messaging).

Manifest (optional): `target/analysis-fixtures/latest-manifest.json` maps `externalEventId` → `matchId` when discovery was run.

---

## 5. Submission gate verification

Required **all**:

| Gate | Property |
|------|----------|
| Phase C | `-Dmatch.schedule.analysis.phase=c` |
| Creation | `-Danalysis.create.enabled=true` |
| Schedule submit | `-Dmatch.schedule.analysis.submit.enabled=true` |
| Pinned fixture | `-Dmatch.schedule.analysis.targetExternalEventId=<id>` |

`MatchScheduleAnalysisGateContractTest` (no browser):

- Submit without `analysis.create.enabled` → Skip
- Submit without `targetExternalEventId` → Skip
- Pre-submit with `analysis.create.enabled=true` → Skip

Submit suite run **without** gates → **SKIP** (no submit click; verified 2026-10-17).

---

## 6. One-click submission protection

- `AnalysisSubmissionAttempt.authorized("schedule:<externalEventId>")`
- `recordSubmitClick()` enforces single click; second click → `IllegalStateException`
- `ScheduleCreateAnalysisFlow.submitScheduleOfficialExactlyOnce` — no retry on failure
- Post-submit error → `PostSubmitOutcomeHandler` + optional `EnhancedBugReportWriter` markdown attachment

---

## 7. Success UI locators (real app)

| Element | Locator strategy |
|---------|------------------|
| Success message | `AnalysisSuccessDialog.SUCCESS_MESSAGE` = «تم إرسال المباراة للتحليل» — xpath contains text |
| View status | «عرض حالة التحليل» — button/a aria-label or text |
| Create another | «تحليل مباراة أخرى» |
| Submit button | `CreateAnalysisPage` — `button[aria-label='بدء التحليل']` |
| Success detection (fallback) | URL `/analysis/{id}`, success toast, page source message (`CreateAnalysisPage.isCreationSuccessState`) |

---

## 8. Analysis ID acquisition

1. `CreateAnalysisPage.extractAnalysisIdFromCurrentUrl(driver.getCurrentUrl())` after submit  
2. Fallback: same extractor on post-navigation URL  
3. Listing search: `AnalysesListPage.searchByAnalysisId(String.valueOf(id))` + `containsAnalysisIdentityMarker`

Test **fails INCONCLUSIVE** if outcome SUCCESS but ID remains null.

---

## 9. Official Match identity linkage verification

`ScheduleOfficialMatchIdentityResolver.resolve(...)` using:

- Configured target `externalEventId`
- Schedule row `externalEventId` / `matchIdNumeric`
- Create form URL parameter
- UI `#ID` from official match combobox label (regex `#(\d+)`, `مباراة #(\d+)`)
- Optional manifest entry from `OfficialFixtureManifestStore.load()`

**Does not** infer DB linkage from URL alone when internal ID differs and UI/manifest silent → `MAPPING_UNVERIFIED`.

Listing cross-check: team names + optional `externalEventId` in page source.

---

## 10. Analysis Listing verification

- Navigate: `AnalysesListPage.openViaSidebarNavigation()` (aria-label «التحليلات»)
- Official type: page source contains «مباراة رسمية»
- Find record: by analysis ID search first, else team-based title (`home × away`)
- Processing status: `readFirstVisibleCardStatus()` when exposed on card

---

## 11. Refresh persistence verification

`AnalysesListPage.refresh()` then re-assert ID marker or visible team title.

---

## 12. Compile result

```
mvn test-compile → PASS (2026-10-17)
```

---

## 13. Safe validation results

| Check | Result |
|-------|--------|
| `MatchScheduleAnalysisGateContractTest` | **PASS** (3 tests) |
| Submit suite without gates | **SKIP** (exit 0, no submit) |
| Pre-submit suite | Unchanged; still requires `analysis.create.enabled=false` |
| Real Create Analysis submission during engineering | **NO** |

---

## 14. Actual analysis created

**NO** — authorized submit was not executed with creation gates enabled.

---

## 15. Remaining blockers

| Blocker | Notes |
|---------|--------|
| Live authorized run | User must pick eligible «غير محللة» row and set `targetExternalEventId` |
| `MAPPING_UNVERIFIED` | May fail test after successful create until UI `#ID` or manifest mapping exists |
| US vs prod UX | Full-page create vs popup — **not** classified as product defect without BA sign-off |
| MCH-ANL-010 business PASS | Requires successful authorized run with persistence proof |

---

## 16. Exact PowerShell — ONE authorized real creation

Replace `<verified-externalEventId>` after confirming row on `/matches` (DevTools → link href `externalEventId=`).

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
$env:ZULFI_USER = "<portal email>"
$env:ZULFI_PASSWORD = "<portal password>"

mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-submit-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" `
  "-Dexecution.pace=demo" `
  "-Danalysis.create.enabled=true" `
  "-Dmatch.schedule.analysis.submit.enabled=true" `
  "-Dmatch.schedule.analysis.targetExternalEventId=<verified-externalEventId>"
```

Optional video override: `$env:ANALYSIS_VIDEO_URL = "https://..."`

**Do not** run `OfficialMatchAnalysisHappyPathTest` / `ANL-OFF-HP-001` in the same session.

Pre-submit only (no create):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-mch-anl-010-presubmit-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" "-Dexecution.pace=demo" `
  "-Danalysis.create.enabled=false" "-Dmatch.schedule.analysis.submit.enabled=false"
```

---

## Final readiness status

**READY_FOR_SINGLE_AUTHORIZED_SUBMIT**

MCH-ANL-010 is **not** marked fully PASSED until you run the submit suite above and persistence verification succeeds.
