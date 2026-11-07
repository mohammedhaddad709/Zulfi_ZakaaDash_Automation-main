# SUPERSEDED — see MCH-ANL_TEST_CASE_REGISTRY.md

> The MCH-SCHED-ANL-002…008 flow classes were merged into `MatchScheduleAnalysisAcceptanceTest` (work item 33009). This file is kept only as history; success-dialog follow-ups (view status / create another) belong to the existing analysis flow, not to this story.

# MCH-SCHED-ANL Flow Test Case Registry

**User story:** US-FAP-MCH-ANALYSIS — entry from **جدول المباريات**  
**Test classes:** `MatchScheduleAnalysisFlowAcceptanceTest` (002–004), `MatchScheduleAnalysisSubmitChainAcceptanceTest` (005–008 chained)  
**Suite:** `src/test/resources/suites/zulfi/match-schedule-analysis-flow-suite.xml`  
**Chain order:** 005 → 006 → 008 → 007 (one browser; fixture id from 003 harness or auto-discover)  
**Gate:** `-Dmatch.schedule.analysis.phase=b|c` (+ submit flags for 005–007)

| Test ID | Title | Phase | Submit | Method |
|---------|-------|-------|--------|--------|
| MCH-SCHED-ANL-002 | Sidebar → جدول المباريات — عنوان ووصف الصفحة | b | No | mchSchedAnl002_openScheduleFromSidebar |
| MCH-SCHED-ANL-003 | صف «غير محللة» → نموذج تحليل رسمي (تطابق الفرق والحدث) | b | No | mchSchedAnl003_scheduleRowBindsOfficialCreateForm |
| MCH-SCHED-ANL-004 | إغلاق دون إرسال — الصف يبقى «غير محللة» | b | No | mchSchedAnl004_closeWithoutSubmitPreservesScheduleRow |
| MCH-SCHED-ANL-005 | حوار النجاح بعد الإرسال — رسالة + الزرّان | c | Yes | mchSchedAnl005_successDialogShowsBothActions |
| MCH-SCHED-ANL-006 | «عرض حالة التحليل» → صفحة /analysis/{id} | c | Yes | mchSchedAnl006_viewStatusOpensAnalysisDetail |
| MCH-SCHED-ANL-007 | «تحليل مباراة أخرى» → إعادة فتح واجهة الإنشاء | c | Yes | mchSchedAnl007_createAnotherReopensCreateForm |
| MCH-SCHED-ANL-008 | بعد التحليل — الصف «قيد/اكتمل» وليس «غير محللة» | b | No (verify only) | mchSchedAnl008_scheduleRowShowsPostSubmitAnalysisStatus |

**Related:** MCH-SCHED-ANL-HP-001 (full E2E + listing) in same suite file.

**008 note:** use `targetExternalEventId` of a fixture **already submitted** (e.g. after 005). **005** now includes the same grid check immediately after submit.

### Defect reporting

| Trigger | Artifact |
|---------|----------|
| Step failure (listener) | `target/bug-reports/LOCAL-*.md` + Allure «Bug investigation report» |
| Schedule bind / dialog (in-test) | `docs/matchschedule/defects/SCHED-FLOW-*.md` via `ScheduleAnalysisFlowDefectReporter` |
| Video on failure | `-Dreport.video=ON_FAILURE` (TestNG listener → Allure) |

### Submit run example

```powershell
mvn test -Psuite "-DsuiteXmlFile=src/test/resources/suites/zulfi/match-schedule-analysis-flow-suite.xml" `
  "-Dmatch.schedule.analysis.phase=c" `
  "-Danalysis.create.enabled=true" `
  "-Dmatch.schedule.analysis.submit.enabled=true" `
  "-Dmatch.schedule.analysis.targetExternalEventId=<from-presubmit>" `
  "-Dreport.video=ON_FAILURE"
```
