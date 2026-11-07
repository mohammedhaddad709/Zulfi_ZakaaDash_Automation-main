# ANL-FRD Test Case Registry — US-FAP-ANL-FRIENDLY

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ANL-FRD-001 | Display Friendly Fields | **ودية** → اسم/تاريخ؛ no official selector | a | Implemented | anlFrd001_displayFriendlyFields | |
| ANL-FRD-002 | Require Name | Empty name → **هذا الحقل مطلوب.** | b | Implemented | anlFrd002_requireName | |
| ANL-FRD-003 | Name min length + invalid chars | &lt; 3 chars; disallowed charset | b | Implemented | anlFrd003_nameValidation | 2 methods |
| ANL-FRD-004 | Name max 150 | Block input at 150 | b | Implemented | anlFrd004_nameMaxLength | |
| ANL-FRD-005 | Trim name | Trim / spaces-only empty | b | Partial | anlFrd005_trimName | DOM value after blur |
| ANL-FRD-006 | Require date | No date → required message | c | Implemented | anlFrd006_requireDate | |
| ANL-FRD-007 | Reject future date | Future → business message | c | Implemented | anlFrd007_rejectFutureDate | |
| ANL-FRD-008 | Continue existing inputs | Description + video still available | d | Implemented | anlFrd008_continueExistingInputs | |
| ANL-FRD-009 | Create friendly analysis | Successful submit stores name/date | e | Gated | anlFrd009_createFriendly | E2E with 010/012 |
| ANL-FRD-010 | No official ID | No official selector / type ودية at submit | e | Gated | anlFrd009_createFriendly | same E2E as 009 |
| ANL-FRD-011 | Not on league schedule | Manifest count unchanged | e | Partial | anlFrd009_createFriendly | needs discovery manifest |
| ANL-FRD-012 | Appears in التحليلات | Search by friendly name | e | Gated | anlFrd009_createFriendly | same E2E as 009 |
| ANL-FRD-013 | No auto-match official | No auto-link by similarity | e | Skip | - | non-observable without API |
| ANL-FRD-014 | Failed submit → schedule safe | Failed submit; manifest unchanged | e | Partial | anlFrd014_failedSubmitSafe | |

**Test class:** `com.taf.systems.zulfi.features.analysis.friendly.FriendlyMatchAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.AnlFriendlyAnalysisGate` (`-Danl.friendly.phase=`; submit: `analysis.friendly.create.enabled` + `analysis.create.enabled`)
