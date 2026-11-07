# Portal — User Story → Automation Map

> **Canonical index:** [`docs/testing/FEATURE_TEST_CATALOG.md`](testing/FEATURE_TEST_CATALOG.md) — registries, suites, phase gates, overlap rules, and gap register.  
> **Naming / status:** [`docs/testing/TEST_ID_NAMING_STANDARD.md`](testing/TEST_ID_NAMING_STANDARD.md)  
> **Regression tiers:** [`docs/testing/REGRESSION_TIERS.md`](testing/REGRESSION_TIERS.md)

| User Story | IDs | Suite | Mutates data? |
|------------|-----|-------|---------------|
| **US-FAP-CLUBS** (Yellow League listing — **الأندية**) | CLUB-001…010 | `clubs-yellow-league-suite.xml` | **No** |
| **US-FAP-CLUB-001** (Club Information — **عن النادي**) | CLUB-INF-001…030 | `club-information-suite.xml` | **No** (phase c error ACs Planned) |
| US-FAP-HOME-001 | HOME-001-001…045 | `home-overview-suite.xml` | **No** (dialog/list only) |
| US-FAP-HOME-CURRENT-RESULTS | HOME-RES-001…014 | `home-current-results-suite.xml` | **No** (same Home page, separate story) |
| US-FAP-MCH | MCH-SCHED-001…016 | `mch-schedule-suite.xml` | **No** |
| US-FAP-ANL-MATCH-TYPE | ANL-TYPE-001…013 | `match-type-official-suite.xml` | No (e gated); **003–012 Planned** |
| US-FAP-ANL-FRIENDLY | ANL-FRD-001…014 | `friendly-match-analysis-suite.xml` | e gated |
| US-FAP-MCH-ANALYSIS | MCH-ANL-001…016 | `match-schedule-analysis-suite.xml` | c/d gated |
| US-FAP-ANL-002 | ANL-STS-001…035 | `anl-sts-suite.xml` | d observe submit gated |
| US-FAP-ANL-003 | ANL-LST-001…059 | `anl-lst-suite.xml` | **No** (dialog only) |
| US-FAP-ANL-SYNC | ANL-SYNC-001…013 | `anl-sync-suite.xml` | d lifecycle triple-gated |
| Official analysis HP / fixtures | ANL-OFF-* | `analysis-official-hp-suite.xml`, selection suites | HP gated |

Detail by folder: `docs/clubs/`, `docs/clubinfo/`, `docs/home/`, `docs/analysis/`, `docs/matchschedule/`.

**Tier 1 smoke:** `scripts/run-portal-safe-smoke.ps1`
