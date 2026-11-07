# Phase 12.3 — Home/Away Assertion Mismatch (Index 0)

**Project:** `E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation`  
**Date:** 2026-10-16  
**Defect class (pre-fix live run):** **AUTOMATION_DEFECT**  
**Submission:** None (`analysis.create.enabled=false`)

---

## 1. Exact root cause

The Home Team step failed because **`TeamFieldObservation` resolution preferred `input.domProperty.placeholder` over `input.domProperty.value`**.

On Index 0 after official fixture autofill, PrimeVue keeps the **empty-state prompt** on the placeholder while the **club name** is on the input **value**:

| DOM property | Home field (live Allure) |
|--------------|---------------------------|
| `domProperty.value` | **الأخدود** (correct club) |
| `domProperty.placeholder` | **أدخل الفريق المضيف** (empty-field prompt: “Enter home team”) |
| **Resolved (buggy)** | أدخل الفريق المضيف via `input.domProperty.placeholder` |
| **Expected (manifest)** | الأخدود |

This is **not** a missing “نادي” prefix, **not** reversed Home/Away fields, and **not** a product data error—the UI and `value` matched the manifest; automation chose the wrong DOM source.

**Comparison site:** `OfficialFixtureSelectionTest.verifyOfficialFixtureSelectionByIndex` → `expected.homeTeam().equals(homeObs.resolvedDisplayText())` after `CreateAnalysisPage.observeHomeTeamField()`.

---

## 2. Expected Home / Away strings (Index 0, manifest)

| Field | Expected |
|-------|----------|
| Fixture label | الأخدود × نادي البكيرية |
| Home | **الأخدود** |
| Away | **نادي البكيرية** |
| Match date | **26/08/2026** |

Source: `OfficialFixtureManifestEntry` from same-run discovery (`FixtureLabelParser.splitTeams` on dropdown label).

---

## 3. Actual Home / Away strings (pre-fix live run)

| Field | Observed (automation) | Source (buggy) |
|-------|------------------------|----------------|
| Home | **أدخل الفريق المضيف** | `input.domProperty.placeholder` |
| Away | *(not reached — failed on Home)* | — |

**DOM evidence (UTF-8 Allure attachment `Home team DOM observation`, run `20261016-163358-104`):**

```
fieldId=analysis-home-team
resolvedDisplayText=أدخل الفريق المضيف
resolutionSource=input.domProperty.placeholder
rootTagName=INPUT
inputTagName=INPUT
domPropertyValue=الأخدود
domPropertyPlaceholder=أدخل الفريق المضيف
attributeValue=الأخدود
attributePlaceholder=أدخل الفريق المضيف
inputReadonly=true
inputDisabled=false
inputDisplayed=true
```

*(Fix typo in copy: attributeValue in artifact is `الأخدود`.)*

**Unicode (expected home):** `U+0627 U+0644 U+0623 U+062E U+062F U+0648 U+062F`  
**Unicode (buggy observed):** `U+0623 U+062F U+062E U+0644 U+0020 U+0627 U+0644 U+0641 U+0631 U+064A U+0642 U+0020 U+0627 U+0644 U+0645 U+0636 U+064A U+0641`

---

## 4. Actual DOM sources (club name location)

| Selector | Resolves to | Club name lives in |
|----------|-------------|-------------------|
| `#analysis-home-team` | **`INPUT`** (id on the input itself) | **`input.value`** (DOM property + attribute) |
| `#analysis-away-team` | Same pattern (inferred from symmetric UI) | **`input.value`** when autofilled |

Placeholder remains the static empty prompt; it must **not** override a populated **value**.

---

## 5. Fields reversed?

**No.** Selected fixture **الأخدود × نادي البكيرية**; screenshot shows **الأخدود** in Home (left) and **نادي البكيرية** in Away (right). Manifest home/away order matches label split.

---

## 6. Unicode normalization needed?

**No.** Exact equality is correct once **`value`** is chosen. No stripping of “نادي”, no `contains()`, no NFC/NFKD normalization applied.

---

## 7. Modified files

