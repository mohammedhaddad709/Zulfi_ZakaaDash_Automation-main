# Phase 12.2 — Index 0 Smoke & Team Field Reading Stabilization

**Project:** `E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation`  
**Date:** 2026-10-16  
**Scope:** Index 0 smoke only (`analysis-fixture-sel-000-suite.xml`); no full 46-index suite; no submit (`analysis.create.enabled=false`)  
**Overall status:** **READY FOR CREDENTIALED RE-RUN** (automation fix implemented; live Index 0 **not PASS** in agent shell)

---

## 1. Actual root cause (field-reading issue)

| Layer | Finding |
|-------|---------|
| **Classification** | **AUTOMATION_DEFECT** (not PRODUCT_DEFECT) |
| **Symptom** | `waitForOfficialFixtureAutofill()` timed out with `home=` / `away=` while fixture label and date were correct. |
| **Evidence** | Phase 12.1 failure screenshot shows **الأخدود** / **نادي البكيرية** visibly in team fields; defect attachment `home=` / `away=` empty. |
| **Cause** | Team names are bound on the **inner PrimeVue `input`** via DOM **properties** (`value` / `placeholder`). Legacy reads used HTML **attributes** (`getAttribute("value"|"placeholder")`) and `fieldText()`, which often return empty while the UI is correct. |
| **Secondary** | `readinessDiagnostics()` previously echoed attribute-based `getHomeTeamValue()` / `getAwayTeamValue()`, masking the real display source in failure messages. |

**Phase 12.2 fix:** Read-only `observeTeamField()` / `TeamFieldObservation` using `document.getElementById(fieldId)` + inner `input:not([type=hidden])`, resolving display text in strict priority: `domProperty.placeholder` → `domProperty.value` → attribute fallbacks → `input.textContent` (no polluted `root.textContent` fallback).

---

## 2. Resolved DOM structure (Home / Away)

| Item | Home | Away |
|------|------|------|
| **Stable id (resolved root)** | `#analysis-home-team` | `#analysis-away-team` |
| **Resolution** | `document.getElementById("analysis-home-team")` | `document.getElementById("analysis-away-team")` |
| **Expected root tag** | Container or `INPUT` (PrimeVue field host) | Same pattern |
| **Child control** | `input:not([type=hidden])` under root (or root if it is the input) | Same |
| **Authoritative display (live UI, Phase 12.1)** | Visible club name in readonly gray field (**الأخدود**) | **نادي البكيرية** |
| **Automation read target** | `input.domProperty.placeholder` and/or `input.domProperty.value` | Same |
| **Non-authoritative for assert** | HTML `placeholder` / `value` attributes alone | Same |
| **Wrapper text** | Not used for assert (avoids label + value concatenation) | Same |
| **Non-editability** | `readOnly` DOM property and/or `disabled` on inner input | Same |

**UI requirement note:** Tests still describe “placeholder” in step titles for traceability, but assertions use **manifest `homeTeam` / `awayTeam` metadata** compared to **`resolvedDisplayText`** and record **`resolutionSource`** (e.g. `input.domProperty.placeholder`). If live DOM uses `value` instead of `placeholder`, the test passes only on exact metadata match — not on attribute-only reads.

---

## 3. Modified files

| File | Change |
|------|--------|
| `src/main/java/com/taf/systems/zulfi/pages/analysis/TeamFieldObservation.java` | **New** — structured DOM observation + diagnostic block |
| `src/main/java/com/taf/systems/zulfi/analysis/fixture/FixtureDiscoveryResult.java` | **New** — scroll progress + `enumerationComplete` signal |
| `src/main/java/com/taf/systems/zulfi/pages/analysis/CreateAnalysisPage.java` | `observeHomeTeamField` / `observeAwayTeamField`; JS id-based inspection; `discoverOfficialFixtureOptionsWithProgress`; scroll-idle end detection; `readinessDiagnostics` uses display reads; `isReadOnly` uses `domProperty.readOnly` |
| `src/test/java/com/taf/systems/zulfi/features/analysis/OfficialFixtureDiscoveryTest.java` | Uses `FixtureDiscoveryResult`; persists honest `enumerationComplete` |
| `src/test/java/com/taf/systems/zulfi/features/analysis/OfficialFixtureSelectionTest.java` | Strict home/away asserts via observations; Allure DOM attachments |

---

## 4. Index 0 fixture (trusted manifest metadata)

From last successful discovery (`target/analysis-fixtures/latest-manifest.json`, **stale until next credentialed run**):

| Field | Value |
|-------|--------|
| **Index** | 0 |
| **Display label** | الأخدود × نادي البكيرية |
| **Expected home** | الأخدود |
| **Expected away** | نادي البكيرية |
| **Enabled** | true |

---

## 5. Date result

| Run | Match date |
|-----|------------|
| Phase 12.1 live (pre-fix) | **26/08/2026** (automation read OK) |
| Phase 12.2 agent smoke | **Not observed** (tests skipped — no credentials) |

---

## 6. Home / Away observations

| Run | Home actual (automation) | Away actual | DOM source recorded |
|-----|--------------------------|-------------|---------------------|
| Phase 12.1 live ANL-OFF-SEL-000 | *(empty)* | *(empty)* | Legacy attribute / partial read |
| Phase 12.1 screenshot (human) | الأخدود | نادي البكيرية | Visible UI — correct product behavior |
| Phase 12.2 agent smoke | **Not run** | **Not run** | Allure attachments pending credentialed run |

