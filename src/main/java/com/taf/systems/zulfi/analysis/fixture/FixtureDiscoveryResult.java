package com.taf.systems.zulfi.analysis.fixture;

import com.taf.systems.zulfi.analysis.preflight.OfficialFixtureOptionSnapshot;
import java.util.List;

public record FixtureDiscoveryResult(
        List<OfficialFixtureOptionSnapshot> orderedSnapshots,
        int scrollPassesExecuted,
        int scrollPassesWithNoNewFixtures,
        boolean endOfListLikely,
        boolean enumerationComplete) {}
