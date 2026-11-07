# US-FAP-ANL-003 — Test Strategy (Analyses Listing)

**User story:** Browse, search, and filter match analyses on **التحليلات** — read-only; no delete; no analysis submit from this suite.

**Cross-story:** `ANL_CROSS_STORY_INTEGRATION.md` (HOME-001 navigation, ANL-STS / ANL-002 status labels).

---

## 1. Phases

| Phase | `-Danl.lst.phase=` | Focus |
|-------|----------------------|--------|
| **none** | default | All skipped |
| **a** | `a` | AC-01…08 access, title/description/CTA, search field, card structure, ordering, status/dashboard DOM, no delete, view-details smoke |
| **b** | `b` | AC-16…22 search; AC-29…38 status + video source filters; AC-43…48 reset filters (search preserved) |
| **c** | `c` | AC-23…28 date range controls, inclusive range smoke, invalid range message |
| **d** | `d` | AC-39…42 combined AND; AC-49…53 empty vs no-results; AC-54…57 loading/error (**Skip** unless hooks); AC-58 refresh; AC-59 integrity |

**Mutations:** Opening **تحليل مباراة جديدة** dialog is allowed. **Never** submit create-analysis form.

Optional flags:

- `-Danl.lst.allowEmptyStateChecks=true` — global empty (AC-49…50)
- `-Danl.lst.allowErrorStateHooks=true` — loading/error (AC-54…57)
- `-Danl.lst.noMatchSearch=...` — override no-results search needle (AC-51)

---

## 2. Architecture

```
AnalysesListingAcceptanceTest
  → AnalysesListingFlow
  → AnalysesListPage
       → AnalysesListFiltersSection
  → CreateAnalysisPage (dialog title only)
  → AnalysisDetailsPage (view-details smoke)
```

Registry: **`ANL-LST_TEST_CASE_REGISTRY.md`**

Messages: **`com.taf.systems.zulfi.validations.AnalysesListingMessages`**  
Status reuse: **`AnalysisProcessingStatusMessages`** (قيد التحليل / اكتمل التحليل)

---

## 3. Suite

`src/test/resources/suites/zulfi/anl-lst-suite.xml`

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" `
  "-Danl.lst.phase=a" "-Dexecution.pace=fast"
```

Phase **b** (search + filters + reset):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" `
  "-Danl.lst.phase=b" "-Dexecution.pace=fast"
```

Phase **c** (date range):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" `
  "-Danl.lst.phase=c" "-Dexecution.pace=fast"
```

Phase **d** (combined + empty/no-results; skips when tenant lacks data):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" `
  "-Danl.lst.phase=d" "-Dexecution.pace=fast"
```
