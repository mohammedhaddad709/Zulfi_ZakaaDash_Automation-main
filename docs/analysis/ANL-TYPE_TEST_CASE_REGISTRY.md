# ANL-TYPE Test Case Registry — US-FAP-ANL-MATCH-TYPE

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| ANL-TYPE-001 | AC Display Match Type | **نوع المباراة** visible; **no** auto-selection on open | a | Implemented | anlType001_displayMatchType | |
| ANL-TYPE-002 | AC Select Official Type | **رسمية** → **اختر المباراة** visible | a | Implemented | anlType002_selectOfficialType | |
| ANL-TYPE-003 | AC Show Only Played | Dropdown: played only; no upcoming | b | Planned | - | needs schedule/eligibility tags |
| ANL-TYPE-004 | AC Exclude Under Analysis | Hide **قيد التحليل** | b | Planned | - | |
| ANL-TYPE-005 | AC Exclude Completed | Hide **اكتمل التحليل** | b | Planned | - | |
| ANL-TYPE-006 | AC Select Eligible | Retain Match ID + form association | b | Partial | - | see ANL-OFF-SEL-* |
| ANL-TYPE-007 | AC Schedule Metadata | No manual name/date; schedule source | b | Partial | - | see ANL-OFF-SEL / HP |
| ANL-TYPE-008 | AC Require Official | Submit/continue without match → **يرجى اختيار المباراة.** | c | Planned | - | |
| ANL-TYPE-009 | AC No Eligible Empty | Empty state Arabic copy | c | Planned | - | |
| ANL-TYPE-010 | AC Official→Friendly | Hide official; clear ID | d | Planned | - | |
| ANL-TYPE-011 | AC Friendly→Official | Show dropdown; require selection | d | Planned | - | |
| ANL-TYPE-012 | AC Revalidate Submit | Ineligible at submit → Arabic message | e | Planned | - | may overlap duplicate toast PRODUCT_DEFECT |
| ANL-TYPE-013 | AC Successful Linkage | One analysis ↔ one Match ID | e | Partial | - | ANL-OFF-HP-001 + phase e |

**Test class:** `com.taf.systems.zulfi.features.analysis.matchtype.MatchTypeOfficialAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.AnlMatchTypeAnalysisGate` (`-Danl.match.type.phase=`; phase b+ discovery: `match-type-official-b-suite.xml`)
