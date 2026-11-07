# Test ID Naming Standard — معيار تسمية حالات الاختبار

Professional QA convention for **Football_Auto_TAF_Foundation**. Every automated case ID must appear in exactly one feature registry under `docs/**/` unless explicitly cross-referenced as **Grouped**.

## Prefix per user story

| Prefix pattern | User story | Business area | Example |
|----------------|------------|---------------|---------|
| `LOGIN-HP-001`, `LOGIN-VAL-*`, `LOGIN-UI-*` | Platform login | Auth | `LOGIN-HP-001` |
| `HOME-001-NNN` | US-FAP-HOME-001 | Home | `HOME-001-006` maps to AC-06 |
| `HOME-RES-NNN` | US-FAP-HOME-CURRENT-RESULTS | Home | **Not** `HOME-001-*` |
| `CLUB-00N` (3 digits) | US-FAP-CLUBS | Clubs (**الأندية** listing) | `CLUB-001` |
| `CLUB-INF-NNN` | US-FAP-CLUB-001 | Clubs (**عن النادي**) | Distinct from `CLUB-*` |
| `CLUB-DET-NNN` | US-FAP-CLUB-DETAILS | Club page `/clubs/{id}` | Distinct from `CLUB-*` / `CLUB-INF-*` |
| `STD-NNN` | US-FAP-STANDINGS | League table (Home snippet + `/standings`) | `STD-004` |
| `ZDC-NNN` | US-FAP-ZULFI-DATA | Cross-page Default Club data (E2E) | `ZDC-004` |
| `MCH-SCHED-NNN` | US-FAP-MCH | Schedule browse | |
| `MCH-ANL-NNN` | US-FAP-MCH-ANALYSIS | Schedule → analysis action | |
| `ANL-SYNC-NNN` | US-FAP-ANL-SYNC | Schedule status sync | |
| `ANL-TYPE-NNN` | US-FAP-ANL-MATCH-TYPE | Analysis create (match type) | |
| `ANL-FRD-NNN` | US-FAP-ANL-FRIENDLY | Analysis create (friendly) | |
| `ANL-STS-NNN` | US-FAP-ANL-002 | Processing status (details) | |
| `ANL-LST-NNN` | US-FAP-ANL-003 | Analyses listing / filters | |
| `ANL-OFF-*`, `PREFLIGHT-OFF-*` | Official fixture HP / preflight | Analysis | See `ANALYSIS_TEST_SUITES.md` |

### HOME-001 vs HOME-001-001

- **Story shorthand:** `HOME-001` = US-FAP-HOME-001 feature.
- **Test IDs:** always **three-part** `HOME-001-NNN` (zero-padded AC index) in code and Allure.

### CLUB vs CLUB-INF

- **CLUB-001…010** — Yellow League **listing** page only.
- **CLUB-INF-001…030** — **About club** content page; never reuse `CLUB-00N` IDs for club info ACs.

## Phase letters (Maven `-D*.phase=`)

| Letter | Typical scope | Mutation |
|--------|-----------------|----------|
| **a** | Navigation, titles, read-only smoke | None |
| **b** | Data-dependent UI (cards, filters, validators) | None |
| **c** | Deeper navigation, date filters, completed-record UI | None (unless story-specific submit phase) |
| **d** | Combined filters, empty/no-results, negative observe | Usually none; **ANL-STS** may observe one submit when gated |
| **e** | Submit / E2E create paths (**ANL-FRD**, **ANL-TYPE**, friendly/official) | **Gated** create |

**Default:** property unset or `none` → TestNG **Skip** (fail-safe).

Story-specific submit phases (e.g. **MCH-ANL** phase **c**, **ANL-FRD** phase **e**) override the generic letter table — see each `US-FAP-*_TEST_STRATEGY.md`.

## Registry columns (required)

All `*_TEST_CASE_REGISTRY.md` files use:

`Test ID | AC | Title | Phase | Status | Test class/method | Notes`

## Status values

| Status | Meaning | Reporting |
|--------|---------|-----------|
| **Implemented** | Runnable when phase gate + credentials (+ data if b+) satisfied | Count as automated coverage |
| **Grouped** | Covered inside another test method listed in **Test class/method** | One execution covers multiple IDs |
| **Partial** | Some AC aspects covered; gaps documented in **Notes** | Do not mark story “done” |
| **Planned** | ID reserved; no automation yet (or scaffold Skip) | Track in `FEATURE_TEST_CATALOG.md` gap table |
| **Gated** | Implemented but requires explicit submit/lifecycle properties | Tier 3 only |
| **Skip** | `@Test` Skip or gate — needs hooks, empty tenant, or env | Not a failure |

Do **not** use “Automation” as a column name in new edits — use **Status** + **Test class/method**.

## Suite XML naming

`{feature-token}-suite.xml` in `src/test/resources/`, TestNG `parallel="false"`, one primary acceptance class per story suite unless documented (e.g. official selection index suites).
