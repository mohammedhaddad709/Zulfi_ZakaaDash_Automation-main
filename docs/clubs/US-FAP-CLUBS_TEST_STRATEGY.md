# US-FAP-CLUBS — Test Strategy (Yellow League Clubs)

**User story:** Sidebar **الأندية** → browse Yellow League clubs → open club detail by **Club ID** (not name alone).

---

## 1. Phases

| Phase | `-Dclubs.yellow.phase=` | Focus |
|-------|-------------------------|--------|
| **none** | default | All skipped |
| **a** | `a` | Sidebar nav, page title, description, read-only listing |
| **b** | `b` | Club cards (name, optional logo), selectable cards, navigation by ID in URL |
| **c** | `c` | Empty / error / loading (Skip unless hooks or controlled data) |

Read-only only — **no** add, edit, or delete.

---

## 2. Architecture

```
ClubsBrowseAcceptanceTest
  → ClubsBrowseFlow
  → ClubsListPage (+ HomePage for authenticated entry)
```

Registry: **`CLUB-TEST_CASE_REGISTRY.md`**

---

## 3. Locators

- Sidebar: `a[aria-label='الأندية']`, `a[href*='club']`
- Cards: `[data-testid='club-card']`, `[data-club-id]`, `a[href*='/club']`
- Club ID: `data-club-id` attribute or href segment `/clubs/{id}` / query `clubId=`

---

## 4. Suite

`src/test/resources/suites/zulfi/clubs-yellow-league-suite.xml`

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/clubs-yellow-league-suite.xml" `
  "-Dclubs.yellow.phase=a" "-Dexecution.pace=fast"
```

Phase **b** (cards + navigation):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/clubs-yellow-league-suite.xml" `
  "-Dclubs.yellow.phase=b" "-Dexecution.pace=fast"
```