| File | Change |
|------|--------|
| `src/main/java/com/taf/systems/zulfi/validations/TeamFieldDisplayRules.java` | **New** — value-before-placeholder; reject empty prompts (`أدخل الفريق المضيف`, `أدخل …`, `اختر …`) |
| `src/main/java/com/taf/systems/zulfi/validations/TeamFieldComparisonDiagnostics.java` | **New** — UTF-8 expected vs actual report + code points |
| `src/main/java/com/taf/systems/zulfi/pages/analysis/CreateAnalysisPage.java` | Resolution delegated to `TeamFieldDisplayRules` |
| `src/test/java/com/taf/systems/zulfi/features/analysis/OfficialFixtureSelectionTest.java` | Team comparison Allure attachment; business failure messages |
| `src/main/java/com/taf/systems/zulfi/reporting/TestStepReporter.java` | Surface multiline “Verification Failed” text instead of only `expected [true] but found [false]` |
| `src/test/java/com/taf/systems/zulfi/validations/TeamFieldDisplayRulesTest.java` | **New** — regression for value vs placeholder |

---

## 8. Assertion implementation

### Before

1. If `isMeaningfulTeamDisplay(placeholder)` → use placeholder (**won when prompt ≠ empty**).
2. Else use value.

### After

1. If `isMeaningfulClubDisplay(value)` → **`input.domProperty.value`**.
2. Else if meaningful placeholder (not empty prompt) → placeholder.
3. Attribute / textContent fallbacks unchanged.
4. Allure: **Team field expected vs actual (UTF-8)** attachment before assert.

---

## 9. Smoke execution result (post-fix)

| Run | Shell | Tests | Result |
|-----|-------|-------|--------|
| Pre-fix live | User credentialed | 2 run, **1 failure** | **FAIL** — Home Team |
| Post-fix agent | No `ZULFI_USER` | 4 **skipped** | **Not validated live** |
| Post-fix unit | Agent | `TeamFieldDisplayRulesTest` | **PASS** |

**Credentialed re-run required** in the same shell used for the failing run (terminal with login evidence).

```powershell
Set-Location E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test-compile
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/analysis-fixture-sel-000-suite.xml" `
  "-Dexecution.pace=demo" "-Danalysis.create.enabled=false" `
  "-Danalysis.fixture.selection.onlyIndex=0" "-Dreport.open=true"
```

Expected after fix: Home source **`input.domProperty.value`**, Away **`input.domProperty.value`**, **ANL-OFF-SEL-000 PASS**.

---

## 10. Maven exit codes

| Command | Exit |
|---------|------|
| Pre-fix Index 0 smoke (user) | **1** |
| `mvn test-compile` (post-fix) | **0** |
| `TeamFieldDisplayRulesTest` | **0** |
| Post-fix Index 0 smoke (agent, no creds) | **0** (skips only — **not** a pass) |

---

## 11. Allure paths

| Run | Path |
|-----|------|
| **Failing live run** | `test-execution-reports/runs/20261016-163358-104/allure-html/index.html` |
| Agent skip run | `test-execution-reports/runs/20261016-163847-208/allure-html/index.html` |

---

## 12. Screenshots

| Artifact | Path |
|----------|------|
| Pre-fix failure (UI correct) | `test-execution-reports/runs/20261016-163358-104/screenshots/ANL-OFF-SEL-000___Verify_Official_Fixture_at_Index_0_____________________________FAIL-1792168437623-94e12079-5e64-4491-88c8-3ac3865a8fc8.png` |

---

## 13. Defect classification

| | |
|--|--|
| **Pre-fix failure** | **AUTOMATION_DEFECT** — wrong DOM source priority |
| **Product** | **Not indicated** — `domPropertyValue=الأخدود` and screenshot match manifest |

---

## 14. Remaining blockers

1. **Live post-fix Index 0 smoke** must be executed in Mohamed’s credentialed PowerShell session (agent process env has no `ZULFI_USER` / `ZULFI_PASSWORD`).
2. Confirm Away resolves to **`نادي البكيرية`** from **`input.domProperty.value`** on the same run.

---

**Stop condition:** Full 46-index suite not run; analysis creation not enabled.
