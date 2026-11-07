# US-FAP-ZULFI-DATA — Test Strategy (Default Club data consistency, cross-page E2E)

**Story (QA-derived, 2026-10-22):** Every figure the portal shows about the Default Club (`portal.defaultClub.label`, default **نادي الزلفي**) must agree wherever it appears, and must be derivable from the match results in **جدول المباريات** (the source of truth for results round by round).

| Surface | Figures |
|---------|---------|
| Home | «نتائج الفريق للموسم الحالي» (last 5), «عدد الأهداف», «هداف الفريق», «المباريات القادمة», standings snippet, each «عرض المزيد» |
| جدول المباريات (`/matches`) | every round of «دوري يلو» + «كأس خادم الحرمين الشريفين»; analysis status per played match |
| ترتيب الدوري (`/standings`) | P / W / D / L / GF:GA / GD / Pts / «آخر 5 مباريات» |
| الأندية (`/clubs`) | «#N» rank badge |
| Club page (`/clubs/{id}`) | rank badge, «إحصائيات الفريق», results, leaderboards, fixtures tab |

Registry: [`ZDC_TEST_CASE_REGISTRY.md`](ZDC_TEST_CASE_REGISTRY.md)

## Phases

| Phase | `-Dclub.data.phase=` | Scope |
|-------|----------------------|-------|
| none | default | all Skip |
| **b** | `b` | all ZDC cases (live data, read-only) |

Tier: **Tier 2 regression** and the Zulfi **E2E** set (`tools/systems.py` → `e2e`). Never submits anything.

## Architecture

```
ZulfiDataConsistencyE2ETest (extends ClubDataUiTest → AuthenticatedUiTest)
  → ClubDataFlow (Default Club resolved by name on «الأندية»)
  → HomeClubWidgets · ScheduleFixturesView · StandingsPage/StandingsTable · ClubDetailsPage · ClubsListPage
  → ClubWidgetReader (one JS read per widget) → clubdata records (Fixture, SeasonStats, StandingsRow, ClubWidgets)
```

Rules worth knowing:

* Result cards (Home / club) write the score **club-first** («2 - 0» for an away 0-2 win); schedule rows are **home-first**. `Fixture.clubFirstScore` converts.
* Standings form badges are read left-to-right = **oldest → newest**.
* Team names compare via `Fixture.canonical` (drops «نادي», unifies أ/إ/آ/ى/ة).
* Dates have no year; `ArabicDates.seasonOrderKey` orders July → June. Arabic-Indic digits are normalised.
* Inactive tab panels stay in the DOM hidden; readers only read visible rows.

## Suite

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/zulfi-data-consistency-suite.xml" "-Dclub.data.phase=b" "-Dexecution.pace=fast"
```
