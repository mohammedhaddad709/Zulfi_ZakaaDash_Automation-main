# Backend Evidence Request — MCH-ANL-010 / Analysis 15 / externalEventId 16651987

**From:** QA Automation (US-FAP-MCH-ANALYSIS)  
**Date:** 2026-10-17  
**Purpose:** Unblock **INTERNAL_MATCH_MAPPING_UNVERIFIED** without additional submissions or data creation.

---

## Context

- **One** authorized analysis submission was performed for schedule binding `externalEventId=16651987`.
- **Analysis ID 15** exists with description `MCH-ANL-010 authorized E2E 20261017-171620` and teams نادي الطائي × نادي الجندل.
- QA cannot infer the **internal Official Match ID** from the portal UI or from undocumented HTTP paths (session-authenticated GET attempts returned **404** on heuristic URLs only — **not** claimed as product API behavior).

---

## Requested information

Please provide:

1. **Documented read-only API** to retrieve an analysis by **Analysis ID** (path, method, response schema).
2. **Precise semantics** of:
   - `externalEventId` (schedule / external event binding), and  
   - **internal Official Match ID** (portal/database entity).
3. **Stored Official Match ID** linked to **Analysis 15** (authoritative value from backend).
4. **Authoritative Official Match record** corresponding to **`externalEventId=16651987`** (internal id + any external ids).
5. **Supported authentication** for read-only verification (session cookie, Bearer, API key, etc.).
6. **Product confirmation:** Must **Official Match #ID** be **visible** on the Create Analysis UI when entering via `/analysis/create?externalEventId=…`? (User story vs live DOM — pending BA/product.)

---

## QA follow-up (after response only)

- Add **read-only contract verification** in `Football_Auto_TAF_Foundation` using the **exact** documented endpoint.
- **Do not** change, delete, or recreate Analysis **15**.
- **Do not** run submit suites or enable creation gates for this fixture.

---

## Reference reports

- [MCH_ANALYSIS_EXECUTION_AND_DEFECT_REPORT.md](../MCH_ANALYSIS_EXECUTION_AND_DEFECT_REPORT.md) — canonical handoff  
- [MCH-ANL-010 acceptance blocker](../acceptance-blockers/MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md)
