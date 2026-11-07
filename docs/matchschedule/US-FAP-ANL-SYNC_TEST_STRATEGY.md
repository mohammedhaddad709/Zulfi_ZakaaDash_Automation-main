# US-FAP-ANL-SYNC — Test Strategy (Analysis Status ↔ Match Schedule)

**User story:** Synchronize Official Match Analysis Status with Match Schedule  
**Primary actor:** Authorized Football Analytics User  
**Automation project:** `Football_Auto_TAF_Foundation`  
**Related:** US-FAP-MCH-ANALYSIS (entry **تحليل المباراة**), US-FAP-MCH (schedule display/filter), HOME-RES (status labels on Home cards)

---

## 1. Scope boundaries

| In scope | Out of scope |
|----------|--------------|
| Played official rows show **غير محللة** / **قيد التحليل** / **اكتمل التحليل** | Unplayed rows — no analysis status (MCH-SCHED) |
| Action visibility per status mapping | Analysis processing pipeline internals |
| **عرض التفاصيل** / **عرض لوحة التحليل** navigation (read-only open) | Dashboard analytics correctness (existing result US) |
| Refresh returns latest status (session-level) | Mandating polling/WebSocket/SSE mechanism |
| API failure must not infer **غير محللة** | Introducing **فشل التحليل** user-facing status |
| Friendly analyses must not mutate official schedule | Friendly form validation (ANL-FRD) |

---

## 2. Status → action mapping (automation oracle)

| Analysis status | Primary actions | Must **not** show |
|-----------------|-----------------|-------------------|
| **غير محللة** | **تحليل المباراة** | **عرض التفاصيل**, **عرض لوحة التحليل** |
| **قيد التحليل** | **عرض التفاصيل** | **تحليل المباراة** |
| **اكتمل التحليل** | **عرض التفاصيل**, **عرض لوحة التحليل** | **تحليل المباراة** |

Copy centralized in `MatchScheduleAnalysisStatusMessages`.

---

## 3. Phased delivery

| Phase | `-Danl.sync.phase=` | Focus | Mutates data? |
|-------|---------------------|-------|---------------|
| **none** | default | All tests **skipped** | No |
| **A** | `a` | Schedule load + **غير محللة** row action rules (when data exists) | No |
| **B** | `b` | Row-level status text + action visibility; refresh; score preservation | No |
| **C** | `c` | Navigation: **عرض التفاصيل** / **عرض لوحة التحليل** (open only, no submit) | No |
| **D** | `d` | Lifecycle / negative scenarios needing fixtures or hooks | Mixed |

Phase **D** fixture binding: `-Danl.sync.fixture.index=<n>` and/or `-Danl.sync.fixture.tags=tag1,tag2`.

**Creation (AC-002 only):** never implied by phase. Requires **all**:

- `-Danl.sync.lifecycle.enabled=true`
- `-Danl.sync.creation.enabled=true`
- `-Danalysis.create.enabled=true` (global AnalysisCreateGate)

Default: **read-only** — no schedule submit, no analysis create.

---

## 4. Architecture (TAF)

```
AnalysisStatusSyncAcceptanceTest
        ↓
AnalysisStatusSyncFlow
        ↓
MatchSchedulePage (extended — ANL-SYNC row helpers; MCH-ANL pending methods unchanged)
        ↓
ElementActions / waits / Allure / TestStepReporter (existing)
```

Registry: **`ANL-SYNC_TEST_CASE_REGISTRY.md`**

---

## 5. Overlap with MCH-ANL (avoid duplicate execution)

| Concern | MCH-ANL owner | ANL-SYNC owner |
|---------|---------------|----------------|
| Open **تحليل المباراة** pop-up / submit | MCH-ANL-002…012 | — |
| **غير محللة** shows start action | MCH-ANL-001 (entry US) | ANL-SYNC-001 (sync mapping) |
| Hide start when **قيد** / **اكتمل** | MCH-ANL-013…016 | ANL-SYNC-003, 013 |
| Status labels after lifecycle | — | ANL-SYNC-002, 004, 008 |

Run **either** phased MCH-ANL **or** ANL-SYNC for duplicate-negative cases in CI smoke; full regression may run both with distinct phase gates.

---

## 6. Suite & commands

`src/test/resources/suites/zulfi/anl-sync-suite.xml`

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation

# Phase A — mapping smoke (skips when no «غير محللة» row)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sync-suite.xml" `
  "-Danl.sync.phase=a" "-Dexecution.pace=fast"

# Phase B — row assertions (needs live statuses in grid)
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sync-suite.xml" `
  "-Danl.sync.phase=b" "-Dexecution.pace=fast"
```

Credentials: `ZULFI_USER`, `ZULFI_PASSWORD`.

---

## 7. Skipped-by-design cases

| ID | Reason |
|----|--------|
| ANL-SYNC-009 | Retrieval failure simulation — needs fault injection hook |
| ANL-SYNC-012 | Friendly→official isolation — paired ANL-FRD + schedule assert |
| ANL-SYNC-002 | Lifecycle create — explicit triple gate; scaffold Skip until wired to MCH submit observer |

---

## 8. DOM review gate

Before Phase B live pass, confirm locators in `MatchSchedulePage` for:

- Schedule data rows (`tbody tr`, `[data-testid='match-schedule-row']`, etc.)
- Status column text (three Arabic states)
- Row-scoped action buttons/links

MCH-ANL-specific pop-up locators remain in `ScheduleOfficialAnalysisPopupPage`.
