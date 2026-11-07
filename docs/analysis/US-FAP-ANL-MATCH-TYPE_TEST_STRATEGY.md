# US-FAP-ANL-MATCH-TYPE — Test Strategy (Match Type + Official Match Link)

**User story:** Select Match Type and link an Official Match (Home → «تحليل مباراة جديدة»)  
**Distinct from:** US-FAP-MCH-ANALYSIS (schedule entry **تحليل المباراة**) — see `docs/matchschedule/`.

---

## 1. Scope

| In scope | Shared / existing | Out of scope (other US) |
|----------|-------------------|-------------------------|
| **نوع المباراة** رسمية / ودية (no auto-select on open) | Video URL, description, submit pipeline | Friendly match **field** definitions (dedicated Friendly US) |
| **اختر المباراة** when رسمية | `CreateAnalysisPage`, `ANL-OFF-SEL-*` index/manifest | Schedule-only locked match (MCH-ANL) |
| Eligibility: played, not قيد التحليل / اكتمل التحليل | `OfficialFixtureDiscoveryTest` | Status sync back to schedule |
| Type switch clears opposite type data | `TeamFieldDisplayRules`, duplicate toast rules | |

---

## 2. Phases

| Phase | `-Danl.match.type.phase=` | Focus | Submit? |
|-------|---------------------------|-------|---------|
| **none** | default | All skipped | No |
| **a** | `a` | AC display + show official field (001–002) | No |
| **b** | `b` | Dropdown eligibility + select + metadata (003–007) | No |
| **c** | `c` | Required / empty state messages (008–009) | No |
| **d** | `d` | Official ↔ Friendly switch (010–011) | No |
| **e** | `e` | Submit revalidation + linkage (012–013) | **Yes** (+ `analysis.create.enabled=true`) |

---

## 3. Reuse map (avoid duplicate automation)

| New ID | AC | Existing automation |
|--------|-----|---------------------|
| ANL-TYPE-006, 007 | Select + schedule metadata | **ANL-OFF-SEL-NNN**, **ANL-OFF-HP-001** (partial) |
| Dropdown inventory | Played / eligible options | **ANL-OFF-DIS-001** manifest (best-effort; schedule rules need product tags) |

New tests focus on **gaps**: unset match type, conditional official field, **ودية** switch clearing, validation copy, empty state.

---

## 4. Validation messages (assert verbatim when UI stable)

| Key | Arabic |
|-----|--------|
| Match type required | يرجى اختيار نوع المباراة. |
| Official match required | يرجى اختيار المباراة. |
| No eligible official | لا توجد مباريات رسمية متاحة للتحليل حالياً. |
| No longer eligible at submit | هذه المباراة لم تعد متاحة لبدء تحليل جديد. يرجى اختيار مباراة أخرى أو تحديث القائمة. |

---

## 5. Architecture

```
MatchTypeOfficialAcceptanceTest
    → MatchTypeOfficialAnalysisFlow (open dialog, select types, no submit unless phase e)
    → CreateAnalysisPage (+ future schedule-aware option rules)
    → OfficialFixtureManifest / ANL-OFF-DIS when phase b needs inventory
```

---

## 6. Suite

`src/test/resources/suites/zulfi/match-type-official-suite.xml`  
Phase **b+** (006/007): `match-type-official-b-suite.xml` (runs discovery first).

Registry: **`ANL-TYPE_TEST_CASE_REGISTRY.md`**

### Run examples

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation

# Phase A — form display only (credentials required)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-type-official-suite.xml" `
  "-Danl.match.type.phase=a" "-Danalysis.create.enabled=false" "-Dexecution.pace=fast"

# Phase B — selection + autofill (manifest)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-type-official-b-suite.xml" `
  "-Danl.match.type.phase=b" "-Danalysis.fixture.index=2" "-Dexecution.pace=fast"
```
