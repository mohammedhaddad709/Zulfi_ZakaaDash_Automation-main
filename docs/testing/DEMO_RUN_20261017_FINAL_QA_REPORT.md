# Demo Run QA Report (20261017)

## Artifacts

- **Allure HTML:** `test-execution-reports/demo-consolidated/20261017/allure-html/index.html`
- **CSV matrix:** `test-execution-reports/demo-consolidated/20261017/PORTAL_TEST_CASE_EXECUTION_MATRIX.csv`
- **Original full demo master log:** `test-execution-reports/full-demo-safe/full-demo-safe-20261017-173903.log`
- **Post-fix rerun master log (in progress):** `test-execution-reports/full-demo-safe/full-demo-safe-20261017-225109.log`

## Automation recovery (wave 2 — 20261017 evening)

| Fix | Purpose |
|-----|---------|
| `AnalysesListPage.readVisibleAnalysisCardsOnce` | Re-fetch card list per index (Vue stale DOM → `IndexOutOfBoundsException`) |
| `PortalOverlaySupport` + `HomePage` / `HomeHeroSection` | Dismiss toasts; skip «ابدأ التحليل» when create dialog already open |
| `HomeSectionSupport.visibleBodyContains` | Hero/KPI assertions use visible body text, not only page source |
| `OfficialFixturePreflightTest` + `OfficialFixturePreflightFlow` | Reuse open dialog instead of second Home CTA click behind mask |
| `CreateAnalysisPage.closeCreateAnalysisDialogIfOpen` | Robust close (JS click, ESC, navigate home/back) |
| `AnalysesListFiltersSection` / `MatchSchedulePage` club filter | Broader PrimeVue field/combobox locators + `.p-select-label` |
| `ClubInformationPage.isSectionTitleVisible` | Body-text fallback for section headings |
| `CreateAnalysisPage.selectMatchTypeFriendlyByUser` | Wait for friendly metadata fields after type change |
| `run-portal-full-demo-safe.ps1 -OnlyNames` | Targeted rerun of failed shards |

## Verified green after fixes (targeted mvn)

| Shard | Result |
|-------|--------|
| **MCH-ANL-010-READONLY** | PASS (1/1) — archive `test-execution-reports/history/20261017-194642-033` |
| **ANL-PREFLIGHT** | PASS (1/1) — archive `test-execution-reports/history/20261017-195100-453` |
| **ANL-FIXTURE-SEL smoke** | PASS (2/2) — **ANL-OFF-SEL-SMOKE** (label + description + link, no submit) — archive `test-execution-reports/history/20261017-202008-530` |
| **HOME-001 AC-27** | PASS — `home001_027` «عرض التفاصيل» navigation — archive `test-execution-reports/history/20261017-202445-604` |

### Official fixture selection (post–[Fixture selection agent](e08037d6-1425-4bcc-ba06-1dadb4bb5644))

- **Root cause of index 0–7 “Broken”:** attachment read date **after** dialog close, not missing autofill on step 08.
- **Default smoke:** user flow by enabled fixture label; index sweep only with `-Danalysis.fixture.selection.indexMode=true` + `analysis-fixture-selection-full-suite.xml`.

## Product / acceptance (unchanged — not automation passes)

- **MCH-ANL-007** — product defect stays OPEN/FAILED.
- **MCH-ANL-010 submit** historical TestNG FAILED preserved; no second submit on `externalEventId=16651987`.
- **MAPPING_UNVERIFIED** — internal Official Match ID on Analysis 15; see `docs/matchschedule/acceptance-blockers/`.

## Rerun command

```powershell
cd Football_Auto_TAF_Foundation
.\scripts\rerun-demo-failed-suites.ps1
# or explicit shards:
.\scripts\run-portal-full-demo-safe.ps1 -OnlyNames HOME-001,MCH-SCHED,ANL-LST
```

After rerun completes, regenerate consolidated report:

```powershell
.\scripts\generate-demo-consolidated-report.ps1
```
