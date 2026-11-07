package com.taf.systems.zulfi.pages.matchschedule;

import com.taf.systems.zulfi.pages.analysis.CreateAnalysisPage;
import org.openqa.selenium.WebDriver;

/**
 * Analysis create view opened from Match Schedule (locked official match). Product uses full-page
 * {@code /analysis/create?externalEventId=} or Home modal — both share {@link CreateAnalysisPage}.
 */
public final class ScheduleOfficialAnalysisPopupPage {

    private final WebDriver driver;
    private final CreateAnalysisPage form;

    public ScheduleOfficialAnalysisPopupPage(WebDriver driver) {
        this.driver = driver;
        this.form = new CreateAnalysisPage(driver);
    }

    public ScheduleOfficialAnalysisPopupPage waitUntilReady() {
        form.waitForScheduleEntryFromMatchSchedule();
        return this;
    }

    public CreateAnalysisPage form() {
        return form;
    }

    public boolean isOpen() {
        return form.isScheduleCreateViewOpen();
    }

    public boolean isMatchTypeOfficialLocked() {
        return form.isMatchType(CreateAnalysisPage.MATCH_TYPE_OFFICIAL)
                && form.isMatchTypeLockedToOfficial();
    }

    public boolean isOfficialMatchSelectorLocked() {
        return form.isOfficialMatchSelectorLockedForScheduleFlow();
    }

    public boolean displaysReadOnlyScheduleMetadata() {
        return form.isOfficialMatchSelected()
                && form.hasPopulatedTeamPlaceholders()
                && form.isMatchDateReadOnlyOrDisabled()
                && form.verifyTeamFieldsNonEditable().bothNonEditable();
    }

    public String readDisplayedOfficialMatchId() {
        return form.readBoundOfficialMatchIdFromForm();
    }

    public ScheduleOfficialAnalysisPopupPage closeWithoutSubmit() {
        form.closeScheduleCreateViewIfOpen();
        return this;
    }
}
