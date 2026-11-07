# US-FAP-STANDINGS — Test Strategy (league table)

**Story (QA-derived from the live product, 2026-10-22):** Home shows «ترتيب دوري الدرجة الأولى» (top rows, Default Club highlighted) with «عرض الجدول الدوري كامل» → `/standings` («ترتيب الدوري السعودي الدرجة الأولى»): position, club + logo, لعب / فاز / تعادل / خسر / +/- / الفارق / النقاط / آخر 5 مباريات, zone colours with a legend. Read-only.

| Phase | `-Dstandings.phase=` | Scope |
|-------|----------------------|-------|
| **a** | `a` | Tier 1 smoke: snippet columns + highlight, full-table navigation (STD-001…002) |
| **b** | `b` | Tier 2: arithmetic, ordering, form badges, snippet = full table, zones, logos, clubs-grid rank (003…009) |
| **c** | `c` | Copy/design: league name spelling (010) |

Registry: [`STD_TEST_CASE_REGISTRY.md`](STD_TEST_CASE_REGISTRY.md) · Page objects: `StandingsPage`, `StandingsTable`, `HomeClubWidgets`.
Cross-checks against **جدول المباريات** live in US-FAP-ZULFI-DATA (ZDC-003/004/016).

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/standings-suite.xml" "-Dstandings.phase=c" "-Dexecution.pace=fast"
```
