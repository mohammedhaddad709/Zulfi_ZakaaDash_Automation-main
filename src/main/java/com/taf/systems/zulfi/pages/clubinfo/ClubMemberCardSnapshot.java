package com.taf.systems.zulfi.pages.clubinfo;

/** Parsed member/player card on Club Information — US-FAP-CLUB-001. */
public record ClubMemberCardSnapshot(
        int index,
        ClubInformationPage.MemberSection section,
        String name,
        String role,
        boolean photoOrPlaceholderPresent,
        boolean usesPlaceholderHeuristic,
        boolean profileNavigationLink,
        String rawText) {}
