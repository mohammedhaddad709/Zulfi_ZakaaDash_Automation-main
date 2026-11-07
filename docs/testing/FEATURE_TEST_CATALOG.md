# Feature Test Catalog — فهرس اختبارات البوابة | Zulfi Portal TAF

**Canonical index** for user-story automation. Detail lives in per-feature registries; naming rules in `TEST_ID_NAMING_STANDARD.md`. Regression tiers in `REGRESSION_TIERS.md`.

| Business area | User story ID | Test ID prefix | Registry | Strategy | Suite XML (`src/test/resources/`) | Phase gate property | Default safe? | Submit / lifecycle gates | Overlap / do-not-duplicate |
|---------------|---------------|----------------|----------|----------|-------------------------------------|---------------------|---------------|--------------------------|----------------------------|
| Auth | LOGIN (platform) | `LOGIN-*` | `AGENTS.md` | `docs/02_LOGIN_FEATURE_READY_NO_TESTS.md` | `login-regression-suite.xml`, `login-hp-only-suite.xml` | — | **Yes** | No submit | Tier 0 only |
| Home | US-FAP-HOME-001 | `HOME-001-*` | `docs/home/HOME-001_TEST_CASE_REGISTRY.md` | `docs/home/US-FAP-HOME-001_TEST_STRATEGY.md` | `home-overview-suite.xml` | `home.overview.phase` | **Yes** (phase ≤ a–c read-only) | Never submits create form | Recent cards AC-30…36 overlap **ANL-STS** / **ANL-LST** — different layers OK; avoid duplicate navigation smoke with **ANL-LST-001** same batch |
| Home | US-FAP-HOME-CURRENT-RESULTS | `HOME-RES-*` | `docs/home/HOME-RES_TEST_CASE_REGISTRY.md` | `docs/home/US-FAP-HOME-CURRENT-RESULTS_TEST_STRATEGY.md` | `home-current-results-suite.xml` | `home.current.results.phase` | **Yes** | No | **HOME-RES-009** ≡ **MCH-SCHED-014** deep link — one per run |
| Clubs | US-FAP-CLUBS (**الأندية**) | `CLUB-001`…`010` | `docs/clubs/CLUB-TEST_CASE_REGISTRY.md` | `docs/clubs/US-FAP-CLUBS_TEST_STRATEGY.md` | `clubs-yellow-league-suite.xml` | `clubs.yellow.phase` | **Yes** | No | Distinct from **CLUB-INF-*** (different sidebar) |
| Clubs | US-FAP-CLUB-001 (**عن النادي**) | `CLUB-INF-*` | `docs/clubinfo/CLUB-INF_TEST_CASE_REGISTRY.md` | `docs/clubinfo/US-FAP-CLUB-001_TEST_STRATEGY.md` | `club-information-suite.xml` | `club.info.phase` | **Yes** | No | Phase **c** error ACs **Planned** (Skip/hooks); suite + phase a/b **complete** |
| Clubs | US-FAP-CLUB-DETAILS (`/clubs/{id}`) | `CLUB-DET-*` | `docs/clubdetails/CLUB-DET_TEST_CASE_REGISTRY.md` | `docs/clubdetails/US-FAP-CLUB-DETAILS_TEST_STRATEGY.md` | `club-details-suite.xml` | `club.details.phase` | **Yes** | No | Supersedes sidebar **عن النادي** (CLUB-INF) — content now the club page tab |
| Standings | US-FAP-STANDINGS | `STD-*` | `docs/standings/STD_TEST_CASE_REGISTRY.md` | `docs/standings/US-FAP-STANDINGS_TEST_STRATEGY.md` | `standings-suite.xml` | `standings.phase` | **Yes** | No | Schedule cross-checks live in **ZDC** |
| Cross-page (E2E) | US-FAP-ZULFI-DATA | `ZDC-*` | `docs/clubdata/ZDC_TEST_CASE_REGISTRY.md` | `docs/clubdata/US-FAP-ZULFI-DATA_TEST_STRATEGY.md` | `zulfi-data-consistency-suite.xml` | `club.data.phase` (b) | **Yes** | No | Zulfi E2E set (`tools/systems.py`); **ZDC-012** ≡ **HOME-RES-009** deep link |
| Schedule | US-FAP-MCH | `MCH-SCHED-*` | `docs/matchschedule/MCH-SCHED_TEST_CASE_REGISTRY.md` | `docs/matchschedule/US-FAP-MCH_SCHEDULE_TEST_STRATEGY.md` | `mch-schedule-suite.xml` | `mch.schedule.phase` | **Yes** | No | Status labels overlap **ANL-SYNC**; action overlap **MCH-ANL-001** |
| Schedule | US-FAP-MCH-ANALYSIS | `MCH-ANL-*` | `docs/matchschedule/MCH-ANL_TEST_CASE_REGISTRY.md` | `docs/matchschedule/US-FAP-MCH-ANALYSIS_TEST_STRATEGY.md` | `match-schedule-analysis-suite.xml` (read-only) · `match-schedule-analysis-submit-suite.xml` | `match.schedule.analysis.phase` | **Yes** until phase **c** | Phase **c** + `-Danalysis.create.enabled=true` + `-Dmatch.schedule.analysis.submit.enabled=true` (one submit; optional duplicate probe) | Do not re-run **MCH-ANL-010** with **ANL-OFF-HP-001** same session |
| Schedule | US-FAP-ANL-SYNC | `ANL-SYNC-*` | `docs/matchschedule/ANL-SYNC_TEST_CASE_REGISTRY.md` | `docs/matchschedule/US-FAP-ANL-SYNC_TEST_STRATEGY.md` | `anl-sync-suite.xml` | `anl.sync.phase` | **Yes** (a–c) | Phase **d** lifecycle: `anl.sync.lifecycle.enabled` + `anl.sync.creation.enabled` + `analysis.create.enabled` | **ANL-SYNC-001** vs **MCH-ANL-001** — pick one in smoke |
| Analysis | US-FAP-ANL-MATCH-TYPE | `ANL-TYPE-*` | `docs/analysis/ANL-TYPE_TEST_CASE_REGISTRY.md` | `docs/analysis/US-FAP-ANL-MATCH-TYPE_TEST_STRATEGY.md` | `match-type-official-suite.xml` (+ `match-type-official-b-suite.xml` for b+) | `anl.match.type.phase` | **Yes** (a–d) | Phase **e** + `analysis.create.enabled=true` | AC **003–012 Planned**; **013** defers to **ANL-OFF-HP-001** |
| Analysis | US-FAP-ANL-FRIENDLY | `ANL-FRD-*` | `docs/analysis/ANL-FRD_TEST_CASE_REGISTRY.md` | `docs/analysis/US-FAP-ANL-FRIENDLY_TEST_STRATEGY.md` | `friendly-match-analysis-suite.xml` (+ `friendly-match-analysis-e-suite.xml` for e) | `anl.friendly.phase` | **Yes** (a–d) | Phase **e** + `analysis.create.enabled` + `analysis.friendly.create.enabled` | **ANL-FRD-011/014 Partial** |
| Analysis | US-FAP-ANL-002 | `ANL-STS-*` | `docs/analysis/ANL-STS_TEST_CASE_REGISTRY.md` | `docs/analysis/US-FAP-ANL-002_PROCESSING_STATUS_TEST_STRATEGY.md` | `anl-sts-suite.xml` | `anl.sts.phase` | **Yes** (a–c) | Phase **d** observe: `anl.sts.observeSubmit=true` + `analysis.create.enabled` (single submit in **001**) | Shares **AnalysesListPage** with **ANL-LST** |
| Analysis | US-FAP-ANL-003 | `ANL-LST-*` | `docs/analysis/ANL-LST_TEST_CASE_REGISTRY.md` | `docs/analysis/US-FAP-ANL-003_LISTING_TEST_STRATEGY.md` | `anl-lst-suite.xml` | `anl.lst.phase` | **Yes** | Dialog open OK; **never** submit | Suite **implemented** (was “pending” in older maps); phase **d** empty/error ACs Skip/hooks |
| Analysis | Official HP / fixtures | (cross-cutting) | `ANL-OFF-*`, `PREFLIGHT-OFF-*` | `docs/analysis/ANALYSIS_TEST_SUITES.md` | `analysis-official-hp-suite.xml`, `analysis-fixture-selection-suite.xml`, `official-fixture-preflight-suite.xml`, … | `analysis.create.enabled` (default **false**) | **Yes** (selection/preflight) | HP submit: `analysis.create.enabled=true` + `ZULFI_OFFICIAL_FIXTURE` | Index **1** fixture known duplicate — selection only |

