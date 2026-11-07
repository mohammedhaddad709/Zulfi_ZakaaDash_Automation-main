package com.taf.systems.zulfi.flows.matchschedule;

/** Outcome of التحليلات listing checks after schedule submit. */
public record ListingVerificationResult(String listingStatus, Long analysisId) {}
