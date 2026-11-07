# [PRODUCT_DEFECT][Match Schedule → Analysis] MCH-SCHED-ANL-005

## Executive summary (for Dev / BA)
During automated acceptance **MCH-SCHED-ANL-005**, the portal did not behave as specified for **US-FAP-MCH-ANALYSIS**.
Failure point: **Post-submit schedule row status**. externalEventId=16651997; status=; postSubmit=false; stillNotAnalyzed=false; startAnalysisAbsent=false; excludedFromEligible=false

| Field | Value |
|-------|-------|
| **Artifact ID** | SCHED-FLOW-1792414943836-32f2f57a |
| **Test Case ID** | MCH-SCHED-ANL-005 |
| **Acceptance criterion** | Post-submit schedule flow |
| **Classification** | PRODUCT_DEFECT |
| **Severity (proposed)** | Major (pending triage) |
| **Status** | Open |
| **Portal URL** | https://zulfi.zakaadash.com/zakaadashZulfi-portal-vue/ |
| **Browser URL at capture** | https://zulfi.zakaadash.com/zakaadashZulfi-portal-vue/matches |
| **Captured** | 2026-10-19T16:02:23.836547+03:00 |

## Fixture context (from schedule row)
| Field | Value |
|-------|-------|
| externalEventId | 16651997 |
| Match # (schedule) | — |
| Home | نادي العدالة |
| Away | نادي الصقر |
| Score | 1-1 |

## Preconditions
1. Authenticated Football Analytics user (`ZULFI_USER` / `ZULFI_PASSWORD`).
2. Yellow League **جدول المباريات** contains a **played** row with status **«غير محللة»**.
3. Entry path: hover row → **«بدء تحليل المباراة»** → `/analysis/create?externalEventId=...`.
4. No manual override of match type or official fixture dropdown (schedule-sourced flow).

## Steps to reproduce (manual)
1. Login → sidebar **«جدول المباريات»** (or `/matches`).
2. Locate the fixture above (or any **غير محللة** played match).
3. Hover the row until **«بدء تحليل المباراة»** appears → click.
4. Observe create form binding (teams, **رسمية**, URL `externalEventId`).
5. Continue the scenario step that failed in automation: **Post-submit schedule row status**.

## Expected result
«قيد التحليل» or «اكتمل التحليل» without «غير محللة»

## Actual result
externalEventId=16651997; status=; postSubmit=false; stillNotAnalyzed=false; startAnalysisAbsent=false; excludedFromEligible=false

## Developer notes
- Verify API mapping `externalEventId` → official match record and that the create route pre-fills read-only team fields.
- Confirm PrimeVue / router state is not cleared on hover or dialog open (Vue re-render).
- If failure is post-submit: check analysis persistence, listing API, and Arabic success dialog actions.
- Compare with passing run **MCH-SCHED-ANL-HP-001** on the same environment.

## Evidence
### Screenshot
See Allure attachment «MCH-SCHED-ANL-005_STEP_FAIL.png» or run folder under test-execution-reports/latest/screenshots/

### Video
On failure: Allure «Screen recording» (report.video=ON_FAILURE) and video/*.mp4 in project root.

### Allure
- Step attachments: Expected / Actual / STEP_xx markdown
- Full run: `test-execution-reports/latest/allure-html/index.html`

> Local defect artifact — attach to Jira/Azure DevOps manually. Not an external ticket ID.
