package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.analysis.AnalysisPostSubmitOutcome;
import com.taf.systems.zulfi.pages.analysis.AnalysisSuccessDialog;

/** Outcome when schedule submit stops at success dialog (no listing verification). */
public record ScheduleSubmitSuccessSnapshot(
        SchedulePreparedCreateContext prepared,
        AnalysisPostSubmitOutcome outcome,
        AnalysisSuccessDialog dialog,
        Long analysisId) {}
