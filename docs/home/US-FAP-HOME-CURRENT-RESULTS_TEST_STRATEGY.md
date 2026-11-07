# US-FAP-HOME-CURRENT-RESULTS — Test Strategy

**User story:** Home → **نتائج الفريق الحالية** → **عرض المزيد** → **جدول المباريات** with **النادي** = Default Club.

---

## 1. Phases

| Phase | `-Dhome.current.results.phase=` | Focus |
|-------|-----------------------------------|--------|
| **none** | default | All skipped |
| **a** | `a` | Section title, View More navigation, Home remains usable |
| **b** | `b` | Result cards (played/scores/status/order/club) |
| **c** | `c` | Empty / error / loading (mostly Skip until controllable fixtures) |

Read-only only — **no** data mutation.

---

## 2. Default Club (config-driven)

| Source | Key |
|--------|-----|
| Property | `portal.defaultClub.label` (default `نادي الزلفي`) |
| Fragment assert | `portal.defaultClub.fragment` (default `الزلفي`) |
| Env override | `ZULFI_DEFAULT_CLUB`, `ZULFI_DEFAULT_CLUB_FRAGMENT` |

Tests **never** hardcode «Al Zulfi» — they read `DefaultClubConfig`.

---

## 3. Architecture

```
HomeCurrentResultsAcceptanceTest
  → HomeCurrentResultsFlow
  → CurrentTeamResultsSection (+ HomePage)
  → MatchSchedulePage (club filter after View More)
```

Registry: **`HOME-RES_TEST_CASE_REGISTRY.md`**

---

## 4. Suite

`src/test/resources/suites/zulfi/home-current-results-suite.xml`

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/home-current-results-suite.xml" `
  "-Dhome.current.results.phase=a" "-Dexecution.pace=fast"
```
