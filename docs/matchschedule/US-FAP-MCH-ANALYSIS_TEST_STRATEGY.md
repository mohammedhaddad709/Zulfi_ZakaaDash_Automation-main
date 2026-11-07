# US-FAP-MCH-ANALYSIS — Test Strategy (Match Schedule → Official Analysis)

**User story:** Start Official Match Analysis from Match Schedule  
**Primary actor:** Authorized Football Analytics User (Technical Analyst / Football Manager — same flow)  
**Automation project:** `Football_Auto_TAF_Foundation`  
**Related (existing):** Home → «ابدأ التحليل» → Create Analysis (`ANL-OFF-*`) — **different entry point**, shared form where DOM aligns.

---

## 1. Scope boundaries

| In scope (this US) | Out of scope (other stories / existing tests) |
|--------------------|-----------------------------------------------|
| Entry from **Match Schedule** via **تحليل المباراة** | Home hero **ابدأ التحليل** HP (`ANL-OFF-HP-001`) |
| Eligibility: played, **غير محللة**, official Yellow League schedule | 46-index official **dropdown** manifest suite (fixture picker from Home) |
| Locked official match + schedule-sourced read-only metadata | Analysis status **sync back** to schedule (dedicated sync US) |
| Description (optional), Video URL (required), submit **when authorized** | Arbitrary fixture rotation after failed submit |

---

## 2. Business rules → automation checks

| Rule | Automation intent |
|------|-------------------|
| BR-MCH-ANL-001 | Only **played** + **غير محللة** rows show **تحليل المباراة** |
| BR-MCH-ANL-002 | Pop-up opens with match type **رسمية** (not user-selected) |
| BR-MCH-ANL-003 | Official match **locked** — no replacement from this flow |
| BR-MCH-ANL-004 | Metadata from schedule — **no manual** match name/date/type/official picker |
| BR-MCH-ANL-005 / 006 / 007 | Submit creates **one** analysis linked by **Match ID**; reject if already **قيد التحليل** / **اكتمل التحليل** |
| BR-MCH-ANL-008 | Submit-time eligibility revalidation (not only button visibility) |
| BR-MCH-ANL-009 | Club names primary; **#ID** supporting metadata visible |

---

## 3. Phased delivery

| Phase | Property | Tests | Mutates data? |
|-------|----------|-------|---------------|
| **A** | `-Dmatch.schedule.analysis.phase=a` | MCH-ANL-001 … 006, 013, 017 | **No** |
| **B** | `phase=b` | MCH-ANL-007 … 009, 019 | **No** |
| **C** | `phase=c` + `-Danalysis.create.enabled=true` + `-Dmatch.schedule.analysis.submit.enabled=true` | MCH-ANL-010 (+ grouped 011, 012) | **Yes** (one submit) |
| **C + probe** | adds `-Dmatch.schedule.analysis.duplicateCheck.enabled=true` | grouped 014, 015, 016 | One rejected click, no new record expected |

Default: **`phase=none`** → all MCH-ANL tests **skipped** (suite safe). Phase a/b refuse to run with creation enabled.

---

## 4. Test case registry (AC traceability)

See **`MCH-ANL_TEST_CASE_REGISTRY.md`** for the full AC/BR matrix, run commands and the observed-behaviour notes.

---

## 5. Architecture (TAF)

```
MatchScheduleAnalysisAcceptanceTest  (one class for the story: steps, expected vs actual, grouped verifications)
        |
MatchScheduleTableFlow / MatchScheduleAnalysisFlow / MatchScheduleAnalysisHappyPathFlow
        |
MatchSchedulePage  (rows, hover, scanRowActionAvailability, start-action href)
ScheduleOfficialAnalysisPopupPage -> CreateAnalysisPage (form), AnalysisDetailsPage, AnalysesListPage
        |
TestStepReporter (step journal + explicit classification) -> EnhancedBugReportWriter
EvidenceRegistry + BrowserVideoRecorder (real screenshots/video linked from the report)
```

---

## 6. Configuration

| Key | Purpose |
|-----|---------|
| `match.schedule.analysis.phase` | `none` \| `a` \| `b` \| `c` |
| `match.schedule.analysis.targetExternalEventId` | Pin the match for phase c (env `MCH_ANALYSIS_EXTERNAL_EVENT_ID`); never switched silently |
| `match.schedule.analysis.submit.enabled` | Required **true** for the phase-c submit |
| `analysis.create.enabled` | Required **true** for the phase-c submit (global gate) |
| `match.schedule.analysis.duplicateCheck.enabled` | Enables the stale-form duplicate probe (one extra click that must be rejected) |
| `match.schedule.analysis.description` | Optional fixed description marker (default: unique `MCH-ANL-010 auto <timestamp>`) |
| `report.video` | `OFF` \| `ON_FAILURE` \| `ALWAYS` (class default: ON_FAILURE) |

Credentials: `ZULFI_USER`, `ZULFI_PASSWORD` (env or `config/credentials.local.properties`).

---

## 7. DOM review gate (before Phase A live pass)

Record locators for:

- Match Schedule route / sidebar **جدول المباريات** (or product label)
- Row **Analysis Status** = **غير محللة**
- Action **تحليل المباراة**
- Read-only block: Match #ID, home/away clubs, result, date
- Absence or disabled state of official match **replacer** control

Update `MatchSchedulePage` / `ScheduleOfficialAnalysisPopupPage` after review.

---

## 8. Suite

`match-schedule-analysis-suite.xml` (read-only, phases a/b) and `match-schedule-analysis-submit-suite.xml` (authorized submit) — both run `MatchScheduleAnalysisAcceptanceTest`.

---

## 9. Defect classification (this feature)

| Observation | Class |
|-------------|--------|
| English technical duplicate toast on Arabic UI | **PRODUCT_DEFECT** (see existing duplicate toast doc) |
| Expected Arabic: **تم بدء تحليل هذه المباراة بالفعل. حدّث الصفحة لعرض حالتها الحالية.** | Assert message text on concurrent submit |
| Wrong locator / readonly mis-read | **AUTOMATION_DEFECT** |
| Stale test match no longer **غير محللة** | **TEST_DATA_ERROR** |
