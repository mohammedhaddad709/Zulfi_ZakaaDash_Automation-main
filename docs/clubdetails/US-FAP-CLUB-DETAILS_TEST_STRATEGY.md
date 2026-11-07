# US-FAP-CLUB-DETAILS — Test Strategy (club page `/clubs/{id}`)

**Story (QA-derived from the live product, 2026-10-22):** From **الأندية**, «عرض تفاصيل النادي» opens the club page: header (name, league rank badge, logo), breadcrumb and four tabs — **نظرة عامة** (season statistics, current-season results, «أعلى تصنيف», «أفضل الهدافين», «أفضل صانعي الأهداف»), **جدول المباريات والنتائج** (next match + all fixtures with analysis status), **قائمة الفريق** (first team / U21 / U18 × position groups with photo, position, appearances, minutes, market value, nationality, age, shirt number) and **عن النادي** (biography). Read-only.

| Phase | `-Dclub.details.phase=` | Scope |
|-------|--------------------------|-------|
| **a** | `a` | Tier 1 smoke: entry by ID, header, tabs, breadcrumb (CLUB-DET-001…004) |
| **b** | `b` | Tier 2: statistics, results, leaderboards, profile link, fixtures, squad data/photos/localisation, about (005…018) |
| **c** | `c` | Design: RTL, overflow desktop + 390px mobile, clipped text (019…021) |

Registry: [`CLUB-DET_TEST_CASE_REGISTRY.md`](CLUB-DET_TEST_CASE_REGISTRY.md) · Page object: `ClubDetailsPage` · Default Club: `DefaultClubConfig` (resolved to its ID on «الأندية», never hardcoded).

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/club-details-suite.xml" "-Dclub.details.phase=c" "-Dexecution.pace=fast"
```
