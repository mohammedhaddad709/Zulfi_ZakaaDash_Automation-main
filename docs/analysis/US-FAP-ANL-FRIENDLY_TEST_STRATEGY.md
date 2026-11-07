# US-FAP-ANL-FRIENDLY — Test Strategy (Friendly Match Metadata)

**User story:** Create Friendly Match Analysis metadata (Home → «تحليل مباراة جديدة» → **ودية**).  
**Related:** US-FAP-ANL-MATCH-TYPE (type switch / official link) — `docs/analysis/US-FAP-ANL-MATCH-TYPE_TEST_STRATEGY.md`.

---

## 1. Scope

| In scope | Reuses existing | Out of scope |
|----------|-----------------|--------------|
| **اسم المباراة** / **تاريخ المباراة** when **ودية** | Description, Video URL, submit pipeline | Official fixture dropdown / schedule CRUD |
| Name length, charset, trim, max 150 | `CreateAnalysisPage`, `AnalysesListPage` | Auto-link friendly → official (negative AC only) |
| Date required, no future dates | `MatchDateDisplayRules` (display shape) | Redefining video accessibility rules |

**Expected DOM (portal convention):** `#analysis-match-name`, `#analysis-match-date` (same date container as official autofill).

---

## 2. Phases

| Phase | `-Danl.friendly.phase=` | Tests | Submit? |
|-------|---------------------------|-------|---------|
| **none** | default | All skipped | No |
| **a** | `a` | FRD-001 display fields | No |
| **b** | `b` | FRD-002…005 name validation | No |
| **c** | `c` | FRD-006…007 date validation | No |
| **d** | `d` | FRD-008 existing inputs available | No |
| **e** | `e` | FRD-009…014 create + list + guards | **Yes** (+ gates below) |

**Submit gates (phase e only):**

- `-Danalysis.create.enabled=true`
- `-Danalysis.friendly.create.enabled=true`

At most **one** friendly submit per authorized test method (`AnalysisSubmissionAttempt`).  
Phase **e** consolidates **ANL-FRD-009 / 010 / 012** into a single E2E method to avoid duplicate records.

---

## 3. Validation copy (assert verbatim when stable)

| Scenario | Arabic |
|----------|--------|
| Empty field | هذا الحقل مطلوب. |
| Name &lt; 3 chars | يجب ألا يقل اسم المباراة عن 3 أحرف. |
| Invalid name chars | يحتوي اسم المباراة على أحرف غير مسموح بها. |
| Future date | يجب أن يكون تاريخ المباراة اليوم أو تاريخاً سابقاً. |

---

## 4. Architecture

```
FriendlyMatchAcceptanceTest
  → FriendlyMatchAnalysisFlow (open + type «ودية»)
  → FriendlyMatchCreateFlow (metadata + optional submit)
  → CreateAnalysisPage (friendly field API)
  → validations: FriendlyMatchNameRules, FriendlyMatchValidationMessages
```

Registry: **`ANL-FRD_TEST_CASE_REGISTRY.md`**

---

## 5. Suites

| File | Purpose |
|------|---------|
| `friendly-match-analysis-suite.xml` | ANL-FRD acceptance only |
| `friendly-match-analysis-e-suite.xml` | Optional: run after login only (no official discovery required) |

### Run examples

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation

# Phase A — display
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/friendly-match-analysis-suite.xml" `
  "-Danl.friendly.phase=a" "-Danalysis.create.enabled=false" "-Dexecution.pace=fast"

# Phase E — one real friendly record (explicit authorization)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/friendly-match-analysis-suite.xml" `
  "-Danl.friendly.phase=e" "-Danalysis.create.enabled=true" `
  "-Danalysis.friendly.create.enabled=true" "-Dexecution.pace=demo"
```