## Planned / gap register (document only)

| Item | Status | Notes |
|------|--------|-------|
| **ANL-TYPE-003…012** | **Planned** | Registry rows marked Planned; implement after DOM eligibility tags stable |
| **CLUB-INF-026…030** | **Planned** | Loading/error/retry — need failure hooks |
| **ANL-LST-054…057** | **Planned** | Listing load/error hooks |
| **ANL-SYNC-002** | **Gated scaffold** | Wired to MCH submit observer; not Tier 1 |
| **ANL-FRD-013** | **Skip** | Non-observable without API |
| **CLUB-INF-001…030** | **Retired (2026-10-22)** | Superseded by CLUB-DET (incl. new CLUB-DET-022/023) — `docs/testing/REBASELINE_2026-10-22.md` |
| **CLUB-003, MCH-SCHED-003/013, ANL-LST-002/023/024/027/029/034/043** | **Re-baselined (2026-10-22)** | New UI accepted as reference — `docs/testing/REBASELINE_2026-10-22.md` |
| **HOME-001-008, ANL-SYNC-003/006** | **Requirement drift (open)** | KPI «عدد اللاعبين» replaced; status chip instead of «عرض التفاصيل» — awaiting a decision |
| **ANL-TYPE-008** | **Blocked by safety contract** | Needs a submit click without an official match; not allowed outside phase e |

## Cross-story integration

`docs/analysis/ANL_CROSS_STORY_INTEGRATION.md`

## Quick smoke

Tier 1: `docs/testing/REGRESSION_TIERS.md` or `scripts/run-portal-safe-smoke.ps1`
