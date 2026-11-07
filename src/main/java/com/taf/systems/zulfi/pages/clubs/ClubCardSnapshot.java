package com.taf.systems.zulfi.pages.clubs;

/** Parsed Yellow League club card on the listing page. */
public record ClubCardSnapshot(
        int index,
        String clubId,
        String name,
        String href,
        boolean logoPresent,
        String rawText) {}
