# Phase 8 — Official Fixture Preflight (Design Only)

**Status:** Planned — not implemented as a state-changing test.

## Objective

Identify eligible official fixture candidates without submitting Create Analysis.

## Proposed flow (read-only)

1. Fresh browser + login.
2. Open Create Analysis dialog.
3. Read official fixture labels from PrimeVue overlay.
4. Record enabled/disabled state where observable.
5. Compare against configured `ZULFI_OFFICIAL_FIXTURE` and optional search fragment.
6. Cross-check known duplicate fixture policy (e.g. externalEventId references in UI messages only).
7. Close browser — **no Submit click**.

## Result taxonomy

- `ELIGIBLE_VERIFIED`
- `POTENTIALLY_ELIGIBLE`
- `INELIGIBLE`
- `UNKNOWN`

## Limitations

- Absence from dropdown does not prove backend eligibility.
- Cannot delete or modify existing analyses during preflight.

## Next step

Implement as a dedicated **non-mutating** utility or `@Test(enabled=false)` scaffold after business approval.
