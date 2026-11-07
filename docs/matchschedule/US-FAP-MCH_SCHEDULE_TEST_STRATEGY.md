# US-FAP-MCH — Test Strategy (View & Filter Yellow League Match Schedule)

**User story:** View and filter Match Schedule for the Yellow League current season  
**Primary actor:** Authorized Football Analytics User  
**Automation project:** `Football_Auto_TAF_Foundation`  
**Mutates data:** **No** (read-only browse/filter)

---

## 1. Scope boundaries

| In scope (this US) | Out of scope (other stories / suites) |
|--------------------|----------------------------------------|
| Open **جدول المباريات**, title/description | **MCH-ANL-*** — **تحليل المباراة** pop-up, submit (`match-schedule-analysis-suite.xml`) |
| Unplayed vs played row display | **ANL-SYNC-*** — analysis status sync lifecycle (dedicated sync US) |
| Club filter **النادي** / **جميع الأندية** | Home card layout (**HOME-RES-001…008**) except shared deep link |
| Preselected club from Home **عرض المزيد** | Create Analysis from Home hero (**ANL-OFF-***) |
| Empty filter / loading / error (phase **c**, controllable only) | Friendly match creation (**ANL-FRD-***) |

**Overlap policy:** **HOME-RES-009** and **MCH-SCHED-014** both assert View More → schedule + club filter. Prefer **one** run per regression batch (documented in registry). Do **not** re-run **MCH-ANL-004** metadata checks here — schedule row parsing only.

---

## 2. Phased delivery

| Phase | Property | Tests | Notes |
|-------|----------|-------|-------|
| **A** | `-Dmch.schedule.phase=a` | MCH-SCHED-001 … 003 | Navigation + page chrome |
| **B** | `phase=b` | MCH-SCHED-004 … 014 | Rows + filter + Home deep link |
| **C** | `phase=c` | MCH-SCHED-015 … 016 | Empty/error — **Skip** unless hooks |

Default: **`phase=none`** → all MCH-SCHED tests **skipped**.

---

## 3. Architecture (TAF)

```
MatchScheduleBrowseAcceptanceTest   (orchestration, TestStepReporter)
        ↓
MatchScheduleBrowseFlow             (sidebar, direct, optional HomeCurrentResultsFlow)
        ↓
MatchSchedulePage                   (extended — shared with MCH-ANL navigation)
        ↓
MatchSchedulePageMessages / MatchScheduleRowRules
        ↓
DefaultClubConfig                   (MCH-SCHED-012…014, links HOME-RES-009)
```

---

## 4. Configuration

| Key | Purpose |
|-----|---------|
| `mch.schedule.phase` | `none` \| `a` \| `b` \| `c` |
| `mch.schedule.emptyFilterClub` | Club fragment for MCH-SCHED-015 (env `MCH_SCHEDULE_EMPTY_FILTER_CLUB`) |
| `portal.defaultClub.*` / `ZULFI_DEFAULT_CLUB*` | Same as Home — **not hardcoded** in tests |

Credentials: `ZULFI_USER`, `ZULFI_PASSWORD`.

---

## 5. Registry & suite

- **`MCH-SCHED_TEST_CASE_REGISTRY.md`** — AC traceability MCH-SCHED-001…016  
- **`src/test/resources/suites/zulfi/mch-schedule-suite.xml`** — `MatchScheduleBrowseAcceptanceTest` only

---

## 6. DOM review gate

Before expecting green Phase **B**, confirm:

- Page title **جدول المباريات** and Yellow League description fragment  
- Grid/table row locators (`data-testid` preferred)  
- PrimeVue **النادي** combobox + **جميع الأندية**  
- Played score pattern vs unplayed **HH:MM**  
- Analysis status labels on played rows only  
- Empty-state Arabic for zero filter results  

Update `MatchSchedulePage` locators after portal DOM review.
