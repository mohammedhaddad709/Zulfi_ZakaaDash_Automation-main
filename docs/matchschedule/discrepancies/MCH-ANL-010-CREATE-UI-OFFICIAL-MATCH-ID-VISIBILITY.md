# Requirement Discrepancy (Pending Product Confirmation) — Official Match #ID on Create Analysis

| Field | Value |
|-------|-------|
| **Reference** | MCH-ANL-010-UI-DISC-Official-Match-Hash-ID |
| **User story** | US-FAP-MCH-ANALYSIS |
| **Related AC** | Display official match information including **#ID**; link analysis to correct Official Match ID |
| **Classification** | **Requirement / UX discrepancy (pending confirmation)** — **not** filed as confirmed product defect |
| **Status** | **Pending BA / Product** |
| **Observed route** | `/analysis/create?externalEventId=16651987` (full page, schedule entry) |

---

## User story expectation (summary)

- Selected official match information should include a visible **#ID** (internal Official Match identifier).
- Analysis must be linked to the **correct** Official Match ID in storage.

---

## Live UI observation (read-only; no submit)

| Observation | When |
|-------------|------|
| Schedule binding appears via URL query **`externalEventId=16651987`** | Pre-submit and post-submit read-only probes |
| Official Match combobox label **does not** expose a numeric **#ID** | Pre-submit `20261017-140612-377` (`officialMatchId=` empty) |
| Same after Analysis **15** exists | Details mapping probe `20261017-143400-664` (`createRouteUiOfficialMatchId=` empty) |
| Teams, date, score hydrate from schedule context | Pre-submit PASS |

---

## Interpretation (QA — no backend accusation)

Possible explanations (not mutually exclusive):

1. **#ID is required in backend storage** but **not rendered** on the schedule-bound create view (UX gap vs AC wording).
2. **`externalEventId` is the primary binding key** in this integration; internal Official Match ID is resolved server-side and **not surfaced** to the client on this route.
3. **#ID appears only** in a different entry path (e.g. Home modal / dropdown selection) than Match Schedule deep link.

QA **does not** assert that storage is wrong without backend evidence. This document records **missing visible #ID** for product to confirm intended behavior.

---

## Related automation stance

Documented in [MCH_ANL_010_HAPPY_PATH_READINESS_REPORT.md](../MCH_ANL_010_HAPPY_PATH_READINESS_REPORT.md): tests assert URL `externalEventId` and form metadata; **`MAPPING_UNVERIFIED`** when internal ID is not observable.

---

## Product / BA actions requested

1. Confirm whether **#ID must be visible** on Create Analysis for schedule entry `externalEventId=…`.
2. If yes: treat as defect or backlog item with acceptance criteria for DOM visibility.
3. If no: update user story / AC to match **`externalEventId` binding** and document where internal Official Match ID is shown (details, API, admin).

---

## Separate from

- **MCH-ANL-007** — Description required indicator (**confirmed PRODUCT_DEFECT**, OPEN).
- **Internal match mapping acceptance blocker** — [MCH-ANL-010-AC-BLOCKER](../acceptance-blockers/MCH-ANL-010-INTERNAL-MATCH-MAPPING-AC-BLOCKER.md).
