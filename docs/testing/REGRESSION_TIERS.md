# Regression Tiers — مستويات الانحدار

Sequential, fail-safe execution model for Mohamed’s portal runs. **Never** enable Tier 3 in shared CI without explicit sign-off.

## Tier 0 — Login smoke | تسجيل الدخول

**Goal:** Auth only, no protected feature surface.

```powershell
cd E:\FA_Zulfi_Portal\Football_Auto_TAF_Foundation
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/login-hp-only-suite.xml" "-Dexecution.pace=fast"
```

Optional full login regression (still no analysis):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/login-regression-suite.xml" "-Dexecution.pace=fast"
```

Requires: none beyond valid env URL (credentials only for HP/regression tests that login).

---

## Tier 1 — Phase **a** portal safe smoke | دخان آمن

**Goal:** All major sidebar features, **read-only**, `-Danalysis.create.enabled=false` implicit.

**Script (recommended):**

```powershell
.\scripts\run-portal-safe-smoke.ps1
```

**Manual equivalents** (same order as script; skip if suite file missing):

```powershell
$mvn = @("test", "-Dexecution.pace=fast", "-Danalysis.create.enabled=false")
$suites = @(
  @{ f = "home-overview-suite.xml"; p = "-Dhome.overview.phase=a" },
  @{ f = "home-current-results-suite.xml"; p = "-Dhome.current.results.phase=a" },
  @{ f = "clubs-yellow-league-suite.xml"; p = "-Dclubs.yellow.phase=a" },
  @{ f = "club-details-suite.xml"; p = "-Dclub.details.phase=a" },   # replaces club-information (retired 2026-10-22)
  @{ f = "standings-suite.xml"; p = "-Dstandings.phase=a" },
  @{ f = "mch-schedule-suite.xml"; p = "-Dmch.schedule.phase=a" },
  @{ f = "anl-sync-suite.xml"; p = "-Danl.sync.phase=a" },
  @{ f = "anl-sts-suite.xml"; p = "-Danl.sts.phase=a" },
  @{ f = "anl-lst-suite.xml"; p = "-Danl.lst.phase=a" }
)
foreach ($s in $suites) {
  mvn @mvn "-DsuiteXmlFile=src/test/resources/$($s.f)" $s.p
}
```

Requires: `ZULFI_USER`, `ZULFI_PASSWORD`.

**Not in Tier 1:** `match-schedule-analysis-suite.xml`, friendly/type phase e, official HP, lifecycle **ANL-SYNC-002**.

---

## Tier 2 — Phase **b** (and selective **c**) | بيانات حية

**Goal:** Filters, cards, validators with live tenant data. Still **no submit** unless story explicitly allows read-only phase c.

Examples:

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/home-overview-suite.xml" "-Dhome.overview.phase=b" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/mch-schedule-suite.xml" "-Dmch.schedule.phase=b" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-lst-suite.xml" "-Danl.lst.phase=b" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-suite.xml" "-Dmatch.schedule.analysis.phase=b" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/anl-sts-suite.xml" "-Danl.sts.phase=c" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/club-details-suite.xml" "-Dclub.details.phase=c" "-Dexecution.pace=fast"
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/standings-suite.xml" "-Dstandings.phase=c" "-Dexecution.pace=fast"
```

ANL-STS processing-state cases need an analysis that is «قيد التحليل» (read-only): add
`-Danl.sts.processingAnalysisId=<id>` (e.g. `37` on 2026-10-22) or env `ZULFI_PROCESSING_ANALYSIS_ID`; completed-state
cases need `-Danl.sts.completedAnalysisId=<id>` once an analysis completes.

**Default Club data (E2E, read-only):** every figure about the Default Club cross-checked between Home, جدول المباريات,
ترتيب الدوري, الأندية and the club page.

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/zulfi-data-consistency-suite.xml" "-Dclub.data.phase=b" "-Dexecution.pace=fast"
```

Use `FEATURE_TEST_CATALOG.md` for the full property list per story.

---

## Tier 3 — Gated submit / lifecycle | إنشاء تحليل

**Goal:** At most **one** submit per authorized test method (`AnalysisSubmissionAttempt`).

| Flow | Suite | Minimum properties |
|------|-------|-------------------|
| Official HP | `analysis-official-hp-suite.xml` | `-Danalysis.create.enabled=true` + `-DZULFI_OFFICIAL_FIXTURE="…"` |
| Schedule analysis | `match-schedule-analysis-suite.xml` | `-Dmatch.schedule.analysis.phase=c` + create gate |
| Friendly create | `friendly-match-analysis-e-suite.xml` | `-Danl.friendly.phase=e` + `analysis.friendly.create.enabled=true` + create gate |
| STS observe | `anl-sts-suite.xml` | `-Danl.sts.phase=d` + `-Danl.sts.observeSubmit=true` + create gate |
| Sync after create | `anl-sync-suite.xml` | `-Danl.sync.phase=d` + lifecycle + creation gates |

**Never** combine multiple Tier 3 suites in one Maven invocation without understanding data mutation order.
