# Zulfi automation — architecture refactor (2026-10-22)

**Scope:** `com.taf.systems.zulfi` (main + test). No product behaviour, assertion strength, phase defaults or
submit gates changed. Companion docs: `REBASELINE_2026-10-22.md` (requirement drift decisions),
`ZULFI_RUN_2026-10-22_REPORT.md` (run evidence), `docs/clubdata/DEFECTS_2026-10-22.md` (product defects).

## 1. Layering (unchanged contract, now enforced consistently)

```
tests (features/*)        → scenario + assertions + TestStepReporter steps, phase gate first line
  flows/*                 → multi-page journeys (login, open feature, verify persistence)
    pages/*               → one class per page/section; locators private; returns domain values
      ui/*                → PrimeVue + portal widgets reused by every page (no business rules)
validations/*             → pure rules + UI copy constants (unit-testable, no WebDriver)
clubdata/*, analysis/*    → domain records and calculators (Fixture, SeasonStats, StandingsRow …)
config/*                  → gates, credentials, profile (SystemProfile → ZulfiConfig/PortalConfig)
reporting/*               → evidence, bug reports, classification (PRODUCT_DEFECT vs AUTOMATION_DEFECT …)
```

Rule of thumb applied in the refactor: **a portal quirk is solved once in `ui/*`**, never re-solved inside a page.

## 2. Shared UI components (`zulfi/ui`)

| Component | Owns | Replaced |
|-----------|------|----------|
| `PrimeVueSelect` | open (loader/toast aware, intercepted-click fallback), visible options, `pick(fragment)` with one re-open when the overlay was still closing, `clear()` via hover-only clear icon, `label()` | Four private copies in `MatchSchedulePage` (`openClubFilterDropdown`, `waitForOpenSelectOptions`, `tryClickOptionContaining`, `freshVisibleOptions`, `readOptionText`, `OVERLAY_OPTIONS`) and the hand-rolled dropdown code in `AnalysesListFiltersSection` |
| `PrimeVueDatePicker` | calendar-only picking (typing is ignored by the portal), selectable-day check, `dd/MM/yyyy` value | Typed date entry in Create Analysis and listing filters |
| `UiRetry` | bounded retry on `StaleElementReferenceException` for Vue re-renders | Three ad-hoc stale loops (ANL-LST card actions, ANL-SYNC, schedule rows) |
| `PortalOverlaySupport` | full-screen loader wait, toast dismissal (waits for the loader first) | Per-page sleeps / click retries |
| `PortalDom` | section-by-heading, JS row reads, broken images, overflow, Arabic-Indic normalisation | Duplicated text/number normalisers |
| `PrimeVueIndexedSelect` | official-fixture index ordering (data logic, not a widget driver) | — kept, different concern |

Failures inside these components throw `IllegalStateException` with the visible options/URL, which the reporter
classifies as `AUTOMATION_DEFECT`; assertion failures remain `PRODUCT_DEFECT`.

## 3. Phase gates

`config/PhaseGates` is the single implementation of `-D<feature>.phase=a|b|c|d|e`:

* `parse(property, enumClass)` — unset / blank / unknown ⇒ `NONE` (every phased test skips), case-insensitive.
* `require(property, current, minimum, feature, doc)` — `SkipException` with an actionable message:
  `"<feature> tests require -D<p>=<min> (current=<x>). See <doc>"`.

The 11 feature gates (`AnlFriendly…`, `AnlMatchType…`, `AnlsLst`, `AnlsSts`, `AnlsSync`, `ClubInfo`,
`ClubsYellowLeague`, `HomeCurrentResults`, `HomeOverview`, `MatchScheduleAnalysis`, `MchSchedule`) and
`FeaturePhaseGate` (CLUB-DET, STD, ZDC) keep their public API and delegate. Contract pinned by
`PhaseGatesContractTest` (unit suite).

Deliberately **not** extracted: each acceptance class's private `runStep` / `requirePhase` wrappers. They are
2-line delegates that carry feature-specific step titles and story references; a shared base class would add an
inheritance level without removing logic.

## 4. Output paths

`OfficialFixtureManifestStore.latestManifest()` resolves under `OutputPaths.root()` (was a hardcoded `target/…`
constant); `AnalysisCreateGate` follows. Dead `SCREENSHOTS_PATH` / `RECORDINGS_PATH` constants removed.

## 5. False-pass hardening (exception handling review)

All broad `catch` blocks in Zulfi code were reviewed. Kept: evidence/diagnostic best-effort catches (reporting,
video, bug-report writers) and documented fallbacks (sidebar → direct URL, KPI cards → legacy parser). Fixed where
an exception produced a **passing** answer:

| Location | Before | After |
|----------|--------|-------|
| `CreateAnalysisPage.probeTeamFieldEditBlocked` | any exception ⇒ "field not editable" (read-only check passes) | only `InvalidElementStateException` (browser refused input) means blocked; other errors propagate as automation failures |
| `ClubsListPage.hasSecondaryEmptyCopy` | any exception ⇒ "secondary copy present" | missing/stale container ⇒ that title proves nothing; method returns `false` unless a container shows ≥ 2 lines |
| `MatchSchedulePage.clubFilterSelectionContains` | page-source fallback matched the option text anywhere (HOME-RES-009 false PASS) | reads the select label only |

## 6. Other cleanups

* Inline fully-qualified class names → imports (66 files).
* Score regex in `MatchScheduleRowRules` / `HomeResultCardRules` is hyphen-only so kick-off times (`12:30`) are not
  parsed as results; covered by `MatchScheduleRowRulesTest`.
* Schedule reads: one JS read per refresh, settled-count wait, no recursion on the transient «لا توجد مباريات».
* Retired `ClubInformationAcceptanceTest` (CLUB-INF) skips unless `-Dclub.info.legacy=true`; excluded from
  generated suites in `tools/systems.py`.

## 7. Verification

* `mvn test -DsuiteXmlFile=src/test/resources/suites/zulfi/portal-unit-tests-suite.xml` — 25/25 pass.
* Tier 2 read-only regression (all Zulfi suites, `execution.pace=fast`, `analysis.create.enabled=false`) re-run
  after the refactor; results are in `ZULFI_RUN_2026-10-22_REPORT.md` §Post-refactor.

## 8. Guidance for new page objects

1. Selects / date pickers / loaders / stale retries → use `ui/*`; do not copy overlay code into a page.
2. Waits must be route-aware (Home renders the same widgets as feature pages).
3. Read only visible elements (hidden tab panels stay in the DOM).
4. Never return `true` from a `catch` in a verification method; throw `IllegalStateException` with what was seen.
5. New phased feature → enum + `PhaseGates.parse/require`; add to `tools/systems.py`, then run
   `python tools/gen_suites.py` and `python tools/gen_test_catalog.py`.