---

## 7. Non-editability verification

| Run | Result |
|-----|--------|
| Phase 12.1 | Not reached (failed at autofill wait) |
| Phase 12.2 agent | **Not run** (skipped) |
| Implementation | `areTeamFieldsReadOnly()` — disabled or `readOnly` on visible inner inputs |

---

## 8. Smoke TestNG result

**Command (reference):**

```powershell
Set-Location E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test-compile
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/analysis-fixture-sel-000-suite.xml" `
  "-Dexecution.pace=demo" "-Danalysis.create.enabled=false" `
  "-Danalysis.fixture.selection.onlyIndex=0" "-Dreport.open=true"
```

| Execution | Time (local) | ANL-OFF-DIS-001 | ANL-OFF-SEL-000 | Tests run | Failures | Skipped | Maven exit |
|-----------|--------------|-----------------|-----------------|-----------|----------|---------|------------|
| Agent shell (no `ZULFI_USER` / `ZULFI_PASSWORD`) | 2026-10-16 ~19:26 | **SKIPPED** | **SKIPPED** | 4 | 0 | 4 | **0** (BUILD SUCCESS — skips only) |
| Credentialed live Index 0 | — | **Required** | **Required** | — | — | — | **Pending** |

**Do not treat the agent run as PASS.** Skips are expected without portal credentials.

---

## 9. Allure HTML path

| Run | Path |
|-----|------|
| Phase 12.2 agent (skip run) | `test-execution-reports/runs/20261016-162618-840/allure-html/index.html` |
| Phase 12.1 live failure (reference) | `test-execution-reports/runs/20261016-153346-242/allure-html/index.html` |

---

## 10. Screenshots

| Artifact | Path |
|----------|------|
| Phase 12.1 Index 0 failure (UI correct, automation empty) | `test-execution-reports/latest/screenshots/ANL-OFF-SEL-000___Verify_Official_Fixture_at_Index_0_____________________________FAIL-1792159501733-855cb6da-36dd-40b3-bbee-bf63e5e4cae8.png` |
| Phase 12.2 skip | `test-execution-reports/runs/20261016-162618-840/screenshots/` (login skip captures) |

---

## 11. Enumeration completeness

| Topic | Finding |
|-------|---------|
| **Prior behavior** | Fixed **8** scroll passes; `enumerationComplete=false` always in discovery test |
| **Phase 12.2** | `discoverOfficialFixtureOptionsWithProgress()` stops after **2 consecutive passes** with **no new FIXTURE rows** and **non-advancing scroll** (`endOfListLikely`) |
| **`enumerationComplete`** | Set to **`true` only when `endOfListLikely`** — not merely because 46 items were seen |
| **Last live manifest** | 46 fixtures (indexes 0–45); `enumerationComplete=false` (pre-12.2 logic) |
| **Unknown** | Whether fixtures exist beyond index 45 until credentialed discovery re-run with new logic |

---

## 12. Indexed selector verification (Step 3)

| Check | Status |
|-------|--------|
| Zero-based canonical indexes | Yes (`OfficialFixtureIndexCatalog`) |
| Round headers excluded from index | Yes |
| Disabled options retain indexes | Yes (model; 0 disabled in last manifest) |
| Label checked after selection | Yes (`getSelectedOfficialFixture()` vs manifest) |
| Stale manifest guard | Yes (`OfficialFixtureManifestSession.suiteRunId`) |
| `analysis-fixture-sel-000-suite.xml` includes discovery in same JVM | **Yes** — discovery before selection |

---

## 13. Preflight (Step 4)

| Check | Result |
|-------|--------|
| Prior long full-suite Maven | User-terminated (~19:22); no concurrent agent Maven during 12.2 smoke |
| `analysis.create.enabled=false` | Enforced in tests |
| Submit not clicked | Yes (selection-only flow) |
| Report preservation | New run archived under `test-execution-reports/history/20261016-162618-840` |

---

## 14. Remaining blockers

1. **Credentials:** `ZULFI_USER` and `ZULFI_PASSWORD` not set in the agent PowerShell session (Process/User/Machine). Live Index 0 smoke must be executed in Mohamed’s credentialed shell.
2. **Unverified fix:** Team-field observation has **not** passed a live browser test after Phase 12.2 changes (`mvn test-compile` **passed**).
3. **Enumeration:** Re-run discovery with new scroll-idle logic to set `enumerationComplete` honestly.

---

## 15. Recommendation for full index suite

1. Run Index 0 smoke with credentials and confirm **ANL-OFF-SEL-000 PASS**, Allure attachments show `resolutionSource` (`input.domProperty.placeholder` or `input.domProperty.value`) and exact **الأخدود** / **نادي البكيرية** match.
2. If Index 0 passes, run `analysis-fixture-selection-suite.xml` with `-Danalysis.fixture.selection.onlyIndex` unset (or batched indexes), still with `-Danalysis.create.enabled=false`.
3. Do **not** enable analysis creation or run **ANL-OFF-HP-001** until selection suite is green.
4. Keep index **1** (known duplicate) for selection-only; exclude from submit paths per existing gates.

---

## Maven exit codes (this phase)

| Command | Exit code |
|---------|-----------|
| `mvn test-compile` | **0** |
| Index 0 smoke (agent, skipped) | **0** |

---

**Stop condition honored:** Full 46-index suite not executed; HP / submit not enabled.
