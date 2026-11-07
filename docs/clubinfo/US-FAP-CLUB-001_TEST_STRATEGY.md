# US-FAP-CLUB-001 — Test Strategy (Club Information — عن النادي)

**User story:** Sidebar **عن النادي** → read-only club overview, history, board, administrative team, and football team member cards.

**ID namespace:** `CLUB-INF-001…030` in `com.taf.systems.zulfi.clubinfo.ClubInfoTestIds` (distinct from **US-FAP-CLUBS** `CLUB-001…010`).

---

## 1. Phases

| Phase | `-Dclub.info.phase=` | Focus |
|-------|------------------------|--------|
| **none** | default | All skipped |
| **a** | `a` | AC-01…AC-09 — access, section titles, overview/history content & read-only, board section title |
| **b** | `b` | AC-10…AC-25 — member cards (photo/placeholder, name, role), football cards no profile nav, read-only |
| **c** | `c` | AC-26…AC-30 — loading / error / retry (**Skip** unless hooks or controlled environment) |

Read-only only — **no** add, edit, delete, or player profile navigation (First Release).

---

## 2. Architecture

```
ClubInformationAcceptanceTest
  → ClubInformationFlow
  → ClubInformationPage (+ HomePage for authenticated entry)
```

Registry: **`CLUB-INF_TEST_CASE_REGISTRY.md`**

Gate: **`ClubInfoGate`** (`club.info.phase`)

Messages: **`ClubInfoPageMessages`**

---

## 3. Locators

- Sidebar: `a[aria-label='عن النادي']`
- Direct route override: `-Dportal.clubInfo.path=` (default `club/about`)
- Sections: headings containing **نبذة عن النادي**, **تاريخ النادي**, **مجلس الإدارة**, **الفريق الإداري**, **الفريق الرياضي**
- Member cards: `[data-testid*='member-card']`, `article`, card-like containers under section roots
- Error: **تعذر تحميل بيانات النادي. حاول مرة أخرى.** + **إعادة المحاولة**
- Broken image heuristic: `naturalWidth === 0`, placeholder/avatar selectors, default src patterns

---

## 4. Suite

`src/test/resources/suites/zulfi/club-information-suite.xml`

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/club-information-suite.xml" `
  "-Dclub.info.phase=a" "-Dexecution.pace=fast"
```

Phase **b** (member cards + read-only):

```powershell
mvn test "-DsuiteXmlFile=src/test/resources/suites/zulfi/club-information-suite.xml" `
  "-Dclub.info.phase=b" "-Dexecution.pace=fast"
```

---

## 5. Relation to US-FAP-CLUBS

| Story | Sidebar | IDs | Property |
|-------|---------|-----|----------|
| **US-FAP-CLUBS** | **الأندية** | CLUB-001…010 | `clubs.yellow.phase` |
| **US-FAP-CLUB-001** | **عن النادي** | CLUB-INF-001…030 | `club.info.phase` |

Both are read-only in the First Release; automation namespaces must not collide.
