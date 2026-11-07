package com.taf.systems.zulfi.analysis.fixture;

import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;

/** Zero-based index in the complete fixture-option list (round headers excluded). */
public record IndexedFixtureEntry(int fixtureIndex, OfficialFixtureOptionSnapshot snapshot) {

    public String displayLabel() {
        return snapshot.displayLabel();
    }

    public boolean enabled() {
        return snapshot.enabled();
    }
}
