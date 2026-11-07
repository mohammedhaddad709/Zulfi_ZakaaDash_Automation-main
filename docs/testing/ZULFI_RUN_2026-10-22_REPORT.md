# Zulfi Portal — Tier 2 regression report (2026-10-22)

Read-only run (`-Danalysis.create.enabled=false`, pace fast, Chrome) on the new machine after the automation
stabilisation. Every suite ran at its highest read-only phase (a–c/d). Evidence: Allure + screenshots under
`test-execution-reports/history/`. Defects and requirement drift: [`docs/clubdata/DEFECTS_2026-10-22.md`](../clubdata/DEFECTS_2026-10-22.md).

## Results

| Suite (phase) | Total | Pass | Fail | Skip | Failures are… |
|---------------|------:|-----:|-----:|-----:|---------------|
| Unit & contracts | 20 | 20 | 0 | 0 | — |
| LOGIN regression | 12 | 12 | 0 | 0 | — |
| HOME-001 (c) | 37 | 20 | 2 | 15 | drift HOME-001-008 · DEF-ZUL-008 |
| HOME-RES (c) | 14 | 9 | 1 | 4 | DEF-ZUL-002 |
| CLUBS (c) | 10 | 7 | 1 | 2 | drift CLUB-003 |
| CLUB-INF (c) | 30 | 5 | 9 | 16 | drift (page moved into club tab) |
| **CLUB-DET (c) — new** | 21 | 18 | 3 | 0 | DEF-ZUL-004/005/006 |
| **STD (c) — new** | 10 | 8 | 2 | 0 | DEF-ZUL-003/007 |
| **ZDC (b, E2E) — new** | 16 | 12 | 4 | 0 | DEF-ZUL-001 ×2, DEF-ZUL-002 ×2 |
| MCH-SCHED (c) | 16 | 11 | 3 | 2 | drift 003/013 · DEF-ZUL-002 |
| MCH-ANL (b) | 12 | 8 | 4 | 0 | DEF-ZUL-009 ×3, DEF-ZUL-010 |
| ANL-SYNC (c) | 13 | 5 | 2 | 6 | drift (status chip instead of «عرض التفاصيل») |
| ANL-STS (c, processing id 37) | 35 | 14 | 4 | 17 | DEF-ZUL-008 |
| ANL-LST (c) | 28 | 9 | 7 | 12 | drift ×6 · DEF-ZUL-008 (also reveals DEF-ZUL-011) |
| ANL-TYPE (d) | 13 | 5 | 2 | 6 | DEF-ZUL-010 · ANL-TYPE-008 blocked by safety contract |
| ANL-FRD (d) | 13 | 8 | 1 | 4 | DEF-ZUL-012 |
| Fixture selection | 3 | 2 | 0 | 1 | — |
| Official preflight | 1 | 1 | 0 | 0 | — |
| **Total** | **304** | **174** | **45** | **85** | 0 known automation defects |

Skips are gated by design: higher phases (d/e, lifecycle/submit), failure-injection hooks (loading/error/retry),
data that does not exist yet (no «اكتمل التحليل» analysis, no empty-season club), or optional ids
(`anl.sts.completedAnalysisId`).

## Cross-page data check (Default Club نادي الزلفي)

| Figure | Home | Standings | Club page | «الأندية» | Match Schedule |
|--------|------|-----------|-----------|-----------|----------------|
| Played / W / D / L | — | 8 / 3 / 5 / 0 | 8 / 3 / 5 / 0 | — | **6 / 2 / 4 / 0** |
| Goals for : against | 10 | 10 : 2 | 10 : 2 | — | **8 : 2** |
| Points | — | 14 | 14 | — | **10** |
| Rank | — | 2 | 2 | 2 | — |
| Last 5 results | D W W D D (newest first) | form D D W W D | same as Home | — | R8…R4 ✔ |
| Top scorer | بن مالانغو 2 | — | بن مالانغو / ديجفي بريغو / ديمبا ديالو 2 | — | — |
| Next match | R9 النجمة × الزلفي, 28 Oct 03:05 م | — | same | — | same ✔ |

Bold = disagreement: the schedule lacks round 1 (and most of rounds 2–3), so results-derived totals are 2 matches short
(DEF-ZUL-001).

## Automation fixes in this cycle

Loader mask intercepting clicks, route/content-aware waits (clubs, analyses, standings, schedule), stale-safe reads
(JS single-call readers, re-located options/rows), schedule score rule vs kick-off times, Arabic dates and digits,
renamed controls/ids, calendar-only date picker, match-type overlay left open, false PASS removed from HOME-RES-009,
empty-result «passes» replaced by schedule cross-checks (HOME-RES-003/007). Details: `docs/testing/UI_COPY_CHANGELOG.md`.
