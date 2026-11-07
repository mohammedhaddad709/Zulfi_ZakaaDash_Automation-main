# Create Analysis — TestNG Suites

All paths under `src/test/resources/`. Default Maven run uses `-Psuite -DsuiteXmlFile=...`.

| Suite file | Purpose | Mutates data? |
|------------|---------|---------------|
| `analysis-default-safe-suite.xml` | **Recommended default** — discovery + selection smoke | **No** |
| `analysis-discovery-suite.xml` | ANL-OFF-DIS-001 manifest only | **No** |
| `analysis-fixture-selection-smoke-suite.xml` | Discovery + limited indexes | **No** |
| `analysis-fixture-selection-full-suite.xml` | Discovery + every discovered index | **No** |
| `analysis-fixture-sel-000-suite.xml` | Alias: smoke with index 0 property | **No** |
| `analysis-fixture-selection-suite.xml` | Deprecated alias of full suite | **No** |
| `analysis-official-hp-suite.xml` | ANL-OFF-HP-001 only | **Only with** `-Danalysis.create.enabled=true` |
| `login-regression-suite.xml` | Login feature (unchanged) | **No** |
| `demo-suite.xml` | Login HP demo pacing | **No** |
| `mch-schedule-suite.xml` | **US-FAP-MCH** MCH-SCHED-001…016 (browse/filter) | **No** |
| `match-schedule-analysis-suite.xml` | **US-FAP-MCH-ANALYSIS** MCH-ANL-001…016 | **Only phase c+d with creation gate** |
| `match-type-official-suite.xml` | **US-FAP-ANL-MATCH-TYPE** ANL-TYPE-001…013 | **No** (phase **e** submit → use HP suite) |
| `match-type-official-b-suite.xml` | Discovery + ANL-TYPE (phase **b+**) | **No** |
| `friendly-match-analysis-suite.xml` | **US-FAP-ANL-FRIENDLY** ANL-FRD-001…014 | **No** (phase **e** gated) |
| `friendly-match-analysis-e-suite.xml` | Discovery + ANL-FRD (011/014 manifest guards) | **Only phase e + friendly create gate** |
| `anl-sts-suite.xml` | **US-FAP-ANL-002** ANL-STS-001…035 (processing status) | **No** (phase **d** + `-Danl.sts.observeSubmit=true` + create gate) |
| `anl-lst-suite.xml` | **US-FAP-ANL-003** ANL-LST-001…059 (browse/search/filter) | **No** |

Match Schedule browse: `docs/matchschedule/US-FAP-MCH_SCHEDULE_TEST_STRATEGY.md`  
Match Schedule analysis: `docs/matchschedule/US-FAP-MCH-ANALYSIS_TEST_STRATEGY.md`  
Match Type + Official link: `docs/analysis/US-FAP-ANL-MATCH-TYPE_TEST_STRATEGY.md`  
Friendly metadata: `docs/analysis/US-FAP-ANL-FRIENDLY_TEST_STRATEGY.md`  
Analyses listing: `docs/analysis/US-FAP-ANL-003_LISTING_TEST_STRATEGY.md`  
Cross-story map: `docs/analysis/ANL_CROSS_STORY_INTEGRATION.md`, `docs/PORTAL_USER_STORY_AUTOMATION_MAP.md`  
Processing status: `docs/analysis/US-FAP-ANL-002_PROCESSING_STATUS_TEST_STRATEGY.md`  
Cross-story integration: `docs/analysis/ANL_CROSS_STORY_INTEGRATION.md`

## Home (current results)

| Suite | Purpose |
|-------|---------|
| `home-current-results-suite.xml` | HOME-RES-001…014 — read-only Home + View More → Schedule |

## Smoke selection properties

```text
-Danalysis.fixture.selection.onlyIndex=0
-Danalysis.fixture.selection.indexes=0,5
```

Requires credentials: `ZULFI_USER`, `ZULFI_PASSWORD`.

## Happy Path authorization

```powershell
mvn test -Psuite `
  "-DsuiteXmlFile=src/test/resources/suites/zulfi/analysis-official-hp-suite.xml" `
  "-Danalysis.create.enabled=true" `
  "-Danalysis.fixture.index=0" `
  "-Dexecution.pace=demo"
```

Fixture identity (pick one):

- **Default:** `analysis.fixture.index=0` in `webapp.properties` (uses `target/analysis-fixtures/latest-manifest.json` when present, else verified form label after selection).
- **Override label:** env `ZULFI_OFFICIAL_FIXTURE` or `-Danalysis.fixture.label=...`
- **Override index:** `-Danalysis.fixture.index=N`

Do **not** use index **1** for submit (`نادي الطائي × نادي البكيرية` — known duplicate).

**Never** add `OfficialMatchAnalysisHappyPathTest` to default regression or `analysis-default-safe-suite.xml`.
