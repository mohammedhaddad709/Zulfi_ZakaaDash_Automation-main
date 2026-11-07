package com.taf.systems.zulfi.flows.matchschedule;

import com.taf.systems.zulfi.pages.matchschedule.MatchScheduleAnalysisRowContext;
import com.taf.systems.zulfi.pages.matchschedule.ScheduleOfficialAnalysisPopupPage;

/** Result of opening the official analysis pop-up from an eligible schedule row. */
public record ScheduleAnalysisOpenResult(
        MatchScheduleAnalysisRowContext rowContext, ScheduleOfficialAnalysisPopupPage popup) {}
