# [MINOR][Match Schedule Analysis] Description field incorrectly displays a required indicator although it is optional

| Field | Value |
|-------|-------|
| **Bug ID / local reference** | MCH-ANL-007-DESC-REQ-16651987 |
| **Related User Story** | US-FAP-MCH-ANALYSIS |
| **Related Test Case** | MCH-ANL-007 |
| **Related Acceptance Criterion** | Complete Existing Analysis Inputs |
| **Classification** | PRODUCT_DEFECT |
| **Severity (proposed)** | Minor |
| **Priority (proposed)** | P2 (pending triage) |
| **Status** | Open |
| **Reproducibility** | Observed on live schedule create route (16651987) |

## Environment
- Portal: https://zulfi.zakaadash.com/zakaadashZulfi-portal-vue/
- Browser: Chrome (automation)
- Captured: 2026-10-17T17:06:53.3150473+03:00
- Schedule target externalEventId: 16651987

## Preconditions
- Authenticated user with access to Match Schedule analysis entry
- Played official match eligible for analysis
- Navigate: Match Schedule → «بدء تحليل المباراة» → `/analysis/create?externalEventId=...`
- No form submission performed during defect capture

## Reproduction Steps
1. Open Create Analysis from Match Schedule for externalEventId `16651987`.
2. Locate the Description field (`#analysis-description`).
3. Observe the visible label and required indicator in the DOM.

## Expected Result
Description is **optional** and must **not** be represented as mandatory (no misleading required marker).

## Actual Result
field=analysis-description; label=وصف *; requiredMarker=true; ariaRequired=false; htmlRequired=false; validation=

## DOM evidence
- Visible label: `وصف *`
- Label DOM snippet: `<label for="analysis-description" class="w-fit text-md" title="وصف">وصف <span class="text-red">*</span></label>`
- aria-required on control: false
- HTML required on control: false

## Screenshot
screenshots\MCH-ANL-007_A1_description_label.png

## Impact
Users may believe Description is mandatory, conflicting with approved US-FAP-MCH-ANALYSIS acceptance criteria. Non-blocking for MCH-ANL-010 when Description is populated.

## Recommended fix
Remove required marker from Description label for schedule/create official flow; align PrimeVue field metadata with optional AC.

## Verification steps after fix
1. Re-run MCH-ANL-007 CHECK A1 — label must not show required marker.
2. Re-run CHECK A2 — empty Description with valid video must not show client-side required validation.
3. Confirm Video URL remains required (CHECK A3).

> Local defect artifact — not an external ticket. Attach to Allure for triage.
